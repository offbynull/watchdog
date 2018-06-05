/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.watchdog.user;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

final class KillDurationListener implements BranchListener, MethodEntryListener {
    
    private static final ScheduledThreadPoolExecutor TIMER;
    
    private volatile boolean timeExceededFlag = false; // touched by both timer thread and main thread
    private boolean killProcessedFlag = false;         // touched by only main thread

    static {
        ThreadFactory threadFactory = (r) -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Watchdog timer thread");
            return t;
        };
        TIMER = new ScheduledThreadPoolExecutor(1, threadFactory);
        TIMER.setRemoveOnCancelPolicy(true);
        TIMER.setKeepAliveTime(1L, TimeUnit.SECONDS);
        TIMER.allowCoreThreadTimeOut(true);
    }
    
    static KillDurationListener create(long delay, BlockedInterrupter blockedIoInterrupter) {
        if (blockedIoInterrupter == null) {
            throw new NullPointerException();
        } 
        if (delay < 0L) {
            throw new IllegalArgumentException();
        }
        Thread thread = Thread.currentThread();
        KillDurationListener listener = new KillDurationListener();
        TIMER.schedule(() -> {
            blockedIoInterrupter.interrupt(thread);     // close any IO resources that may be blocking
            listener.timeExceededFlag = true;  // set time exceeded flag, if the code unblocks with no exception then it'll get one
        }, delay, TimeUnit.MILLISECONDS);
        
        return listener;
    }

    @Override
    public void onBranch() {
        hitCheck();
    }

    @Override
    public void onMethodEntry() {
        hitCheck();
    }

    public boolean isTimeExceeded() {
        return timeExceededFlag;
    }
    
    private void hitCheck() {
        // Has time exceeded??? If not, return
        if (!timeExceededFlag) {
            return;
        }

        // Have we already thrown a CodeInterruptedException??? If yes, return -- subsequent hits may be from cleanup regions (finally
        // blocks).
        if (killProcessedFlag) {
            return;
        }
        
        // Throw CodeInterruptedException exception
        killProcessedFlag = true;
        throw new CodeInterruptedException();
    }
}

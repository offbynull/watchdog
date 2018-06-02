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
    
    private volatile boolean hit = false;
    private final Thread thread;

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
    
    static KillDurationListener create(long delay, boolean interruptCode, boolean interruptBlocking) {
        if (delay < 0L) {
            throw new IllegalArgumentException();
        }
        Thread thread = Thread.currentThread();
        KillDurationListener listener = new KillDurationListener(thread);
        TIMER.schedule(() -> {
            if (interruptCode) {
                listener.hit = true;
            }
            if (interruptBlocking) {
                thread.interrupt();
            }
        }, delay, TimeUnit.MILLISECONDS);
        
        return listener;
    }

    private KillDurationListener(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void onBranch() {
        hitCheck();
    }

    @Override
    public void onMethodEntry() {
        hitCheck();
    }
    
    private void hitCheck() {
        if (hit) {
            throw new CodeInterruptedException();
        }
    }
}

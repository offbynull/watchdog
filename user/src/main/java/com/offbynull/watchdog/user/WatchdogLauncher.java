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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Launches your instrumented code alongside a watchdog.
 * @author Kasra Faghihi
 */
public final class WatchdogLauncher {

    private static final ScheduledThreadPoolExecutor TIMER;

    static {
        ThreadFactory threadFactory = (r) -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(WatchdogLauncher.class.getName() + " timer thread");
            return t;
        };
        TIMER = new ScheduledThreadPoolExecutor(1, threadFactory);
        TIMER.setRemoveOnCancelPolicy(true);
        TIMER.setKeepAliveTime(1L, TimeUnit.SECONDS);
        TIMER.allowCoreThreadTimeOut(true);
    }
    
    private WatchdogLauncher() {
        // do nothing
    }

    /**
     * Run a piece of instrumented code with a watchdog. {@link WatchdogException} thrown when watchdog triggers.
     * <p>
     * Equivalent to calling {@code launch(time, () -> { throw new WatchdogException(); }, logic)}.
     * @param time maximum amount of time (in milliseconds) before watchdog triggers
     * @param logic code to execute
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code time} is negative
     * @throws WatchdogException if watchdog triggers
     */
    public static void launch(long time, Consumer<Watchdog> logic) {
        launch(time, () -> { throw new WatchdogException("Triggered -- watchdog timer has elapsed"); }, logic);
    }

    /**
     * Run a piece of instrumented code with a watchdog. {@link WatchdogListener} invoked when watchdog triggers.
     * @param time maximum amount of time (in milliseconds) before watchdog triggers
     * @param listener listener to invoke when watchdog triggers
     * @param logic code to execute
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code time} is negative
     */
    public static void launch(long time, WatchdogListener listener, Consumer<Watchdog> logic) {
        if (listener == null || logic == null) {
            throw new NullPointerException();
        }
        if (time < 0L) {
            throw new IllegalArgumentException();
        }

        Watchdog watchdog = null;
        ScheduledFuture<?> future = null;
        try {
            watchdog = new Watchdog(listener);

            final Watchdog lambdaWatchdog = watchdog; // hack to stop compiler error when using watchdog directly in lambda
            future = TIMER.schedule(() -> lambdaWatchdog.hit(), time, TimeUnit.MILLISECONDS);

            logic.accept(watchdog);
        } finally {
            if (watchdog != null) {
                watchdog.shutdown();
            }
            if (future != null) {
                future.cancel(false);
            }
        }
    }
}

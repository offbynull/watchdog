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


/**
 * Launches your instrumented code alongside a watchdog.
 * @author Kasra Faghihi
 */
public final class WatchdogLauncher {
    
    private WatchdogLauncher() {
        // do nothing
    }

    /**
     * Run and watch a piece of instrumented code.
     * @param preBranchListener listener to invoke on branch instructions
     * @param postMethodEntryListener listener to invoke on method entrypoints
     * @param callable callable to execute
     * @param <V> the result type of {@code callable}
     * @return {@code callable}'s result
     * @throws NullPointerException if any argument is {@code null}
     * @throws Exception {@code callable}'s exception
     */
    public static <V> V watch(
            PreBranchListener preBranchListener,
            PostMethodEntryListener postMethodEntryListener,
            WatchdogCallable<V> callable) throws Exception {
        if (preBranchListener == null || postMethodEntryListener == null || callable == null) {
            throw new NullPointerException();
        }

        Watchdog watchdog = null;
        try {
            watchdog = new Watchdog(preBranchListener, postMethodEntryListener);
            return callable.call(watchdog);
        } finally {
            if (watchdog != null) {
                watchdog.shutdown();
            }
        }
    }
    
    /**
     * Run and watch a piece of instrumented code such that it finishes within the specified duration. If the duration passes while the
     * callable is...
     * <ul>
     * <li>waiting (e.g. blocked on IO, thread sync, etc..), the thread's {@link Thread#interrupt() } will be invoked.</li>
     * <li>running (e.g. in a hard loop), the thread will throw {@link CodeInterruptedException} (once in an instrumented method).</li>
     * </ul>
     * Equivalent to calling {@code watch(delay, true, true, callable)}.
     * @param delay maximum amount of time (in milliseconds) to wait before watchdog triggers
     * @param callable callable to execute
     * @param <V> the result type of {@code callable}
     * @return callable result
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code delay} is negative
     * @throws Exception {@code callable}'s exception
     */
    public static <V> V watch(long delay, WatchdogCallable<V> callable) throws Exception {
        return watch(delay, true, true, callable);
    }

    /**
     * Run and watch a piece of instrumented code such that it finishes within the specified duration. If the duration passes while the
     * callable is, the action depends on the {@code interruptCode} and {@code interruptBlocking} parameters.
     * @param delay maximum amount of time (in milliseconds) to wait before watchdog triggers
     * @param interruptCode if {@code true}, the thread's {@link Thread#interrupt() } will be invoked once the specified duration elapses
     * @param interruptBlocking if {@code true}, the thread will throw {@link CodeInterruptedException} (once in an instrumented method)
     * once the specified duration elapses
     * @param callable callable to execute
     * @param <V> the result type of {@code callable}
     * @return callable result
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code delay} is negative
     * @throws Exception {@code callable}'s exception
     */
    public static <V> V watch(long delay, boolean interruptCode, boolean interruptBlocking, WatchdogCallable<V> callable) throws Exception {
        KillDurationListener timeLimitListener = KillDurationListener.create(delay, interruptCode, interruptBlocking);
        return watch(timeLimitListener, timeLimitListener, callable);
    }
}

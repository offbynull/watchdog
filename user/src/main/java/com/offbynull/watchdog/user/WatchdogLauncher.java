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
     * Run and monitor instrumented code.
     * @param branchListener listener to invoke on branch instructions
     * @param instantiateListener listener to invoke on object instantiation instructions
     * @param methodEntryListener listener to invoke on method entrypoints
     * @param callable callable to execute
     * @param <V> the result type of {@code callable}
     * @return {@code callable}'s result
     * @throws NullPointerException if any argument is {@code null}
     * @throws Exception {@code callable}'s exception
     */
    public static <V> V monitor(
            BranchListener branchListener,
            InstantiateListener instantiateListener,
            MethodEntryListener methodEntryListener,
            WatchdogCallable<V> callable) throws Exception {
        if (branchListener == null || instantiateListener == null || methodEntryListener == null || callable == null) {
            throw new NullPointerException();
        }

        Watchdog watchdog = null;
        try {
            watchdog = new Watchdog(branchListener, instantiateListener, methodEntryListener);
            return callable.call(watchdog);
        } finally {
            if (watchdog != null) {
                watchdog.shutdown();
            }
        }
    }
    
    /**
     * Run and monitor instrumented code such that it finishes within the specified duration. If the duration passes while the callable
     * is...
     * <ul>
     * <li>waiting (e.g. blocked on IO, thread sync, etc..), the thread's {@link Thread#interrupt() } will be invoked.</li>
     * <li>running (e.g. in a hard loop), the thread will throw {@link CodeInterruptedException} (once in an instrumented method).</li>
     * </ul>
     * Equivalent to calling {@code monitor(delay, true, true, callable)}.
     * @param delay maximum amount of time (in milliseconds) to wait before watchdog triggers
     * @param runnable runnable to execute
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code delay} is negative
     * @throws RuntimeException {@code runnable}'s exception
     */
    public static void watch(long delay, WatchdogRunnable runnable) {
        if (runnable == null) {
            throw new NullPointerException();
        }

        try {
            WatchdogCallable<Object> callable = (Watchdog wd) -> {
                runnable.run(wd);
                return null;
            };
            watch(delay, callable);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(); // should never happen
        }
    }

    /**
     * Run and monitor instrumented code such that it finishes within the specified duration. If the duration passes while the callable
     * is...
     * <ul>
     * <li>waiting (e.g. blocked on IO, thread sync, etc..), the thread's {@link Thread#interrupt() } will be invoked.</li>
     * <li>running (e.g. in a hard loop), the thread will throw {@link CodeInterruptedException} (once in an instrumented method).</li>
     * </ul>
     * Equivalent to calling {@code watch(delay, callable, (t) -> t.interrupt())}.
     * @param delay maximum amount of time (in milliseconds) to wait before watchdog triggers
     * @param callable callable to execute
     * @param <V> the result type of {@code callable}
     * @return callable result
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code delay} is negative
     * @throws Exception {@code callable}'s exception
     */
    public static <V> V watch(long delay, WatchdogCallable<V> callable) throws Exception {
        return watch(delay, callable, (t) -> t.interrupt());
    }

    /**
     * Run and monitor instrumented code such that it finishes within the specified duration. If the duration passes while the callable
     * is...
     * <ul>
     * <li>waiting (e.g. blocked on IO, thread sync, etc..), {@code blockedInterrupter.interrupt(...)} will be invoked</li>
     * <li>running (e.g. in a hard loop), the thread will throw an exception (once in an instrumented method)</li>
     * </ul>
     * ... and this method will throw a {@link WatchdogTimeoutException}.
     * @param delay maximum amount of time (in milliseconds) to wait before watchdog triggers
     * @param callable callable to execute
     * @param blockedInterrupter I/O unblocker (code to execute to break out of blocking I/O -- executed in separate thread)
     * @param <V> the result type of {@code callable}
     * @return callable result
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code delay} is negative
     * @throws WatchdogTimeoutException delay elapsed while code was still running
     * @throws Exception {@code callable}'s exception
     */
    public static <V> V watch(long delay, WatchdogCallable<V> callable, BlockedInterrupter blockedInterrupter) throws Exception {
        if (delay < 0L) {
            throw new IllegalArgumentException();
        }
        if (callable == null || blockedInterrupter == null) {
            throw new NullPointerException();
        } 
        KillDurationListener timeLimitListener = KillDurationListener.create(delay, blockedInterrupter);
        InstantiateListener instantiateListener = (obj) -> { };
        
        try {
            V ret = monitor(timeLimitListener, instantiateListener, timeLimitListener, callable);
            if (timeLimitListener.isTimeExceeded()) {
                throw new WatchdogTimeoutException();
            }
            return ret;
        } catch (Exception e) {
            if (timeLimitListener.isTimeExceeded()) {
                throw new WatchdogTimeoutException(e);
            }
            throw e;
        }
    }
}

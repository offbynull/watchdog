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
 * Watchdog launcher.
 * @author Kasra Faghihi
 */
public final class WatchdogLauncher {
    
    private WatchdogLauncher() {
        // do nothing
    }
    
    /**
     * Run and watch instrumented code such that it finishes within the specified duration.
     * @param delay maximum amount of time (in milliseconds) to wait before watchdog triggers
     * @param runnable runnable to execute
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code delay} is negative
     * @throws IllegalStateException if this method was invoked from code already being watched
     * @throws WatchdogTimeoutException delay elapsed while code was still running
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
     * Run and watch instrumented code such that it finishes within the specified duration.
     * @param delay maximum amount of time (in milliseconds) to wait before watchdog triggers
     * @param callable callable to execute
     * @param <V> the result type of {@code callable}
     * @return callable result
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code delay} is negative
     * @throws IllegalStateException if this method was invoked from code already being watched
     * @throws WatchdogTimeoutException delay elapsed while code was still running
     * @throws Exception {@code callable}'s exception
     */
    public static <V> V watch(long delay, WatchdogCallable<V> callable) throws Exception {
        if (delay < 0L) {
            throw new IllegalArgumentException();
        }
        if (callable == null) {
            throw new NullPointerException();
        }
        
        Watchdog watchdog = null;
        try {
            watchdog = Watchdog.create(delay);
            
            V ret = callable.call(watchdog);
            if (watchdog.isTimeExceeded()) {
                throw new WatchdogTimeoutException();
            }
            
            return ret;
        } catch (Exception e) {
            if (watchdog != null && watchdog.isTimeExceeded()) {
                throw new WatchdogTimeoutException(e);
            }
            throw e;
        } finally {
            if (watchdog != null) {
                watchdog.shutdown();
            }
        }
    }
}

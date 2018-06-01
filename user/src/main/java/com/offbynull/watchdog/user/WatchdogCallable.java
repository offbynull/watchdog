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

import java.util.concurrent.Callable;

/**
 * A watchdog'd task that returns a result and may throw an exception. Similar to a {@link Callable}, but also passes in a {@link Watchdog}
 * argument.
 * @param <V> the result type of method {@link #call(com.offbynull.watchdog.user.Watchdog) }
 * @author Kasra Faghihi
 */
public interface WatchdogCallable<V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     * @param watchdog watchdog
     * @return computed result
     * @throws NullPointerException if any argument is {@code null}
     * @throws Exception if unable to compute a result
     */
    V call(Watchdog watchdog) throws Exception;
}

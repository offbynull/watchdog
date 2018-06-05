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
 * Blocked thread interrupter.
 * @author Kasra Faghihi
 */
public interface BlockedInterrupter {

    /**
     * Takes action to unblock a thread. Typically releases resources (e.g. database connections, sockets, files, etc..) such that the main
     * thread, if it were blocking on one of those resources, can continue executing.
     * @param thread blocked thread
     * @throws NullPointerException if thread is {@code null}
     */
    void interrupt(Thread thread);
}

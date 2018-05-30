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
 * Watchdog.
 * @author Kasra Faghihi
 */
public final class Watchdog {
    private static final ThreadLocal<Watchdog> TLS = ThreadLocal.withInitial(() -> null);
    
    /**
     * Watchdog placeholder. If you don't have a {@link Watchdog} object available for passing down the invocation chain, you can use this
     * placeholder instead.
     */
    public static final Watchdog PLACEHOLDER = new Watchdog(null); // Don't set this to null, because we want this field to be
                                                                   // actually referenced by the bytecode in other classes. If we
                                                                   // set this to null, the compiler will try to optimize by
                                                                   // loading NULL directly onto the operand stack instead of
                                                                   // actually loading the field.

    private volatile boolean triggered = false;     // Indicates if the watchdog hit / maximum time has elapsed
    private final WatchdogListener listener;        // Listener to invoke when the timer hits
    
    private Watchdog() {
        listener = null;
    }
    
    Watchdog(WatchdogListener listener) {
        this.listener = listener;
        TLS.set(this);
    }
    
    /**
     * Do not use -- for internal use only.
     */
    public void check() { // Check to see if watchdog flag was triggered
        if (triggered) {
            listener.triggered();
        }
    }
    
    /**
     * Do not use -- for internal use only.
     * @return n/a
     * @throws IllegalStateException n/a
     */
    public static Watchdog get() { // Get the watchdog instance from threadlocal storage (set in obj constructor)
        Watchdog ret = TLS.get();
        if (ret == null) {
            TLS.remove(); // you need to remove at this point cause memory has been allocated by initial call to threadLocal.get()
            throw new IllegalStateException("Bad state -- watchdog does not exist in TLS");
        }
        return ret;
    }
    
    // Maximum time has been triggered -- the next call to check should cause an exception
    void hit() {
        triggered = true;
        listener.triggered();
    }
    
    // This object is finished with and must not be used again after this is invoked
    void shutdown() {
        TLS.remove();
    }
}

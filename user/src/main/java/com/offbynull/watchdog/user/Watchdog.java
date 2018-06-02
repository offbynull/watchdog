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
    public static final Watchdog PLACEHOLDER = new Watchdog();           // Don't set this to null, because we want this field to be
                                                                         // actually referenced by the bytecode in other classes. If we
                                                                         // set this to null, the compiler will try to optimize by
                                                                         // loading NULL directly onto the operand stack instead of
                                                                         // actually loading the field.

    private final BranchListener branchListener;
    private final InstantiateListener instantiateListener;
    private final MethodEntryListener methodEntryListener;
    
    private Watchdog() {
        branchListener = null;
        instantiateListener = null;
        methodEntryListener = null;
    }
    
    Watchdog(BranchListener branchListener, InstantiateListener instantiateListener, MethodEntryListener methodEntryListener) {
        if (branchListener == null || instantiateListener == null || methodEntryListener == null) {
            throw new NullPointerException();
        }

        this.branchListener = branchListener;
        this.instantiateListener = instantiateListener;
        this.methodEntryListener = methodEntryListener;
        TLS.set(this);
    }


    /**
     * Do not use -- for internal use only.
     */
    public void onBranch() {
        branchListener.onBranch();
    }


    /**
     * Do not use -- for internal use only.
     * @param obj n/a
     */
    public void onInstantiate(Object obj) {
        instantiateListener.onInstantiate(obj);
    }

    /**
     * Do not use -- for internal use only.
     */
    public void onMethodEntry() {
        methodEntryListener.onMethodEntry();
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
    
    // This object is finished with and must not be used again after this is invoked
    void shutdown() {
        TLS.remove();
    }
}

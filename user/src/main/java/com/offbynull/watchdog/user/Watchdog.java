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

import java.io.Closeable;
import java.util.ArrayList;
import static java.util.Collections.synchronizedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


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
    public static final Watchdog PLACEHOLDER = new Watchdog();  // Don't set this to null, because we want this field to be actually
                                                                // referenced by the bytecode in other classes. If we set this to null, the
                                                                // compiler may try to optimize by loading NULL directly onto the operand
                                                                // stack instead of actually loading the field?

    // Internal timer for unblocking IO
    private static final ScheduledThreadPoolExecutor TIMER;
    static {
        ThreadFactory threadFactory = (r) -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(Watchdog.class.getSimpleName() + " timer thread");
            return t;
        };
        TIMER = new ScheduledThreadPoolExecutor(1, threadFactory);
        //TIMER.setRemoveOnCancelPolicy(true);
        TIMER.setKeepAliveTime(1L, TimeUnit.SECONDS);
        TIMER.allowCoreThreadTimeOut(true);
    }
    
    // Class fields
    private volatile boolean timeExceededFlag = false;          // touched by both timer thread and main thread
    private int uninterruptibleSectionCounter = 0;              // touched by only main thread
    private boolean killProcessedFlag = false;                  // touched by only main thread

    private final List<BlockedInterrupter> blockedInterrupters; // touched by both timer thread and main thread (sync implementation used)
    
    static Watchdog create(long delay) {
        if (delay < 0L) {
            throw new IllegalArgumentException();
        }

        if (TLS.get() != null) {
            throw new IllegalStateException("Watchdog already active");
        }

        Thread thread = Thread.currentThread();
        Watchdog watchdog = new Watchdog();
        TIMER.schedule(() -> {
            watchdog.timeExceededFlag = true;
            synchronized (watchdog.blockedInterrupters) {
                for (BlockedInterrupter blockedInterrupter : watchdog.blockedInterrupters) {
                    try {
                        blockedInterrupter.interrupt(thread);
                    } catch (Exception e) {
                        // can't do anything here -- swallow exception so we can keep processing
                    }
                }
            }
        }, delay, TimeUnit.MILLISECONDS);

        TLS.set(watchdog);
        return watchdog;
    }

    private Watchdog() {
        this.blockedInterrupters = synchronizedList(new ArrayList<>());
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

    


    /**
     * Do not use -- for internal use only.
     */
    public void onBranch() {
        hitCheck();
    }

    /**
     * Do not use -- for internal use only.
     */
    public void onMethodEntry() {
        hitCheck();
    }
    
    /**
     * Enter uninterruptible section.
     * <p>
     * Watched methods enter uninterruptible sections in portions of code that should not be interrupted if the watchdog timer elapses. For
     * example, catch/finally blocks that perform some type of clean up typically should not be interrupted.
     * <p>
     * Example usage...
     * <code>
     * watchdog.enterUninterruptibleSection();
     * try {
     *     for (Resource res : resources) {
     *         res.shutdown();
     *     }
     * } finally {
     *     watchdog.exitUninterruptibleSection();
     * }
     * </code>
     * As seen in the example above, an invocation of this method must have a corresponding invocation of
     * {@link #exitUninterruptibleSection() }.
     */
    public void enterUninterruptibleSection() {
        uninterruptibleSectionCounter++;
    }
    
    /**
     * Exit uninterruptible section.
     * @throws IllegalStateException if not in a uninterruptible section
     */
    public void exitUninterruptibleSection() {
        if (uninterruptibleSectionCounter == 0) {
            throw new IllegalStateException();
        }
        uninterruptibleSectionCounter--;
        hitCheck();
    }

    /**
     * Execute the supplied runnable in a uninterruptible section. Equivalent to...
     * <code>
     * enterUninterruptibleSection();
     * try {
     *     runnable.run();
     * } finally {
     *     exitUninterruptibleSection();
     * }
     * </code>
     * @param runnable runnable to execute
     * @throws NullPointerException if any argument is {@code null}
     */
    public void wrapUninterruptibleSection(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        
        enterUninterruptibleSection();
        try {
            runnable.run();
        } finally {
            exitUninterruptibleSection();
        }
    }
    
    boolean isTimeExceeded() {
        return timeExceededFlag;
    }

    private void hitCheck() {
        // Has time exceeded??? If not, return
        if (!timeExceededFlag) {
            return;
        }

        // Has "killing" been disabled by the user? This is typically done during portions of code that should not be interrupted -- for
        // example, cleaning up in finally blocks.
        if (uninterruptibleSectionCounter > 0) {
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
    
    
    
    /**
     * Add blocked interrupter.
     * <p>
     * Blocked interrupters are invoked once the watchdog times out. Each blocked interrupter typically releases one or more resources (e.g.
     * streams, sockets, files, database connections, etc..), such that the main thread, if it were blocking on those resources, can
     * continue executing.
     * <p>
     * Example usage...
     * <code>
     * try (FileInputStream fis = new FileInputStream("in.txt")) {
     *     watchdog.watchBlocking(t -&gt; fis.close());
     * 
     *     String fileData = IOUtils.toString(fis);
     *     System.out.println(fileData);
     * }
     * </code>
     * If watching a large number of resources, you may choose to have a corresponding invocation of
     * {@link #unwatchBlocking(com.offbynull.watchdog.user.BlockedInterrupter)  } for each invocation of this method.
     * @param blockedInterrupter blocked interrupter
     * @throws NullPointerException if any argument is {@code null}
     */
    public void watchBlocking(BlockedInterrupter blockedInterrupter) {
        if (blockedInterrupter == null) {
            throw new NullPointerException();
        }
        
        blockedInterrupters.add(blockedInterrupter);
    }

    /**
     * Remove blocked interrupter.
     * @param blockedInterrupter blocked interrupter
     * @throws NullPointerException if any argument is {@code null}
     */
    public void unwatchBlocking(BlockedInterrupter blockedInterrupter) {
        if (blockedInterrupter == null) {
            throw new NullPointerException();
        }
        
        blockedInterrupters.remove(blockedInterrupter);
    }
    
    /**
     * Wrap a {@link Closeable} such that it automatically gets watched and unwatched in try-with-resources blocks.
     * <p>
     * Typical usage pattern for this method is to interlace calls to it with the resources you're acquiring in your try-with-resource
     * block. For example, imagine you have the following try-with-resource block...
     * <code>
     * try (FileInputStream fis = new FileInputStream("in.txt");
     *      FileOutputStream fos = new fileOutputStream("out.txt)) {
     *     IOUtils.copy(fis, fos);
     * }
     * </code>
     * To watch the resources being acquired, you'd add a call to this method immediately after acquiring each resource...
     * <code>
     * try (FileInputStream fis = new FileInputStream("in.txt");
     *      Closeable cfis = watchdog.wrapBlocking(fis);
     *      FileOutputStream fos = new FileOutputStream("out.txt");
     *      Closeable cfos = watchdog.wrapBlocking(fos)) {
     *     IOUtils.copy(fis, fos);
     * }
     * </code>
     * @param closeable closeable to wrap
     * @return wrapped closeable
     */
    public Closeable wrapBlocking(Closeable closeable) {
        BlockedInterrupter blockedInterrupter = (t) -> closeable.close();
        watchBlocking(blockedInterrupter);

        Closeable wrappedCloseable = () -> {
            unwatchBlocking(blockedInterrupter);
            closeable.close();
        };
        
        return wrappedCloseable;
    }
    
    
    
    
    // This object is finished with and must not be used again after this is invoked
    void shutdown() {
        TLS.remove();
    }
}

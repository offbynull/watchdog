/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.watchdog.instrumenter;

import static com.offbynull.watchdog.instrumenter.testhelpers.TestUtils.loadClassesInZipResourceAndInstrument;
import com.offbynull.watchdog.user.WatchdogException;
import com.offbynull.watchdog.user.WatchdogLauncher;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import static org.apache.commons.lang3.reflect.ConstructorUtils.invokeConstructor;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeExactStaticMethod;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class InstrumentationTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void mustInstrumentBranches() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("TightLoopTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("TightLoopTest");
            
            expectedException.expect(WatchdogException.class);
            WatchdogLauncher.launch(0L, (wd) -> {
                createObject(cls, wd);
            });
        }
    }

    @Test
    public void mustInstrumentLookupSwitch() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("LookupSwitchTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("LookupSwitchTest");
            
            expectedException.expect(WatchdogException.class);
            WatchdogLauncher.launch(0L, (wd) -> {
                createObject(cls, wd);
            });
        }
    }

    @Test
    public void mustInstrumentTableSwitch() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("TableSwitchTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("TableSwitchTest");
            
            expectedException.expect(WatchdogException.class);
            WatchdogLauncher.launch(0L, (wd) -> {
                createObject(cls, wd);
            });
        }
    }

    @Test
    public void mustInstrumentRecursive() throws Exception { // entry point of methods must get a check -- this is what this test does
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("RecursiveTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("RecursiveTest");
            
            expectedException.expect(WatchdogException.class);
            WatchdogLauncher.launch(0L, (wd) -> {
                createObject(cls, wd);
            });
        }
    }

    @Test
    public void mustInstrumentProperlyOnBothWatchdogArgAndWatchAnnotation() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("DoubleInstrumentationTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("DoubleInstrumentationTest");
            
            expectedException.expect(WatchdogException.class);
            WatchdogLauncher.launch(0L, (wd) -> {
                invokeStaticMethod(cls, "test", wd);
            });
        }
    }
    
    private <T> T createObject(Class<T> cls, Object... args) {
        try {
            return (T) invokeConstructor(cls, args);
        } catch (InvocationTargetException ite) {
            throw (RuntimeException) ite.getTargetException();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void invokeStaticMethod(Class<?> cls, String name, Object... args) {
        try {
            invokeExactStaticMethod(cls, name, args);
        } catch (InvocationTargetException ite) {
            throw (RuntimeException) ite.getTargetException();
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
}

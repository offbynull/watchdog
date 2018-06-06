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

import com.offbynull.watchdog.instrumenter.generators.DebugGenerators.MarkerType;
import com.offbynull.watchdog.instrumenter.testhelpers.TestUtils;
import com.offbynull.watchdog.instrumenter.testhelpers.TestUtils.JarEntry;
import static com.offbynull.watchdog.instrumenter.testhelpers.TestUtils.createJar;
import static com.offbynull.watchdog.instrumenter.testhelpers.TestUtils.getClasspath;
import static com.offbynull.watchdog.instrumenter.testhelpers.TestUtils.loadClassesInZipResourceAndInstrument;
import static com.offbynull.watchdog.instrumenter.testhelpers.TestUtils.readZipFromResource;
import com.offbynull.watchdog.user.WatchdogTimeoutException;
import com.offbynull.watchdog.user.WatchdogLauncher;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import static org.apache.commons.lang3.reflect.ConstructorUtils.invokeConstructor;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeExactStaticMethod;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public final class InstrumentationTest {

    @Test
    public void mustInstrumentBranchesViaArgument() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("TightLoopTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("TightLoopTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(100L,
                        wd -> {
                            createObject(cls, wd);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentBranchesViaAnnotation() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("TightLoopAnnotationTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("TightLoopAnnotationTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(100L,
                        wd -> {
                            createObject(cls);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentLookupSwitchViaArgument() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("LookupSwitchTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("LookupSwitchTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(100L,
                        wd -> {
                            createObject(cls, wd);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentLookupSwitchViaAnnotation() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("LookupSwitchAnnotationTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("LookupSwitchAnnotationTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(100L,
                        wd -> {
                            createObject(cls);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentTableSwitchViaArgument() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("TableSwitchTest.zip", new InstrumentationSettings(MarkerType.NONE))) {
            Class<?> cls = (Class<?>) classLoader.loadClass("TableSwitchTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(100L,
                        wd -> {
                            createObject(cls, wd);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentTableSwitchViaAnnotation() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("TableSwitchAnnotationTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("TableSwitchAnnotationTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(100L,
                        wd -> {
                            createObject(cls);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentRecursiveViaArgument() throws Exception { // entrypoint of methods must get a check - this is what test does
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("RecursiveTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("RecursiveTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(10L,
                        wd -> {
                            createObject(cls, wd);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentRecursiveViaAnnotation() throws Exception { // entrypoint of methods must get a check - this is what test does
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("RecursiveAnnotationTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("RecursiveAnnotationTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(10L,
                        wd -> {
                            createObject(cls);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentRecursiveViaClassAnnotation() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("RecursiveClassAnnotationTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("RecursiveClassAnnotationTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(10L,
                        wd -> {
                            createObject(cls);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentRecursiveViaMixOfAnnotationAndArgument() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("RecursiveMixTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("RecursiveMixTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(10L,
                        wd -> {
                            createObject(cls);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustInstrumentRecrusiveProperlyOnBothWatchdogArgAndWatchAnnotation() throws Exception {
        try (URLClassLoader classLoader = loadClassesInZipResourceAndInstrument("RecursiveMixSameMethodTest.zip")) {
            Class<?> cls = (Class<?>) classLoader.loadClass("RecursiveMixSameMethodTest");
            
            assertThrows(WatchdogTimeoutException.class, () -> {
                WatchdogLauncher.watch(0L,
                        wd -> {
                            createObject(cls);
                            return null;
                        },
                        t -> { }
                );
            });
        }
    }

    @Test
    public void mustTimeoutProperlyOnBlockedThread() throws Exception {            
        assertThrows(WatchdogTimeoutException.class, () -> {
            WatchdogLauncher.watch(0L,
                    wd -> {
                        Thread.sleep(Long.MAX_VALUE); // sleep forever
                        return null;
                    },
                    t -> {
                        t.interrupt();
                    }
            );
        });
    }
    
    @Test
    public void mustNotDoubleInstrument() throws Exception {
        // Load class
        byte[] classContent = readZipFromResource("LookupSwitchTest.zip").get("LookupSwitchTest.class");
        
        // Create JAR out of class so it can be found by the instrumenter
        List<TestUtils.JarEntry> originalJarEntries = new ArrayList<>();
        originalJarEntries.add(new JarEntry("LookupSwitchTest.class", classContent));
        File originalJarFile = createJar(originalJarEntries.toArray(new JarEntry[0]));
        
        // Get current classpath (includes core JVM classes) and add to it the newly created JAR
        List<File> classpath = getClasspath();
        classpath.addAll(classpath);
        classpath.add(originalJarFile);

        // Create the instrumenter and try to double instrument
        Instrumenter instrumenter = new Instrumenter(classpath);
        InstrumentationSettings settings = new InstrumentationSettings(MarkerType.CONSTANT);
        
        byte[] classInstrumented1stPass = instrumenter.instrument(classContent, settings).getInstrumentedClass();
        byte[] classInstrumented2stPass = instrumenter.instrument(classInstrumented1stPass, settings).getInstrumentedClass();
        
        assertArrayEquals(classInstrumented1stPass, classInstrumented2stPass);
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

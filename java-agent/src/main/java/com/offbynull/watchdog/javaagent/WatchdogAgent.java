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
package com.offbynull.watchdog.javaagent;

import com.offbynull.watchdog.instrumenter.InstrumentationResult;
import com.offbynull.watchdog.instrumenter.InstrumentationSettings;
import com.offbynull.watchdog.instrumenter.Instrumenter;
import com.offbynull.watchdog.instrumenter.asm.ClassResourceClassInformationRepository;
import com.offbynull.watchdog.instrumenter.generators.DebugGenerators.MarkerType;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

/**
 * Java Agent that instruments watchdog.
 * @author Kasra Faghihi
 */
public final class WatchdogAgent {

    private WatchdogAgent() {
        // do nothing
    }
    
    /**
     * Java agent premain.
     * @param agentArgs args passed in to agent
     * @param inst instrumentation for agent
     * @throws NullPointerException if {@code inst} is {@code null}
     * @throws IllegalArgumentException if {@code agentArgs} is present but not in the expected format, or if the passed in arguments were
     * not parseable
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        // How do agent args work? http://stackoverflow.com/questions/23287228/how-do-i-pass-arguments-to-a-java-instrumentation-agent
        // e.g. java -javaagent:/path/to/agent.jar=argumentstring
        
        MarkerType markerType = MarkerType.NONE;
        if (agentArgs != null && !agentArgs.isEmpty()) {
            String[] splitArgs = agentArgs.split(",");
            for (String splitArg : splitArgs) {
                String[] keyVal = splitArg.split("=", 2);
                if (keyVal.length != 2) {
                    throw new IllegalArgumentException("Unrecognized arg passed to Watchdog Java agent: " + splitArg);
                }
                
                String key = keyVal[0];
                String val = keyVal[1];

                switch (key) {
                    case "markerType":
                        try {
                            markerType = MarkerType.valueOf(val);
                        } catch (IllegalArgumentException iae) {
                            throw new IllegalArgumentException("Unable to parse marker type -- must be one of the following: "
                                    + Arrays.toString(MarkerType.values()), iae);
                        }
                        break;                      
                    default:
                        throw new IllegalArgumentException("Unrecognized arg passed to Watchdog Java agent: " + keyVal);
                }
            }
        }
        
        inst.addTransformer(new WatchdogClassFileTransformer(markerType));
    }
    
    private static final class WatchdogClassFileTransformer implements ClassFileTransformer {
        private final MarkerType markerType;

        WatchdogClassFileTransformer(MarkerType markerType) {
            if (markerType == null) {
                throw new NullPointerException();
            }

            this.markerType = markerType;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                byte[] classfileBuffer) throws IllegalClassFormatException {
//            ClassReader cr = new ClassReader(classfileBuffer);
//            ClassNode classNode = new SimpleClassNode();
//            cr.accept(classNode, 0);
//            String classNameFromBytes = classNode.name;
            
            // If class is internal to the watchdog user project, don't instrument them
            //   FYI: If the class being transformed is a lambda, className will show up as null.
            if (className == null || className.startsWith("com/offbynull/watchdog/user/")) {
                return null;
            }
            
            // If loader is null, don't attempt instrumentation (this is a core class?)
            if (loader == null) {
                return null;
            }
            
//            System.out.println(className + " " + (loader == null));
            
            try {
                InstrumentationSettings settings = new InstrumentationSettings(markerType);
                Instrumenter instrumenter = new Instrumenter(new ClassResourceClassInformationRepository(loader));
                InstrumentationResult result = instrumenter.instrument(classfileBuffer, settings);
                return result.getInstrumentedClass();
            } catch (Throwable e) {
                System.err.println("FAILED TO INSTRUMENT: " + e);
                return null;
            }
        }
        
    }
}

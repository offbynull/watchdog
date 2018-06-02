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
package com.offbynull.watchdog.instrumenter;

import com.offbynull.watchdog.instrumenter.asm.VariableTable.Variable;
import com.offbynull.watchdog.instrumenter.generators.DebugGenerators.MarkerType;
import static com.offbynull.watchdog.instrumenter.generators.DebugGenerators.debugMarker;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.call;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.ifObjectsEqual;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.loadStaticField;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.loadVar;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.merge;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.saveVar;
import com.offbynull.watchdog.user.Watchdog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

final class EntryPointInstrumentationPass implements InstrumentationPass {
    
    private static final Method GET_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "get");
    private static final Method ON_METHOD_ENTRY_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "onMethodEntry");
    private static final Field PLACEHOLDER_FIELD = FieldUtils.getDeclaredField(Watchdog.class, "PLACEHOLDER");

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        for (Entry<MethodNode, MethodProperties> entry : state.identifiedMethods().entrySet()) {
            MethodNode methodNode = entry.getKey();
            MethodProperties methodProperties = entry.getValue();
            
            boolean argMode = methodProperties.argMode();
            Variable watchdogVar = methodProperties.watchdogVariable();
            
            MarkerType markerType = state.instrumentationSettings().getMarkerType();
            InsnList preambleInsnList;
            if (argMode) {
                preambleInsnList =
                        merge(
                                debugMarker(markerType, "Checking if watchdog placeholder supplied"),
                                ifObjectsEqual(loadVar(watchdogVar), loadStaticField(PLACEHOLDER_FIELD),
                                        merge(
                                                debugMarker(markerType, "Watchdog placeholder supplied -- get wacthdog from TLS"),
                                                call(GET_METHOD),
                                                saveVar(watchdogVar)
                                        )
                                ),
                                debugMarker(markerType, "Invoking watchdog method entry tracker"),
                                call(ON_METHOD_ENTRY_METHOD, loadVar(watchdogVar))
                        );                
            } else {
                preambleInsnList =
                        merge(
                                debugMarker(markerType, "Get watchdog from TLS"),
                                call(GET_METHOD),
                                saveVar(watchdogVar),
                                debugMarker(markerType, "Invoking watchdog method entry tracker"),
                                call(ON_METHOD_ENTRY_METHOD, loadVar(watchdogVar))
                        );
            }
            
            InsnList insnList = methodNode.instructions;
            insnList.insert(preambleInsnList);
        }
    }
}

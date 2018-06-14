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

import com.offbynull.watchdog.instrumenter.LoopAnalyzer.Loop;
import com.offbynull.watchdog.instrumenter.asm.VariableTable.Variable;
import com.offbynull.watchdog.instrumenter.generators.DebugGenerators.MarkerType;
import static com.offbynull.watchdog.instrumenter.generators.DebugGenerators.debugMarker;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.call;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.loadVar;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.merge;
import com.offbynull.watchdog.user.Watchdog;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import static com.offbynull.watchdog.instrumenter.LoopAnalyzer.findLoops;

final class LoopPointInstrumentationPass implements InstrumentationPass {
    
    private static final Method ON_BRANCH_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "onBranch");

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        for (Entry<MethodNode, MethodProperties> entry : state.identifiedMethods().entrySet()) {
            MethodNode methodNode = entry.getKey();
            MethodProperties methodProperties = entry.getValue();

            Variable watchdogVar = methodProperties.watchdogVariable();
            
            MarkerType markerType = state.instrumentationSettings().getMarkerType();
            InsnList insnList = methodNode.instructions;
            
            Set<Loop> loops = findLoops(methodNode.instructions, methodNode.tryCatchBlocks);
            
            // Call the watchdog
            loops.stream()
                    .map(x -> x.getFromInsnNode())
                    .forEach(insnNode -> {
                        InsnList trackInsnList = merge(
                            debugMarker(markerType, "Invoking watchdog branch tracker"),
                            call(ON_BRANCH_METHOD, loadVar(watchdogVar))
                        );

                        insnList.insertBefore(insnNode, trackInsnList);
                    });
        }
    }
}

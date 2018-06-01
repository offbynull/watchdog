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
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.loadVar;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.merge;
import com.offbynull.watchdog.user.Watchdog;
import java.lang.reflect.Method;
import java.util.Map;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

final class BranchPointInstrumentationPass implements InstrumentationPass {
    
    private static final Method PRE_BRANCH_INSTRUCTION_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "preBranchInstruction");

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        for (Map.Entry<MethodNode, MethodProperties> entry : state.identifiedMethods().entrySet()) {
            MethodNode methodNode = entry.getKey();
            MethodProperties methodProperties = entry.getValue();

            Variable watchdogVar = methodProperties.getWatchdogVariable();
            
            MarkerType markerType = state.instrumentationSettings().getMarkerType();
            InsnList insnList = methodNode.instructions;
            
            // Call the watchdog
            AbstractInsnNode insnNode = insnList.getFirst();
            while (insnNode != null) {
                // On branch, invoke WatchDog.check()
                if (insnNode instanceof JumpInsnNode
                        || insnNode instanceof LookupSwitchInsnNode
                        || insnNode instanceof TableSwitchInsnNode) {
                    InsnList trackInsnList = merge(
                            debugMarker(markerType, "Checking watchdog"),
                            call(PRE_BRANCH_INSTRUCTION_METHOD, loadVar(watchdogVar))
                    );
                    insnList.insertBefore(insnNode, trackInsnList);
                }
                
                // Move to next instruction
                insnNode = insnNode.getNext();
            }
        }
    }
}

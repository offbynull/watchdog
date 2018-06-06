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
    
    private static final Method ON_BRANCH_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "onBranch");

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        for (Map.Entry<MethodNode, MethodProperties> entry : state.identifiedMethods().entrySet()) {
            MethodNode methodNode = entry.getKey();
            MethodProperties methodProperties = entry.getValue();

            Variable watchdogVar = methodProperties.watchdogVariable();
            
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
                            debugMarker(markerType, "Invoking watchdog branch tracker"),
                            call(ON_BRANCH_METHOD, loadVar(watchdogVar))
                    );

                    insnList.insertBefore(insnNode, trackInsnList);
                }
                
                // IMPORTANT NOTE: We're applying checks BEFORE a branching operation. As such, branches resulting from a catch being
                // triggered are not supported. But, it doesn't matter anyways -- going into a catch is normally a one-time thing. If you
                // loop inside the catch or loop back out of the catch, it'll count a loop operation.
                //
                // There may be edge cases where you can loop infinitely through just by throwing and catching, but I doubt any JVM language
                // does or will ever do this.
                
                // Move to next instruction
                insnNode = insnNode.getNext();
            }
        }
    }
}

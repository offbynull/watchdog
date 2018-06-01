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

import com.offbynull.watchdog.instrumenter.asm.VariableTable;
import com.offbynull.watchdog.instrumenter.asm.VariableTable.Variable;
import com.offbynull.watchdog.instrumenter.generators.DebugGenerators.MarkerType;
import static com.offbynull.watchdog.instrumenter.generators.DebugGenerators.debugMarker;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.call;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.loadVar;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.merge;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.saveVar;
import com.offbynull.watchdog.user.Watch;
import com.offbynull.watchdog.user.Watchdog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

final class ThreadLocalTriggerInstrumentationPass implements InstrumentationPass {

    private static final Type WATCHDOG_TYPE = Type.getType(Watchdog.class);
    private static final Type WATCH_TYPE = Type.getType(Watch.class);
    
    private static final Method GET_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "get");
    private static final Method PRE_BRANCH_INSTRUCTION_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "preBranchInstruction");
    private static final Method POST_METHOD_ENTRY_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "postMethodEntry");
    private static final Field PLACEHOLDER_FIELD = FieldUtils.getDeclaredField(Watchdog.class, "PLACEHOLDER");

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        Collection<MethodNode> methodNodes = classNode.methods;
        
        boolean classAnnotated = classNode.visibleAnnotations != null
                && classNode.visibleAnnotations.stream().anyMatch(a -> Type.getType(a.desc).equals(WATCH_TYPE));

        for (MethodNode methodNode : methodNodes) {
            // Skip methods without implementation (abstract/interface/etc..)
            if (methodNode.instructions == null) {
                continue;
            }
            
            // Skip method if method has already been instrumented by a previous pass
            MethodDescription methodDesc = new MethodDescription(classNode.name, methodNode.name, methodNode.desc);
            if (state.getSkipMethods().contains(methodDesc)) {
                continue;
            }
            
            // Since this method is going to be instrumented now, add it to the skip collection so it doesn't get instrumented by other
            // again by one of the other passes.
            state.getSkipMethods().add(methodDesc);
            
            // Skip if method or class isn't annotated for instrumentation
            boolean methodAnnotated = methodNode.visibleAnnotations != null
                    && methodNode.visibleAnnotations.stream().anyMatch(a -> Type.getType(a.desc).equals(WATCH_TYPE));
            if (!methodAnnotated && !classAnnotated) {
                continue;
            }



            // Get new LVT index for the watchdog
            VariableTable varTable = new VariableTable(classNode, methodNode);
            Variable watchdogVar = varTable.acquireExtra(WATCHDOG_TYPE);



            MarkerType markerType = state.instrumentationSettings().getMarkerType();
            InsnList insnList = methodNode.instructions;
            
            // At the beginning of the method, grab the watchdog from threadlocal storage
            InsnList preambleInsnList =
                    merge(
                            debugMarker(markerType, "Get watchdog from TLS"),
                            call(GET_METHOD),
                            saveVar(watchdogVar),
                            debugMarker(markerType, "Checking watchdog"),
                            call(POST_METHOD_ENTRY_METHOD, loadVar(watchdogVar))
                    );
            AbstractInsnNode lastPreambleInsnNode = preambleInsnList.getLast();
            insnList.insert(preambleInsnList);
            
            // Call the watchdog
            AbstractInsnNode insnNode = lastPreambleInsnNode.getNext(); // first original instruction will be after our preamble
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
                
                // On get static field, if field is a PLACEHOLDER remove the op and replace it with the op to load up the watchdog var
                if (insnNode instanceof FieldInsnNode && insnNode.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                    String owner = fieldInsnNode.owner;
                    String name = fieldInsnNode.name;
                    if (WATCHDOG_TYPE.getInternalName().equals(owner) && PLACEHOLDER_FIELD.getName().equals(name)) {
                        InsnList replaceInsnList = loadVar(watchdogVar);

                        AbstractInsnNode replaceLastInsnNode = replaceInsnList.getLast();
                        
                        insnList.insertBefore(insnNode, replaceInsnList);
                        insnList.remove(insnNode);
                        
                        // update insnNode to last instruction of replacement list -- so when insnNode.getNext() is called below it will
                        // move to the next instruction to the method (it does this because we've already shoved these isntructions into
                        // the instructions for the method above -- the call to insertBefore)
                        insnNode = replaceLastInsnNode;
                    }
                }
                
                // Move to next instruction
                insnNode = insnNode.getNext();
            }
        }
    }
}

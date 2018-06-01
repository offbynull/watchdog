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
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.loadVar;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.merge;
import com.offbynull.watchdog.user.Watchdog;
import java.lang.reflect.Field;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

final class ReplacePlaceholderInstrumentationPass implements InstrumentationPass {

    private static final Type WATCHDOG_TYPE = Type.getType(Watchdog.class);

    private static final Field PLACEHOLDER_FIELD = FieldUtils.getDeclaredField(Watchdog.class, "PLACEHOLDER");

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
                // On get static field, if field is a PLACEHOLDER remove the op and replace it with the op to load up the watchdog var
                  // This is to make things easier on the user, normally they'd pass down the watchdogArgVar directly instead of using the
                  // placeholder. But, they have the option of passing down the placeholder as well.
                if (insnNode instanceof FieldInsnNode && insnNode.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                    String owner = fieldInsnNode.owner;
                    String name = fieldInsnNode.name;
                    if (WATCHDOG_TYPE.getInternalName().equals(owner) && PLACEHOLDER_FIELD.getName().equals(name)) {
                        InsnList replaceInsnList = merge(
                                debugMarker(markerType, "Replaced watchdog placeholder"),
                                loadVar(watchdogVar)
                        );

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

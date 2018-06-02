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
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.merge;
import com.offbynull.watchdog.user.Watchdog;
import java.lang.reflect.Method;
import java.util.Map;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

final class ArrayInstantiationInstrumentationPass implements InstrumentationPass {
    
    private static final Method ON_INSTANTIATE_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "onInstantiate", Object.class);
    private static final String CLS_NAME = Type.getInternalName(ON_INSTANTIATE_METHOD.getDeclaringClass());
    private static final String METHOD_NAME = ON_INSTANTIATE_METHOD.getName();
    private static final String METHOD_DESC = Type.getType(ON_INSTANTIATE_METHOD).getDescriptor();

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        for (Map.Entry<MethodNode, MethodProperties> entry : state.identifiedMethods().entrySet()) {
            MethodNode methodNode = entry.getKey();
            MethodProperties methodProperties = entry.getValue();

            Variable watchdogVar = methodProperties.watchdogVariable();
            
            MarkerType markerType = state.instrumentationSettings().getMarkerType();
            InsnList insnList = methodNode.instructions;
            
            AbstractInsnNode insnNode = insnList.getFirst();
            while (insnNode != null) {
                if ((insnNode instanceof IntInsnNode && insnNode.getOpcode() == Opcodes.NEWARRAY)
                        || (insnNode instanceof TypeInsnNode && insnNode.getOpcode() == Opcodes.ANEWARRAY)
                        || (insnNode instanceof MultiANewArrayInsnNode)) { // for array
                    InsnList trackAfterInsnList = merge(
                            debugMarker(markerType, "Invoking watchdog instantiation tracker (array)"),
                            new InsnNode(Opcodes.DUP),                                                           // [obj, obj]
                            new VarInsnNode(Opcodes.ALOAD, watchdogVar.getIndex()),                              // [obj, obj, wdVar]
                            new InsnNode(Opcodes.DUP_X1),                                                        // [obj, wdVar, obj, wdVar]
                            new InsnNode(Opcodes.POP),                                                           // [obj, wdVar, obj]
                            new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CLS_NAME, METHOD_NAME, METHOD_DESC, false) // [obj]
                    );
                    
                    AbstractInsnNode lastTrackInsnNode = trackAfterInsnList.getLast();
                    insnList.insert(insnNode, trackAfterInsnList);
                    insnNode = lastTrackInsnNode;
                }
                
                // Move to next instruction
                insnNode = insnNode.getNext();
            }
        }
    }
}

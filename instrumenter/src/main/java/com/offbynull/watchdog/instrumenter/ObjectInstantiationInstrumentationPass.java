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

import com.offbynull.watchdog.instrumenter.asm.VariableTable;
import com.offbynull.watchdog.instrumenter.asm.VariableTable.Variable;
import com.offbynull.watchdog.instrumenter.generators.DebugGenerators.MarkerType;
import static com.offbynull.watchdog.instrumenter.generators.DebugGenerators.debugMarker;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.loadVar;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.merge;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.mergeIf;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.saveVar;
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
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

final class ObjectInstantiationInstrumentationPass implements InstrumentationPass {
    
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
                // NOTE: It may seem like both arrays and objects can be handled by the code for arrays, but remember that creating a new
                // object via NEW opcode doesn't invoke that objects constructor. If you try to make use of that object before the
                // constructor (<init>) has been called, the JVM will crap out because on the operandstack it'll be marked as an
                // uninitialized object.

                if (insnNode instanceof MethodInsnNode
                        && insnNode.getOpcode() == Opcodes.INVOKESPECIAL
                        && ((MethodInsnNode) insnNode).name.equals("<init>")) { // for object initialization
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                    
                    
                    
                    // Init
                    VariableTable varTable = methodProperties.variableTable();

                    Type methodOwner = Type.getObjectType(methodInsnNode.owner);
                    Type methodDesc = Type.getMethodType(methodInsnNode.desc);
                    Type[] methodParams = methodDesc.getArgumentTypes();
                    
                    Variable thisArg = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? varTable.getArgument(0) : null;
                    
                    Variable[] vars = new Variable[methodParams.length];  // Acquire LVT slots
                    for (int i = methodParams.length - 1; i >= 0; i--) {
                        Type methodParam = methodParams[i];
                        vars[i] = varTable.acquireExtra(methodParam);
                    }
                    Variable newObjVar = varTable.acquireExtra(methodOwner);



                    // Before the invoke special isntruction, duplicate the object pointer going into the INVOKESPECIAL. This requires
                    // saving the args and loading them back onto the stack + doing 1 extra load to get a duplicate the object pointer.
                      // Save args for invoke special
                    InsnList saveInitArgsInsnList = new InsnList();
                    saveInitArgsInsnList.add(debugMarker(markerType, "Saving INVOKESPECIAL <init> args from stack onto LVT"));
                    for (int i = methodParams.length - 1; i >= 0; i--) {
                        saveInitArgsInsnList.add(saveVar(vars[i]));
                    }
                    saveInitArgsInsnList.add(saveVar(newObjVar));

                    InsnList pushWatchdogArgsInsnList = new InsnList();
                    pushWatchdogArgsInsnList.add(debugMarker(markerType, "Pushing args for watchdog instantiation onto stack"));
                    pushWatchdogArgsInsnList.add(loadVar(watchdogVar));
                    pushWatchdogArgsInsnList.add(loadVar(newObjVar));

                      // Load args for invokespecial
                    InsnList loadInitArgsInsnList = new InsnList();
                    loadInitArgsInsnList.add(debugMarker(markerType, "Loading INVOKESPECIAL <init> args onto stack from LVT"));
                    loadInitArgsInsnList.add(loadVar(newObjVar));
                    for (int i = 0; i < methodParams.length; i++) {
                        loadInitArgsInsnList.add(loadVar(vars[i]));
                    }
                    
                    
                    
                    // After the invoke special instruction, the constructor (<init>) should have consumed all the original stack items. If
                    // the invokespecial was not for the owning class, we'll have loaded up the args for Watchdog.onInstantiate(), so invoke
                    // it.
                    LabelNode notForOwningObjectLabelNode = new LabelNode();
                    LabelNode endLabelNode = new LabelNode();
                    InsnList replaceInsnList = merge(
                            saveInitArgsInsnList,
                            pushWatchdogArgsInsnList,
                            loadInitArgsInsnList,
                            new MethodInsnNode(Opcodes.INVOKESPECIAL, methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, false),
                            mergeIf(thisArg != null, () -> new Object[] { // perform for non-static method -- check performed
                                debugMarker(markerType, "Checking if INVOKESPECIAL was for owning object"),
                                new VarInsnNode(Opcodes.ALOAD, thisArg.getIndex()),
                                new VarInsnNode(Opcodes.ALOAD, newObjVar.getIndex()),
                                new JumpInsnNode(Opcodes.IF_ACMPNE, notForOwningObjectLabelNode),
                                debugMarker(markerType, "TRUE -- popping watchdog instantiation args off stack"),
                                new InsnNode(Opcodes.POP),
                                new InsnNode(Opcodes.POP),
                                new JumpInsnNode(Opcodes.GOTO, endLabelNode),
                                notForOwningObjectLabelNode,
                                debugMarker(markerType, "FALSE -- Invoking watchdog instantiation tracker"),
                                new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CLS_NAME, METHOD_NAME, METHOD_DESC, false),
                                endLabelNode,
                            }),
                            mergeIf(thisArg == null, () -> new Object[] { // perform for static method -- no check performed
                                debugMarker(markerType, "Invoking watchdog instantiation tracker (object)"),
                                new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CLS_NAME, METHOD_NAME, METHOD_DESC, false)
                            })
                    );
                    
                    
                    
                    // Deinit
                    for (int i = 0; i < methodParams.length; i++) { // Release LVT slots
                        varTable.releaseExtra(vars[i]);
                    }
                    varTable.releaseExtra(newObjVar);
                    
                    
                    
                    // Replace invokespecial with replacement instructions
                    AbstractInsnNode newEndInsnNode = replaceInsnList.getLast();
                    insnList.insert(insnNode, replaceInsnList);
                    insnList.remove(insnNode);
                    insnNode = newEndInsnNode;
                }
                
                // Move to next instruction
                insnNode = insnNode.getNext();
            }
        }
    }
}

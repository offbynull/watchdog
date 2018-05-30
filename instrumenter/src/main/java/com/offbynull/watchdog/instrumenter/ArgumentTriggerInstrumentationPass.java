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

import static com.offbynull.watchdog.instrumenter.asm.SearchUtils.findMethodsWithParameter;
import com.offbynull.watchdog.instrumenter.asm.VariableTable;
import com.offbynull.watchdog.instrumenter.asm.VariableTable.Variable;
import com.offbynull.watchdog.instrumenter.generators.DebugGenerators.MarkerType;
import static com.offbynull.watchdog.instrumenter.generators.DebugGenerators.debugMarker;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.call;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.ifObjectsEqual;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.loadNull;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.loadVar;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.merge;
import static com.offbynull.watchdog.instrumenter.generators.GenericGenerators.saveVar;
import com.offbynull.watchdog.user.Watchdog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import org.apache.commons.lang3.Validate;
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

final class ArgumentTriggerInstrumentationPass implements InstrumentationPass {

    private static final Type WATCHDOG_TYPE = Type.getType(Watchdog.class);
    
    private static final Method GET_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "get");
    private static final Method CHECK_METHOD = MethodUtils.getMatchingMethod(Watchdog.class, "check");
    private static final Field PLACEHOLDER_FIELD = FieldUtils.getDeclaredField(Watchdog.class, "PLACEHOLDER");

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        Collection<MethodNode> methodNodes = findMethodsWithParameter(classNode.methods, WATCHDOG_TYPE);

        for (MethodNode methodNode : methodNodes) {
            // Skip methods without implementation (abstract/interface/etc..)
            if (methodNode.instructions == null) {
                continue;
            }

            
            
            // Find the LVT index of the watchdog argument
            VariableTable varTable = new VariableTable(classNode, methodNode);
            Variable watchdogArgVar = null;
            for (int i = 0; i < varTable.getArgCount(); i++) {
                Variable arg = varTable.getArgument(i);
                if (arg.getType().equals(WATCHDOG_TYPE)) {
                    watchdogArgVar = arg;
                    break;
                }
            }
            Validate.validState(watchdogArgVar != null); // sanity check -- this should never be null at this point



            MarkerType markerType = state.instrumentationSettings().getMarkerType();
            InsnList insnList = methodNode.instructions;
            
            // At the beginning of the method, if watchdog arg is null grab it from threadlocal storage
            InsnList preambleInsnList =
                    merge(
                            debugMarker(markerType, "Checking if watchdog supplied"),
                            ifObjectsEqual(loadNull(), loadVar(watchdogArgVar), 
                                    merge(
                                            debugMarker(markerType, "Watchdog not supplied -- grabbing from TLS"),
                                            call(GET_METHOD),
                                            saveVar(watchdogArgVar)
                                    )
                            )
                    );
            insnList.insert(preambleInsnList);
            
            // Call the watchdog
            AbstractInsnNode insnNode = insnList.getFirst();
            while (insnNode != null) {
                // On branch, invoke WatchDog.check()
                if (insnNode instanceof JumpInsnNode
                        || insnNode instanceof LookupSwitchInsnNode
                        || insnNode instanceof TableSwitchInsnNode) {
                    InsnList trackInsnList = merge(
                            debugMarker(markerType, "Checking watchdog"),
                            call(CHECK_METHOD, loadVar(watchdogArgVar))
                    );
                    insnList.insertBefore(insnNode, trackInsnList);
                }
                
                // On get static field, if field is a PLACEHOLDER remove the op and replace it with the op to load up the watchdog var
                  // This is to make things easier on the user, normally they'd pass down the watchdogArgVar directly instead of using the
                  // placeholder. But, they have the option of passing down the placeholder as well.
                if (insnNode instanceof FieldInsnNode && insnNode.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                    String owner = fieldInsnNode.owner;
                    String name = fieldInsnNode.name;
                    if (WATCHDOG_TYPE.getInternalName().equals(owner) && PLACEHOLDER_FIELD.getName().equals(name)) {
                        InsnList replaceInsnList = loadVar(watchdogArgVar);
                        
                        insnList.insertBefore(insnNode, replaceInsnList);
                        insnList.remove(insnNode);
                        
                        // update insnNode to last instruction of replacement list -- so when insnNode.getNext() is called below it will
                        // move to the next instruction to the method (it does this because we've already shoved these isntructions into
                        // the instructions for the method above -- the call to insertBefore)
                        insnNode = replaceInsnList.getLast();
                    }
                }
                
                // Move to next instruction
                insnNode = insnNode.getNext();
            }
        }
    }
}

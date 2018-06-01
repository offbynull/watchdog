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
import com.offbynull.watchdog.user.Watch;
import com.offbynull.watchdog.user.Watchdog;
import java.util.Collection;
import java.util.Map;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

final class AnalyzeInstrumentationPass implements InstrumentationPass {

    private static final Type WATCHDOG_TYPE = Type.getType(Watchdog.class);
    private static final Type WATCH_TYPE = Type.getType(Watch.class);

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        Collection<MethodNode> methodNodes = classNode.methods;

        for (MethodNode methodNode : methodNodes) {
            // Skip methods without implementation (abstract/interface/etc..)
            if (methodNode.instructions == null) {
                continue;
            }
            
            // Find the LVT index of the watchdog argument
            VariableTable varTable = new VariableTable(classNode, methodNode);
            Variable watchdogVar = null;
            boolean watchdogVarIsArg = false;
            for (int i = 0; i < varTable.getArgCount(); i++) {
                Variable arg = varTable.getArgument(i);
                if (arg.getType().equals(WATCHDOG_TYPE)) {
                    watchdogVar = arg;
                    watchdogVarIsArg = true;
                    break;
                }
            }
            
            // No watchdog arg? if class or method is annotated for instrumentation then create a placeholder for the variable
            if (watchdogVar == null) {
                  // at some point in future, move classAnnotated cals out of the main loop -- it's wasteful to keep it here
                boolean classAnnotated = classNode.visibleAnnotations != null
                        && classNode.visibleAnnotations.stream().anyMatch(a -> Type.getType(a.desc).equals(WATCH_TYPE));
                boolean methodAnnotated = methodNode.visibleAnnotations != null
                        && methodNode.visibleAnnotations.stream().anyMatch(a -> Type.getType(a.desc).equals(WATCH_TYPE));
                if (!methodAnnotated && !classAnnotated) {
                    continue;
                }

                watchdogVar = varTable.acquireExtra(WATCHDOG_TYPE);
            }

            // Add to list of identified methods
            Map<MethodNode, MethodProperties> identifiedMethods = state.identifiedMethods();
            identifiedMethods.put(methodNode, new MethodProperties(varTable, watchdogVar, watchdogVarIsArg));
        }
    }
}

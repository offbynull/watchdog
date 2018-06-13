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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

final class LoopAnalyzer {
    private LoopAnalyzer() {
        // do nothing
    }
    
    static Set<Loop> walkCycles(
            InsnList insnList,
            List<TryCatchBlockNode> tryCatchBlockNodes) {
        return walkCycles(insnList.getFirst(), insnList, tryCatchBlockNodes, new HashSet<>());
    }
    
    private static Set<Loop> walkCycles(
            AbstractInsnNode startInsnNode,
            InsnList insnList,
            List<TryCatchBlockNode> tryCatchBlockNodes,
            Set<LabelNode> walkedLabels) {
        Set<Loop> loops = new HashSet<>();
        
        AbstractInsnNode insnNode = startInsnNode;
        top:
        while (insnNode != null) {
            Validate.isTrue(insnNode.getOpcode() != Opcodes.JSR); // sanity check -- JSRs should be filtered out
            
            
            // If the instruction is in a try-catch block, there's a possibility it can branch out to the catch handler
            if (!(insnNode instanceof LabelNode) && !(insnNode instanceof FrameNode)) {
                int insnNodeIdx = insnList.indexOf(insnNode);
                AbstractInsnNode catchableInsnNode = insnNode;
                tryCatchBlockNodes.stream()
                        .filter(x -> insnNodeIdx >= insnList.indexOf(x.start))
                        .filter(x -> insnNodeIdx <= insnList.indexOf(x.end))
                        .forEach(x -> {
                            processBranch(walkedLabels, insnList, tryCatchBlockNodes, catchableInsnNode, x.handler, loops);
                        });
            }
            
            
            if (insnNode instanceof LabelNode) {
                // If label, add it to the walked labels
                LabelNode labelNode = (LabelNode) insnNode;
                walkedLabels.add(labelNode);
            } else if (insnNode instanceof JumpInsnNode) {
                // If jump, walk branch
                JumpInsnNode jumpInsnNode = (JumpInsnNode) insnNode;
                LabelNode labelNode = jumpInsnNode.label;
                
                processBranch(walkedLabels, insnList, tryCatchBlockNodes, jumpInsnNode, labelNode, loops);
                
                if (jumpInsnNode.getOpcode() == Opcodes.GOTO) { // There is no fail condition for a GOTO -- don't walk to next insn
                    break;
                }
            } else if (insnNode instanceof LookupSwitchInsnNode) {
                // If switch, walk cases and default
                LookupSwitchInsnNode lookupSwitchInsnNode = (LookupSwitchInsnNode) insnNode;

                for (LabelNode labelNode : lookupSwitchInsnNode.labels) {
                    processBranch(walkedLabels, insnList, tryCatchBlockNodes, lookupSwitchInsnNode, labelNode, loops);
                }

                LabelNode labelNode = lookupSwitchInsnNode.dflt;
                processBranch(walkedLabels, insnList, tryCatchBlockNodes, lookupSwitchInsnNode, labelNode, loops);
            } else if (insnNode instanceof TableSwitchInsnNode) {
                // If switch, walk cases and default
                TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) insnNode;

                for (LabelNode labelNode : tableSwitchInsnNode.labels) {
                    processBranch(walkedLabels, insnList, tryCatchBlockNodes, tableSwitchInsnNode, labelNode, loops);
                }

                LabelNode labelNode = tableSwitchInsnNode.dflt;
                processBranch(walkedLabels, insnList, tryCatchBlockNodes, tableSwitchInsnNode, labelNode, loops);
            } else {
                // If not a return/throw, this instruction will move to the next instruction in the list
                switch (insnNode.getOpcode()) {
                    case Opcodes.RETURN:
                    case Opcodes.IRETURN:
                    case Opcodes.FRETURN:
                    case Opcodes.LRETURN:
                    case Opcodes.DRETURN:
                    case Opcodes.ARETURN:
                    case Opcodes.ATHROW: {
                        // Should we have special handling for ATHROW? If in a try-catch of type being thrown, otherwise we break out.
                        //
                        // This seems like something we should do, but the model we have right now of marking every instruction in a
                        // try-catch block as a possible branch to the catch handler seems good enough for now. Do this in the future maybe.
                        break top; // break out of main loop -- don't walk to next insn
                    }
                }
            }
            
            // Walk to next insn
            insnNode = insnNode.getNext();
        }
        
        return loops;
    }

    private static void processBranch(
            Set<LabelNode> walkedLabels, InsnList insnList, List<TryCatchBlockNode> tryCatchBlockNodes,
            AbstractInsnNode fromInsnNode, LabelNode toInsnNode,
            Set<Loop> container) {
        if (!walkedLabels.contains(toInsnNode)) {
            Set<Loop> cycles = walkCycles(toInsnNode, insnList, tryCatchBlockNodes, walkedLabels);
            container.addAll(cycles);
        } else {
            container.add(new Loop(fromInsnNode, toInsnNode));
        }
    }
    
    
    
    
    public static final class Loop {
        private final AbstractInsnNode fromInsnNode;
        private final LabelNode toInsnNode;

        Loop(AbstractInsnNode fromInsnNode, LabelNode toInsnNode) {
            Validate.notNull(fromInsnNode);
            Validate.notNull(toInsnNode);
            this.fromInsnNode = fromInsnNode;
            this.toInsnNode = toInsnNode;
        }

        public AbstractInsnNode getFromInsnNode() {
            return fromInsnNode;
        }

        public LabelNode getToInsnNode() {
            return toInsnNode;
        }


        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.fromInsnNode);
            hash = 79 * hash + Objects.hashCode(this.toInsnNode);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Loop other = (Loop) obj;
            if (!Objects.equals(this.fromInsnNode, other.fromInsnNode)) {
                return false;
            }
            if (!Objects.equals(this.toInsnNode, other.toInsnNode)) {
                return false;
            }
            return true;
        }
        
    }
}

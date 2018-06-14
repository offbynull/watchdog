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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
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
        return walk(insnList.getFirst(), insnList, tryCatchBlockNodes, new HashSet<>());
    }
    
    private static Set<Loop> walk(
            AbstractInsnNode startInsnNode,
            InsnList insnList,
            List<TryCatchBlockNode> tryCatchBlockNodes,
            Set<LabelNode> walkedLabels) {
        Set<Loop> loops = new LinkedHashSet<>();
        
        AbstractInsnNode insnNode = startInsnNode;
        while (insnNode != null) {
            Validate.isTrue(insnNode.getOpcode() != Opcodes.JSR); // sanity check -- JSRs should be filtered out
            
            // If the instruction is in a try-catch block, there's a possibility it can branch out to the catch handler
            for (TryCatchBlockNode tryCatchBlockNode : tryCatchBlockNodes) {
                int idx = insnList.indexOf(insnNode);
                int startIdx = insnList.indexOf(tryCatchBlockNode.start);
                int endIdx = insnList.indexOf(tryCatchBlockNode.end);
                if (idx >= startIdx && idx <= endIdx) {
                    forkWalk(walkedLabels, insnList, tryCatchBlockNodes, insnNode, tryCatchBlockNode.handler, loops);
                }
            }

            if (insnNode instanceof LabelNode) {
                // If label, add it to the walked labels
                LabelNode labelNode = (LabelNode) insnNode;
                walkedLabels.add(labelNode);
                
                insnNode = insnNode.getNext();
            } else if (insnNode instanceof JumpInsnNode) {
                // If jump, walk branch
                JumpInsnNode jumpInsnNode = (JumpInsnNode) insnNode;
                LabelNode labelNode = jumpInsnNode.label;
                
                if (jumpInsnNode.getOpcode() == Opcodes.GOTO) {
                    forkWalk(walkedLabels, insnList, tryCatchBlockNodes, insnNode, labelNode, loops);
                    insnNode = null;
                } else {
                    forkWalk(walkedLabels, insnList, tryCatchBlockNodes, insnNode, labelNode, loops);
                    insnNode = insnNode.getNext();
                }
            } else if (insnNode instanceof LookupSwitchInsnNode) {
                // If switch, walk cases and default
                LookupSwitchInsnNode lookupSwitchInsnNode = (LookupSwitchInsnNode) insnNode;
                
                for (LabelNode labelNode : lookupSwitchInsnNode.labels) {
                    forkWalk(walkedLabels, insnList, tryCatchBlockNodes, insnNode, labelNode, loops);
                }
                forkWalk(walkedLabels, insnList, tryCatchBlockNodes, insnNode, lookupSwitchInsnNode.dflt, loops);
                insnNode = null;
            } else if (insnNode instanceof TableSwitchInsnNode) {
                // If switch, walk cases and default
                TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) insnNode;

                for (LabelNode labelNode : tableSwitchInsnNode.labels) {
                    forkWalk(walkedLabels, insnList, tryCatchBlockNodes, insnNode, labelNode, loops);
                }
                forkWalk(walkedLabels, insnList, tryCatchBlockNodes, insnNode, tableSwitchInsnNode.dflt, loops);
                insnNode = null;
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
                        insnNode = null;
                        break;
                    }
                    default: {
                        insnNode = insnNode.getNext();
                        break;
                    }
                }
            }
        }
        
        return loops;
    }
    
    private static void forkWalk(
            Set<LabelNode> walkedLabels, InsnList insnList, List<TryCatchBlockNode> tryCatchBlockNodes,
            AbstractInsnNode currentInsnNode, LabelNode labelNode, Set<Loop> container) {
        if (walkedLabels.contains(labelNode)) {
            Loop loop = new Loop(currentInsnNode, labelNode);
            container.add(loop);
            return;
        }
        
        walkedLabels = new HashSet<>(walkedLabels); // create a copy so it doesn't add to actual set
        Set<Loop> cycles = walk(labelNode, insnList, tryCatchBlockNodes, walkedLabels);
        container.addAll(cycles);
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

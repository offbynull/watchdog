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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

final class LoopFinder {
    static Set<Loop> findLoops(
            InsnList insnList,
            List<TryCatchBlockNode> tryCatchBlockNodes) {
        MultiValuedMap<AbstractInsnNode, AbstractInsnNode> graph = buildGraph(insnList, tryCatchBlockNodes);
        
        Set<Loop> loops = new HashSet<>();
        for (AbstractInsnNode insnNode : graph.keySet()) {
            Set<Loop> newLoops = detectCycles(graph, insnList, insnNode);
            loops.addAll(newLoops);
        }
        return loops;
    }
    
    private static MultiValuedMap<AbstractInsnNode, AbstractInsnNode> buildGraph(
            InsnList insnList,
            List<TryCatchBlockNode> tryCatchBlockNodes) {
        MultiValuedMap<AbstractInsnNode, AbstractInsnNode> graph = new HashSetValuedHashMap();

        LinkedHashSet<AbstractInsnNode> queuedInsnNodes = new LinkedHashSet<>();
        queuedInsnNodes.add(insnList.getFirst());
        while (!queuedInsnNodes.isEmpty()) {
            AbstractInsnNode insnNode = queuedInsnNodes.iterator().next();
            Validate.isTrue(insnNode.getOpcode() != Opcodes.JSR); // sanity check -- JSRs should be filtered out
            
            
            Set<AbstractInsnNode> branchInsnNodes = new HashSet<>();
            
            
            // If in try blocks, add catch location as branch point -- this is horribly inefficient, but it's obvious what's happening
            int insnNodeIdx = insnList.indexOf(insnNode);
            tryCatchBlockNodes.stream()
                    .filter(x -> insnNodeIdx >= insnList.indexOf(x.start))
                    .filter(x -> insnNodeIdx <= insnList.indexOf(x.end))
                    .map(x -> x.handler)
                    .forEach(branchInsnNodes::add);
            
            
            // If operation branches, collect branch points
            if (insnNode instanceof JumpInsnNode) {
                JumpInsnNode jumpInsnNode = (JumpInsnNode) insnNode;
                LabelNode branchToInsnNode = jumpInsnNode.label;
                branchInsnNodes.add(branchToInsnNode);
                if (jumpInsnNode.getOpcode() != Opcodes.GOTO) {
                    AbstractInsnNode nextInsnNode = insnNode.getNext();
                    if (nextInsnNode != null) {
                        branchInsnNodes.add(nextInsnNode);
                    }
                }
            } else if (insnNode instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode lookupSwitchInsnNode = (LookupSwitchInsnNode) insnNode;
                for (LabelNode branchToInsnNode : lookupSwitchInsnNode.labels) {
                    branchInsnNodes.add(branchToInsnNode);
                }
                branchInsnNodes.add(lookupSwitchInsnNode.dflt);
            } else if (insnNode instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) insnNode;
                for (LabelNode branchToInsnNode : tableSwitchInsnNode.labels) {
                    branchInsnNodes.add(branchToInsnNode);
                }
                branchInsnNodes.add(tableSwitchInsnNode.dflt);
            } else {
                if (insnNode.getOpcode() == Opcodes.RETURN
                    || insnNode.getOpcode() == Opcodes.IRETURN
                    || insnNode.getOpcode() == Opcodes.FRETURN
                    || insnNode.getOpcode() == Opcodes.LRETURN
                    || insnNode.getOpcode() == Opcodes.DRETURN
                    || insnNode.getOpcode() == Opcodes.ARETURN
                    || insnNode.getOpcode() == Opcodes.ATHROW) {
                    // do nothing
                    // special handling for ATHROW? branches IF in a try-catch of type being thrown -- otherwise we break out
                } else {
                    AbstractInsnNode nextNode = insnNode.getNext();
                    if (nextNode != null) {
                        branchInsnNodes.add(nextNode);
                    }
                }
            }
            
            branchInsnNodes.forEach(n -> graph.put(insnNode, n));
        }
        
        return graph;
    }
    
    private static Set<Loop> detectCycles(
            MultiValuedMap<AbstractInsnNode, AbstractInsnNode> graph,
            InsnList insnList,
            AbstractInsnNode insnNode) {
        Set<Loop> ret = new HashSet<>();
        
        Set<AbstractInsnNode> visited = new HashSet<>();
        Set<AbstractInsnNode> queue = new HashSet<>();
        
        Collection<AbstractInsnNode> branchInsnNodes = graph.get(insnNode);
        queue.addAll(branchInsnNodes);
        visited.add(insnNode);
        
        while (!queue.isEmpty()) {
            AbstractInsnNode nextInsnNode = queue.iterator().next();
            queue.remove(nextInsnNode);
            
            if (nextInsnNode == insnNode) {
                // cycle detected
                int minIdx = visited.stream().mapToInt(x -> insnList.indexOf(x)).min().getAsInt();
                int maxIdx = visited.stream().mapToInt(x -> insnList.indexOf(x)).max().getAsInt();
                AbstractInsnNode minInsnNode = insnList.get(minIdx);
                AbstractInsnNode maxInsnNode = insnList.get(maxIdx);
                Loop loop = new Loop(minInsnNode, maxInsnNode);
                ret.add(loop);
            }
            
            branchInsnNodes = graph.get(nextInsnNode);
            for (AbstractInsnNode branchInsnNode : branchInsnNodes) {
                if (!visited.add(branchInsnNode)) { // skip if already visited
                    continue;
                }
                queue.add(branchInsnNode);
            }
            
            visited.add(nextInsnNode);
        }
        
        return ret;
    }
    
    public static final class Loop {
        private final AbstractInsnNode minInsnNode;
        private final AbstractInsnNode maxInsnNode;

        private Loop(AbstractInsnNode minInsnNode, AbstractInsnNode maxInsnNode) {
            this.minInsnNode = minInsnNode;
            this.maxInsnNode = maxInsnNode;
        }

        public AbstractInsnNode getMinInsnNode() {
            return minInsnNode;
        }

        public AbstractInsnNode getMaxInsnNode() {
            return maxInsnNode;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.minInsnNode);
            hash = 79 * hash + Objects.hashCode(this.maxInsnNode);
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
            if (!Objects.equals(this.minInsnNode, other.minInsnNode)) {
                return false;
            }
            if (!Objects.equals(this.maxInsnNode, other.maxInsnNode)) {
                return false;
            }
            return true;
        }
        
    }}

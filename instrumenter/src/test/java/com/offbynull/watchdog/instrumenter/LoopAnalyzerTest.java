package com.offbynull.watchdog.instrumenter;

import com.offbynull.watchdog.instrumenter.LoopAnalyzer.Loop;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import static com.offbynull.watchdog.instrumenter.LoopAnalyzer.findLoops;

public class LoopAnalyzerTest {
    
    @Test
    public void mustFindLoop() {
        LabelNode label1 = new LabelNode();
        JumpInsnNode jump1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        InsnList insnList = new InsnList();
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(label1);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(2));
        insnList.add(jump1);
        insnList.add(new InsnNode(Opcodes.NOP));
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jump1, label1));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustTightLoop() {
        LabelNode label1 = new LabelNode();
        JumpInsnNode jump1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        InsnList insnList = new InsnList();
        insnList.add(label1);
        insnList.add(jump1);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jump1, label1));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindMultipleLoopsToSameLabel() {
        LabelNode label1 = new LabelNode();
        JumpInsnNode jumpTo1_1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo1_2 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo1_3 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        InsnList insnList = new InsnList();
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(label1);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(2));
        insnList.add(jumpTo1_1);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(3));
        insnList.add(jumpTo1_2);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(4));
        insnList.add(jumpTo1_3);
        insnList.add(new InsnNode(Opcodes.NOP));
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jumpTo1_1, label1));
        expectedLoops.add(new Loop(jumpTo1_2, label1));
        expectedLoops.add(new Loop(jumpTo1_3, label1));
        assertEquals(expectedLoops, actualLoops);
    }

    @Test
    public void mustFindMultipleOverlappingLoops() {
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        
        JumpInsnNode jumpTo1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo2 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label2);
        InsnList insnList = new InsnList();
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(label1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(label2);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(2));
        insnList.add(jumpTo1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(2));
        insnList.add(jumpTo2);
        insnList.add(new InsnNode(Opcodes.NOP));
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jumpTo1, label1));
        expectedLoops.add(new Loop(jumpTo2, label2));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindMultipleIndependentLoops() {
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        JumpInsnNode jumpTo1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo2 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label2);
        InsnList insnList = new InsnList();
        insnList.add(label1);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(2));
        insnList.add(jumpTo1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(label2);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(3));
        insnList.add(jumpTo2);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jumpTo1, label1));
        expectedLoops.add(new Loop(jumpTo2, label2));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindMultipleLoopsInsideLargerLoop() {
        LabelNode labelMain = new LabelNode();
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        JumpInsnNode jumpMain = new JumpInsnNode(Opcodes.IF_ICMPEQ, labelMain);
        JumpInsnNode jumpTo1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo2_1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label2);
        JumpInsnNode jumpTo2_2 = new JumpInsnNode(Opcodes.IF_ICMPNE, label2);
        InsnList insnList = new InsnList();
        insnList.add(labelMain);
        insnList.add(label1);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(2));
        insnList.add(jumpTo1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(label2);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(3));
        insnList.add(jumpTo2_1);
        insnList.add(jumpTo2_2);
        insnList.add(jumpMain);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jumpTo1, label1));
        expectedLoops.add(new Loop(jumpTo2_1, label2));
        expectedLoops.add(new Loop(jumpTo2_2, label2));
        expectedLoops.add(new Loop(jumpMain, labelMain));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustNotAccountForDeadCode() {
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        LabelNode labelDead = new LabelNode();
        JumpInsnNode jumpTo1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo2 = new JumpInsnNode(Opcodes.IF_ICMPNE, label2);
        JumpInsnNode forceTo2 = new JumpInsnNode(Opcodes.GOTO, label2);
        JumpInsnNode jumpToDead = new JumpInsnNode(Opcodes.IF_ICMPNE, labelDead);
        InsnList insnList = new InsnList();
        insnList.add(label1);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(2));
        insnList.add(jumpTo1);
        insnList.add(forceTo2);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelDead);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(2));
        insnList.add(jumpToDead);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(label2);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(3));
        insnList.add(jumpTo2);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jumpTo1, label1));
        expectedLoops.add(new Loop(jumpTo2, label2));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustNotFindAnyLoopsInABasicTableSwitch() {
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        LabelNode label3 = new LabelNode();
        LabelNode labelDefault = new LabelNode();
        LabelNode labelEnd = new LabelNode();
        TableSwitchInsnNode tableSwitch = new TableSwitchInsnNode(0, 3, labelDefault, label1, label2, label3);
        JumpInsnNode forceToEnd_1 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_2 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_3 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        InsnList insnList = new InsnList();
        insnList.add(tableSwitch);
        insnList.add(label1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_1);
        insnList.add(label2);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_2);
        insnList.add(label3);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_3);
        insnList.add(labelDefault);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelEnd);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindLoopInSelfLoopingTableSwitch() {
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        LabelNode label3 = new LabelNode();
        LabelNode labelDefault = new LabelNode();
        LabelNode labelEnd = new LabelNode();
        TableSwitchInsnNode tableSwitch = new TableSwitchInsnNode(0, 3, labelDefault, label1, label2, label3);
        JumpInsnNode forceToEnd_1 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_2 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_3 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        InsnList insnList = new InsnList();
        insnList.add(labelDefault);
        insnList.add(tableSwitch);
        insnList.add(label1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_1);
        insnList.add(label2);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_2);
        insnList.add(label3);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_3);
        insnList.add(labelEnd);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(tableSwitch, labelDefault));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindLoopsInTableSwitchCasesThatJumpIntoEachother() {
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        LabelNode label3 = new LabelNode();
        LabelNode labelDefault = new LabelNode();
        LabelNode labelEnd = new LabelNode();
        TableSwitchInsnNode tableSwitch = new TableSwitchInsnNode(0, 3, labelDefault, label1, label2, label3);
        JumpInsnNode jumpTo1_1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo1_2 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo1_3 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode forceToEnd_1 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_2 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_3 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        InsnList insnList = new InsnList();
        insnList.add(tableSwitch);
        insnList.add(label1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(jumpTo1_1);
        insnList.add(forceToEnd_1);
        insnList.add(label2);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(jumpTo1_2);
        insnList.add(forceToEnd_2);
        insnList.add(label3);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(jumpTo1_3);
        insnList.add(forceToEnd_3);
        insnList.add(labelDefault);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelEnd);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jumpTo1_1, label1));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustNotFindAnyLoopsInABasicLookupSwitch() {
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        LabelNode label3 = new LabelNode();
        LabelNode labelDefault = new LabelNode();
        LabelNode labelEnd = new LabelNode();
        LookupSwitchInsnNode tableSwitch = new LookupSwitchInsnNode(labelDefault, new int[] { 0, 1, 2}, new LabelNode[] { label1, label2, label3 });
        JumpInsnNode forceToEnd_1 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_2 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_3 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        InsnList insnList = new InsnList();
        insnList.add(tableSwitch);
        insnList.add(label1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_1);
        insnList.add(label2);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_2);
        insnList.add(label3);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_3);
        insnList.add(labelDefault);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelEnd);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindLoopInSelfLoopingLookupSwitch() {
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        LabelNode label3 = new LabelNode();
        LabelNode labelDefault = new LabelNode();
        LabelNode labelEnd = new LabelNode();
        LookupSwitchInsnNode tableSwitch = new LookupSwitchInsnNode(labelDefault, new int[] { 0, 1, 2}, new LabelNode[] { label1, label2, label3 });
        JumpInsnNode forceToEnd_1 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_2 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_3 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        InsnList insnList = new InsnList();
        insnList.add(labelDefault);
        insnList.add(tableSwitch);
        insnList.add(label1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_1);
        insnList.add(label2);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_2);
        insnList.add(label3);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(forceToEnd_3);
        insnList.add(labelEnd);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(tableSwitch, labelDefault));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindLoopsInLookupSwitchCasesThatJumpIntoEachother() {
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        LabelNode label3 = new LabelNode();
        LabelNode labelDefault = new LabelNode();
        LabelNode labelEnd = new LabelNode();
        LookupSwitchInsnNode tableSwitch = new LookupSwitchInsnNode(labelDefault, new int[] { 0, 1, 2}, new LabelNode[] { label1, label2, label3 });
        JumpInsnNode jumpTo1_1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo1_2 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo1_3 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode forceToEnd_1 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_2 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        JumpInsnNode forceToEnd_3 = new JumpInsnNode(Opcodes.GOTO, labelEnd);
        InsnList insnList = new InsnList();
        insnList.add(tableSwitch);
        insnList.add(label1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(jumpTo1_1);
        insnList.add(forceToEnd_1);
        insnList.add(label2);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(jumpTo1_2);
        insnList.add(forceToEnd_2);
        insnList.add(label3);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(jumpTo1_3);
        insnList.add(forceToEnd_3);
        insnList.add(labelDefault);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelEnd);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jumpTo1_1, label1));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindLoopInLoopingTryCatch() {
        LabelNode labelTryStart = new LabelNode();
        LabelNode labelTryEnd = new LabelNode();
        LabelNode labelTryCatch = new LabelNode();
        JumpInsnNode jumpToTryCatch = new JumpInsnNode(Opcodes.IF_ICMPEQ, labelTryCatch);
        InsnList insnList = new InsnList();
        insnList.add(labelTryCatch);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelTryStart);
        insnList.add(jumpToTryCatch);
        insnList.add(labelTryEnd);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        tryCatchBlockNodes.add(new TryCatchBlockNode(labelTryStart, labelTryEnd, labelTryCatch, null));
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jumpToTryCatch, labelTryCatch));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindLoopInMultipleLoopingTryCatch() {
        LabelNode labelTryStart_1 = new LabelNode();
        LabelNode labelTryStart_2 = new LabelNode();
        LabelNode labelTryEnd_1 = new LabelNode();
        LabelNode labelTryEnd_2 = new LabelNode();
        LabelNode labelTryCatch = new LabelNode();
        JumpInsnNode jumpToTryCatch = new JumpInsnNode(Opcodes.IF_ICMPEQ, labelTryCatch);
        InsnList insnList = new InsnList();
        insnList.add(labelTryCatch);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelTryStart_1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelTryStart_2);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelTryEnd_1);
        insnList.add(jumpToTryCatch);
        insnList.add(labelTryEnd_2);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        tryCatchBlockNodes.add(new TryCatchBlockNode(labelTryStart_1, labelTryEnd_1, labelTryCatch, null));
        tryCatchBlockNodes.add(new TryCatchBlockNode(labelTryStart_2, labelTryEnd_2, labelTryCatch, null));
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(insnList.get(3), labelTryCatch));
        expectedLoops.add(new Loop(insnList.get(5), labelTryCatch));
        expectedLoops.add(new Loop(insnList.get(7), labelTryCatch));
        assertEquals(expectedLoops, actualLoops);
    }

    @Test
    public void mustFindLoopInTightSelfLoopingTryCatch() {
        LabelNode labelTryStart = new LabelNode();
        LabelNode labelTryEnd = new LabelNode();
        InsnList insnList = new InsnList();
        insnList.add(labelTryStart);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(labelTryEnd);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        tryCatchBlockNodes.add(new TryCatchBlockNode(labelTryStart, labelTryEnd, labelTryStart, null));
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(insnList.get(1), labelTryStart));
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindAnyLoopsInSelfLoopingTryCatchWithNoInsturctions() {
        LabelNode labelTryStart = new LabelNode();
        LabelNode labelTryEnd = new LabelNode();
        InsnList insnList = new InsnList();
        insnList.add(labelTryStart);
        insnList.add(labelTryEnd);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        tryCatchBlockNodes.add(new TryCatchBlockNode(labelTryStart, labelTryEnd, labelTryStart, null));
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        assertEquals(expectedLoops, actualLoops);
    }
    
    @Test
    public void mustFindMultipleLoopsInsideLargerLoopInsideMultipleTryCatch() {
        LabelNode labelTryStart = new LabelNode();
        LabelNode labelTryEnd = new LabelNode();
        LabelNode labelTryCatch = new LabelNode();
        LabelNode labelMain = new LabelNode();
        LabelNode label1 = new LabelNode();
        LabelNode label2 = new LabelNode();
        JumpInsnNode jumpMain = new JumpInsnNode(Opcodes.IF_ICMPEQ, labelMain);
        JumpInsnNode jumpTo1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label1);
        JumpInsnNode jumpTo2_1 = new JumpInsnNode(Opcodes.IF_ICMPEQ, label2);
        JumpInsnNode jumpTo2_2 = new JumpInsnNode(Opcodes.IF_ICMPNE, label2);
        InsnList insnList = new InsnList();
        insnList.add(labelMain);
        insnList.add(label1);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(2));
        insnList.add(jumpTo1);
        insnList.add(new InsnNode(Opcodes.NOP));
        insnList.add(label2);
        insnList.add(new LdcInsnNode(1));
        insnList.add(new LdcInsnNode(3));
        insnList.add(jumpTo2_1);
        insnList.add(jumpTo2_2);
        insnList.add(jumpMain);
        
        List<TryCatchBlockNode> tryCatchBlockNodes = new ArrayList<>();
        
        Set<Loop> actualLoops = findLoops(insnList, tryCatchBlockNodes);
        Set<Loop> expectedLoops = new HashSet<>();
        expectedLoops.add(new Loop(jumpTo1, label1));
        expectedLoops.add(new Loop(jumpTo2_1, label2));
        expectedLoops.add(new Loop(jumpTo2_2, label2));
        expectedLoops.add(new Loop(jumpMain, labelMain));
        assertEquals(expectedLoops, actualLoops);
    }
}

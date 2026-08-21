package me.darknet.assembler.compile.analysis.jvm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A simulation which visits each ASM node of a method in a linear
 * fashion. The simulation is not for proper stack/local analysis.
 */
public class IndexedStraightforwardSimulation {
    public void execute(IndexedExecutionEngine engine, MethodNode method) {
        if (method == null) throw new IllegalStateException("Cannot analyze 'null' method");
        InsnList instructions = method.instructions;
        for (int i = 0; i < instructions.size(); i++) {
            engine.index(i);
            AbstractInsnNode instruction = instructions.get(i);
            if (instruction instanceof LabelNode label) {
                engine.label(label);
            } else if (!(instruction instanceof LineNumberNode)) {
                engine.execute(instruction);
            }
        }
    }
}

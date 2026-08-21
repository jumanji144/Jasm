package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.compile.analysis.jvm.IndexedExecutionEngine;
import me.darknet.assembler.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Simple execution engine to track where variables stored in the {@link VarCache} are first assigned.
 */
public class VarCacheUpdater implements IndexedExecutionEngine, Opcodes {
    private final VarCache varCache;
    private int currentIndex = 0;

    public VarCacheUpdater(@NotNull VarCache varCache) {
        this.varCache = varCache;
    }

    @Override
    public void label(LabelNode label) {
        // no-op
    }

    @Override
    public void execute(AbstractInsnNode instruction) {
        if (instruction instanceof VarInsnNode varInsn) {
            int opcode = varInsn.getOpcode();
            boolean write = switch (opcode) {
                case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> true;
                default -> false;
            };
            Type hint = switch (opcode) {
                case ILOAD, ISTORE, RET -> JvmTypeUtils.INT;
                case LLOAD, LSTORE -> JvmTypeUtils.LONG;
                case FLOAD, FSTORE -> JvmTypeUtils.FLOAT;
                case DLOAD, DSTORE -> JvmTypeUtils.DOUBLE;
                default -> null;
            };
            if (write) {
                var variable = varCache.getFirstByIndex(varInsn.var);
                if (variable != null) {
                    variable.updateFirstAssignedOffset(currentIndex);
                    variable.updateTypeHint(hint);
                }
            }
            return;
        }

        if (instruction instanceof IincInsnNode iincInsn) {
            var variable = varCache.getFirstByIndex(iincInsn.var);
            if (variable != null) {
                variable.updateFirstAssignedOffset(currentIndex);
                variable.updateTypeHint(JvmTypeUtils.INT);
            }
        }
    }

    @Override
    public void index(int index) {
        currentIndex = index;
    }
}

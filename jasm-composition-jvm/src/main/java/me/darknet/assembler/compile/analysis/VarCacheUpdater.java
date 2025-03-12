package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.jvm.IndexedExecutionEngine;
import org.jetbrains.annotations.NotNull;

import static dev.xdark.blw.code.JavaOpcodes.*;

/**
 * Simple execution engine to track where variables stored in the {@link VarCache} are first assigned.
 */
public class VarCacheUpdater implements IndexedExecutionEngine {
    private final VarCache varCache;
    private int currentIndex = 0;

    /**
     * @param varCache
     *         Cache to update.
     */
    public VarCacheUpdater(@NotNull VarCache varCache) {
        this.varCache = varCache;
    }

    @Override
    public void label(Label label) {
        // no-op
    }

    @Override
    public void execute(VarInstruction instruction) {
        int index = instruction.variableIndex();
        int opcode = instruction.opcode();
        boolean write;
        ClassType hint;
        switch (opcode) {
            case ILOAD, RET -> {
                write = false;
                hint = Types.INT;
            }
            case LLOAD -> {
                write = false;
                hint = Types.LONG;
            }
            case FLOAD -> {
                write = false;
                hint = Types.FLOAT;
            }
            case DLOAD -> {
                write = false;
                hint = Types.DOUBLE;
            }
            case ALOAD -> {
                write = false;
                hint = null;
            }
            case ISTORE -> {
                write = true;
                hint = Types.INT;
            }
            case LSTORE -> {
                write = true;
                hint = Types.LONG;
            }
            case FSTORE -> {
                write = true;
                hint = Types.FLOAT;
            }
            case DSTORE -> {
                write = true;
                hint = Types.DOUBLE;
            }
            case ASTORE -> {
                write = true;
                hint = null;
            }
            default -> {
                write = false;
                hint = null;
            }
        }

        if (write) {
            var variable = varCache.getFirstByIndex(index);
            if (variable != null) {
                variable.updateFirstAssignedOffset(currentIndex);
                variable.updateTypeHint(hint);
            }
        }
    }

    @Override
    public void execute(SimpleInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(ConstantInstruction<?> instruction) {
        // no-op
    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(TableSwitchInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(InstanceofInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(CheckCastInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(AllocateInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(AllocateMultiDimArrayInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(MethodInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(FieldInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(ImmediateJumpInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(ConditionalJumpInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(VariableIncrementInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(PrimitiveConversionInstruction primitiveConversionInstruction) {
        // no-op
    }

    @Override
    public void execute(Instruction instruction) {
        // no-op
    }

    @Override
    public void index(int index) {
        currentIndex = index;
    }
}

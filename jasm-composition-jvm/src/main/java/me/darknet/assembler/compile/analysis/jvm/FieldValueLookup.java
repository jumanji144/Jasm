package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.instruction.FieldInstruction;
import me.darknet.assembler.compile.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to plug in field values into the analysis engine.
 *
 * @see ValuedJvmAnalysisEngine
 */
public interface FieldValueLookup {
    /**
     * @param instruction
     *                    Instruction with field declaration.
     * @param context
     *                    Field context, for non-static getters.
     *
     * @return Value of field, or {@code null} if unknown.
     */
    @Nullable
    Value accept(@NotNull FieldInstruction instruction, @Nullable Value.ObjectValue context);
}

package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.parser.processor.ProcessorContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Completes the resolved operands of one instruction after every operand has resolved.
 * <p>
 * An operand resolver sees only its own source element. An operand whose meaning depends on a sibling operand, such as
 * a list of arguments whose widths a descriptor declares, is completed here, where the source instruction and every
 * resolved operand are visible. Errors reported to the context stop the instruction from lowering.
 */
@FunctionalInterface
public interface OperandRefiner {
    /**
     * @param context
     * 		Processor context receiving diagnostics.
     * @param source
     * 		Source instruction, whose operands have all been verified.
     * @param operands
     * 		Resolved operands, index-aligned with the operands the instruction declares.
     *
     * @return The operands to use, index-aligned with {@code operands}.
     */
    @NotNull
    List<ValidatedOperand> refine(@NotNull ProcessorContext context, @NotNull ASTInstruction source,
                                  @NotNull List<ValidatedOperand> operands);
}

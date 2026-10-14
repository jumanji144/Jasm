package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.parser.processor.ProcessorContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Produces the typed semantic value for one operand that already passed verification.
 * <p>
 * Resolvers run after {@link Operand#verify}, so an implementation may assume the operand
 * shape is valid and only has to report the additional semantic failures it discovers itself.
 */
@FunctionalInterface
public interface OperandValueResolver {
    /**
     * @param context
     * 		Processor context receiving diagnostics.
     * @param element
     * 		Source element for the operand.
     *
     * @return Resolved value, or {@code null} after reporting a diagnostic to {@code context}.
     */
    @Nullable
    OperandValue resolve(@NotNull ProcessorContext context, @NotNull ASTElement element);
}

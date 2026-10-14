package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.parser.processor.ProcessorContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolvers for target-neutral operand values such as member paths and switch keys.
 */
public final class OperandValues {
    private OperandValues() {}

    /**
     * @param context
     * 		Processor context receiving diagnostics.
     * @param element
     * 		Source element for the member reference.
     * @return Member path split at the last {@code '.'}, or {@code null} after reporting a diagnostic.
     */
    public static @Nullable MemberPath memberPath(@NotNull ProcessorContext context, @NotNull ASTElement element) {
        String literal = element.content();
        int separator = literal.lastIndexOf('.');
        if (separator <= 0 || separator == literal.length() - 1) {
            context.throwError("Expected member path in owner.name form: " + literal, element.location());
            return null;
        }
        return new MemberPath(literal.substring(0, separator), literal.substring(separator + 1));
    }

    /**
     * @param context
     * 		Processor context receiving diagnostics.
     * @param element
     * 		Source element for the switch key.
     * @return Integer switch key, or {@code null} after reporting a diagnostic.
     */
    public static @Nullable SwitchKey switchKey(@NotNull ProcessorContext context, @NotNull ASTElement element) {
        String normalized = element.content().toLowerCase();
        boolean negative = normalized.startsWith("-");
        if (negative || normalized.startsWith("+"))
            normalized = normalized.substring(1);

        int radix = 10;
        if (normalized.startsWith("0x")) {
            radix = 16;
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("0b")) {
            radix = 2;
            normalized = normalized.substring(2);
        }

        if (normalized.isEmpty()) {
            context.throwError("Expected integer switch key", element.location());
            return null;
        }

        long parsed;
        try {
            parsed = radix == 10 ? Long.parseLong(normalized, radix) : Long.parseUnsignedLong(normalized, radix);
        } catch (NumberFormatException exception) {
            context.throwError("Expected integer switch key", element.location());
            return null;
        }

        if (negative)
            parsed = -parsed;
        return new SwitchKey((int) parsed);
    }
}

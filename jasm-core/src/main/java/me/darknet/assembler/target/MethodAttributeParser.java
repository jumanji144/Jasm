package me.darknet.assembler.target;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.assembler.processing.MethodTargetData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parser for one target-owned method-body attribute.
 */
public interface MethodAttributeParser {
    /**
     * @return Exact source key handled by this parser.
     */
    @NotNull String key();

    /**
     * @param context
     * 		Processor context receiving diagnostics.
     * @param value
     * 		Attribute value to parse, or {@code null} when the source omitted it.
     * @param declaration
     * 		Source element of the containing method.
     *
     * @return Parsed target data, or {@code null} when validation failed.
     */
    @Nullable MethodTargetData parse(
            @NotNull ProcessorContext context,
            @Nullable ASTElement value,
            @NotNull ASTElement declaration);
}
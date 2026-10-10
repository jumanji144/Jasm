package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.processing.MethodTargetData;
import org.jetbrains.annotations.NotNull;

/**
 * Validated Dalvik method register metadata.
 *
 * @param registers
 * 		Declared register count. Numeric-but-invalid source values retain their parsed value for diagnostics.
 * @param source
 * 		Source number that declared the register count.
 */
public record DalvikMethodData(int registers, @NotNull ASTNumber source) implements MethodTargetData {}

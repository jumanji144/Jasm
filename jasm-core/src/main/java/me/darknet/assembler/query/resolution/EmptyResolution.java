package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import org.jetbrains.annotations.NotNull;

/**
 * Empty resolution.
 */
public record EmptyResolution() implements Resolution {
	public static final EmptyResolution INSTANCE = new EmptyResolution();

	@Override
	public @NotNull ASTElement element() {
		return ASTIdentifier.STUB;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}
}

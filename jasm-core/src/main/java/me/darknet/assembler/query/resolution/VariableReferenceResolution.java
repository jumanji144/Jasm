package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.query.VariableUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of a variable use within an instruction.
 *
 * @param parentClass
 * 		The class containing the reference, or {@code null} if the method is top-level.
 * @param method
 * 		The method containing the reference.
 * @param reference
 * 		The identifier used by the instruction to refer to the variable.
 * @param usage
 * 		The semantic usage information for the reference, including its access kind and resolved declaration when known.
 */
public record VariableReferenceResolution(@Nullable ASTClass parentClass,
                                          @NotNull ASTMethod method,
                                          @NotNull ASTIdentifier reference,
                                          @NotNull VariableUsage usage) implements VariableResolution {
	@Override
	public @NotNull ASTElement element() {
		return reference;
	}

	@Override
	public @NotNull ASTIdentifier identifier() {
		return reference;
	}
}

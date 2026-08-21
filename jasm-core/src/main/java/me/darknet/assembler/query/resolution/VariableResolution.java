package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of a variable, which may be either a declaration or a reference.
 *
 * @see VariableDeclarationResolution
 * @see VariableReferenceResolution
 */
public sealed interface VariableResolution extends Resolution permits VariableDeclarationResolution, VariableReferenceResolution {
	/**
	 * @return The class containing the variable, or {@code null} if the method is top-level.
	 */
	@Nullable ASTClass parentClass();

	/**
	 * @return The method containing the reference.
	 */
	@NotNull ASTMethod method();

	/**
	 * @return The identifier used by the instruction to refer to the variable.
	 */
	@NotNull ASTIdentifier identifier();
}

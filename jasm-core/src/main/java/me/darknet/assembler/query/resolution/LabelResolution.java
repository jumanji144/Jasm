package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of a label, which may be either a declaration or a reference.
 *
 * @see LabelDeclarationResolution
 * @see LabelReferenceResolution
 */
public sealed interface LabelResolution extends Resolution permits LabelDeclarationResolution, LabelReferenceResolution {
	/**
	 * @return The class containing the label, or {@code null} if resolving at the top-level is a method.
	 */
	@Nullable ASTClass parentClass();

	/**
	 * @return The method containing the label.
	 */
	@NotNull ASTMethod method();

	/**
	 * @return The identifier of the label name used.
	 */
	@NotNull ASTIdentifier identifier();
}

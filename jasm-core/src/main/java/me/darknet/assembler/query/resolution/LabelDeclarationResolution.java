package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.query.LabelInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of a label declaration.
 *
 * @param parentClass
 * 		The class containing the label, or {@code null} if resolving at the top-level is a method.
 * @param method
 * 		The method containing the label.
 * @param declaration
 * 		The identifier of the label declaration.
 * @param label
 * 		The label info of the label declaration.
 */
public record LabelDeclarationResolution(@Nullable ASTClass parentClass,
                                         @NotNull ASTMethod method,
                                         @NotNull ASTLabel declaration,
                                         @NotNull LabelInfo label) implements LabelResolution {
	@Override
	public @NotNull ASTElement element() {
		return declaration;
	}

	@Override
	public @NotNull ASTIdentifier identifier() {
		return declaration.identifier();
	}
}

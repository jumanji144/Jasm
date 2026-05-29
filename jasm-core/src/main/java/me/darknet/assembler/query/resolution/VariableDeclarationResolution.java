package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.query.VariableInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution for a variable declaration.
 *
 * @param parentClass
 * 		The class containing the variable, or {@code null} if it's a local variable in a method.
 * @param method
 * 		The method containing the variable.
 * @param declaration
 * 		The identifier of the variable declaration.
 * @param variable
 * 		The variable info of the variable declaration.
 */
public record VariableDeclarationResolution(@Nullable ASTClass parentClass, @NotNull ASTMethod method,
                                            @NotNull ASTIdentifier declaration,
                                            @NotNull VariableInfo variable) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return declaration;
	}
}

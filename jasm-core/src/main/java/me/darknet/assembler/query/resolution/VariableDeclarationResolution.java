package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.query.VariableInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of the identifier that declares a variable within a method.
 *
 * @param parentClass
 * 		The class containing the declaration, or {@code null} if the method is top-level.
 * @param method
 * 		The method containing the declaration.
 * @param declaration
 * 		The identifier that introduces the variable, such as a method parameter or inferred local name.
 * @param variable
 * 		The semantic information describing the declared variable.
 */
public record VariableDeclarationResolution(@Nullable ASTClass parentClass,
                                            @NotNull ASTMethod method,
                                            @NotNull ASTIdentifier declaration,
                                            @NotNull VariableInfo variable) implements VariableResolution {
	@Override
	public @NotNull ASTElement element() {
		return declaration;
	}

	@Override
	public @NotNull ASTIdentifier identifier() {
		return declaration;
	}
}

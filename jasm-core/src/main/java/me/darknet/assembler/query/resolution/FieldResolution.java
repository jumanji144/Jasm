package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of a field declaration.
 *
 * @param parentClass
 * 		The class containing the field, or {@code null} if resolving at the top-level is a field.
 * @param field
 * 		The field declaration.
 */
public record FieldResolution(@Nullable ASTClass parentClass,
                              @NotNull ASTField field) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return field;
	}
}

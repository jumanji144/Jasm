package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import org.jetbrains.annotations.Nullable;

/**
 * Visitor for field declarations and their values.
 */
public interface ASTFieldVisitor extends ASTDeclarationVisitor {
    /**
     * Visits the value assigned to the field.
     *
     * @param value
     * 		Field initializer value.
     */
    void visitValue(@Nullable ASTElement value);
}

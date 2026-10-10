package me.darknet.assembler.ast.specific;

import me.darknet.assembler.visitor.Modifiers;

import org.jetbrains.annotations.NotNull;

/**
 * AST model of some access-modifier bearing element.
 */
public interface ASTAccessed {
    /**
     * @return Wrapped modifier identifiers of the element.
     */
    @NotNull
    Modifiers getModifiers();
}

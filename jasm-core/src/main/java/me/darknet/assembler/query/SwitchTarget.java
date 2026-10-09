package me.darknet.assembler.query;

import me.darknet.assembler.ast.ASTElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A branch target of a switch instruction.
 *
 * @param context
 * 		Case key text, or {@code default} for the default branch.
 * @param labelName
 * 		Name of the label the branch jumps to.
 * @param contextElement
 * 		AST element for the case key, or {@code null} when the key is not written out, such as the
 *        {@code default} key of a table switch or the implied keys of a packed switch.
 * @param labelElement
 * 		AST element for the label reference. Always present for a target that is returned.
 */
public record SwitchTarget(@NotNull String context, @NotNull String labelName,
                           @Nullable ASTElement contextElement, @Nullable ASTElement labelElement) {}

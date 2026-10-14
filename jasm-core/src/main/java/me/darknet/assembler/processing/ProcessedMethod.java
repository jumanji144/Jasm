package me.darknet.assembler.processing;

import me.darknet.assembler.ast.specific.ASTMethod;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Target-validated semantic view of one method.
 * <p>
 * The target that validated this method is not repeated here: a processed method is only ever reached
 * through the {@link ValidatedUnit} that holds it, and the unit carries its target. Keeping a second copy
 * would mean two places to keep in agreement for a fact one lookup already answers.
 *
 * @param source
 * 		Original method AST retained for source queries and declarations.
 * @param code
 * 		Source-preserving processed code entries.
 * @param extensions
 * 		Immutable target-owned semantic method extensions.
 */
public record ProcessedMethod(@NotNull ASTMethod source,
                              @NotNull List<ProcessedCodeEntry> code,
                              @NotNull MethodExtensions extensions) {
    public ProcessedMethod {
        code = List.copyOf(code);
        extensions = Objects.requireNonNull(extensions, "extensions");
    }
}

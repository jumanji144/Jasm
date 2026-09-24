package me.darknet.assembler.processing;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.target.TargetContext;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recoverable semantic view of one source unit.
 *
 * @param declarations
 * 		Top-level source declarations.
 * @param target
 * 		Target context used while processing this unit.
 * @param methods
 * 		Identity map from source methods to their processed views.
 *
 * @see SemanticProcessor
 */
public record PartialProcessedUnit(@NotNull List<ASTElement> declarations,
                                   @NotNull TargetContext target,
                                   @NotNull Map<ASTMethod, ProcessedMethod> methods) {}

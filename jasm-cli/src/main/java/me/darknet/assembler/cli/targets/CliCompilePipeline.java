package me.darknet.assembler.cli.targets;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.target.TargetContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Shared parse, shape-validation, and semantic-processing stages for one CLI source unit.
 */
final class CliCompilePipeline {
	private CliCompilePipeline() {}

	static @NotNull ValidatedUnit process(
			@NotNull CompileRequest request,
			@NotNull SourceUnit unit,
			@NotNull TargetContext targetContext) {
		Objects.requireNonNull(request, "request");
		Objects.requireNonNull(unit, "unit");
		Objects.requireNonNull(targetContext, "targetContext");
		CliRuntime runtime = request.runtime();
		Outcome<List<ASTElement>> parsed = Processor.processSourceResult(unit.code(), unit.sourceName(), targetContext);
		runtime.warnings(parsed.warnings());
		if (parsed.hasErrors()) {
			throw runtime.failure("Failed to parse source file:\n" + CliUtils.formatErrors(parsed.errors()));
		}

		List<ASTElement> ast = parsed.requireValue();
		validateShape(request, ast);
		Outcome<ValidatedUnit> processed = SemanticProcessor.process(ast, targetContext);
		runtime.warnings(processed.warnings());
		if (processed.hasErrors()) {
			throw runtime.failure("Failed to process source file:\n" + CliUtils.formatErrors(processed.errors()));
		}
		return processed.requireValue();
	}

	private static void validateShape(@NotNull CompileRequest request, @NotNull List<ASTElement> ast) {
		CliRuntime runtime = request.runtime();
		if (ast.size() != 1) {
			throw runtime.failure("Expected exactly one class, method, field or annotation declaration");
		}

		switch (ast.getFirst().type()) {
			case CLASS -> {
			}
			case METHOD, FIELD -> {
				if (request.overlay().isEmpty()) {
					throw runtime.failure("Overlay is required for non-class code");
				}
			}
			case ANNOTATION -> {
				if (request.overlay().isEmpty() || request.annotationTarget().isEmpty()) {
					throw runtime.failure("Overlay and annotation target are required for annotation code");
				}
			}
			default -> throw runtime.failure("Expected exactly one class, method, field or annotation declaration");
		}
	}
}

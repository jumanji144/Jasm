package me.darknet.assembler.backend.dalvik.compile;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.visitor.DalvikRootVisitor;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.transformer.Transformer;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Compiler for the Dalvik targets.
 */
public class DalvikCompiler implements Compiler<DalvikCompilerOptions, DalvikClassRepresentation, DalvikClassResult> {
	@Override
	public @NotNull Outcome<DalvikClassResult> compile(@NotNull ValidatedUnit unit,
	                                                   @NotNull DalvikCompilerOptions dalvikOptions) {
		List<ASTElement> declarations = unit.declarations();
		DiagnosticSink sink = new DiagnosticSink(DiagnosticPhase.BACKEND_EMISSION);

		// Validate that the unit is intended for the Dalvik target context.
		if (unit.target() != DalvikTargetContext.INSTANCE) {
			sink.error(DiagnosticCode.INTERNAL_INVARIANT,
					"Unit was processed for a different target",
					declarations.isEmpty() ? null : declarations.getFirst().location());
			return Outcome.of(new DalvikClassResult(null), sink.diagnostics());
		}

		// Validate that the unit contains exactly one declaration.
		if (declarations.size() != 1) {
			sink.error(DiagnosticCode.MALFORMED_DECLARATION,
					"Expected exactly one declaration",
					declarations.isEmpty() ? null : declarations.getFirst().location());
			return Outcome.of(new DalvikClassResult(null), sink.diagnostics());
		}

		// Validate that the declaration and overlay are compatible with the Dalvik target context.
		DalvikClassRepresentation overlayRepresentation = dalvikOptions.getOverlay();
		ClassDefinition overlay = overlayRepresentation == null ? null : overlayRepresentation.definition();
		ASTElement declaration = declarations.getFirst();
		if (declaration.type() == ElementType.ANNOTATION) {
			sink.error(DiagnosticCode.MALFORMED_DECLARATION,
					"Standalone annotations are only supported for the JVM target",
					declaration.location());
			return Outcome.of(new DalvikClassResult(null), sink.diagnostics());
		}
		if ((declaration.type() == ElementType.FIELD || declaration.type() == ElementType.METHOD) && overlay == null) {
			sink.error(DiagnosticCode.MALFORMED_DECLARATION,
					"Overlay is required for top-level field and method declarations",
					declaration.location());
			return Outcome.of(new DalvikClassResult(null), sink.diagnostics());
		}
		if (declaration instanceof ASTClass classDeclaration) {
			rejectJvmOnlyClassAttributes(classDeclaration, sink);
			if (sink.hasErrors())
				return Outcome.of(new DalvikClassResult(null), sink.diagnostics());
		}

		// Compile the declaration into a Dalvik class representation.
		DalvikRootVisitor visitor = new DalvikRootVisitor(overlay, sink);
		try {
			sink.addAll(new Transformer(visitor).transform(unit));
		} catch (Throwable failure) {
			sink.error(DiagnosticCode.BACKEND_FAILURE,
					"Failed to compile Dalvik source: " + failure.getMessage(),
					declaration.location());
		}

		// Check for errors, and report a failed outcome if any were found.
		if (sink.hasErrors())
			return Outcome.of(new DalvikClassResult(null), sink.diagnostics());

		// Check if the user targeted a field/method of a class, but didn't specify an overlay class.
		ClassDefinition definition = visitor.getDefinition();
		if (definition == null) {
			sink.error(DiagnosticCode.MALFORMED_DECLARATION,
					"Cannot build class, type name not specified",
					declaration.location());
			return Outcome.of(new DalvikClassResult(null), sink.diagnostics());
		}

		// No errors, proper class definition visited, give the user their successful outcome.
		return Outcome.of(
				new DalvikClassResult(new DalvikClassRepresentation(definition)),
				sink.diagnostics()
		);
	}

	private static void rejectJvmOnlyClassAttributes(@NotNull ASTClass declaration,
	                                                 @NotNull DiagnosticSink sink) {
		if (declaration.getVersion() != null) {
			sink.error(DiagnosticCode.UNSUPPORTED_CAPABILITY,
					"A class file version is a JVM-only attribute; the dex version is set per file",
					declaration.getVersion().location());
		}
		if (declaration.getSourceDebugExtension() != null) {
			sink.error(DiagnosticCode.UNSUPPORTED_CAPABILITY,
					"Dalvik does not use source debug extension attributes",
					declaration.getSourceDebugExtension().location());
		}
	}
}

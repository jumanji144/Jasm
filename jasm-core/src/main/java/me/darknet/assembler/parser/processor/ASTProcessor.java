package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.target.TargetContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Processor for an AST.
 */
public class ASTProcessor {
	private final TargetContext target;
	private final DeclarationRegistry registry;

	/**
	 * @param target Target services used while processing declarations.
	 */
	public ASTProcessor(@NotNull TargetContext target) {
		this.target = target;
		this.registry = DeclarationRegistry.createDefault();
	}

	/**
	 * Process the given source AST and retain recovered elements alongside diagnostics.
	 *
	 * @param ast AST to process.
	 * @return Processed elements and target-validation diagnostics.
	 */
	public @NotNull Outcome<List<ASTElement>> processAST(@NotNull List<ASTElement> ast) {
		ProcessorContext context = new ProcessorContext(target, registry, DiagnosticPhase.TARGET_VALIDATION, DiagnosticCode.MALFORMED_DECLARATION);
		for (ASTElement element : ast) {
			if (element instanceof ASTDeclaration declaration) {
				ASTElement parsed = context.parseDeclaration(declaration);
				if (parsed != null)
					context.add(parsed);
			} else {
				context.throwUnexpectedElementError("declaration", element);
			}
		}
		context.reportUnattachedAttributes();
		return Outcome.of(context.getResult(), context.diagnostics());
	}
}

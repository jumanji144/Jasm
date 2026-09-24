package me.darknet.assembler.processing;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import static me.darknet.assembler.error.DiagnosticCode.*;
import static me.darknet.assembler.error.DiagnosticPhase.*;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.OperandValue;
import me.darknet.assembler.instructions.ValidatedOperand;
import me.darknet.assembler.parser.processor.DeclarationRegistry;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.assembler.target.MethodAttributeParser;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds an explicit source-preserving semantic view for one target.
 */
public final class SemanticProcessor {
    private SemanticProcessor() {}

    /**
     * Processes source declarations and refuses to expose a semantic value when any error is reported.
     *
     * @param declarations
     * 		Source declarations to process.
     * @param target
     * 		Target whose definitions and operand schemas should be used.
     *
     * @return A validated unit with any warnings, or a failure carrying the semantic diagnostics.
     */
    public static @NotNull Outcome<ValidatedUnit> process(@NotNull List<ASTElement> declarations,
                                                          @NotNull TargetContext target) {
        Outcome<PartialProcessedUnit> partial = processPartial(declarations, target);
        if (partial.hasErrors())
            return Outcome.failure(partial.diagnostics());
        return Outcome.success(new ValidatedUnit(partial.requireValue()), partial.warnings());
    }

    /**
     * Processes source declarations while retaining successfully lowered methods after errors.
     *
     * @param declarations
     * 		Source declarations to process.
     * @param target
     * 		Target whose definitions and operand schemas should be used.
     *
     * @return A partial processed unit and diagnostics, including errors that made some methods unavailable.
     */
    public static @NotNull Outcome<PartialProcessedUnit> processPartial(@NotNull List<ASTElement> declarations,
                                                                        @NotNull TargetContext target) {
        Objects.requireNonNull(declarations, "declarations");
        Objects.requireNonNull(target, "target");

        DiagnosticSink collector = new DiagnosticSink();
        Map<ASTMethod, ProcessedMethod> methods = new IdentityHashMap<>();
        for (ASTElement declaration : declarations) {
            // Every declaration is attempted: a source with several bad methods reports all of them, and a
            // failed member is absent from the result rather than present and invalid.
            processDeclaration(declaration, target, methods, collector);
        }
        return Outcome.of(new PartialProcessedUnit(List.copyOf(declarations), target, Collections.unmodifiableMap(methods)), collector.diagnostics());
    }

    /**
     * @param declaration
     * 		Declaration to process.
     * @param target
     * 		Target whose definitions and operand schemas should be used.
     * @param methods
     * 		Map collecting the processed view of every method that processed.
     * @param collector
     * 		Sink receiving every diagnostic the declaration produced.
     */
    private static void processDeclaration(ASTElement declaration, TargetContext target,
                                           Map<ASTMethod, ProcessedMethod> methods,
                                           DiagnosticSink collector) {
        if (declaration instanceof ASTMethod method) {
            ProcessedMethod processed = processMethod(method, target, collector);
            if (processed != null)
                methods.put(method, processed);
            return;
        }
        if (declaration instanceof ASTClass klass) {
            for (ASTElement child : klass.contents()) {
                processDeclaration(child, target, methods, collector);
            }
        }
    }

    /**
     * @param method
     * 		Method to lower.
     * @param target
     * 		Target whose definitions and operand schemas should be used.
     * @param collector
     * 		Sink receiving the method's diagnostics.
     *
     * @return The processed method, or {@code null} after reporting why it cannot be lowered.
     */
    private static ProcessedMethod processMethod(ASTMethod method, TargetContext target, DiagnosticSink collector) {
        MethodExtensions extensions = processMethodAttributes(method, target, collector);
        if (extensions == null)
            return null;

        List<ProcessedCodeEntry> entries = new ArrayList<>();
        if (method.getCode() == null)
            return new ProcessedMethod(method, entries, extensions);

        ProcessorContext validation = new ProcessorContext(target, DeclarationRegistry.createDefault(), SEMANTIC_LOWERING, OPERAND_SHAPE);

        // Instruction definitions are target semantics, so lookup, shape validation, and operand resolution happen here once.
        for (ASTInstruction source : method.getCode().getInstructions()) {
            if (source instanceof ASTLabel label) {
                entries.add(new ProcessedLabel(label));
                continue;
            }

            Instruction<?> definition = target.instructions().get(source.identifier().content());
            if (definition == null) {
                validation.throwError(
                        UNKNOWN_INSTRUCTION,
                        "Unknown instruction: " + source.identifier().content(),
                        source.identifier().location()
                );
                break;
            }
            definition.verify(source, validation);
            if (validation.hasErrors())
                break;

            // A registered instruction the target cannot lower yet is rejected here, so the diagnostic
            // names the instruction instead of the emitter throwing once it reaches it.
            if (!definition.isAvailable()) {
                validation.throwError(UNAVAILABLE_INSTRUCTION, definition.unavailableReason(), source.identifier().location());
                break;
            }

            List<ValidatedOperand> operands = new ArrayList<>(definition.operandCount());
            int operandCount = definition.operandCount();
            for (int i = 0; i < operandCount; i++) {
                ASTElement operand = source.arguments().get(i);
                if (operand == null) {
                    validation.throwError(OPERAND_SHAPE, "Expected operand " + i + " to be present", source.location());
                    break;
                }
                // Resolvers run only for operands that verified, so a resolver may assume the shape
                // is sound and report just the extra semantic failures it finds itself. Roles are not
                // copied here: they stay on the definition, which is their single declaration site.
                OperandValue value = definition.operand(i).resolve(validation, operand);
                if (validation.hasErrors())
                    break;
                operands.add(new ValidatedOperand(i, value, operand));
            }
            if (validation.hasErrors())
                break;
            entries.add(new ProcessedInstruction(source, definition, operands));
        }

        // The per-method context is cancelled at the end of the method rather than after the whole unit,
        // which is what lets a later method still be lowered after an earlier one failed.
        collector.addAll(validation.diagnostics());
        if (validation.hasErrors())
            return null;
        return new ProcessedMethod(method, entries, extensions);
    }

    /**
     * @param method
     * 		Method whose raw target attributes should be lowered.
     * @param target
     * 		Target whose method-attribute parsers should be used.
     * @param collector
     * 		Sink receiving attribute diagnostics.
     *
     * @return Immutable method extensions, or {@code null} after reporting an attribute error.
     */
    private static @Nullable MethodExtensions processMethodAttributes(ASTMethod method, TargetContext target, DiagnosticSink collector) {
        ProcessorContext validation = new ProcessorContext(target, DeclarationRegistry.createDefault(), SEMANTIC_LOWERING, MALFORMED_DECLARATION);
        List<MethodTargetData> extensions = new ArrayList<>();
        for (Pair<ASTIdentifier, ASTElement> attribute : method.getMethodAttributes().pairs()) {
            String key = attribute.first().content();

            MethodAttributeParser parser = target.methodAttributes().get(key);
            if (parser == null) {
                validation.throwError(UNSUPPORTED_FORM, "Unsupported method attribute: " + key, attribute.first().location());
                continue;
            }

            MethodTargetData extension = parser.parse(validation, attribute.second(), method);
            if (extension != null)
                extensions.add(extension);
        }
        collector.addAll(validation.diagnostics());
        if (validation.hasErrors())
            return null;
        return new MethodExtensions(extensions);
    }
}

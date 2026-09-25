package me.darknet.assembler.transformer;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTMember;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.ast.specific.ASTRecordComponent;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.processing.ProcessedCodeEntry;
import me.darknet.assembler.processing.ProcessedInstruction;
import me.darknet.assembler.processing.ProcessedLabel;
import me.darknet.assembler.processing.ProcessedMethod;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTClassVisitor;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.assembler.visitor.ASTRecordComponentVisitor;
import me.darknet.assembler.visitor.ASTRootVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Traverses a source AST while pairing each method with its validated semantic view, then delegates emission to
 * an {@link ASTRootVisitor}.
 */
public class Transformer {
    private final ASTRootVisitor visitor;

    public Transformer(ASTRootVisitor visitor) {
        this.visitor = visitor;
    }

    /**
     * Transforms a processed AST unit into the transformer's {@link Transformer#visitor}.
     *
     * @param unit
     * 		Semantically validated source unit.
     *
     * @return Transformation diagnostics. A traversal has no value of its own, so this reports diagnostics
     * 		rather than an outcome: the caller still holds the unit and the visitor it built, and the emitted
     * 		representation belongs to that visitor.
     */
    public @NotNull List<Diagnostic> transform(ValidatedUnit unit) {
        Objects.requireNonNull(unit, "unit");
        DiagnosticSink sink = new DiagnosticSink(DiagnosticPhase.BACKEND_EMISSION);
        for (ASTElement declaration : unit.declarations()) {
            switch (declaration) {
                case ASTAnnotation annotation ->
                        transformAnnotation(annotation, visitor.visitAnnotation(annotation), sink);
                case ASTField field -> {
                    ASTFieldVisitor fieldVisitor = visitor.visitField(field.getModifiers(), field.getName(), field.getDescriptor());
                    transformField(field, fieldVisitor, sink);
                }
                case ASTMethod method -> {
                    ProcessedMethod processed = requireProcessed(unit, method);
                    transformMethod(method, processed, visitor.visitMethod(method, processed), sink);
                }
                case ASTClass clazz -> transformClass(clazz, unit, sink);
                case null -> sink.error(DiagnosticCode.UNSUPPORTED_FORM, "Null declaration", null);
                default -> sink.error(DiagnosticCode.UNSUPPORTED_FORM,
                        "Unsupported declaration: " + declaration.type(), declaration.location());
            }
        }
        return sink.diagnostics();
    }

    /**
     * Traverses one class declaration after its class visitor has been created.
     *
     * @param source
     * 		Class source declaration.
     * @param unit
     * 		Validated unit supplying semantic method views.
     * @param sink
     * 		Sink receiving traversal diagnostics.
     */
    private void transformClass(ASTClass source, ValidatedUnit unit, DiagnosticSink sink) {
        ASTClassVisitor classVisitor = visitor.visitClass(source.getModifiers(), source.getName());
        transformMember(source, classVisitor, sink);
        if (classVisitor == null)
            return;

        classVisitor.visitSourceFile(source.getSourceFile());
        classVisitor.visitSuperClass(source.getSuperName());
        classVisitor.visitOuterClass(source.getOuterClass());
        classVisitor.visitOuterMethod(source.getOuterMethod());
        classVisitor.visitNestHost(source.getNestHost());
        for (var nestMember : source.getNestMembers()) {
            classVisitor.visitNestMember(nestMember);
        }
        for (var anInterface : source.getInterfaces()) {
            classVisitor.visitInterface(anInterface);
        }
        for (var permittedSubclass : source.getPermittedSubclasses()) {
            classVisitor.visitPermittedSubclass(permittedSubclass);
        }

        for (ASTRecordComponent recordComponent : source.getRecordComponents()) {
            var componentVisitor = classVisitor.visitRecordComponent(recordComponent.getComponentType(),
                    recordComponent.getComponentDescriptor(), recordComponent.getSignature());
            transformRecordComponent(recordComponent, componentVisitor, sink);
        }

        for (var inner : source.getInners()) {
            classVisitor.visitInnerClass(inner.getModifiers(), inner.name(), inner.outerClass(), inner.innerClass());
        }

        for (ASTElement declaration : source.contents()) {
            if (declaration instanceof ASTField field) {
                ASTFieldVisitor fieldVisitor = classVisitor.visitField(field.getModifiers(), field.getName(), field.getDescriptor());
                transformField(field, fieldVisitor, sink);
            } else if (declaration instanceof ASTMethod method) {
                ProcessedMethod processed = requireProcessed(unit, method);
                transformMethod(method, processed, classVisitor.visitMethod(method, processed), sink);
            } else {
                sink.error(DiagnosticCode.UNSUPPORTED_FORM,
                        "Don't know how to process: " + declaration.type(), declaration.location());
            }
        }

        classVisitor.visitEnd();
    }

    /**
     * Traverses one source method with its paired semantic code view.
     *
     * @param source
     * 		Method source declaration.
     * @param processed
     * 		Validated semantic method view.
     * @param methodVisitor
     * 		Backend method visitor, or {@code null} when the target cannot emit the method.
     * @param sink
     * 		Sink receiving traversal diagnostics.
     */
    private static void transformMethod(ASTMethod source, ProcessedMethod processed,
                                        @Nullable ASTMethodVisitor methodVisitor, DiagnosticSink sink) {
        transformMember(source, methodVisitor, sink);
        if (methodVisitor == null)
            return;

        // Preserve source/JASM parameter indexes so target visitors can apply their own layout rules.
        for (int i = 0; i < source.getParameters().size(); i++) {
            methodVisitor.visitParameter(i, source.getParameters().get(i));
        }

        // Preserve declared exceptions before the target emits the body.
        for (var declaredException : source.getDeclaredExceptions()) {
            methodVisitor.visitDeclaredException(declaredException);
        }

        // Target visitors decide whether source parameters, including a receiver, have runtime slots.
        source.getParameterAnnotations().forEach((id, annotations) -> {
            int sourceParameterIndex = findSourceParameterIndex(source, id.content());
            if (sourceParameterIndex < 0)
                return;
            for (ASTAnnotation annotation : annotations) {
                transformAnnotation(annotation, methodVisitor.visitParameterAnnotation(
                        annotation.getVisibility(), sourceParameterIndex, annotation.getClassType()), sink);
            }
        });

        if (source.getAnnotationDefaultValue() != null) {
            methodVisitor.visitAnnotationDefaultValue(sink, source.getAnnotationDefaultValue());
        }

        if (source.getCode() == null) {
            methodVisitor.visitEnd();
            return;
        }

        ASTInstructionVisitor instructionVisitor = methodVisitor.visitCode(sink);
        if (instructionVisitor != null) {
            // Walk validated entries so emitters never re-associate source instructions positionally.
            for (ProcessedCodeEntry entry : processed.code()) {
                if (entry instanceof ProcessedLabel label) {
                    instructionVisitor.visitInstruction(label);
                    instructionVisitor.visitLabel(label.source().identifier());
                } else if (entry instanceof ProcessedInstruction instruction) {
                    instructionVisitor.visitInstruction(instruction);
                    instruction.definition().transform(instruction, instructionVisitor);
                }
            }

            for (var exception : source.getExceptionHandlers()) {
                instructionVisitor.visitException(
                        exception.start(), exception.end(), exception.handler(), exception.exceptionType()
                );
            }

            instructionVisitor.visitEnd();
        }

        methodVisitor.visitEnd();
    }

    private static void transformAnnotation(@NotNull ASTAnnotation source,
                                             @Nullable ASTAnnotationVisitor target,
                                             @NotNull DiagnosticSink sink) {
        if (target == null)
            return;
        ASTAnnotationVisitor.accept(target, source.getValueMap().pairs(), sink);
    }

    private static void transformMember(@NotNull ASTMember source,
                                        @Nullable ASTDeclarationVisitor target,
                                        @NotNull DiagnosticSink sink) {
        if (target == null) {
            sink.error(DiagnosticCode.UNSUPPORTED_FORM, "Unable to process member", null);
            return;
        }
        for (ASTAnnotation annotation : source.getVisibleAnnotations()) {
            transformAnnotation(annotation, target.visitAnnotation(annotation.getVisibility(), annotation.getClassType()), sink);
        }
        for (ASTAnnotation annotation : source.getInvisibleAnnotations()) {
            transformAnnotation(annotation, target.visitAnnotation(annotation.getVisibility(), annotation.getClassType()), sink);
        }
        for (ASTAnnotation annotation : source.getVisibleTypeAnnotations()) {
            transformAnnotation(annotation, target.visitTypeAnnotation(annotation.getVisibility(), annotation.getClassType(),
                    annotation.getTypeRef(), annotation.getTypePath()), sink);
        }
        for (ASTAnnotation annotation : source.getInvisibleTypeAnnotations()) {
            transformAnnotation(annotation, target.visitTypeAnnotation(annotation.getVisibility(), annotation.getClassType(),
                    annotation.getTypeRef(), annotation.getTypePath()), sink);
        }
        if (source.getSignature() != null)
            target.visitSignature(source.getSignature());
        if (source.isDeprecated())
            target.visitDeprecated();
    }

    private static void transformField(@NotNull ASTField source,
                                       @Nullable ASTFieldVisitor target,
                                       @NotNull DiagnosticSink sink) {
        transformMember(source, target, sink);
        if (target == null)
            return;
        if (source.getFieldValue() != null)
            target.visitValue(source.getFieldValue());
        target.visitEnd();
    }

    private static void transformRecordComponent(@NotNull ASTRecordComponent source,
                                                 @NotNull ASTRecordComponentVisitor target,
                                                 @NotNull DiagnosticSink sink) {
        for (ASTAnnotation annotation : source.getVisibleAnnotations()) {
            transformAnnotation(annotation, target.visitAnnotation(annotation.getVisibility(), annotation.getClassType()), sink);
        }
        for (ASTAnnotation annotation : source.getInvisibleAnnotations()) {
            transformAnnotation(annotation, target.visitAnnotation(annotation.getVisibility(), annotation.getClassType()), sink);
        }
        for (ASTAnnotation annotation : source.getVisibleTypeAnnotations()) {
            transformAnnotation(annotation, target.visitTypeAnnotation(annotation.getVisibility(), annotation.getClassType(),
                    annotation.getTypeRef(), annotation.getTypePath()), sink);
        }
        for (ASTAnnotation annotation : source.getInvisibleTypeAnnotations()) {
            transformAnnotation(annotation, target.visitTypeAnnotation(annotation.getVisibility(), annotation.getClassType(),
                    annotation.getTypeRef(), annotation.getTypePath()), sink);
        }
    }

    private static int findSourceParameterIndex(ASTMethod method, String name) {
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (Objects.equals(name, method.getParameters().get(i).content()))
                return i;
        }
        return -1;
    }

    private static @NotNull ProcessedMethod requireProcessed(ValidatedUnit unit, ASTMethod method) {
        return Objects.requireNonNull(unit.methods().get(method),
                "Validated unit is missing method: " + method.getName().content());
    }
}

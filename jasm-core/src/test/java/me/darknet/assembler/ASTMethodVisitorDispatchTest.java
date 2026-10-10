package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.transformer.Transformer;
import me.darknet.assembler.instructions.SemanticInstruction;
import me.darknet.assembler.processing.ProcessedMethod;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.test.FixtureTarget;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTClassVisitor;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.assembler.visitor.ASTRootVisitor;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link ASTMethod} dispatches through the single neutral {@code visitCode} protocol
 * and traverses the processed, source-aligned code entries.
 */
public class ASTMethodVisitorDispatchTest {
    @Test
    void dispatchesNeutralVisitCodeOverProcessedEntries() {
        List<ASTElement> ast = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.processDeclarations("<test>", """
                        .method public demo (II)V {
                          parameters: { a, b },
                          exceptions: {
                            { A, B, B, * }
                          },
                          code: {
                          A:
                            nop
                            return
                          B:
                          }
                        }
                        """, FixtureTarget.JVM.context()),
                "Method source should process"
        );
        ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(ast, FixtureTarget.JVM.context()),
                "Method should produce a processed view"
        );

        ASTMethod method = (ASTMethod) ast.getFirst();
        ProcessedMethod processed = unit.methods().get(method);
        assertNotNull(processed, "Processed view should be registered");

        RecordingMethodVisitor methodVisitor = new RecordingMethodVisitor();
        ASTRootVisitor rootVisitor = new ASTRootVisitor() {
            @Override
            public @Nullable ASTAnnotationVisitor visitAnnotation(@NotNull ASTAnnotation annotation) {
                return null;
            }

            @Override
            public @Nullable ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name) {
                return null;
            }

            @Override
            public @Nullable ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name,
                                                         ASTIdentifier descriptor) {
                return null;
            }

            @Override
            public ASTMethodVisitor visitMethod(@NotNull ASTMethod source, @NotNull ProcessedMethod semantic) {
                assertSame(processed, semantic, "Transformer should pair source and semantic method views");
                return methodVisitor;
            }
        };
        List<me.darknet.assembler.error.Diagnostic> diagnostics = new Transformer(rootVisitor).transform(unit);

        assertTrue(diagnostics.isEmpty(), DiagnosticAssertions.formatErrors(diagnostics));
        assertEquals(List.of(0, 1), methodVisitor.parameters, "Source parameter indexes should reach the visitor");
        assertEquals(1, methodVisitor.visitCodeCalls, "Neutral visitCode should be invoked exactly once");
        assertEquals(List.of("nop", "return"), methodVisitor.instructions, "Non-label instructions should be traversed in source order");
        assertEquals(List.of("A", "B"), methodVisitor.labels, "Labels should be visited through the neutral protocol");
        assertEquals(1, methodVisitor.exceptions, "Exception handlers should be visited");
        assertTrue(methodVisitor.ended, "visitEnd should be called after the body");
    }

    private static final class RecordingMethodVisitor implements ASTMethodVisitor {
        private final List<Integer> parameters = new ArrayList<>();
        private final List<String> instructions = new ArrayList<>();
        private final List<String> labels = new ArrayList<>();
        private int visitCodeCalls;
        private int exceptions;
        private boolean ended;

        @Override
        public void visitParameter(int index, @NotNull ASTIdentifier name) {
            parameters.add(index);
        }

        @Override
        public void visitAnnotationDefaultValue(@NotNull DiagnosticSink sink, ASTElement defaultValue) {
        }

        @Override
        public @NotNull ASTInstructionVisitor visitCode(@NotNull DiagnosticSink sink) {
            visitCodeCalls++;
            return new ASTInstructionVisitor() {
                @Override
                public void visitInstruction(@NotNull SemanticInstruction instruction) {
                    if (!(instruction.source() instanceof me.darknet.assembler.ast.primitive.ASTLabel)) {
                        instructions.add(instruction.source().identifier().content());
                    }
                }

                @Override
                public void visitLabel(@NotNull ASTIdentifier label) {
                    labels.add(label.content());
                }

                @Override
                public void visitLineNumber(@NotNull ASTNumber line) {
                }

                @Override
                public void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end,
                                          @NotNull ASTIdentifier handler, @NotNull ASTIdentifier type) {
                    exceptions++;
                }

                @Override
                public void visitEnd() {
                }
            };
        }

        @Override
        public ASTAnnotationVisitor visitParameterAnnotation(@NotNull AnnotationVisibility visibility, int index,
                                                             @NotNull ASTIdentifier classType) {
            return null;
        }

        @Override
        public void visitSignature(@Nullable ASTString signature) {
        }

        @Override
        public void visitEnd() {
            ended = true;
        }

        @Override
        public ASTAnnotationVisitor visitAnnotation(@NotNull AnnotationVisibility visibility, @NotNull ASTIdentifier classType) {
            return null;
        }

        @Override
        public ASTAnnotationVisitor visitTypeAnnotation(@NotNull AnnotationVisibility visibility, @NotNull ASTIdentifier classType,
                                                       @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
            return null;
        }
    }
}

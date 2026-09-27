package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.compile.DalvikClassResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.backend.dalvik.io.DalvikDexIO;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.instructions.ConstMethodHandleInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodTypeInstruction;
import me.darknet.dex.tree.definitions.constant.NullConstant;
import me.darknet.dex.tree.definitions.instructions.FillArrayDataInstruction;
import me.darknet.dex.tree.type.InstanceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikCompilerTest {

    @Test
    void compilesClassToDalvikRepresentation() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .field public static final answer I { value: 42 }
                    .field public static ref Ljava/lang/String; { value: null }
                    .method public static hello ()V {
                        code: {
                            return-void
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            assertTrue(result.representation() instanceof DalvikClassRepresentation);

            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            assertEquals("Example", definition.getType().internalName());
            assertNotNull(definition.getSuperClass());
            assertEquals("java/lang/Object", definition.getSuperClass().internalName());
            assertNotNull(definition.getField("answer", "I"));
            assertTrue(definition.getField("ref", "Ljava/lang/String;").getStaticValue() instanceof NullConstant);
            assertNotNull(definition.getMethod("hello", "()V"));
            assertNotNull(definition.getMethod("hello", "()V").getCode());
            assertEquals(1, definition.getMethod("hello", "()V").getCode().getInstructions().size());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> ctx = new PrintContext<>("\t");
            printer.print(ctx);

            String printed = TestUtils.normalize(ctx.toString());
            assertTrue(printed.contains(".super java/lang/Object"), printed);
            assertTrue(printed.contains(".field public static final answer I {value: 42}"), printed);
            assertTrue(printed.contains(".field public static ref Ljava/lang/String; {value: null}"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void compilesTopLevelFieldIntoOverlayClass() {
        TestUtils.processDalvik(
                ".field public static final answer I { value: 42 }",
                TestUtils.overlayOptions("top/level/OverlayExample"),
                result -> {
                    ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
                    assertEquals("top/level/OverlayExample", definition.getType().internalName());
                    assertNotNull(definition.getField("answer", "I"));
                }
        );
    }

    @Test
    void compilesTopLevelMethodIntoOverlayClass() {
        TestUtils.processDalvik("""
                .method public static hello ()V {
                    code: {
                        return-void
                    }
                }
                """, TestUtils.overlayOptions("top/level/OverlayExample"), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            assertEquals("top/level/OverlayExample", definition.getType().internalName());
            assertNotNull(definition.getMethod("hello", "()V"));
            assertNotNull(definition.getMethod("hello", "()V").getCode());
            assertEquals(1, definition.getMethod("hello", "()V").getCode().getInstructions().size());
        });
    }

    @Test
    void compilesBranchingAndTypeInstructions() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static branching (Ljava/lang/Object;[I)I {
                        registers: 5,
                        parameters: { value, array },
                        code: {
                        A:
                            check-cast value Ljava/lang/String;
                            instance-of v0 value Ljava/lang/String;
                            if-eqz v0 B
                            array-length v1 array
                            goto C
                        B:
                            new-instance v2 Ljava/lang/RuntimeException;
                            throw v2
                        C:
                            return v1
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            assertNotNull(definition.getMethod("branching", "(Ljava/lang/Object;[I)I"));
            assertNotNull(definition.getMethod("branching", "(Ljava/lang/Object;[I)I").getCode());
            assertEquals(11, definition.getMethod("branching", "(Ljava/lang/Object;[I)I").getCode().getInstructions().size());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> ctx = new PrintContext<>("\t");
            printer.print(ctx);

            String printed = TestUtils.normalize(ctx.toString());
            assertTrue(printed.contains("if-eqz"), printed);
            assertTrue(printed.contains("new-instance"), printed);
            assertTrue(printed.contains("throw"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void compilesBinaryOperationVariants() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static binary ()V {
                        code: {
                            const v0 1
                            const v1 2
                            add-int v2 v0 v1
                            add-int/2addr v2 v1
                            add-int/lit8 v2 v1 3
                            add-int/lit16 v2 v1 300
                            rsub-int v2 v1 300
                            return-void
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            var instructions = ((DalvikClassRepresentation) result.representation()).definition()
                    .getMethod("binary", "()V").getCode().getInstructions();
            assertTrue(instructions.stream().anyMatch(instruction -> instruction.opcode() == Opcodes.ADD_INT));
            assertTrue(instructions.stream().anyMatch(instruction -> instruction.opcode() == Opcodes.ADD_INT_2ADDR));
            assertTrue(instructions.stream().anyMatch(instruction -> instruction.opcode() == Opcodes.ADD_INT_LIT8));
            assertTrue(instructions.stream().anyMatch(instruction -> instruction.opcode() == Opcodes.ADD_INT_LIT16));
            assertTrue(instructions.stream().anyMatch(instruction -> instruction.opcode() == Opcodes.RSUB_INT));

            DalvikClassPrinter printer = new DalvikClassPrinter(
                    ((DalvikClassRepresentation) result.representation()).definition()
            );
            PrintContext<?> context = new PrintContext<>("\t");
            printer.print(context);
            String printed = TestUtils.normalize(context.toString());
            assertTrue(printed.contains("add-int/2addr"), printed);
            assertTrue(printed.contains("add-int/lit8"), printed);
            assertTrue(printed.contains("add-int/lit16"), printed);
            assertTrue(printed.contains("rsub-int"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void compilesVisibleAndInvisibleAnnotations() {
        TestUtils.processDalvik("""
                .visible-annotation demo/Marker {
                    enabled: true,
                    mode: .enum demo/Mode ON,
                    nested: .annotation demo/Nested { name: "nested" },
                    values: { "one", "two" }
                }
                .invisible-annotation demo/Hidden {
                    value: "secret"
                }
                .system-annotation demo/System {
                    flag: true
                }
                .super java/lang/Object
                .class public Example {
                    .visible-annotation demo/FieldMarker {
                        value: "field"
                    }
                    .field public value Ljava/lang/String;
                }
                """, TestUtils.options(), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            assertEquals(3, definition.getAnnotations().size());
            assertTrue(definition.getAnnotations().stream()
                    .anyMatch(annotation -> annotation.visibility() == me.darknet.dex.tree.definitions.annotation.Annotation.VISIBILITY_SYSTEM));
            assertEquals(1, definition.getField("value", "Ljava/lang/String;").getAnnotations().size());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> context = new PrintContext<>("\t");
            printer.print(context);
            String printed = TestUtils.normalize(context.toString());
            assertTrue(printed.contains(".visible-annotation demo/Marker"), printed);
            assertTrue(printed.contains(".invisible-annotation demo/Hidden"), printed);
            var reparsed = AssemblyParseFixture.processDeclarations("<test>", printed, DalvikTargetContext.INSTANCE);
            assertFalse(reparsed.hasErrors(), printed + "\n" + DiagnosticAssertions.formatErrors(reparsed.errors()));

            byte[] bytes = DalvikDexIO.write(definitionToDex(definition));
            ClassDefinition reread = DalvikDexIO.read(bytes).definitions().getFirst();
            assertEquals(3, reread.getAnnotations().size());
            assertEquals(1, reread.getField("value", "Ljava/lang/String;").getAnnotations().size());
        });
    }

    @Test
    void preservesDalvikRegisterWordsAndDebugLines() throws Exception {
        final DalvikClassResult[] compiled = new DalvikClassResult[1];
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static wide (J)J {
                        registers: 4,
                        parameters: { wide },
                        code: {
                            line 12
                            invoke-static/range { v2, v3 } java/lang/Math.abs (J)J
                            move-result-wide v0
                            line 13
                            return-wide v0
                        }
                    }
                }
                """, TestUtils.options(), result -> compiled[0] = result);

        ClassDefinition definition = ((DalvikClassRepresentation) compiled[0].representation()).definition();
        var method = definition.getMethod("wide", "(J)J");
        assertNotNull(method);
        assertEquals(2, method.getCode().getIn());
        assertEquals(2, method.getCode().getOut());
        assertEquals(4, method.getCode().getRegisters());
        assertNotNull(method.getCode().getDebugInfo());
        assertEquals(List.of("wide"), method.getCode().getDebugInfo().parameterNames());
        assertEquals(2, method.getCode().getDebugInfo().lineNumbers().size());

        byte[] encoded = DalvikDexIO.write(definitionToDex(definition));
        ClassDefinition reread = DalvikDexIO.read(encoded).definitions().getFirst();
        var rereadMethod = reread.getMethod("wide", "(J)J");
        assertEquals(2, rereadMethod.getCode().getIn());
        assertEquals(2, rereadMethod.getCode().getOut());
        assertEquals(4, rereadMethod.getCode().getRegisters());
        assertEquals(List.of("wide"), rereadMethod.getCode().getDebugInfo().parameterNames());
    }

    private static DexFile definitionToDex(ClassDefinition definition) {
        return new  DexFile(35, List.of(definition));
    }

    @Test
    void compilesDalvikClassMetadata() {
        TestUtils.processDalvik("""
                .signature "LExample$Inner;"
                .outer-class Example
                .outer-method make ()V
                .inner public { name: Inner, inner: Example$Inner, outer: Example }
                .system-annotation dalvik/annotation/MemberClasses { value: { Example$Inner$Child } }
                .super java/lang/Object
                .class public Example$Inner {
                }
                """, TestUtils.options(), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            assertEquals("LExample$Inner;", definition.getSignature());
            assertEquals("Example", definition.getEnclosingClass().internalName());
            assertEquals("make", definition.getEnclosingMethod().name());
            assertEquals("()V", definition.getEnclosingMethod().descriptor());
            assertEquals(1, definition.getInnerClasses().size());
            assertEquals(List.of("Example$Inner$Child"), definition.getMemberClasses().stream()
                    .map(InstanceType::internalName).toList());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> context = new PrintContext<>("\t");
            printer.print(context);
            String printed = TestUtils.normalize(context.toString());
            assertTrue(printed.contains(".outer-class Example"), printed);
            assertTrue(printed.contains(".outer-method make ()V"), printed);
            assertTrue(printed.contains(".inner public"), printed);
            TestUtils.assertParsesDalvik(printed);

            byte[] bytes = DalvikDexIO.write(definitionToDex(definition));
            ClassDefinition reread = DalvikDexIO.read(bytes).definitions().getFirst();
            assertEquals(definition.getSignature(), reread.getSignature());
            assertEquals(definition.getEnclosingClass(), reread.getEnclosingClass());
            assertEquals(definition.getEnclosingMethod(), reread.getEnclosingMethod());
            assertEquals(definition.getMemberClasses(), reread.getMemberClasses());
        });
    }

    @Test
    void compilesMethodHandleAndMethodTypeConstants() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static constants ()V {
                        code: {
                            const-method-handle v0 { invokestatic, java/lang/Math.abs, (I)I }
                            const-method-type v1 (I)V
                            return-void
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            var instructions = ((DalvikClassRepresentation) result.representation()).definition()
                    .getMethod("constants", "()V").getCode().getInstructions();
            assertTrue(instructions.stream().anyMatch(ConstMethodHandleInstruction.class::isInstance));
            assertTrue(instructions.stream().anyMatch(ConstMethodTypeInstruction.class::isInstance));
        });
    }

    @Test
    void preservesExplicitArrayPayloadWidth() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static arrays ()V {
                        code: {
                            fill-array-data v0 { width: 2, values: { 0x1, 0x2 } }
                            return-void
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            var instructions = ((DalvikClassRepresentation) result.representation()).definition()
                    .getMethod("arrays", "()V").getCode().getInstructions();
            FillArrayDataInstruction payload = instructions.stream()
                    .filter(FillArrayDataInstruction.class::isInstance)
                    .map(FillArrayDataInstruction.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(2, payload.elementSize());
            assertEquals(4, payload.data().length);

            DalvikClassPrinter printer = new DalvikClassPrinter(
                    ((DalvikClassRepresentation) result.representation()).definition()
            );
            PrintContext<?> context = new PrintContext<>("\t");
            printer.print(context);
            String printed = TestUtils.normalize(context.toString());
            assertTrue(printed.contains("width: 2"), printed);
            assertTrue(printed.contains("values: { 0x0001, 0x0002 }"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void compilesSwitchInstructions() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static switches ()V {
                        code: {
                            const v0 5
                            packed-switch v0 { first: 5, targets: { A, B } }
                            const v1 7
                            sparse-switch v1 { 7: C, 8: D }
                            goto D
                        A:
                            return-void
                        B:
                            return-void
                        C:
                            return-void
                        D:
                            return-void
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            var method = definition.getMethod("switches", "()V");
            assertNotNull(method);
            assertNotNull(method.getCode());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> ctx = new PrintContext<>("\t");
            printer.print(ctx);

            String printed = TestUtils.normalize(ctx.toString());
            assertTrue(printed.contains("packed-switch v0"), printed);
            assertTrue(printed.contains("sparse-switch v1"), printed);
            assertTrue(printed.contains("first: 5"), printed);
            assertTrue(printed.contains("7: C"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void compilesArrayPayloadsAndExceptionTables() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static arrays ([I)V {
                        registers: 5,
                        parameters: { array },
                        exceptions: {
                            { Start, End, Handler, java/lang/RuntimeException },
                            { Start, End, Handler2, java/lang/Exception },
                            { Start, End, Handler3, * }
                        },
                        code: {
                        Start:
                            fill-array-data array { 1, 2, 3 }
                            filled-new-array/range { v0, v2 } [I
                            move-result-object v3
                        End:
                            return-void
                        Handler:
                            move-exception v4
                            return-void
                        Handler2:
                            move-exception v4
                            return-void
                        Handler3:
                            move-exception v4
                            return-void
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            var method = definition.getMethod("arrays", "([I)V");
            assertNotNull(method);
            assertNotNull(method.getCode());
            assertEquals(1, method.getCode().tryCatch().size());
            assertEquals(3, method.getCode().tryCatch().getFirst().handlers().size());
            assertTrue(method.getCode().tryCatch().getFirst().handlers().getLast().isCatchAll());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> ctx = new PrintContext<>("\t");
            printer.print(ctx);

            String printed = TestUtils.normalize(ctx.toString());
            assertTrue(printed.contains("exceptions: { {"), printed);
            assertTrue(printed.contains("java/lang/RuntimeException"), printed);
            assertTrue(printed.contains("fill-array-data"), printed);
            assertTrue(printed.contains("width: 1"), printed);
            assertTrue(printed.contains("values: { 0x01, 0x02, 0x03 }"), printed);
            assertTrue(printed.contains("filled-new-array/range"), printed);
            assertTrue(printed.contains("[I"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void compilesInvokeCustomAndPrintsParserCompatibleOrder() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static custom (I)V {
                        registers: 3,
                        code: {
                            invoke-custom/range { v0, v2 } callsite (III)V ConstantBootstraps.nullConstant { "demo", Ljava/lang/String;, .member java/lang/System out Ljava/io/PrintStream; }
                            return-void
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            var method = definition.getMethod("custom", "(I)V");
            assertNotNull(method);
            assertNotNull(method.getCode());
            assertEquals(2, method.getCode().getInstructions().size());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> ctx = new PrintContext<>("\t");
            printer.print(ctx);

            String printed = TestUtils.normalize(ctx.toString());
            assertTrue(printed.contains("invoke-custom/range"), printed);
            assertTrue(printed.contains("callsite (III)V"), printed);
            assertTrue(printed.contains("ConstantBootstraps.nullConstant"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void rejectsInvokePolymorphicUntilBackendSupportExists() {
        Outcome<List<ASTElement>> astResult =
                AssemblyParseFixture.processDeclarations("<test>", """
                        .method public static test ()V {
                            code: {
                                invoke-polymorphic { v0 } java/lang/invoke/MethodHandle.invokeExact ([Ljava/lang/Object;)Ljava/lang/Object; ([Ljava/lang/Object;)Ljava/lang/Object;
                                return-void
                            }
                        }
                        """, DalvikTargetContext.INSTANCE);
        assertFalse(astResult.hasErrors(), DiagnosticAssertions.formatErrors(astResult.errors()));

        var unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(astResult.requireValue(), DalvikTargetContext.INSTANCE),
                "Failed to process Dalvik semantic unit"
        );
        Outcome<DalvikClassResult> compilation = new DalvikCompiler().compile(
                unit,
                TestUtils.overlayOptions("top/level/OverlayExample")
        );
        assertTrue(compilation.hasErrors(), "invoke-polymorphic should fail until the backend supports it");
        assertTrue(DiagnosticAssertions.formatErrors(compilation.errors()).contains("invoke-polymorphic is not supported"),
                DiagnosticAssertions.formatErrors(compilation.errors()));
    }

}

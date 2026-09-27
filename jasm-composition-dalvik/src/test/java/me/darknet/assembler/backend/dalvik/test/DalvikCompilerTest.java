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
    void compilesNestPermittedAndRecordMetadata() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .nest-host java/lang/Object
                .nest-member Example$Inner
                .permitted-subclass Example$Impl
                .visible-annotation demo/ComponentMarker {}
                .record-component name Ljava/lang/String;
                .class public final Example {
                }
                """, TestUtils.options(), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            assertEquals("java/lang/Object", definition.getNestHost().internalName());
            assertEquals("Example$Inner", definition.getNestMembers().getFirst().internalName());
            assertEquals("Example$Impl", definition.getPermittedSubclasses().getFirst().internalName());
            var component = definition.getRecordComponents().getFirst();
            assertEquals("name", component.name());
            assertEquals("Ljava/lang/String;", component.type().descriptor());
            assertEquals(1, component.annotations().size());
            assertEquals(me.darknet.dex.tree.definitions.annotation.Annotation.VISIBILITY_RUNTIME,
                    component.annotations().getFirst().visibility());
        });
    }

    @Test
    void compilesAnnotationDefaultOntoElementMethod() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public abstract interface Example {
                    .method public abstract value ()I {
                        default-value: 42
                    }
                }
                """, TestUtils.options(), result -> {
            var method = result.representation().definition().getMethod("value", "()I");
            assertNotNull(method, "Expected the annotation element method");
            assertEquals(new me.darknet.dex.tree.definitions.constant.IntConstant(42), method.getDefaultValue());
        });
    }

    @Test
    void compilesParameterAnnotationsWithPositionPreserved() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public abstract interface Example {
                    .method public abstract value (ILjava/lang/String;)V {
                        parameters: { first, second },
                        parameter-annotations: {
                            second: {
                                .visible-annotation java/lang/Deprecated {}
                            }
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            var method = result.representation().definition().getMethod("value", "(ILjava/lang/String;)V");
            assertNotNull(method, "Expected the annotated method");
            var annotations = method.getParameterAnnotations();
            assertEquals(2, annotations.size(), "Expected an entry per declared parameter");
            assertTrue(annotations.get(0).isEmpty(), "The unannotated first parameter keeps its position");
            assertEquals(1, annotations.get(1).size(), "The annotation belongs to the second parameter");
            assertEquals(me.darknet.dex.tree.definitions.annotation.Annotation.VISIBILITY_RUNTIME,
                    annotations.get(1).getFirst().visibility());
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
            assertTrue(printed.indexOf("7: C") < printed.indexOf("8: D"), printed);
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
    void compilesInvokePolymorphicWithSeparateDeclaredAndCallSitePrototypes() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static poly (Ljava/lang/Object;)I {
                        registers: 3,
                        parameters: { receiver },
                        code: {
                            invoke-polymorphic { receiver } java/lang/invoke/MethodHandle.invokeExact ([Ljava/lang/Object;)Ljava/lang/Object; (Ljava/lang/Object;)I
                            move-result v0
                            return v0
                        }
                    }
                    .method public static polyRange (Ljava/lang/Object;)I {
                        registers: 3,
                        parameters: { receiver },
                        code: {
                            invoke-polymorphic/range { receiver, receiver } java/lang/invoke/MethodHandle.invokeExact ([Ljava/lang/Object;)Ljava/lang/Object; (Ljava/lang/Object;)I
                            move-result v0
                            return v0
                        }
                    }
                }
                """, TestUtils.options(), result -> {
            ClassDefinition definition = result.representation().definition();
            var listMethod = definition.getMethod("poly", "(Ljava/lang/Object;)I");
            var rangeMethod = definition.getMethod("polyRange", "(Ljava/lang/Object;)I");
            assertNotNull(listMethod, "Expected the list-form method");
            assertNotNull(rangeMethod, "Expected the range-form method");

            var listInvoke = (me.darknet.dex.tree.definitions.instructions.InvokeInstruction)
                    listMethod.getCode().getInstructions().getFirst();
            assertEquals(Opcodes.INVOKE_POLYMORPHIC, listInvoke.opcode());
            assertEquals("([Ljava/lang/Object;)Ljava/lang/Object;", listInvoke.methodType().descriptor());
            assertEquals("(Ljava/lang/Object;)I", listInvoke.type().descriptor());
            assertEquals(4, listInvoke.unitSize());

            var rangeInvoke = (me.darknet.dex.tree.definitions.instructions.InvokeInstruction)
                    rangeMethod.getCode().getInstructions().getFirst();
            assertEquals(Opcodes.INVOKE_POLYMORPHIC, rangeInvoke.opcode(),
                    "The model uses the base opcode for either register encoding form");
            assertEquals("([Ljava/lang/Object;)Ljava/lang/Object;", rangeInvoke.methodType().descriptor());
            assertEquals("(Ljava/lang/Object;)I", rangeInvoke.type().descriptor());
            assertEquals(4, rangeInvoke.unitSize());
        });
    }

    @Test
    void rejectsMalformedInvokePolymorphicPrototypeDuringSemanticProcessing() {
        Outcome<List<ASTElement>> astResult = AssemblyParseFixture.processDeclarations("<test>", """
                .method public static test ()V {
                    code: {
                        invoke-polymorphic { v0 } java/lang/invoke/MethodHandle.invokeExact ([Ljava/lang/Object;)Ljava/lang/Object; notADescriptor
                        return-void
                    }
                }
                """, DalvikTargetContext.INSTANCE);
        assertFalse(astResult.hasErrors(), DiagnosticAssertions.formatErrors(astResult.errors()));

        var processed = SemanticProcessor.process(astResult.requireValue(), DalvikTargetContext.INSTANCE);
        DiagnosticAssertions.assertHasErrors(processed, "A malformed call-site prototype should be rejected");
        DiagnosticAssertions.assertPhase(processed.errors(), me.darknet.assembler.error.DiagnosticPhase.SEMANTIC_LOWERING,
                "Call-site prototypes are validated during semantic lowering");
    }

    @Test
    void roundTripsInvokePolymorphicThroughPrinting() {
        String source = """
                .super java/lang/Object
                .class public Example {
                    .method public static poly (Ljava/lang/Object;)I {
                        registers: 3,
                        parameters: { receiver },
                        code: {
                            invoke-polymorphic { receiver } java/lang/invoke/MethodHandle.invokeExact ([Ljava/lang/Object;)Ljava/lang/Object; (Ljava/lang/Object;)I
                            move-result v0
                            return v0
                        }
                    }
                }
                """;
        TestUtils.processDalvik(source, TestUtils.options(), result -> {
            PrintContext<?> ctx = new PrintContext<>(PrintContext.TAB_INDENT);
            new DalvikClassPrinter(result.representation().definition()).print(ctx);
            String printed = TestUtils.normalize(ctx.toString());
            assertTrue(printed.contains("invoke-polymorphic"), printed);
            assertTrue(printed.contains("([Ljava/lang/Object;)Ljava/lang/Object;"), printed);
            assertTrue(printed.contains("(Ljava/lang/Object;)I"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void roundTripsAnnotationDefaultValueThroughPrinting() {
        ClassDefinition definition = compileDefinition("""
                .super java/lang/Object
                .class public abstract interface Example {
                    .method public abstract value ()I {
                        default-value: 42
                    }
                }
                """);
        String printed = printDefinition(definition);
        assertTrue(printed.contains("default-value"), printed);
        assertTrue(printed.contains("42"), printed);
        TestUtils.assertParsesDalvik(printed);
        assertEquals(new me.darknet.dex.tree.definitions.constant.IntConstant(42),
                compileDefinition(printed).getMethod("value", "()I").getDefaultValue());
    }

    @Test
    void printsParameterAnnotationsWithTheirParameterNames() {
        ClassDefinition definition = compileDefinition("""
                .super java/lang/Object
                .class public abstract interface Example {
                    .method public abstract value (ILjava/lang/String;)V {
                        parameters: { first, second },
                        parameter-annotations: {
                            second: {
                                .visible-annotation java/lang/Deprecated {}
                            }
                        }
                    }
                }
                """);
        String printed = printDefinition(definition);
        assertTrue(printed.contains("parameter-annotations"), printed);
        assertTrue(printed.contains("second"), printed);
        TestUtils.assertParsesDalvik(printed);
        var reread = compileDefinition(printed).getMethod("value", "(ILjava/lang/String;)V");
        assertTrue(reread.getParameterAnnotations().get(0).isEmpty());
        assertEquals(1, reread.getParameterAnnotations().get(1).size());
    }

    @Test
    void printsNestPermittedAndRecordMetadata() {
        ClassDefinition definition = compileDefinition("""
                .super java/lang/Object
                .nest-host java/lang/Object
                .nest-member Example$Inner
                .permitted-subclass Example$Impl
                .visible-annotation demo/ComponentMarker {}
                .record-component name Ljava/lang/String;
                .class public final Example {
                }
                """);
        String printed = printDefinition(definition);
        assertTrue(printed.contains(".nest-host java/lang/Object"), printed);
        assertTrue(printed.contains(".nest-member Example$Inner"), printed);
        assertTrue(printed.contains(".permitted-subclass Example$Impl"), printed);
        assertTrue(printed.contains(".visible-annotation demo/ComponentMarker"), printed);
        assertTrue(printed.contains(".record-component name Ljava/lang/String;"), printed);
        TestUtils.assertParsesDalvik(printed);
        var component = compileDefinition(printed).getRecordComponents().getFirst();
        assertEquals(1, component.annotations().size());
    }

    @Test
    void printsStrictfpForTheStrictFlag() {
        ClassDefinition definition = compileDefinition("""
                .super java/lang/Object
                .class public Example {
                    .method public static strictfp m ()V {
                        registers: 0,
                        code: {
                            return-void
                        }
                    }
                }
                """);
        String printed = printDefinition(definition);
        assertTrue(printed.contains("strictfp"), printed);
        assertFalse(printed.matches("(?s).*\\bstrict\\b(?!fp).*"), printed);
        TestUtils.assertParsesDalvik(printed);
    }

    @Test
    void printsHandleKeywordsTheParserAccepts() {
        ClassDefinition definition = compileDefinition("""
                .super java/lang/Object
                .class public Example {
                    .method public static m ()V {
                        registers: 0,
                        code: {
                            invoke-custom { } callsite ()V { invokespecial, java/lang/Object.<init>, ()V } { }
                            return-void
                        }
                    }
                }
                """);
        String printed = printDefinition(definition);
        assertTrue(printed.contains("invokespecial"), printed);
        assertFalse(printed.contains("invokedirect"), printed);
        assertFalse(printed.contains("invokeconstructor"), printed);
        TestUtils.assertParsesDalvik(printed);
    }

    @Test
    void recoversSafeLocalNamesAndPreservesRegisterLayout() {
        ClassDefinition definition = compileDefinition("""
                .super java/lang/Object
                .class public Example {
                    .method public static named ()V {
                        registers: 2,
                        code: {
                            const v0 1
                            const v1 2
                            return-void
                        }
                    }
                }
                """);
        var method = definition.getMethod("named", "()V");
        var code = method.getCode();
        code.setDebugInfo(new me.darknet.dex.tree.definitions.debug.DebugInformation(List.of(), List.of(), List.of(
                local(0, "first"),
                local(0, "renamed"),
                local(1, "second")
        )));

        String printed = printDefinition(definition);
        assertTrue(printed.contains("const first 1"), printed);
        assertTrue(printed.contains("const second 2"), printed);
        assertFalse(printed.contains("renamed"), printed);
        var reread = compileDefinition(printed).getMethod("named", "()V").getCode();
        assertEquals(code.getRegisters(), reread.getRegisters());
        assertEquals(code.getIn(), reread.getIn());
        assertEquals(code.getOut(), reread.getOut());
        assertEquals(code.getInstructions(), reread.getInstructions());
    }

    @Test
    void fallsBackWhenFirstMentionWouldMoveALocalSlot() {
        ClassDefinition definition = compileDefinition("""
                .super java/lang/Object
                .class public Example {
                    .method public static moved ()V {
                        registers: 2,
                        code: {
                            const v1 1
                            const v0 2
                            return-void
                        }
                    }
                }
                """);
        definition.getMethod("moved", "()V").getCode().setDebugInfo(
                new me.darknet.dex.tree.definitions.debug.DebugInformation(List.of(), List.of(), List.of(
                        local(0, "zero"), local(1, "one")
                )));
        String printed = printDefinition(definition);
        assertTrue(printed.contains("const v1 1"), printed);
        assertTrue(printed.contains("const v0 2"), printed);
        assertFalse(printed.contains("const zero"), printed);
        assertFalse(printed.contains("const one"), printed);
        TestUtils.assertParsesDalvik(printed);
    }

    @Test
    void rejectsUnsafeAndDuplicateLocalNames() {
        ClassDefinition collision = compileDefinition("""
                .super java/lang/Object
                .class public Example {
                    .method public static collision (I)V {
                        registers: 2,
                        code: {
                            const v0 1
                            return-void
                        }
                    }
                }
                """);
        var method = collision.getMethod("collision", "(I)V");
        for (String name : List.of("v0", "p0")) {
            method.getCode().setDebugInfo(new me.darknet.dex.tree.definitions.debug.DebugInformation(
                    List.of(), List.of(), List.of(local(0, name))));
            String printed = printDefinition(collision);
            assertTrue(printed.contains("const v0 1"), printed);
            assertFalse(printed.contains("const p0 1"), printed);
        }

        ClassDefinition duplicate = compileDefinition("""
                .super java/lang/Object
                .class public Example {
                    .method public static duplicate ()V {
                        registers: 2,
                        code: {
                            const v0 1
                            const v1 2
                            return-void
                        }
                    }
                }
                """);
        duplicate.getMethod("duplicate", "()V").getCode().setDebugInfo(
                new me.darknet.dex.tree.definitions.debug.DebugInformation(List.of(), List.of(), List.of(
                        local(0, "same"), local(1, "same")
                )));
        String printed = printDefinition(duplicate);
        assertTrue(printed.contains("const v0 1"), printed);
        assertTrue(printed.contains("const v1 2"), printed);
        assertFalse(printed.contains("const same"), printed);
    }

    @Test
    void omitsUnknownAndPartialParameterNames() {
        ClassDefinition unknown = compileDefinition("""
                .super java/lang/Object
                .class public Example {
                    .method public static unknown (I)V {
                        registers: 2,
                        code: {
                            const v0 1
                            return-void
                        }
                    }
                }
                """);
        assertFalse(printDefinition(unknown).contains("parameters:"));

        ClassDefinition complete = compileDefinition("""
                .super java/lang/Object
                .class public Example {
                    .method public static complete (II)V {
                        registers: 2,
                        parameters: { left, right },
                        code: {
                            return-void
                        }
                    }
                }
                """);
        String printed = printDefinition(complete);
        assertTrue(printed.contains("parameters:"), printed);
        assertTrue(printed.contains("left"), printed);
        assertTrue(printed.contains("right"), printed);

        ClassDefinition partial = compileDefinition("""
                .super java/lang/Object
                .class public Example {
                    .method public static partial (II)V {
                        registers: 2,
                        code: {
                            return-void
                        }
                    }
                }
                """);
        partial.getMethod("partial", "(II)V").setParameterNames(List.of("left"));
        assertFalse(printDefinition(partial).contains("parameters:"));
    }

    private static me.darknet.dex.tree.definitions.debug.DebugInformation.LocalVariable local(int register, String name) {
        return new me.darknet.dex.tree.definitions.debug.DebugInformation.LocalVariable(
                register, name, null, null, null, null);
    }

    private static ClassDefinition compileDefinition(String source) {
        Outcome<List<ASTElement>> ast =
                AssemblyParseFixture.processDeclarations("<test>", source, DalvikTargetContext.INSTANCE);
        assertFalse(ast.hasErrors(), DiagnosticAssertions.formatErrors(ast.errors()));
        var unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(ast.requireValue(), DalvikTargetContext.INSTANCE),
                "Failed to process Dalvik source");
        Outcome<DalvikClassResult> result = new DalvikCompiler().compile(unit, TestUtils.options());
        assertFalse(result.hasErrors(), DiagnosticAssertions.formatErrors(result.errors()));
        return ((DalvikClassRepresentation) result.requireValue().representation()).definition();
    }

    private static String printDefinition(ClassDefinition definition) {
        PrintContext<?> context = new PrintContext<>("\t");
        new DalvikClassPrinter(definition).print(context);
        return TestUtils.normalize(context.toString());
    }

}

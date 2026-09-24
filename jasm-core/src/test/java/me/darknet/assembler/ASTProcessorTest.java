package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTInner;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.ast.specific.ASTRecordComponent;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.target.AnnotationCapabilities;
import me.darknet.assembler.target.AnnotationCapability;
import me.darknet.assembler.target.MethodAttributeRegistry;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.test.FixtureTarget;
import me.darknet.assembler.target.TargetContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class ASTProcessorTest {
    public static <T extends ASTElement> void assertOne(String input, Class<T> clazz, Consumer<T> consumer) {
        assertOne(input, FixtureTarget.JVM.context(), clazz, consumer);
    }

    private static <T extends ASTElement> void assertOne(
            String input,
            TargetContext target,
            Class<T> clazz,
            Consumer<T> consumer
    ) {
        List<ASTElement> results = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.processAst(AssemblyParseFixture.STDIN, input, target),
                "AST processing failed"
        );
        assertEquals(1, results.size(), "Expected exactly one AST node");
        consumer.accept(assertIs(clazz, results.getFirst()));
    }

    public static void assertError(String input, Consumer<List<Diagnostic>> errorConsumer) {
        Outcome<List<ASTElement>> result = AssemblyParseFixture.processAst(
                AssemblyParseFixture.STDIN,
                input,
                FixtureTarget.JVM.context()
        );
        DiagnosticAssertions.assertHasErrors(result, "Expected AST processing to fail");
        errorConsumer.accept(result.errors());
    }

    public static <T> @NotNull T assertIs(Class<T> shouldBe, Object is) {
        assertNotNull(is);
        return assertInstanceOf(shouldBe, is);
    }

    @Test
    public void testSimpleClass() {
        assertOne(".class public HelloWorld { .field public static final a I }", ASTClass.class, (clazz) -> {
            assertEquals("HelloWorld", clazz.getName().content());
            ASTField field = assertIs(ASTField.class, clazz.content(0));
            assertNotNull(field);
            assertEquals("a", field.getName().content());
            assertEquals("I", field.getDescriptor().content());
            List<ASTIdentifier> modifiers = field.getModifiers().getModifiers();
            assertEquals(3, modifiers.size());
            assertEquals("public", modifiers.get(0).content());
            assertEquals("static", modifiers.get(1).content());
            assertEquals("final", modifiers.get(2).content());
        });
    }

    @Test
    public void testInvalidField() {
        assertError(".field privlick", (errors) -> assertEquals(1, errors.size()));
        assertError(".field privlick name I", (errors) -> assertEquals(1, errors.size()));
        assertError(".field privlick name I {value: identifier}", (errors) -> assertEquals(1, errors.size()));
        assertError(".field privlick name I {value: 10}", (errors) -> assertEquals(1, errors.size()));
    }

    @Test
    public void testField() {
        assertOne(".field public static final a I", ASTField.class, (field) -> {
            assertEquals("a", field.getName().content());
            assertEquals("I", field.getDescriptor().content());
            List<ASTIdentifier> modifiers = field.getModifiers().getModifiers();
            assertEquals(3, modifiers.size());
            assertEquals("public", modifiers.get(0).content());
            assertEquals("static", modifiers.get(1).content());
            assertEquals("final", modifiers.get(2).content());
        });
        assertOne(".field public static final a I {value: 10}", ASTField.class, (field) -> {
            assertEquals("a", field.getName().content());
            assertEquals("I", field.getDescriptor().content());
            List<ASTIdentifier> modifiers = field.getModifiers().getModifiers();
            assertEquals(3, modifiers.size());
            assertEquals("public", modifiers.get(0).content());
            assertEquals("static", modifiers.get(1).content());
            assertEquals("final", modifiers.get(2).content());
            assertNotNull(field.getFieldValue());
            assertEquals("10", field.getFieldValue().content());
        });
    }

    @Test
    public void testMethod() {
        assertOne(".method public static main ([Ljava/lang/String;)V {}", ASTMethod.class, (method) -> {
            assertEquals("main", method.getName().content());
            assertEquals("([Ljava/lang/String;)V", method.getDescriptor().content());
        });
        assertOne(
                ".method public static main ([Ljava/lang/String;)V { parameters: {args} }", ASTMethod.class,
                (method) -> {
                    assertEquals("main", method.getName().content());
                    assertEquals("([Ljava/lang/String;)V", method.getDescriptor().content());
                    assertNotNull(method.getParameters());
                    assertEquals(1, method.getParameters().size());
                    assertEquals("args", method.getParameters().getFirst().content());
                }
        );
    }

    @Test
    public void testAnnotation() {
        assertOne(".annotation java/lang/Deprecated {}", ASTAnnotation.class, (annotation) -> {
            assertEquals(AnnotationVisibility.VISIBLE, annotation.getVisibility());
            assertEquals("java/lang/Deprecated", annotation.getClassType().content());
        });
        assertOne(
                ".system-annotation java/lang/Deprecated {}",
                FixtureTarget.DALVIK.context(),
                ASTAnnotation.class,
                annotation -> assertEquals(AnnotationVisibility.SYSTEM, annotation.getVisibility())
        );
        assertOne(".annotation java/lang/Deprecated { value: \"Hello World\" }", ASTAnnotation.class, (annotation) -> {
            assertEquals("java/lang/Deprecated", annotation.getClassType().content());
            assertNotNull(annotation.getValueMap());
            assertEquals(1, annotation.getValueMap().size());
            assertEquals("Hello World", annotation.getValue("value").content());
        });
        assertOne(
                ".annotation java/lang/annotation/Retention { value: .enum java/lang/annotation/RetentionPolicy"
                        + " RUNTIME }",
                ASTAnnotation.class, (annotation) -> {
                    assertEquals("java/lang/annotation/Retention", annotation.getClassType().content());
                    assertNotNull(annotation.getValueMap());
                    assertEquals(1, annotation.getValueMap().size());
                    ASTEnum enumValue = assertIs(ASTEnum.class, annotation.getValue("value"));
                    assertEquals("java/lang/annotation/RetentionPolicy", enumValue.enumOwner().content());
                    assertEquals("RUNTIME", enumValue.enumFieldName().content());
                }
        );
        assertOne(
                ".annotation java/lang/annotation/Target { value: { .enum java/lang/annotation/ElementType FIELD,"
                        + " .enum java/lang/annotation/ElementType METHOD } }",
                ASTAnnotation.class, (annotation) -> {
                    assertEquals("java/lang/annotation/Target", annotation.getClassType().content());
                    assertNotNull(annotation.getValueMap());
                    assertEquals(1, annotation.getValueMap().size());
                    ASTArray array = assertIs(ASTArray.class, annotation.getValue("value"));
                    assertEquals(2, array.values().size());
                    ASTEnum enumValue = assertIs(ASTEnum.class, array.value(0));
                    assertEquals("java/lang/annotation/ElementType", enumValue.enumOwner().content());
                    assertEquals("FIELD", enumValue.enumFieldName().content());
                    enumValue = assertIs(ASTEnum.class, array.value(1));
                    assertEquals("java/lang/annotation/ElementType", enumValue.enumOwner().content());
                    assertEquals("METHOD", enumValue.enumFieldName().content());
                }
        );
    }

    @Test
    public void testSubAnnotation() {
        assertOne(
                ".annotation java/lang/annotation/Annotation { value: .annotation java/lang/annotation/Annotation { value: 100 } }",
                ASTAnnotation.class, (annotation) -> {
                    assertEquals(AnnotationVisibility.VISIBLE, annotation.getVisibility());
                    assertEquals("java/lang/annotation/Annotation", annotation.getClassType().content());
                    assertNotNull(annotation.getValueMap());
                    assertEquals(1, annotation.getValueMap().size());
                    ASTAnnotation subAnnotation = assertIs(ASTAnnotation.class, annotation.getValue("value"));
                    assertEquals(AnnotationVisibility.VISIBLE, subAnnotation.getVisibility());
                    assertEquals("java/lang/annotation/Annotation", subAnnotation.getClassType().content());
                    assertNotNull(subAnnotation.getValueMap());
                    assertEquals(1, subAnnotation.getValueMap().size());
                    assertEquals("100", subAnnotation.getValue("value").content());
                }
        );

        assertOne(
                ".annotation me/darknet/assembler/PrinterTest$TestAnnotation { " + " number: 15, "
                        + " notNull: .annotation org/jetbrains/annotations/NotNull {}, "
                        + " values: { \"Hello, world!\", \"Hello, world!\" }, " + " value: \"Hello, world!\" " + "}",
                ASTAnnotation.class, (annotation) -> {
                    assertEquals("me/darknet/assembler/PrinterTest$TestAnnotation", annotation.getClassType().content());
                    assertNotNull(annotation.getValueMap());
                    assertEquals(4, annotation.getValueMap().size());
                    assertEquals("15", annotation.getValue("number").content());
                    ASTAnnotation subAnnotation = assertIs(ASTAnnotation.class, annotation.getValue("notNull"));
                    assertEquals("org/jetbrains/annotations/NotNull", subAnnotation.getClassType().content());
                    assertNotNull(subAnnotation.getValueMap());
                    assertEquals(0, subAnnotation.getValueMap().size());
                    ASTArray array = assertIs(ASTArray.class, annotation.getValue("values"));
                    assertEquals(2, array.values().size());
                    // assert that all elements are not null
                    for (ASTElement element : array.values()) {
                        assertNotNull(element);
                    }
                    assertEquals("Hello, world!", array.value(0).content());
                    assertEquals("Hello, world!", array.value(1).content());
                    assertEquals("Hello, world!", annotation.getValue("value").content());
                }
        );
    }

    @Test
    void parsesArrayDefaultAnnotationValues() {
        ASTProcessorTest.assertOne(
                ".method public abstract array ()[I { parameters: { this }, default-value: { 0, 1, 2 } }",
                ASTMethod.class,
                method -> {
                    ASTArray value = assertInstanceOf(ASTArray.class, method.getAnnotationDefaultValue());
                    assertEquals(3, value.values().size());
                    assertEquals("0", value.values().get(0).content());
                    assertEquals("1", value.values().get(1).content());
                    assertEquals("2", value.values().get(2).content());
                }
        );
    }

    @Test
    void parsesNestedAnnotationDefaultValues() {
        ASTProcessorTest.assertOne(
                ".method public abstract subanno ()Ljava/lang/annotation/Retention; {" +
                        " parameters: { this }," +
                        " default-value: .annotation java/lang/annotation/Retention {" +
                        "  value: .enum java/lang/annotation/RetentionPolicy CLASS" +
                        " }" +
                        "}",
                ASTMethod.class,
                method -> {
                    ASTDeclaration value = assertInstanceOf(ASTDeclaration.class, method.getAnnotationDefaultValue());
                    assertEquals(".annotation", value.keyword().content());
                    assertEquals("java/lang/annotation/Retention", value.element(0).content());
                    assertNotNull(value.element(1));
                }
        );
    }

    @Test
    void attachesDeprecatedAndSourceDebugExtensionToClass() {
        ASTClass clazz = onlyProcessed(
                ".deprecated " +
                        ".source-debug-extension \"SMAP\\nExample\" " +
                        ".class public Example {}",
                ASTClass.class
        );

        assertTrue(clazz.isDeprecated());
        assertEquals("SMAP\nExample", clazz.getSourceDebugExtension().content());
    }

    @Test
    void attachesClassLevelAttributesToClass() {
        ASTClass clazz = onlyProcessed(
                ".sourcefile \"Example.java\" " +
                        ".outer-class pkg/Outer " +
                        ".outer-method run ()V " +
                        ".nest-host pkg/Outer " +
                        ".nest-member pkg/Outer$Inner " +
                        ".permitted-subclass pkg/Sub " +
                        ".implements java/io/Serializable " +
                        ".super java/lang/Object " +
                        ".class public Example {}",
                ASTClass.class
        );

        assertEquals("Example.java", clazz.getSourceFile().content());
        assertEquals("pkg/Outer", clazz.getOuterClass().content());
        assertEquals("run", clazz.getOuterMethod().getMethodName().content());
        assertEquals("()V", clazz.getOuterMethod().getMethodDesc().content());
        assertEquals("pkg/Outer", clazz.getNestHost().content());
        assertEquals(List.of("pkg/Outer$Inner"), clazz.getNestMembers().stream().map(ASTIdentifier::content).toList());
        assertEquals(List.of("pkg/Sub"), clazz.getPermittedSubclasses().stream().map(ASTIdentifier::content).toList());
        assertEquals(List.of("java/io/Serializable"), clazz.getInterfaces().stream().map(ASTIdentifier::content).toList());
        assertEquals("java/lang/Object", clazz.getSuperName().content());
    }

    @Test
    void attachesDeprecatedToFieldAndMethodAndParsesDeclaredThrows() {
        ASTClass clazz = onlyProcessed(
                """
                .class public Example {
                    .deprecated
                    .field public value I
                    .deprecated
                    .method public work ()V {
                        throws: { java/lang/Exception, java/io/IOException },
                        exceptions: { { Start, End, Handler, java/lang/RuntimeException } },
                        code: {
                            Start:
                            goto Handler
                            End:
                            return
                            Handler:
                            athrow
                        }
                    }
                }
                """,
                ASTClass.class
        );

        ASTField field = assertInstanceOf(ASTField.class, clazz.content(0));
        ASTMethod method = assertInstanceOf(ASTMethod.class, clazz.content(1));

        assertTrue(field.isDeprecated());
        assertTrue(method.isDeprecated());
        assertEquals(
                List.of("java/lang/Exception", "java/io/IOException"),
                method.getDeclaredExceptions().stream().map(ASTIdentifier::content).toList()
        );
        assertEquals(1, method.getExceptionHandlers().size(), "try/catch exceptions should remain separate");
    }

    @Test
    void recordComponentsConsumeOnlyImmediatelyPrecedingGenericAttributes() {
        ASTClass clazz = onlyProcessed(
                ".visible-annotation pkg/ComponentAnno {} " +
                        ".signature \"RC\" " +
                        ".record-component value Ljava/lang/String; " +
                        ".visible-annotation pkg/ClassAnno {} " +
                        ".signature \"CSig\" " +
                        ".inner public { name: Inner, inner: Example$Inner, outer: Example } " +
                        ".class public Example {}",
                ASTClass.class
        );

        assertEquals("CSig", clazz.getSignature().content());
        assertEquals(1, clazz.getVisibleAnnotations().size());
        assertEquals(AnnotationVisibility.VISIBLE, clazz.getVisibleAnnotations().getFirst().getVisibility());
        assertEquals("pkg/ClassAnno", clazz.getVisibleAnnotations().getFirst().getClassType().content());

        List<ASTRecordComponent> components = clazz.getRecordComponents();
        assertEquals(1, components.size());
        ASTRecordComponent component = components.getFirst();
        assertEquals("value", component.getComponentType().content());
        assertEquals("Ljava/lang/String;", component.getComponentDescriptor().content());
        assertEquals("RC", component.getSignature().content());
        assertEquals(1, component.getVisibleAnnotations().size());
        assertEquals(AnnotationVisibility.VISIBLE, component.getVisibleAnnotations().getFirst().getVisibility());
        assertEquals("pkg/ComponentAnno", component.getVisibleAnnotations().getFirst().getClassType().content());

        List<ASTInner> inners = clazz.getInners();
        assertEquals(1, inners.size());
        ASTInner inner = inners.getFirst();
        assertEquals("Inner", inner.name().content());
        assertEquals("Example$Inner", inner.innerClass().content());
        assertEquals("Example", inner.outerClass().content());
    }

    @Test
    void parsesParameterAnnotationsUsingPrinterShape() {
        ASTMethod method = onlyProcessed(
                ".method public test (Ljava/lang/String;I)V {" +
                        " parameters: { this, name, count }," +
                        " parameter-annotations: {" +
                        "  name: { .visible-annotation Visible { value: \"a\" } }," +
                        "  count: { .invisible-annotation Hidden { value: \"b\" } }" +
                        " }" +
                        "}",
                ASTMethod.class
        );

        assertEquals(List.of("this", "name", "count"), method.getParameters().stream().map(ASTIdentifier::content).toList());
        assertEquals(2, method.getParameterAnnotations().size());

        ASTAnnotation visible = findParameterAnnotation(method.getParameterAnnotations(), "name");
        ASTAnnotation invisible = findParameterAnnotation(method.getParameterAnnotations(), "count");

        assertNotNull(visible);
        assertNotNull(invisible);
        assertEquals(AnnotationVisibility.VISIBLE, visible.getVisibility());
        assertEquals(AnnotationVisibility.INVISIBLE, invisible.getVisibility());
        assertEquals("Visible", visible.getClassType().content());
        assertEquals("Hidden", invisible.getClassType().content());
        assertEquals("a", visible.getValue("value").content());
        assertEquals("b", invisible.getValue("value").content());
    }

    @Test
    void rejectsMalformedParameterAnnotationShapes() {
        Outcome<List<ASTElement>> result = AssemblyParseFixture.processAst(
                AssemblyParseFixture.STDIN,
                ".method public broken ()V {" +
                        " parameters: { this }," +
                        " parameter-annotations: {" +
                        "  this: nope," +
                        "  other: { \"bad\" }" +
                        " }" +
                        "}",
                FixtureTarget.JVM.context()
        );

        DiagnosticAssertions.assertHasError(result, DiagnosticCode.PAYLOAD_SHAPE,
                "Malformed parameter annotations should report their payload shape");
        DiagnosticAssertions.assertPhase(result.diagnostics(), DiagnosticPhase.TARGET_VALIDATION,
                "Malformed parameter annotations should be target-validation diagnostics");
        String errors = DiagnosticAssertions.formatDiagnostics(result.errors());
        assertTrue(errors.contains("parameter annotation list"), errors);
        assertTrue(errors.contains("parameter annotation"), errors);
    }

    @Test
    void parsesTypeAnnotationsThroughExtractedAnnotationParser() {
        ASTField field = onlyProcessed(
                ".type-visible-annotation TypeVisible { location: { ref: 0, path: ROOT }, values: { value: \"yes\" } } " +
                        ".type-invisible-annotation TypeHidden { location: { ref: 1, path: LEAF }, values: {} } " +
                        ".field public value I",
                ASTField.class
        );

        assertEquals(1, field.getVisibleTypeAnnotations().size());
        assertEquals(1, field.getInvisibleTypeAnnotations().size());

        ASTAnnotation visible = field.getVisibleTypeAnnotations().getFirst();
        ASTAnnotation invisible = field.getInvisibleTypeAnnotations().getFirst();
        assertEquals(AnnotationVisibility.VISIBLE, visible.getVisibility());
        assertEquals(AnnotationVisibility.INVISIBLE, invisible.getVisibility());
        assertTrue(visible.isTypeAnnotation());
        assertTrue(invisible.isTypeAnnotation());
        assertEquals("TypeVisible", visible.getClassType().content());
        assertEquals("TypeHidden", invisible.getClassType().content());
        assertEquals("0", visible.getTypeRef().content());
        assertEquals("ROOT", visible.getTypePath().content());
        assertEquals("1", invisible.getTypeRef().content());
        assertEquals("LEAF", invisible.getTypePath().content());
    }

    @Test
    void attachesClassVersionAndRetainsItsSourceOwnership() {
        ASTClass clazz = onlyProcessed(".version 21 .class public Example {}", ASTClass.class);
        ASTNumber version = clazz.getVersion();
        assertNotNull(version);
        assertEquals(21, version.asInt());
        assertSame(clazz, version.parent());
        assertTrue(clazz.children().contains(version));
        for (String boundary : List.of("1", "211")) {
            ASTClass boundaryClass = onlyProcessed(
                    ".version " + boundary + " .class public Example {}",
                    ASTClass.class
            );
            assertEquals(boundary, boundaryClass.getVersion().content());
        }
    }

    @Test
    void rejectsClassVersionsOutsideTheSupportedRange() {
        for (String version : List.of("0", "1.5", "212", "300")) {
            Outcome<List<ASTElement>> result = AssemblyParseFixture.processAst(
                    AssemblyParseFixture.STDIN,
                    ".version " + version + " .class public Example {}",
                    FixtureTarget.JVM.context()
            );
            DiagnosticAssertions.assertHasError(result, DiagnosticCode.MALFORMED_DECLARATION,
                    "Out-of-range class version should be rejected: " + version);
            DiagnosticAssertions.assertPhase(result.diagnostics(), DiagnosticPhase.TARGET_VALIDATION,
                    "Class version validation should be target validation: " + version);
        }
    }

    @Test
    void reportsUnattachedAttributesAtTheirSourceValues() {
        String source = ".version 21";
        Outcome<List<ASTElement>> result = AssemblyParseFixture.processAst(
                AssemblyParseFixture.STDIN, source, FixtureTarget.JVM.context()
        );
        DiagnosticAssertions.assertHasError(result, DiagnosticCode.MALFORMED_DECLARATION,
                "A class attribute without a class should be rejected");
        Diagnostic diagnostic = result.errors().getFirst();
        assertEquals(source.indexOf("21") + 1, diagnostic.location().column());
    }

    @Test
    void rejectsDescriptorsWithTheWrongFormAtTheSourceBoundary() {
        Outcome<List<ASTElement>> result = AssemblyParseFixture.processAst(
                AssemblyParseFixture.STDIN,
                ".field public value Ljava/lang/String",
                FixtureTarget.JVM.context()
        );
        DiagnosticAssertions.assertHasError(result, DiagnosticCode.INVALID_DESCRIPTOR,
                "A field descriptor missing its terminator should be rejected");
        DiagnosticAssertions.assertPhase(result.diagnostics(), DiagnosticPhase.TARGET_VALIDATION,
                "Descriptor validation should happen during target validation");
    }

    @Test
    void rejectsDescriptorShapedClassReferencesAtTheirSourceTokens() {
        List<String> sources = List.of(
                ".class public Lpkg/Example; {}",
                ".class public Example { .super Lpkg/Parent; }",
                ".class public Example { .implements Lpkg/Interface; }",
                ".class public Example { .permitted-subclass Lpkg/Child; }",
                ".method public test ()V { throws: { Ljava/lang/Exception; } }"
        );
        List<String> invalidNames = List.of(
                "Lpkg/Example;", "Lpkg/Parent;", "Lpkg/Interface;", "Lpkg/Child;", "Ljava/lang/Exception;"
        );
        for (int index = 0; index < sources.size(); index++) {
            String source = sources.get(index);
            String invalidName = invalidNames.get(index);
            Outcome<List<ASTElement>> result = AssemblyParseFixture.processAst(
                    AssemblyParseFixture.STDIN, source, FixtureTarget.JVM.context()
            );
            DiagnosticAssertions.assertHasError(result, DiagnosticCode.INVALID_DESCRIPTOR,
                    "Descriptor-shaped class reference should be rejected: " + invalidName);
            Diagnostic diagnostic = result.errors().stream()
                    .filter(error -> error.code() == DiagnosticCode.INVALID_DESCRIPTOR)
                    .findFirst()
                    .orElseThrow();
            assertEquals(invalidName.length(), diagnostic.location().length());
            assertEquals(source.indexOf(invalidName) + 1, diagnostic.location().column());
        }
    }

    @Test
    void rejectsMalformedMethodDescriptorsAtTheirSourceTokens() {
        String source = ".method public broken (Ljava/lang/String)V {}";
        Outcome<List<ASTElement>> result = AssemblyParseFixture.processAst(
                AssemblyParseFixture.STDIN, source, FixtureTarget.JVM.context()
        );
        DiagnosticAssertions.assertHasError(result, DiagnosticCode.INVALID_DESCRIPTOR,
                "Malformed method descriptor should be rejected");
        Diagnostic diagnostic = result.errors().stream()
                .filter(error -> error.code() == DiagnosticCode.INVALID_DESCRIPTOR)
                .findFirst()
                .orElseThrow();
        assertEquals("(Ljava/lang/String)V".length(), diagnostic.location().length());
        assertEquals(source.indexOf("(Ljava/lang/String)V") + 1, diagnostic.location().column());
    }

    @Test
    void retainsDalvikMethodAttributesButRejectsThemForJvm() {
        ASTMethod dalvikMethod = onlyProcessed(
                ".method public test ()V { registers: 2 }",
                FixtureTarget.DALVIK.context(),
                ASTMethod.class
        );
        assertEquals(1, dalvikMethod.getMethodAttributes().size());
        assertEquals("registers", dalvikMethod.getMethodAttributes().key(0).content());
        assertEquals("2", dalvikMethod.getMethodAttributes().get(0).content());
        assertSame(dalvikMethod, dalvikMethod.getMethodAttributes().get(0).parent());

        Outcome<List<ASTElement>> jvmResult = AssemblyParseFixture.processAst(
                AssemblyParseFixture.STDIN,
                ".method public test ()V { registers: 2 }",
                FixtureTarget.JVM.context()
        );
        DiagnosticAssertions.assertHasError(jvmResult, DiagnosticCode.UNSUPPORTED_FORM,
                "JVM should reject the Dalvik-only registers method attribute");
        DiagnosticAssertions.assertPhase(jvmResult.diagnostics(), DiagnosticPhase.TARGET_VALIDATION,
                "Method attribute rejection should be target validation");
    }

    @Test
    void reportsDeniedAnnotationCapabilities() {
        Outcome<List<ASTElement>> systemResult = AssemblyParseFixture.processAst(
                AssemblyParseFixture.STDIN,
                ".system-annotation pkg/System {} .class public Example {}",
                FixtureTarget.JVM.context()
        );
        DiagnosticAssertions.assertHasError(systemResult, DiagnosticCode.UNSUPPORTED_CAPABILITY,
                "JVM should reject system annotations");

        Outcome<List<ASTElement>> typeResult = AssemblyParseFixture.processAst(
                AssemblyParseFixture.STDIN,
                ".type-visible-annotation pkg/Type {} .field public value I",
                FixtureTarget.DALVIK.context()
        );
        DiagnosticAssertions.assertHasError(typeResult, DiagnosticCode.UNSUPPORTED_CAPABILITY,
                "Dalvik fixture should reject type annotations");

        for (var denied : List.of(
                new CapabilityCase(AnnotationCapability.ANNOTATION_DEFAULT_VALUES,
                        ".method public broken ()V { default-value: { invalid: true } }"),
                new CapabilityCase(AnnotationCapability.PARAMETER_ANNOTATIONS,
                        ".method public broken ()V { parameters: { p }, parameter-annotations: { p: { \"bad\" } } }"))) {
            Outcome<List<ASTElement>> result = AssemblyParseFixture.processAst(
                    AssemblyParseFixture.STDIN, denied.source(), targetWithout(denied.capability())
            );
            DiagnosticAssertions.assertHasError(result, DiagnosticCode.UNSUPPORTED_CAPABILITY,
                    "Target should reject " + denied.capability().displayName());
            assertEquals(1, result.errors().size(),
                    "A denied capability must stop validation of its unsupported payload");
        }
    }

    @Test
    void preservesInstructionSourceWithoutSemanticVerification() {
        ASTMethod method = onlyProcessed(
                ".method public test ()V { code: {\n" +
                        " return invalid\n" +
                        " not-a-target-instruction invalid\n" +
                        " } }",
                ASTMethod.class
        );
        ASTCode code = method.getCode();
        assertNotNull(code);
        assertEquals(2, code.getInstructions().size());
        ASTInstruction knownInstruction = code.getInstructions().get(0);
        assertEquals("return", knownInstruction.identifier().content());
        assertEquals("invalid", knownInstruction.argument(0).content());
        ASTInstruction unknownInstruction = code.getInstructions().get(1);
        assertEquals("not-a-target-instruction", unknownInstruction.identifier().content());
        assertEquals("invalid", unknownInstruction.argument(0).content());
    }

    private static <T extends ASTElement> T onlyProcessed(String input, Class<T> type) {
        return onlyProcessed(input, FixtureTarget.JVM.context(), type);
    }

    private static <T extends ASTElement> T onlyProcessed(String input, TargetContext target, Class<T> type) {
        List<ASTElement> results = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.processAst(AssemblyParseFixture.STDIN, input, target),
                "AST processing failed"
        );
        assertEquals(1, results.size(), "Expected a single processed AST node");
        return assertInstanceOf(type, results.getFirst());
    }

    private static TargetContext targetWithout(AnnotationCapability denied) {
        TargetContext delegate = FixtureTarget.JVM.context();
        return new TargetContext() {
            @Override
            public Instructions<?> instructions() {
                return delegate.instructions();
            }

            @Override
            public MethodAttributeRegistry methodAttributes() {
                return delegate.methodAttributes();
            }

            @Override
            public AnnotationCapabilities annotationCapabilities() {
                return capability -> capability != denied
                        && delegate.annotationCapabilities().supports(capability);
            }
        };
    }

    private record CapabilityCase(AnnotationCapability capability, String source) {}

    private static ASTAnnotation findParameterAnnotation(Map<ASTIdentifier, List<ASTAnnotation>> annotations, String parameterName) {
        for (var entry : annotations.entrySet()) {
            if (entry.getKey().content().equals(parameterName) && !entry.getValue().isEmpty()) {
                return entry.getValue().getFirst();
            }
        }
        return null;
    }
}

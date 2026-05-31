package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTInner;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.ast.specific.ASTRecordComponent;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.AstAssertions;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class ASTProcessorTest {
    public static <T extends ASTElement> void assertOne(String input, Class<T> clazz, Consumer<T> consumer) {
        AstAssertions.assertOneProcessed(input, clazz, consumer);
    }

    public static void assertError(String input, Consumer<List<Error>> errorConsumer) {
        AstAssertions.assertProcessedError(input, errorConsumer);
    }

    public static <T> @NotNull T assertIs(Class<T> shouldBe, Object is) {
        return AstAssertions.assertIs(shouldBe, is);
    }

    @Test
    public void testSimpleClass() {
        assertOne(".class public HelloWorld { .field public static final a I }", ASTClass.class, (clazz) -> {
            assertEquals("HelloWorld", clazz.getName().content());
            ASTField field = assertIs(ASTField.class, clazz.content(0));
            assertNotNull(field);
            assertEquals("a", field.getName().content());
            assertEquals("I", field.getDescriptor().content());
            List<ASTIdentifier> modifiers = field.getModifiers().modifiers();
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
            List<ASTIdentifier> modifiers = field.getModifiers().modifiers();
            assertEquals(3, modifiers.size());
            assertEquals("public", modifiers.get(0).content());
            assertEquals("static", modifiers.get(1).content());
            assertEquals("final", modifiers.get(2).content());
        });
        assertOne(".field public static final a I {value: 10}", ASTField.class, (field) -> {
            assertEquals("a", field.getName().content());
            assertEquals("I", field.getDescriptor().content());
            List<ASTIdentifier> modifiers = field.getModifiers().modifiers();
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
                    assertNotNull(method.parameters());
                    assertEquals(1, method.parameters().size());
                    assertEquals("args", method.parameters().getFirst().content());
                }
        );
    }

    @Test
    public void testAnnotation() {
        assertOne(".annotation java/lang/Deprecated {}", ASTAnnotation.class, (annotation) -> {
            assertEquals("java/lang/Deprecated", annotation.classType().content());
        });
        assertOne(".annotation java/lang/Deprecated { value: \"Hello World\" }", ASTAnnotation.class, (annotation) -> {
            assertEquals("java/lang/Deprecated", annotation.classType().content());
            assertNotNull(annotation.values());
            assertEquals(1, annotation.values().size());
            assertEquals("Hello World", annotation.value("value").content());
        });
        assertOne(
                ".annotation java/lang/annotation/Retention { value: .enum java/lang/annotation/RetentionPolicy"
                        + " RUNTIME }",
                ASTAnnotation.class, (annotation) -> {
                    assertEquals("java/lang/annotation/Retention", annotation.classType().content());
                    assertNotNull(annotation.values());
                    assertEquals(1, annotation.values().size());
                    ASTEnum enumValue = assertIs(ASTEnum.class, annotation.value("value"));
                    assertEquals("java/lang/annotation/RetentionPolicy", enumValue.enumOwner().content());
                    assertEquals("RUNTIME", enumValue.enumFieldName().content());
                }
        );
        assertOne(
                ".annotation java/lang/annotation/Target { value: { .enum java/lang/annotation/ElementType FIELD,"
                        + " .enum java/lang/annotation/ElementType METHOD } }",
                ASTAnnotation.class, (annotation) -> {
                    assertEquals("java/lang/annotation/Target", annotation.classType().content());
                    assertNotNull(annotation.values());
                    assertEquals(1, annotation.values().size());
                    ASTArray array = assertIs(ASTArray.class, annotation.value("value"));
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
                    assertEquals("java/lang/annotation/Annotation", annotation.classType().content());
                    assertNotNull(annotation.values());
                    assertEquals(1, annotation.values().size());
                    ASTAnnotation subAnnotation = assertIs(ASTAnnotation.class, annotation.value("value"));
                    assertEquals("java/lang/annotation/Annotation", subAnnotation.classType().content());
                    assertNotNull(subAnnotation.values());
                    assertEquals(1, subAnnotation.values().size());
                    assertEquals("100", subAnnotation.value("value").content());
                }
        );

        assertOne(
                ".annotation me/darknet/assembler/PrinterTest$TestAnnotation { " + " number: 15, "
                        + " notNull: .annotation org/jetbrains/annotations/NotNull {}, "
                        + " values: { \"Hello, world!\", \"Hello, world!\" }, " + " value: \"Hello, world!\" " + "}",
                ASTAnnotation.class, (annotation) -> {
                    assertEquals("me/darknet/assembler/PrinterTest$TestAnnotation", annotation.classType().content());
                    assertNotNull(annotation.values());
                    assertEquals(4, annotation.values().size());
                    assertEquals("15", annotation.value("number").content());
                    ASTAnnotation subAnnotation = assertIs(ASTAnnotation.class, annotation.value("notNull"));
                    assertEquals("org/jetbrains/annotations/NotNull", subAnnotation.classType().content());
                    assertNotNull(subAnnotation.values());
                    assertEquals(0, subAnnotation.values().size());
                    ASTArray array = assertIs(ASTArray.class, annotation.value("values"));
                    assertEquals(2, array.values().size());
                    // assert that all elements are not null
                    for (ASTElement element : array.values()) {
                        assertNotNull(element);
                    }
                    assertEquals("Hello, world!", array.value(0).content());
                    assertEquals("Hello, world!", array.value(1).content());
                    assertEquals("Hello, world!", annotation.value("value").content());
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
                method.declaredExceptions().stream().map(ASTIdentifier::content).toList()
        );
        assertEquals(1, method.exceptions().size(), "try/catch exceptions should remain separate");
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
        assertEquals("pkg/ClassAnno", clazz.getVisibleAnnotations().getFirst().classType().content());

        List<ASTRecordComponent> components = clazz.getRecordComponents();
        assertEquals(1, components.size());
        ASTRecordComponent component = components.getFirst();
        assertEquals("value", component.getComponentType().content());
        assertEquals("Ljava/lang/String;", component.getComponentDescriptor().content());
        assertEquals("RC", component.getSignature().content());
        assertEquals(1, component.getVisibleAnnotations().size());
        assertEquals("pkg/ComponentAnno", component.getVisibleAnnotations().getFirst().classType().content());

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

        assertEquals(List.of("this", "name", "count"), method.parameters().stream().map(ASTIdentifier::content).toList());
        assertEquals(2, method.parameterAnnotations().size());

        ASTAnnotation visible = findParameterAnnotation(method.parameterAnnotations(), "name");
        ASTAnnotation invisible = findParameterAnnotation(method.parameterAnnotations(), "count");

        assertNotNull(visible);
        assertNotNull(invisible);
        assertTrue(visible.isVisible());
        assertFalse(invisible.isVisible());
        assertEquals("Visible", visible.classType().content());
        assertEquals("Hidden", invisible.classType().content());
        assertEquals("a", visible.value("value").content());
        assertEquals("b", invisible.value("value").content());
    }

    @Test
    void rejectsMalformedParameterAnnotationShapes() {
        Result<List<ASTElement>> result = AssemblyParseFixture.processAst(
                ".method public broken ()V {" +
                        " parameters: { this }," +
                        " parameter-annotations: {" +
                        "  this: nope," +
                        "  other: { \"bad\" }" +
                        " }" +
                        "}"
        );

        assertTrue(result.hasErr(), "Expected malformed parameter annotations to fail");
        String errors = DiagnosticAssertions.formatErrors(result.errors());
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
        assertTrue(visible.isTypeAnnotation());
        assertTrue(invisible.isTypeAnnotation());
        assertEquals("TypeVisible", visible.classType().content());
        assertEquals("TypeHidden", invisible.classType().content());
        assertEquals("0", visible.typeRef().content());
        assertEquals("ROOT", visible.typePath().content());
        assertEquals("1", invisible.typeRef().content());
        assertEquals("LEAF", invisible.typePath().content());
    }

    private static <T extends ASTElement> T onlyProcessed(String input, Class<T> type) {
        List<ASTElement> results = DiagnosticAssertions.requireOk(
                AssemblyParseFixture.processAst(input),
                "AST processing failed"
        );
        assertEquals(1, results.size(), "Expected a single processed AST node");
        return assertInstanceOf(type, results.getFirst());
    }

    private static ASTAnnotation findParameterAnnotation(Map<ASTIdentifier, List<ASTAnnotation>> annotations, String parameterName) {
        for (var entry : annotations.entrySet()) {
            if (entry.getKey().content().equals(parameterName) && !entry.getValue().isEmpty()) {
                return entry.getValue().getFirst();
            }
        }
        return null;
    }
}

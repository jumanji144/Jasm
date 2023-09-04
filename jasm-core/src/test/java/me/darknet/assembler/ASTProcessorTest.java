package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.*;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class ASTProcessorTest {

    public static <T extends ASTElement> void assertOne(String input, Class<T> clazz, Consumer<T> consumer) {
        parseString(input, (result) -> {
            if (result.hasErr()) {
                for (Error error : result.errors()) {
                    Location location = error.getLocation();
                    System.err.printf(
                            "%s:%d:%d: %s%n", location.getSource(), location.getLine(), location.getColumn(),
                            error.getMessage()
                    );
                }
                Assertions.fail();
            }
            List<ASTElement> results = result.get();
            assertEquals(1, results.size());
            ASTElement element = results.get(0);
            assertNotNull(element);
            assertInstanceOf(clazz, element);
            consumer.accept((T) element);
        });
    }

    public static void assertError(String input, Consumer<List<Error>> errorConsumer) {
        parseString(input, (result) -> {
            assertNotEquals(0, result.errors().size());
            errorConsumer.accept(result.errors());
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> @NotNull T assertIs(Class<T> shouldBe, Object is) {
        assertNotNull(is);
        assertInstanceOf(shouldBe, is);
        return (T) is;
    }

    public static void parseString(String input, Consumer<Result<List<ASTElement>>> consumer) {
        DeclarationParser parser = new DeclarationParser();
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", input);
        Assertions.assertNotNull(tokens);
        Assertions.assertFalse(tokens.isEmpty());
        Result<List<ASTElement>> result = parser.parseAny(tokens);
        if (result.hasErr()) {
            for (Error error : result.errors()) {
                Location location = error.getLocation();
                System.err.printf(
                        "%s:%d:%d: %s%n", location.getSource(), location.getLine(), location.getColumn(),
                        error.getMessage()
                );
                Throwable trace = new Throwable();
                trace.setStackTrace(error.getInCodeSource());
                trace.printStackTrace();
            }
            Assertions.fail();
        }
        ASTProcessor processor = new ASTProcessor(BytecodeFormat.DEFAULT);
        result = processor.processAST(result.get());
        consumer.accept(result);
    }

    @Test
    public void testSimpleClass() {
        assertOne(".class public HelloWorld { .field public static final a I }", ASTClass.class, (clazz) -> {
            assertEquals("HelloWorld", clazz.name().content());
            ASTField field = assertIs(ASTField.class, clazz.contents().get(0));
            assertNotNull(field);
            assertEquals("a", field.name().content());
            assertEquals("I", field.descriptor().content());
            List<ASTIdentifier> modifiers = field.modifiers().modifiers();
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
            assertEquals("a", field.name().content());
            assertEquals("I", field.descriptor().content());
            List<ASTIdentifier> modifiers = field.modifiers().modifiers();
            assertEquals(3, modifiers.size());
            assertEquals("public", modifiers.get(0).content());
            assertEquals("static", modifiers.get(1).content());
            assertEquals("final", modifiers.get(2).content());
        });
        assertOne(".field public static final a I {value: 10}", ASTField.class, (field) -> {
            assertEquals("a", field.name().content());
            assertEquals("I", field.descriptor().content());
            List<ASTIdentifier> modifiers = field.modifiers().modifiers();
            assertEquals(3, modifiers.size());
            assertEquals("public", modifiers.get(0).content());
            assertEquals("static", modifiers.get(1).content());
            assertEquals("final", modifiers.get(2).content());
            assertNotNull(field.fieldValue());
            assertEquals("10", field.fieldValue().content());
        });
    }

    @Test
    public void testMethod() {
        assertOne(".method public static main ([Ljava/lang/String;)V {}", ASTMethod.class, (method) -> {
            assertEquals("main", method.name().content());
            assertEquals("([Ljava/lang/String;)V", method.descriptor().content());
        });
        assertOne(
                ".method public static main ([Ljava/lang/String;)V { parameters: {args} }", ASTMethod.class,
                (method) -> {
                    assertEquals("main", method.name().content());
                    assertEquals("([Ljava/lang/String;)V", method.descriptor().content());
                    assertNotNull(method.parameters());
                    assertEquals(1, method.parameters().size());
                    assertEquals("args", method.parameters().get(0).content());
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
            assertEquals("Hello World", annotation.values().get("value").content());
        });
        assertOne(
                ".annotation java/lang/annotation/Retention { value: .enum java/lang/annotation/RetentionPolicy"
                        + " RUNTIME }",
                ASTAnnotation.class, (annotation) -> {
                    assertEquals("java/lang/annotation/Retention", annotation.classType().content());
                    assertNotNull(annotation.values());
                    assertEquals(1, annotation.values().size());
                    ASTEnum enumValue = assertIs(ASTEnum.class, annotation.values().get("value"));
                    assertEquals("java/lang/annotation/RetentionPolicy", enumValue.enumType().content());
                    assertEquals("RUNTIME", enumValue.enumValue().content());
                }
        );
        assertOne(
                ".annotation java/lang/annotation/Target { value: { .enum java/lang/annotation/ElementType FIELD,"
                        + " .enum java/lang/annotation/ElementType METHOD } }",
                ASTAnnotation.class, (annotation) -> {
                    assertEquals("java/lang/annotation/Target", annotation.classType().content());
                    assertNotNull(annotation.values());
                    assertEquals(1, annotation.values().size());
                    ASTArray array = assertIs(ASTArray.class, annotation.values().get("value"));
                    assertEquals(2, array.values().size());
                    ASTEnum enumValue = assertIs(ASTEnum.class, array.values().get(0));
                    assertEquals("java/lang/annotation/ElementType", enumValue.enumType().content());
                    assertEquals("FIELD", enumValue.enumValue().content());
                    enumValue = assertIs(ASTEnum.class, array.values().get(1));
                    assertEquals("java/lang/annotation/ElementType", enumValue.enumType().content());
                    assertEquals("METHOD", enumValue.enumValue().content());
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
                    ASTAnnotation subAnnotation = assertIs(ASTAnnotation.class, annotation.values().get("value"));
                    assertEquals("java/lang/annotation/Annotation", subAnnotation.classType().content());
                    assertNotNull(subAnnotation.values());
                    assertEquals(1, subAnnotation.values().size());
                    assertEquals("100", subAnnotation.values().get("value").content());
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
                    assertEquals("15", annotation.values().get("number").content());
                    ASTAnnotation subAnnotation = assertIs(ASTAnnotation.class, annotation.values().get("notNull"));
                    assertEquals("org/jetbrains/annotations/NotNull", subAnnotation.classType().content());
                    assertNotNull(subAnnotation.values());
                    assertEquals(0, subAnnotation.values().size());
                    ASTArray array = assertIs(ASTArray.class, annotation.values().get("values"));
                    assertEquals(2, array.values().size());
                    // assert that all elements are not null
                    for (ASTElement element : array.values()) {
                        assertNotNull(element);
                    }
                    assertEquals("Hello, world!", array.values().get(0).content());
                    assertEquals("Hello, world!", array.values().get(1).content());
                    assertEquals("Hello, world!", annotation.values().get("value").content());
                }
        );
    }

}
package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class DeclarationParserTest {

    @SuppressWarnings("unchecked")
    public static <T> @NotNull T assertIs(Class<T> shouldBe, Object is) {
        assertNotNull(is);
        assertInstanceOf(shouldBe, is);
        return (T) is;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ASTElement> void assertOne(String input, Class<T> clazz, Consumer<T> consumer) {
        parseString(input, (result) -> {
            assertEquals(1, result.size());
            ASTElement element = result.get(0);
            assertNotNull(element);
            assertInstanceOf(clazz, element);
            consumer.accept((T) element);
        });
    }

    public static void parseString(String input, Consumer<List<ASTElement>> consumer) {
        DeclarationParser parser = new DeclarationParser();
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", input);
        Assertions.assertNotNull(tokens);
        Assertions.assertFalse(tokens.isEmpty());
        Result<List<ASTElement>> result = parser.parseAny(tokens);
        if (result.hasErr()) {
            for (Error error : result.getErrors()) {
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
        consumer.accept(result.get());
    }

    @Test
    public void testBasicPrimitives() {
        parseString("test", (result) -> {
            assertEquals(1, result.size());
            ASTElement element = result.get(0);
            assertEquals("test", element.content());
            assertInstanceOf(ASTIdentifier.class, element);
        });
        parseString("10 0x10 test \"test\"", (result) -> {
            assertEquals(4, result.size());
            ASTElement element = result.get(0);
            assertEquals("10", element.content());
            assertInstanceOf(ASTNumber.class, element);
            element = result.get(1);
            assertEquals("0x10", element.content());
            assertInstanceOf(ASTNumber.class, element);
            element = result.get(2);
            assertEquals("test", element.content());
            assertInstanceOf(ASTIdentifier.class, element);
            element = result.get(3);
            assertEquals("test", element.content());
            assertInstanceOf(ASTString.class, element);
        });
    }

    @Test
    public void testObject() {
        parseString("{}", (result) -> { // empty object
            assertEquals(1, result.size());
            ASTElement element = result.get(0);
            assertInstanceOf(ASTEmpty.class, element);
        });
        parseString("{test: 10}", (result) -> {
            assertEquals(1, result.size());
            ASTElement element = result.get(0);
            assertInstanceOf(ASTObject.class, element);
            ASTObject object = (ASTObject) element;
            assertEquals(1, object.values().size());
            ASTIdentifier key = object.values().key("test");
            assertNotNull(key);
            ASTElement value = object.values().get("test");
            assertIs(ASTNumber.class, value);
            assertEquals("10", value.content());
        });
    }

    @Test
    public void testArray() {
        assertOne("{}", ASTEmpty.class, (result) -> { // empty array

        });
        assertOne("{entry1, entry2, entry3}", ASTArray.class, (result) -> {
            assertEquals(3, result.values().size());
            assertInstanceOf(ASTIdentifier.class, result.values().get(0));
            assertInstanceOf(ASTIdentifier.class, result.values().get(1));
            assertInstanceOf(ASTIdentifier.class, result.values().get(2));
        });
        assertOne("{10, test, \"Hello\", {test: 10}}", ASTArray.class, (result) -> {
            List<ASTElement> elements = result.values();
            assertEquals(4, elements.size());
            assertInstanceOf(ASTNumber.class, elements.get(0));
            assertInstanceOf(ASTIdentifier.class, elements.get(1));
            assertInstanceOf(ASTString.class, elements.get(2));
            assertInstanceOf(ASTObject.class, elements.get(3));

            ASTObject object = (ASTObject) elements.get(3);
            assertEquals(1, object.values().size());
        });
    }

    @Test
    public void testArrayInObject() {
        assertOne("{test: {10, test, \"Hello\", {test: 10}}}", ASTObject.class, (result) -> {
            ASTElement element = result.values().get("test");
            ASTArray array = assertIs(ASTArray.class, element);
            List<ASTElement> elements = array.values();
            assertEquals(4, elements.size());
            assertInstanceOf(ASTNumber.class, elements.get(0));
            assertInstanceOf(ASTIdentifier.class, elements.get(1));
            assertInstanceOf(ASTString.class, elements.get(2));
            assertInstanceOf(ASTObject.class, elements.get(3));

            ASTObject object = (ASTObject) elements.get(3);
            assertEquals(1, object.values().size());
        });
    }

    @Test
    public void testDeclaration() {
        assertOne(".sourcefile \"Source.java\"", ASTDeclaration.class, (result) -> {
            assertEquals(".sourcefile", result.keyword().content());
            ASTElement value = result.elements().get(0);
            assertNotNull(value);
            assertInstanceOf(ASTString.class, value);
            assertEquals("Source.java", value.content());
        });
        assertOne(
                ".innerclass static { inner_class: A, outer_class: B, inner_name: A }", ASTDeclaration.class,
                (result) -> {
                    assertEquals(".innerclass", result.keyword().content());
                    ASTElement value = result.elements().get(0);
                    assertNotNull(value);
                    assertInstanceOf(ASTIdentifier.class, value);
                    assertEquals("static", value.content());
                    value = result.elements().get(1);
                    assertNotNull(value);
                    assertInstanceOf(ASTObject.class, value);
                    ASTObject object = (ASTObject) value;
                    assertEquals(3, object.values().size());
                    ASTElement innerClass = object.values().get("inner_class");
                    assertNotNull(innerClass);
                    assertInstanceOf(ASTIdentifier.class, innerClass);
                    assertEquals("A", innerClass.content());
                    ASTElement outerClass = object.values().get("outer_class");
                    assertNotNull(outerClass);
                    assertInstanceOf(ASTIdentifier.class, outerClass);
                    assertEquals("B", outerClass.content());
                    ASTElement innerName = object.values().get("inner_name");
                    assertNotNull(innerName);
                    assertInstanceOf(ASTIdentifier.class, innerName);
                    assertEquals("A", innerName.content());
                }
        );
    }

    @Test
    public void testDeclarationInObject() {
        assertOne("{test: .sourcefile \"Source.java\"}", ASTObject.class, (result) -> {
            ASTElement element = result.values().get("test");
            ASTDeclaration declaration = assertIs(ASTDeclaration.class, element);
            assertEquals(".sourcefile", declaration.keyword().content());
            ASTElement value = declaration.elements().get(0);
            assertNotNull(value);
            assertInstanceOf(ASTString.class, value);
            assertEquals("Source.java", value.content());
        });
    }

    @Test
    public void testNestedDeclaration() {
        assertOne(
                ".class public final HelloWorld { .field public name Ljava/lang/String; .field public output I }",
                ASTDeclaration.class, (result) -> {
                    assertNotNull(result);
                    assertEquals(".class", result.keyword().content());
                    assertEquals(4, result.elements().size());
                    ASTElement value = result.elements().get(0);
                    assertIs(ASTIdentifier.class, value);
                    assertEquals("public", value.content());
                    value = result.elements().get(1);
                    assertIs(ASTIdentifier.class, value);
                    assertEquals("final", value.content());
                    value = result.elements().get(2);
                    assertIs(ASTIdentifier.class, value);
                    assertEquals("HelloWorld", value.content());
                    ASTDeclaration declaration = assertIs(ASTDeclaration.class, result.elements().get(3));
                    // declaration only contains a list of declarations
                    assertEquals(2, declaration.elements().size());
                    ASTDeclaration field1 = assertIs(ASTDeclaration.class, declaration.elements().get(0));
                    assertEquals(".field", field1.keyword().content());
                    assertEquals(3, field1.elements().size());
                    assertEquals("public", field1.elements().get(0).content());
                    assertEquals("name", field1.elements().get(1).content());
                    assertEquals("Ljava/lang/String;", field1.elements().get(2).content());
                    ASTDeclaration field2 = assertIs(ASTDeclaration.class, declaration.elements().get(1));
                    assertEquals(".field", field2.keyword().content());
                    assertEquals(3, field2.elements().size());
                    assertEquals("public", field2.elements().get(0).content());
                    assertEquals("output", field2.elements().get(1).content());
                    assertEquals("I", field2.elements().get(2).content());
                }
        );
    }

    @Test
    public void testInvalidInput() {
        DeclarationParser parser = new DeclarationParser();
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", "{ test, 4.... { {");
        Result<List<ASTElement>> result = parser.parseAny(tokens);
        assertTrue(result.hasErr());
        assertEquals(2, result.getErrors().size());
    }

}

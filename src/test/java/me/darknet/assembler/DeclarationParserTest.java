package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.ast.specific.ASTDeclaration;
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

	@Test
	public void testBasicPrimitives() {
		parseString("test", (result) -> {
			assertEquals(1, result.size());
			ASTElement element = result.get(0);
			assertEquals("test", element.getContent());
			assertInstanceOf(ASTIdentifier.class, element);
		});
		parseString("10 0x10 test \"test\"", (result) -> {
			assertEquals(4, result.size());
			ASTElement element = result.get(0);
			assertEquals("10", element.getContent());
			assertInstanceOf(ASTNumber.class, element);
			element = result.get(1);
			assertEquals("0x10", element.getContent());
			assertInstanceOf(ASTNumber.class, element);
			element = result.get(2);
			assertEquals("test", element.getContent());
			assertInstanceOf(ASTIdentifier.class, element);
			element = result.get(3);
			assertEquals("test", element.getContent());
			assertInstanceOf(ASTString.class, element);
		});
	}

	@Test
	public void testObject() {
		parseString("{}", (result) -> { // empty object
			assertEquals(1, result.size());
			ASTElement element = result.get(0);
			assertInstanceOf(ASTObject.class, element);
			ASTObject object = (ASTObject) element;
			assertEquals(0, object.getValues().size());
		});
		parseString("{test: 10}", (result) -> {
			assertEquals(1, result.size());
			ASTElement element = result.get(0);
			assertInstanceOf(ASTObject.class, element);
			ASTObject object = (ASTObject) element;
			assertEquals(1, object.getValues().size());
			ASTIdentifier key = object.getValues().getKey("test");
			assertNotNull(key);
			ASTElement value = object.getValues().get("test");
			assertIs(ASTNumber.class, value);
			assertEquals("10", value.getContent());
		});
	}

	@Test
	public void testArray() {
		assertOne("[]", ASTArray.class, (result) -> { // empty array
			assertEquals(0, result.getValues().size());
		});
		assertOne("[10, test, \"Hello\", {test: 10}]", ASTArray.class, (result) -> {
			List<ASTElement> elements = result.getValues();
			assertEquals(4, elements.size());
			assertInstanceOf(ASTNumber.class, elements.get(0));
			assertInstanceOf(ASTIdentifier.class, elements.get(1));
			assertInstanceOf(ASTString.class, elements.get(2));
			assertInstanceOf(ASTObject.class, elements.get(3));

			ASTObject object = (ASTObject) elements.get(3);
			assertEquals(1, object.getValues().size());
		});
	}

	@Test
	public void testArrayInObject() {
		assertOne("{test: [10, test, \"Hello\", {test: 10}]}", ASTObject.class, (result) -> {
			ASTElement element = result.getValues().get("test");
			ASTArray array = assertIs(ASTArray.class, element);
			List<ASTElement> elements = array.getValues();
			assertEquals(4, elements.size());
			assertInstanceOf(ASTNumber.class, elements.get(0));
			assertInstanceOf(ASTIdentifier.class, elements.get(1));
			assertInstanceOf(ASTString.class, elements.get(2));
			assertInstanceOf(ASTObject.class, elements.get(3));

			ASTObject object = (ASTObject) elements.get(3);
			assertEquals(1, object.getValues().size());
		});
	}

	@Test
	public void testDeclaration() {
		assertOne(".sourcefile \"Source.java\"", ASTDeclaration.class, (result) -> {
			assertEquals(".sourcefile", result.getKeyword().getContent());
			ASTElement value = result.getElements().get(0);
			assertNotNull(value);
			assertInstanceOf(ASTString.class, value);
			assertEquals("Source.java", value.getContent());
		});
		assertOne(".innerclass static { inner_class: A, outer_class: B, inner_name: A }", ASTDeclaration.class, (result) -> {
			assertEquals(".innerclass", result.getKeyword().getContent());
			ASTElement value = result.getElements().get(0);
			assertNotNull(value);
			assertInstanceOf(ASTIdentifier.class, value);
			assertEquals("static", value.getContent());
			value = result.getElements().get(1);
			assertNotNull(value);
			assertInstanceOf(ASTObject.class, value);
			ASTObject object = (ASTObject) value;
			assertEquals(3, object.getValues().size());
			ASTElement innerClass = object.getValues().get("inner_class");
			assertNotNull(innerClass);
			assertInstanceOf(ASTIdentifier.class, innerClass);
			assertEquals("A", innerClass.getContent());
			ASTElement outerClass = object.getValues().get("outer_class");
			assertNotNull(outerClass);
			assertInstanceOf(ASTIdentifier.class, outerClass);
			assertEquals("B", outerClass.getContent());
			ASTElement innerName = object.getValues().get("inner_name");
			assertNotNull(innerName);
			assertInstanceOf(ASTIdentifier.class, innerName);
			assertEquals("A", innerName.getContent());
		});
	}

	@Test
	public void testNestedDeclaration() {
		assertOne(".class public final HelloWorld { .field public name I .field public output I }", ASTDeclaration.class, (result) -> {
			assertNotNull(result);
			assertEquals(".class", result.getKeyword().getContent());
			assertEquals(4, result.getElements().size());
			ASTElement value = result.getElements().get(0);
			assertIs(ASTIdentifier.class, value);
			assertEquals("public", value.getContent());
			value = result.getElements().get(1);
			assertIs(ASTIdentifier.class, value);
			assertEquals("final", value.getContent());
			value = result.getElements().get(2);
			assertIs(ASTIdentifier.class, value);
			assertEquals("HelloWorld", value.getContent());
			ASTDeclaration declaration = assertIs(ASTDeclaration.class, result.getElements().get(3));
			// declaration only contains a list of declarations
			assertEquals(2, declaration.getElements().size());
			ASTDeclaration field1 = assertIs(ASTDeclaration.class, declaration.getElements().get(0));
			assertEquals(".field", field1.getKeyword().getContent());
			assertEquals(3, field1.getElements().size());
			assertEquals("public", field1.getElements().get(0).getContent());
			assertEquals("name", field1.getElements().get(1).getContent());
			assertEquals("I", field1.getElements().get(2).getContent());
			ASTDeclaration field2 = assertIs(ASTDeclaration.class, declaration.getElements().get(1));
			assertEquals(".field", field2.getKeyword().getContent());
			assertEquals(3, field2.getElements().size());
			assertEquals("public", field2.getElements().get(0).getContent());
			assertEquals("output", field2.getElements().get(1).getContent());
			assertEquals("I", field2.getElements().get(2).getContent());
		});
	}

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
		if(result.isErr()) {
			for (Error error : result.getErrors()) {
				Location location = error.getLocation();
				System.err.printf("%s:%d:%d: %s%n", location.getSource(), location.getLine(), location.getColumn(),
						error.getMessage());
			}
			Assertions.fail();
		}
		consumer.accept(result.get());
	}

}

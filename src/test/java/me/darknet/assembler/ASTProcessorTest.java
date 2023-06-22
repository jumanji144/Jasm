package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.ASTProcessor;
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

public class ASTProcessorTest {

	@Test
	public void testSimpleClass() {
		assertOne(".class public HelloWorld { .field public static final a I }", ASTClass.class, (clazz) -> {
			assertEquals("HelloWorld", clazz.getName().getContent());
			ASTField field = assertIs(ASTField.class, clazz.getContents().get(0));
			assertNotNull(field);
			assertEquals("a", field.getName().getContent());
			assertEquals("I", field.getDescriptor().getContent());
			List<ASTIdentifier> modifiers = field.getModifiers().getModifiers();
			assertEquals(3, modifiers.size());
			assertEquals("public", modifiers.get(0).getContent());
			assertEquals("static", modifiers.get(1).getContent());
			assertEquals("final", modifiers.get(2).getContent());
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
			assertEquals("a", field.getName().getContent());
			assertEquals("I", field.getDescriptor().getContent());
			List<ASTIdentifier> modifiers = field.getModifiers().getModifiers();
			assertEquals(3, modifiers.size());
			assertEquals("public", modifiers.get(0).getContent());
			assertEquals("static", modifiers.get(1).getContent());
			assertEquals("final", modifiers.get(2).getContent());
		});
		assertOne(".field public static final a I {value: 10}", ASTField.class, (field) -> {
			assertEquals("a", field.getName().getContent());
			assertEquals("I", field.getDescriptor().getContent());
			List<ASTIdentifier> modifiers = field.getModifiers().getModifiers();
			assertEquals(3, modifiers.size());
			assertEquals("public", modifiers.get(0).getContent());
			assertEquals("static", modifiers.get(1).getContent());
			assertEquals("final", modifiers.get(2).getContent());
			assertNotNull(field.getFieldValue());
			assertEquals("10", field.getFieldValue().getContent());
		});
	}

	@Test
	public void testMethod() {
		assertOne(".method public static main ([Ljava/lang/String;)V {}", ASTMethod.class, (method) -> {
			assertEquals("main", method.getName().getContent());
			assertEquals("([Ljava/lang/String;)V", method.getDescriptor().getContent());
		});
		assertOne(".method public static main ([Ljava/lang/String;)V { parameters: {args} }", ASTMethod.class, (method) -> {
			assertEquals("main", method.getName().getContent());
			assertEquals("([Ljava/lang/String;)V", method.getDescriptor().getContent());
			assertNotNull(method.getParameters());
			assertEquals(1, method.getParameters().size());
			assertEquals("args", method.getParameters().get(0).getContent());
		});
	}

	@Test
	public void testJvmBytecode() {
		assertCode(new String[] {
				"ldc \"Hello World\"",
				"getstatic java/lang/System.out Ljava/io/PrintStream;",
				"swap",
				"invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V",
				"return"
		}, (code) -> {
			List<ASTInstruction> instructions = code.getInstructions();
			assertEquals(5, instructions.size());
			assertEquals("ldc", instructions.get(0).getIdentifier().getContent());
			assertEquals("Hello World", instructions.get(0).getArguments().get(0).getContent());
			assertEquals("getstatic", instructions.get(1).getIdentifier().getContent());
			assertEquals("java/lang/System.out", instructions.get(1).getArguments().get(0).getContent());
			assertEquals("Ljava/io/PrintStream;", instructions.get(1).getArguments().get(1).getContent());
			assertEquals("swap", instructions.get(2).getIdentifier().getContent());
			assertEquals("invokevirtual", instructions.get(3).getIdentifier().getContent());
			assertEquals("java/io/PrintStream.println(Ljava/lang/String;)V", instructions.get(3).getArguments().get(0).getContent());
			assertEquals("return", instructions.get(4).getIdentifier().getContent());
		});
	}

	@Test
	public void testSmaliBytecode() {
		assertCode(new String[]{
				"const-string v1 \"Hello World\"",
				"sget-object v0 Ljava/lang/System;->out Ljava/io/PrintStream;",
				"invoke-virtual {v0, v1} Ljava/io/PrintStream;->println(Ljava/lang/String;)V",
				"return-void"
		}, (code) -> {
			List<ASTInstruction> instructions = code.getInstructions();
			assertEquals(4, instructions.size());
			assertEquals("const-string", instructions.get(0).getIdentifier().getContent());
			assertEquals("v1", instructions.get(0).getArguments().get(0).getContent());
			assertEquals("Hello World", instructions.get(0).getArguments().get(1).getContent());
			assertEquals("sget-object", instructions.get(1).getIdentifier().getContent());
			assertEquals("v0", instructions.get(1).getArguments().get(0).getContent());
			assertEquals("Ljava/lang/System;->out", instructions.get(1).getArguments().get(1).getContent());
		});
	}

	public static void assertCode(String[] instructions, Consumer<ASTCode> consumer) {
		assertOne(".method stub ()V {\n" +
				"code: {" + String.join("\n", instructions) + "\n}}\n", ASTMethod.class, (method) -> {
			assertEquals("stub", method.getName().getContent());
			assertEquals("()V", method.getDescriptor().getContent());
			assertNotNull(method.getCode());
			consumer.accept(method.getCode());
		});
	}

	public static <T extends ASTElement> void assertOne(String input, Class<T> clazz, Consumer<T> consumer) {
		parseString(input, (result) -> {
			if(result.isErr()) {
				for (Error error : result.getErrors()) {
					Location location = error.getLocation();
					System.err.printf("%s:%d:%d: %s%n", location.getSource(), location.getLine(), location.getColumn(),
							error.getMessage());
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
			assertNotEquals(0, result.getErrors().size());
			for (Error error : result.getErrors()) {
				Location location = error.getLocation();
				System.err.printf("%s:%d:%d: %s%n", location.getSource(), location.getLine(), location.getColumn(),
						error.getMessage());
			}
			errorConsumer.accept(result.getErrors());
		});
	}

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
		if(result.isErr()) {
			for (Error error : result.getErrors()) {
				Location location = error.getLocation();
				System.err.printf("%s:%d:%d: %s%n", location.getSource(), location.getLine(), location.getColumn(),
						error.getMessage());
				Throwable trace = new Throwable();
				trace.setStackTrace(error.getInCodeSource());
				trace.printStackTrace();
			}
			Assertions.fail();
		}
		result = ASTProcessor.processAST(result.get());
		consumer.accept(result);
	}

}

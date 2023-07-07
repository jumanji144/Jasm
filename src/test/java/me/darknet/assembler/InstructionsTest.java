package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.*;
import me.darknet.assembler.util.Location;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class InstructionsTest {

	@Test
	public void testJvmBytecode() {
		assertCode(new String[] {
				"ldc \"Hello World\"",
				"getstatic java/lang/System.out Ljava/io/PrintStream;",
				"swap",
				"invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V",
				"return"
		}, BytecodeFormat.JVM, (code) -> {
			List<ASTInstruction> instructions = code.getInstructions();
			assertEquals(5, instructions.size());
			assertEquals("ldc", instructions.get(0).getIdentifier().getContent());
			assertEquals("Hello World", instructions.get(0).getArguments().get(0).getContent());
			assertEquals("getstatic", instructions.get(1).getIdentifier().getContent());
			assertEquals("java/lang/System.out", instructions.get(1).getArguments().get(0).getContent());
			assertEquals("Ljava/io/PrintStream;", instructions.get(1).getArguments().get(1).getContent());
			assertEquals("swap", instructions.get(2).getIdentifier().getContent());
			assertEquals("invokevirtual", instructions.get(3).getIdentifier().getContent());
			assertEquals("java/io/PrintStream.println", instructions.get(3).getArguments().get(0).getContent());
			assertEquals("(Ljava/lang/String;)V", instructions.get(3).getArguments().get(1).getContent());
			assertEquals("return", instructions.get(4).getIdentifier().getContent());
		});
	}

	@Test
	public void testInvokeDynamic() {
		assertCode(new String[] {
			"invokedynamic foo (Ljava/lang/String;)V {invokestatic, me/darknet/assembler/InstructionsTest.bar, (Ljava/lang/String;)V} {}",
		}, BytecodeFormat.JVM, (code) -> {});
	}

	@Test
	public void testLabel() {
		assertCode(new String[] {
				"L1:",
				"getstatic java/lang/System.out Ljava/io/PrintStream;",
				"L2:",
				"ldc \"Hello World\"",
				"L3:",
				"invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V",
				"return",
				"L4:"
		}, BytecodeFormat.JVM, (code) -> {
			List<ASTInstruction> instructions = code.getInstructions();
			assertEquals(8, instructions.size());
			assertEquals("L1", instructions.get(0).getIdentifier().getContent());
			assertEquals("getstatic", instructions.get(1).getIdentifier().getContent());
			assertEquals("L2", instructions.get(2).getIdentifier().getContent());
			assertEquals("ldc", instructions.get(3).getIdentifier().getContent());
			assertEquals("L3", instructions.get(4).getIdentifier().getContent());
			assertEquals("invokevirtual", instructions.get(5).getIdentifier().getContent());
			assertEquals("return", instructions.get(6).getIdentifier().getContent());
			assertEquals("L4", instructions.get(7).getIdentifier().getContent());
		});
	}

	@Test
	public void testLdc() {
		assertCode(new String[] {
				"ldc Ljava/lang/String;",
		}, BytecodeFormat.JVM, (code) -> {
			List<ASTInstruction> instructions = code.getInstructions();
			assertEquals(1, instructions.size());
			assertEquals("ldc", instructions.get(0).getIdentifier().getContent());
			assertEquals("Ljava/lang/String;", instructions.get(0).getArguments().get(0).getContent());
		});
	}

	@Test
	public void testTableSwitch() {
		assertCode(new String[] {
				"tableswitch { min: 10," +
						      "max: 20," +
						      "default: L1," +
						      "cases: {" +
								"L2," +
								"L4," +
								"L8" +
						       "}" +
							 "}",
		}, BytecodeFormat.JVM, (code) -> {
			List<ASTInstruction> instructions = code.getInstructions();
			assertEquals(1, instructions.size());
			assertEquals("tableswitch", instructions.get(0).getIdentifier().getContent());
		});
	}

	@Test
	public void testLookupSwitch() {
		assertCode(new String[] {
				"lookupswitch {" +
								"0: L2," +
								"1: L4," +
								"2: L8," +
								"default: L10" +
						      "}",
		}, BytecodeFormat.JVM, (code) -> {
			List<ASTInstruction> instructions = code.getInstructions();
			assertEquals(1, instructions.size());
			assertEquals("lookupswitch", instructions.get(0).getIdentifier().getContent());
		});
	}

	public static void assertCode(String[] instructions, BytecodeFormat format, Consumer<ASTCode> consumer) {
		assertOne(".method stub ()V {\n" +
				"code: {" + String.join("\n", instructions) + "\n}}\n", format, ASTMethod.class, (method) -> {
			assertEquals("stub", method.getName().getContent());
			assertEquals("()V", method.getDescriptor().getContent());
			assertNotNull(method.getCode());
			consumer.accept(method.getCode());
		});
	}

	public static <T extends ASTElement> void assertOne(String input, BytecodeFormat format, Class<T> clazz, Consumer<T> consumer) {
		parseString(input, format, (result) -> {
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

	public static void parseString(String input, BytecodeFormat format, Consumer<Result<List<ASTElement>>> consumer) {
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
		ASTProcessor processor = new ASTProcessor(format);
		result = processor.processAST(result.get());
		consumer.accept(result);
	}

}

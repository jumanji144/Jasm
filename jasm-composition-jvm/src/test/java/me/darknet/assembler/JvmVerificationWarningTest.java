package me.darknet.assembler;

import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.Warn;
import me.darknet.assembler.test.BinarySampleFixture;
import me.darknet.assembler.test.JvmAssemblerFixture;
import me.darknet.assembler.test.JvmCompilation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JVM verification warnings.
 */
class JvmVerificationWarningTest {

	@Test
	void invalidGeneratedMethodProducesVerifierWarningAtAstLocation() {
		String source = BinarySampleFixture.jvmSample("Example-wrong-return.jasm").read();
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();

		JvmCompilation compilation = JvmAssemblerFixture.compileJvm("Example-wrong-return.jasm", source, options);
		Warn warning = findVerifierWarning(compilation);

		// Verify that the warning is present and points to the correct location in the source code
		assertFalse(compilation.hasErrors(), "Expected warning-only compilation");
		assertTrue(compilation.hasWarnings(), "Expected compilation warnings");
		assertTrue(warning.getMessage().contains("exampleMethod()I"));
		assertTrue(warning.getMessage().contains("Error at instruction"));
		assertEquals(8, warning.getLocation().line());
	}

	@Test
	void verifierWarningLocationMapsToFailingInstruction() {
		String source = """
				.super java/lang/Object
				.class public super Example {
				    .method public static example ()Z {
				        code: {
				        A:
				            new java/util/ArrayList
				            dup
				            invokespecial java/util/ArrayList.<init> ()V
				            invokeinterface java/util/Map.isEmpty ()Z   // line 9
				            ireturn
				        B:
				        }
				    }
				}
				""";
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();

		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);
		Warn warning = findVerifierWarning(compilation);

		// The warning should point to the line of the failing instruction (invokeinterface) in the source code.
		assertFalse(compilation.hasErrors(), "Expected warning-only compilation");
		assertTrue(warning.getMessage().contains("Method owner:"));
		assertEquals(9, warning.getLocation().line(), "Expected warning to point at failing invoke instruction");
	}

	@Test
	void verifierCanBeDisabled() {
		String source = BinarySampleFixture.jvmSample("Example-wrong-return.jasm").read();
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.verifyOutput(false);

		JvmCompilation compilation = JvmAssemblerFixture.compileJvm("Example-wrong-return.jasm", source, options);

		assertNull(compilation.warnings().stream()
				.filter(warning -> warning.getMessage().contains("may fail JVM verification"))
				.findFirst()
				.orElse(null));
	}

	@Test
	void verifierUsesInheritanceCheckerWithoutRuntimeClassAvailability() {
		// JASM is often used in cases where you don't have the classes in the runtime classpath.
		// So requiring it be provided would be stupid, it should instead rely on the provided InheritanceChecker.
		String source = """
				.super java/lang/Object
				.class public super Example {
				    .method public static exampleMethod ()Lmissing/A; {
				        code: {
				        A:
				            new missing/B
				            dup
				            invokespecial missing/B.<init> ()V
				            areturn
				        B:
				        }
				    }
				}
				""";

		// Mock inheritance checker to simulate missing classes in the runtime environment.
		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		options.inheritanceChecker(new InheritanceChecker() {
			@Override
			public boolean isSubclassOf(String child, String parent) {
				return switch (child) {
					case "missing/B" -> "missing/A".equals(parent) || "java/lang/Object".equals(parent);
					case "missing/A" -> "java/lang/Object".equals(parent);
					default -> child.equals(parent);
				};
			}

			@Override
			public String getCommonSuperclass(String type1, String type2) {
				if (type1.equals(type2))
					return type1;
				if (("missing/A".equals(type1) && "missing/B".equals(type2)) ||
						("missing/B".equals(type1) && "missing/A".equals(type2)))
					return "missing/A";
				return "java/lang/Object";
			}
		});

		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, options);

		// The compilation should not produce errors, and the missing-library reference should not trigger a JVM verification warning.
		assertFalse(compilation.hasErrors(), "Expected missing-library reference to remain warning-free");
		assertNull(compilation.warnings().stream()
				.filter(warning -> warning.getMessage().contains("may fail JVM verification"))
				.findFirst()
				.orElse(null));
	}

	private static Warn findVerifierWarning(JvmCompilation compilation) {
		return compilation.warnings().stream()
				.filter(warning -> warning.getMessage().contains("may fail JVM verification"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected JVM verification warning but found: " + compilation.warnings()));
	}
}

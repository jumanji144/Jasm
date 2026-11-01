package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikConstantTypeResolver;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that disassembled {@code const} and {@code const-wide} values are printed as the type their
 * consumers expect, printing unresolved raw bits as fixed-width hexadecimal.
 *
 * @see DalvikConstantTypeResolver
 */
class DalvikConstantTypePrintingTest {
	@Test
	void wideConstantAddedAsLongPrintsAsLong() {
		String printed = roundTrip("""
				.method public static tap (J)J {
				    code: {
                        const-wide v0 1L
                        add-long v0 v0 v2
                        return-wide v0
				    }
				}
				""");

		assertContains(printed, "const-wide v0 1L");
	}

	@Test
	void typedUseDeterminesConstantSpelling() {
		String printed = roundTrip("""
				.method public static intBits ()I {
				    code: {
				        const v0 1065353216
				        return v0
				    }
				}
				.method public static tinyFloat ()F {
				    code: {
				        const v0 1.4E-45F
				        return v0
				    }
				}
				""");

		assertContains(printed, "const v0 1065353216");
		assertContains(printed, "const v0 1.4E-45F");
	}

	@Test
	void evidenceFollowsWideMoves() {
		String printed = roundTrip("""
				.method public static moved ()J {
				    code: {
				        const-wide v0 4607182418800017408L
				        move-wide v2 v0
				        return-wide v2
				    }
				}
				""");

		assertContains(printed, "const-wide v0 4607182418800017408L");
	}

	@Test
	void evidenceReachesConstantsOnEveryMergedPath() {
		String printed = roundTrip("""
				.method public static choose (I)J {
				    code: {
				        if-eqz v2 Other
				        const-wide v0 4607182418800017408L
				        goto Join
				    Other:
				        const-wide v0 4611686018427387904L
				    Join:
				        return-wide v0
				    }
				}
				""");

		assertContains(printed, "const-wide v0 4607182418800017408L");
		assertContains(printed, "const-wide v0 4611686018427387904L");
	}

	@Test
	void floatArrayElementTypesStoredValue() {
		String printed = roundTrip("""
				.method public static floats ()[F {
				    code: {
				        const v0 2
				        new-array v1 v0 [F
				        const v0 0
				        const v2 1.4E-45F
				        aput v2 v1 v0
				        return-object v1
				    }
				}
				""");

		assertContains(printed, "const v2 1.4E-45F");
	}

	@Test
	void wideArrayElementTypesStoredValue() {
		String printed = roundTrip("""
				.method public static longs ()[J {
				    code: {
				        const v0 1
				        new-array v1 v0 [J
				        const v0 0
				        const-wide v2 4607182418800017408L
				        aput-wide v2 v1 v0
				        return-object v1
				    }
				}
				.method public static doubles ()[D {
				    code: {
				        const v0 1
				        new-array v1 v0 [D
				        const v0 0
				        const-wide v2 4.9E-324
				        aput-wide v2 v1 v0
				        return-object v1
				    }
				}
				""");

		assertContains(printed, "const-wide v2 4607182418800017408L");
		assertContains(printed, "const-wide v2 4.9E-324D");
	}

	@Test
	void conflictingUsesPrintRawHexBits() {
		String printed = roundTrip("""
				.method public static mixedZero ()F {
				    code: {
				        const v0 0
				        add-int v1 v0 v0
				        return v0
				    }
				}
				""");

		assertContains(printed, "const v0 #0x00000000");
	}

	@Test
	void unusedConstantsPrintRawHexBits() {
		String source = """
				.method public static unused ()V {
				    code: {
				        const v0 1.0F
				        const v1 42
				        const-wide v2 1.0
				        const-wide v4 3L
				        return-void
				    }
				}
				""";
		String printed = roundTrip(source);

		assertContains(printed, "const v0 #0x3F800000");
		assertContains(printed, "const v1 #0x0000002A");
		assertContains(printed, "const-wide v2 #0x3FF0000000000000");
		assertContains(printed, "const-wide v4 #0x0000000000000003");

		String hex = print(compile(wrap(source)), PrintContext.FloatPrintMode.HEX);
		assertContains(hex, "const v0 #0x3F800000");
		assertContains(hex, "const v1 #0x0000002A");
	}

	/**
	 * Compiles the methods, prints them, and checks the printed form compiles again.
	 */
	private static String roundTrip(String methods) {
		String printed = print(compile(wrap(methods)), PrintContext.FloatPrintMode.STANDARD);
		compile(printed);
		return printed;
	}

	private static String wrap(String methods) {
		return ".class public Example {\n" + methods + "}\n";
	}

	private static void assertContains(String printed, String expected) {
		assertTrue(TestUtils.normalize(printed).contains(expected), printed);
	}

	private static ClassDefinition compile(String source) {
		AtomicReference<ClassDefinition> definition = new AtomicReference<>();
		TestUtils.processDalvik(source, TestUtils.options(), result ->
				definition.set(((DalvikClassRepresentation) result.representation()).definition()));
		return definition.get();
	}

	private static String print(ClassDefinition definition, PrintContext.FloatPrintMode mode) {
		PrintContext<?> context = new PrintContext<>("    ");
		context.setFloatPrintMode(mode);
		new DalvikClassPrinter(definition).print(context);
		return context.toString();
	}
}

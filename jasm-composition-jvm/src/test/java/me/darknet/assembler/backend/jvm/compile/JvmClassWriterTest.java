package me.darknet.assembler.backend.jvm.compile;

import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.TypeAwareness;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.util.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests frame-type resolution and the writer's construction contract.
 */
class JvmClassWriterTest {
	private static TypeAwareness javaOnlyAwareness() {
		return new TypeAwareness() {
			@Override
			public boolean isAwareOf(String type) {
				return type.startsWith("java/");
			}

			@Override
			public String notifyUnknownType(String type) {
				return "Unknown type: " + type;
			}
		};
	}

	private static InheritanceChecker unreachableChecker() {
		return new InheritanceChecker() {
			@Override
			public boolean isSubclassOf(String child, String parent) {
				fail("Unknown frame types must not reach the inheritance checker");
				return false;
			}

			@Override
			public String getCommonSuperclass(String type1, String type2) {
				fail("Unknown frame types must not reach the inheritance checker");
				return null;
			}
		};
	}

	@Test
	void unknownFrameTypesWarnAndFallBackToObject() {
		DiagnosticSink sink = new DiagnosticSink();
		ExposedClassWriter writer = new ExposedClassWriter(sink, javaOnlyAwareness(), unreachableChecker());

		assertEquals("java/lang/Object", writer.commonSuperclass("missing/A", "missing/B"));
		assertEquals(2, sink.warnings().size());
		assertWarning(sink.warnings().get(0), "Unknown type: missing/A");
		assertWarning(sink.warnings().get(1), "Unknown type: missing/B");
	}

	@Test
	void knownFrameTypesAreDelegatedToTheInheritanceChecker() {
		DiagnosticSink sink = new DiagnosticSink();
		InheritanceChecker checker = new InheritanceChecker() {
			@Override
			public boolean isSubclassOf(String child, String parent) {
				return false;
			}

			@Override
			public String getCommonSuperclass(String type1, String type2) {
				assertEquals("java/lang/Integer", type1);
				assertEquals("java/lang/Long", type2);
				return "java/lang/Number";
			}
		};
		ExposedClassWriter writer = new ExposedClassWriter(sink, javaOnlyAwareness(), checker);

		assertEquals("java/lang/Number", writer.commonSuperclass("java/lang/Integer", "java/lang/Long"));
		assertFalse(sink.hasWarnings(), "Known types should not warn");
	}

	@Test
	void oneUnknownFrameTypeIsEnoughToFallBackToObject() {
		DiagnosticSink sink = new DiagnosticSink();
		ExposedClassWriter writer = new ExposedClassWriter(sink, javaOnlyAwareness(), unreachableChecker());

		assertEquals("java/lang/Object", writer.commonSuperclass("java/lang/Integer", "missing/B"));
		assertEquals(1, sink.warnings().size());
		assertWarning(sink.warnings().getFirst(), "Unknown type: missing/B");
	}

	@Test
	void unknownFrameTypesWithoutASinkStaySilent() {
		ExposedClassWriter writer = new ExposedClassWriter(null, javaOnlyAwareness(), unreachableChecker());

		assertEquals("java/lang/Object", writer.commonSuperclass("missing/A", "missing/B"));
	}

	@Test
	void withoutAwarenessEveryFrameTypeIsTreatedAsKnown() {
		DiagnosticSink sink = new DiagnosticSink();
		InheritanceChecker checker = new InheritanceChecker() {
			@Override
			public boolean isSubclassOf(String child, String parent) {
				return false;
			}

			@Override
			public String getCommonSuperclass(String type1, String type2) {
				return "java/lang/Object";
			}
		};
		ExposedClassWriter writer = new ExposedClassWriter(sink, null, checker);

		assertEquals("java/lang/Object", writer.commonSuperclass("missing/A", "missing/B"));
		assertFalse(sink.hasWarnings(), "Unknown-type warnings require a type-awareness service");
	}

	@Test
	void writerRequiresAnInheritanceChecker() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new JvmClassWriter(0, new DiagnosticSink(), javaOnlyAwareness(), null));
		assertEquals("Class writer requires an inheritance checker implementation", exception.getMessage());
	}

	private static void assertWarning(Diagnostic warning, String message) {
		assertEquals(message, warning.message());
		assertEquals(DiagnosticPhase.OUTPUT_VERIFICATION, warning.phase());
		assertEquals(DiagnosticCode.VERIFICATION_WARNING, warning.code());
		assertEquals(Location.UNKNOWN, warning.location());
	}

	private static final class ExposedClassWriter extends JvmClassWriter {
		private ExposedClassWriter(DiagnosticSink sink, TypeAwareness awareness, InheritanceChecker checker) {
			super(0, sink, awareness, checker);
		}

		private String commonSuperclass(String type1, String type2) {
			return getCommonSuperClass(type1, type2);
		}
	}
}

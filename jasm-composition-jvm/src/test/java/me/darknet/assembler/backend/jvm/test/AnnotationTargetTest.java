package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.backend.jvm.compile.AnnotationTarget;
import me.darknet.assembler.backend.jvm.compile.JvmCompilerOptions;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.error.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnotationTargetTest {
	@Test
	void parsesClassAndDottedClassPaths() {
		assertEquals(new AnnotationTarget.ClassTarget(3), parse("Example.3"));
		assertEquals(new AnnotationTarget.ClassTarget(0), parse("pkg/Outer.Inner.0"));
	}

	@Test
	void parsesFieldAndMethodPaths() {
		AnnotationTarget.MemberTarget field = assertInstanceOf(AnnotationTarget.MemberTarget.class, parse("Example.field.value.I.1"));
		assertEquals(AnnotationTarget.MemberKind.FIELD, field.kind());
		assertEquals("value", field.name());
		assertEquals("I", field.descriptor());
		assertEquals(1, field.index());

		AnnotationTarget.MemberTarget method = assertInstanceOf(AnnotationTarget.MemberTarget.class, parse("Example.method.hello.()V.0"));
		assertEquals(AnnotationTarget.MemberKind.METHOD, method.kind());
		assertEquals("hello", method.name());
		assertEquals("()V", method.descriptor());
		assertEquals(0, method.index());
	}

	@Test
	void rejectsMalformedPathsWithTargetValidationDiagnostics() {
		assertDiagnostic(null, "Annotation target path was not specified");
		assertDiagnostic("  ", "Annotation target path was not specified");
		assertDiagnostic("Example", "Invalid annotation target path: Example");
		assertDiagnostic("Example.abc", "Invalid annotation target index: abc");
		assertDiagnostic("Example.-1", "Annotation target index must be nonnegative: -1");
		assertDiagnostic("Example.method..()V.0", "Annotation target member name is missing: Example.method..()V.0");
		assertDiagnostic("Example.field.value.(!)V.0", "Invalid annotation target member descriptor: (!)V");
		assertDiagnostic("Example.method.hello.II.0", "Invalid annotation target member descriptor: II");
	}

	@Test
	void resolvesConfiguredPathAndLeavesTargetNullWhenAbsentOrInvalid() {
		// Valid path yields no diagnostics during target resolution and sets the annotation target
		AnnotationTarget expected = new AnnotationTarget.MemberTarget(AnnotationTarget.MemberKind.METHOD, "hello", "()V", 0);
		JvmCompilerOptions options = new JvmCompilerOptions().withAnnotationPath("Example.method.hello.()V.0");
		assertEquals(List.of(), options.resolveAnnotationTarget());
		assertEquals(expected, options.getAnnotationTarget());

		// Invalid path yields a diagnostic during target resolution and leaves the annotation target null.
		options.withAnnotationPath("Example.method.hello.II.0");
		List<Diagnostic> diagnostics = options.resolveAnnotationTarget();
		assertEquals(1, diagnostics.size());
		assertEquals(DiagnosticPhase.TARGET_VALIDATION, diagnostics.getFirst().phase());
		assertEquals(DiagnosticCode.MALFORMED_DECLARATION, diagnostics.getFirst().code());
		assertNull(options.getAnnotationTarget());

		// Null path yields no diagnostics during target resolution and leaves the annotation target null.
		options.withAnnotationPath(null);
		assertEquals(List.of(), options.resolveAnnotationTarget());
		assertNull(options.getAnnotationTarget());
	}

	private static AnnotationTarget parse(String path) {
		Outcome<AnnotationTarget> outcome = AnnotationTarget.parse(path);
		assertEquals(List.of(), outcome.diagnostics());
		return outcome.requireValue();
	}

	private static void assertDiagnostic(String path, String message) {
		Outcome<AnnotationTarget> outcome = AnnotationTarget.parse(path);
		assertEquals(1, outcome.diagnostics().size());
		Diagnostic diagnostic = outcome.diagnostics().getFirst();
		assertEquals(Severity.ERROR, diagnostic.severity());
		assertEquals(DiagnosticPhase.TARGET_VALIDATION, diagnostic.phase());
		assertEquals(DiagnosticCode.MALFORMED_DECLARATION, diagnostic.code());
		assertEquals(message, diagnostic.message());
		assertNull(diagnostic.location());
	}
}

package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikOperandsTest {

	@Test
	void packedSwitchAcceptsDifferentIntegerFormats() {
		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 127, targets: { L0 } }
				    return-void
				  }
				}
				""");
		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 0x7F, targets: { L0 } }
				    return-void
				  }
				}
				""");
		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 0b01111111, targets: { L0 } }
				    return-void
				  }
				}
				""");
	}

	@Test
	void packedSwitchRequiresAnIntegerFirstKey() {
		// A double is not valid as a key
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 1.5, targets: { L0 } }
				    return-void
				  }
				}
				""", "integer literal");
	}

	@Test
	void invokeCustomRequiresAnArgsArray() {
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    invoke-custom { v0 } callsite ()V { invokestatic, example/Bootstrap.bootstrap, ()V } bad
				    return-void
				  }
				}
				""", "args array");
	}

	@Test
	void filledNewArrayRequiresRegisterIdentifiers() {
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array { 1 } [I
				    return-void
				  }
				}
				""", "register");
	}

	@Test
	void registerListsAndRangesEnforceEncodingCounts() {
		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    invoke-static { v0, v1, v2, v3, v4 } java/lang/Math.abs (IIIII)V
				    return-void
				  }
				}
				""");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    invoke-static { v0, v1, v2, v3, v4, v5 } java/lang/Math.abs (IIIIII)V
				    return-void
				  }
				}
				""", "at most 5 registers but got 6");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    invoke-static/range { v0, v2, v3 } java/lang/Math.abs (III)V
				    return-void
				  }
				}
				""", "Range instructions require first and last register bounds");
	}

	@Test
	void reportsDescriptorProblemsWithReasonAndOffset() {
		// These used to be accepted at parse time and then throw out of the dex type parser during
		// emission, as "Failed to compile Dalvik source: ..." at the class declaration's location.
		assertDalvikError("""
				.method public static test ()V {
				  code: {
				    check-cast v0 Lbad
				    return-void
				  }
				}
				""", "missing terminating ';' at index 4");

		assertDalvikError("""
				.method public static test ()V {
				  code: {
				    const-class v0 I
				    return-void
				  }
				}
				""", "expected a class or array type, not a primitive");

		assertDalvikError("""
				.method public static test ()V {
				  code: {
				    const-method-type v0 [I
				    return-void
				  }
				}
				""", "method descriptor must start with '('");
	}

	@Test
	void validatesConstantDescriptorsInArgumentArrays() {
		// The constant verifier used to accept any identifier, leaving a malformed descriptor to throw
		// out of the dex type factory during emission.
		assertDalvikOk("""
				.method public static custom (I)V {
				  registers: 3,
				  code: {
				    invoke-custom/range { v0, v2 } callsite (III)V ConstantBootstraps.nullConstant { java/lang/System, Ljava/lang/String;, [I, (I)V, null, true }
				    return-void
				  }
				}
				""");

		String template = """
				.method public static custom (I)V {
				  registers: 3,
				  code: {
				    invoke-custom/range { v0, v2 } callsite (III)V ConstantBootstraps.nullConstant { %s }
				    return-void
				  }
				}
				""";
		assertDalvikError(template.formatted("Lbad"),
				"Expected class, method or array descriptor but got identifier 'Lbad'");
		assertDalvikError(template.formatted("[bad"), "unknown type descriptor character 'b'");
		assertDalvikError(template.formatted("(bad"), "unknown type descriptor character 'b'");
	}

	@Test
	void acceptsLPrefixedInternalNameConstants() {
		// A name with a package path but no terminator is an internal name, not a broken descriptor, so
		// it stays legal exactly as it does for the constant mapper.
		assertDalvikOk("""
				.method public static custom (I)V {
				  registers: 3,
				  code: {
				    invoke-custom/range { v0, v2 } callsite (III)V ConstantBootstraps.nullConstant { Lowner/Type }
				    return-void
				  }
				}
				""");
	}

	@Test
	void payloadsDeclareTheirKeySetAndElementTypes() {
		// The packed switch payload declares exactly two keys, so a third is a shape error.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    packed-switch v0 { first: 5, targets: { A }, extra: 1 }
				  A:
				    return-void
				  }
				}
				""", "keys in packed switch");

		// The data payload declares its values as numeric literals, so a non-numeric entry is rejected
		// here rather than being silently dropped while the width is inferred.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    fill-array-data v0 { width: 2, values: { "not a number" } }
				    return-void
				  }
				}
				""", "numeric literal");

		// A sparse switch reads its case keys through the shared parser; sign and radix both work.
		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    sparse-switch v0 { -1: A, 0x10: A, 0b10: A }
				  A:
				    return-void
				  }
				}
				""");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    sparse-switch v0 { hello: A }
				  A:
				    return-void
				  }
				}
				""", "Expected integer switch key");
	}

	@Test
	void binaryLiteralOperationsDeclareTheirSignedRange() {
		// The operand schema owns the range, so an out-of-range literal is rejected while parsing
		// rather than at emission.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    add-int/lit8 v0 v1 128
				    return-void
				  }
				}
				""", "literal in the signed range -128 to 127");

		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    add-int/lit8 v0 v1 127
				    return-void
				  }
				}
				""");

		// The 16-bit forms accept the wider range under the same message shape, and both bounds hold.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    add-int/lit16 v0 v1 32768
				    return-void
				  }
				}
				""", "literal in the signed range -32768 to 32767");

		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    rsub-int v0 v1 -32768
				    return-void
				  }
				}
				""");
	}

	@Test
	void filledNewArrayAcceptsOnlySingleWordArrayTypes() {
		// The dex specification requires the contents to be single-word: "no arrays of long or double,
		// but reference types are acceptable". A long or double element needs two registers, which the
		// register fields cannot express, so the type is rejected instead of encoding overlapping
		// elements. Both the list and the range form share the rule.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array { v0 } [J
				    return-void
				  }
				}
				""", "contents must be single-word");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array { v0 } [D
				    return-void
				  }
				}
				""", "contents must be single-word");

		// The range form is spelled as its first and last register, and shares the rule.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array/range { v0, v3 } [J
				    return-void
				  }
				}
				""", "contents must be single-word");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array/range { v0, v3 } [D
				    return-void
				  }
				}
				""", "contents must be single-word");
	}

	@Test
	void filledNewArrayAcceptsSingleWordAndReferenceElements() {
		// Every single-word primitive and every reference is legal, including an array of arrays: a nested
		// array is a reference, so it is one register even when its own element is a long or a double.
		for (String type : List.of("[I", "[F", "[Z", "[B", "[C", "[S", "[Ljava/lang/Object;", "[[D", "[[J",
				"[[Ljava/lang/String;")) {
			assertDalvikOk("""
					.method public static test ()V {
					  code: {
					    filled-new-array { v0 } %s
					    return-void
					  }
					}
					""".formatted(type));

			// The range form shares the same accepted domain.
			assertDalvikOk("""
					.method public static test ()V {
					  code: {
					    filled-new-array/range { v0, v3 } %s
					    return-void
					  }
					}
					""".formatted(type));
		}
	}

	@Test
	void filledNewArrayRejectsANonArrayType() {
		// The instruction constructs an array, so a bare class name is well-formed but not an array.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array { v0 } Ljava/lang/Object;
				    return-void
				  }
				}
				""", "array descriptor");
	}

	private static Outcome<?> processSemantically(String source) {
		var ast = AssemblyParseFixture.processAst("<test>", source, DalvikTargetContext.INSTANCE);
		if (ast.hasErrors())
			return ast;
		// Operand definitions are semantic, so these helpers must run the lowering stage after AST parsing succeeds.
		return SemanticProcessor.process(ast.requireValue(), DalvikTargetContext.INSTANCE);
	}

	private static void assertDalvikError(String source, String messagePart) {
		Outcome<?> result = processSemantically(source);
		DiagnosticAssertions.assertHasErrors(result, "Expected Dalvik operand validation errors");
		assertTrue(DiagnosticAssertions.formatErrors(result.errors()).contains(messagePart),
				DiagnosticAssertions.formatErrors(result.errors()));
	}

	private static void assertDalvikOk(String source) {
		var result = processSemantically(source);
		DiagnosticAssertions.requireSuccess(result, "Expected valid Dalvik operand input");
	}

	private static void assertErrorContains(String source, String messagePart) {
		Outcome<?> result = processSemantically(source);
		DiagnosticAssertions.assertHasErrors(result, "Expected Dalvik operand validation errors");
		assertTrue(DiagnosticAssertions.formatErrors(result.errors()).contains(messagePart));
	}
}

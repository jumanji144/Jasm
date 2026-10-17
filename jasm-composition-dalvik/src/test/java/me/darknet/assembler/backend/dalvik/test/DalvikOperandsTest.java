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
	void ordinaryListsCountLogicalArgumentsAgainstTheirDescriptor() {
		// One entry names one argument, so a list may be longer than five words. Whether its words fit an encoding is
		// decided once they are expanded, which is a backend concern and is covered by the encoding boundary tests.
		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    invoke-static { v0, v1, v2, v3, v4 } java/lang/Math.abs (IIIII)V
				    return-void
				  }
				}
				""");
		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    invoke-static { v0, v1, v2, v3, v4, v5 } java/lang/Math.abs (IIIIII)V
				    return-void
				  }
				}
				""");

		// A wide argument is one entry, so (JJ)V takes two entries and not the four words they occupy.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    invoke-static { v0, v1, v2, v3, v4 } java/lang/Math.abs (JJ)V
				    return-void
				  }
				}
				""", "invoke-static takes 2 arguments for (JJ)V");
	}

	@Test
	void retiredInvokeRangeSpellingsAreUnknownInstructions() {
		// Ranges are chosen from the words, so the invoke /range mnemonics no longer exist in source.
		for (String mnemonic : List.of("invoke-static/range", "invoke-virtual/range", "invoke-direct/range",
				"invoke-super/range", "invoke-interface/range", "invoke-custom/range", "invoke-polymorphic/range")) {
			assertErrorContains("""
					.method public static test ()V {
					  code: {
					    %s { v0, v1 } demo/Target.call (II)V
					    return-void
					  }
					}
					""".formatted(mnemonic), "Unknown instruction: " + mnemonic);
		}
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
				    invoke-custom { v0, v1, v2 } callsite (III)V ConstantBootstraps.nullConstant { java/lang/System, Ljava/lang/String;, [I, (I)V, null, true }
				    return-void
				  }
				}
				""");

		String template = """
				.method public static custom (I)V {
				  registers: 3,
				  code: {
				    invoke-custom { v0, v1, v2 } callsite (III)V ConstantBootstraps.nullConstant { %s }
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
				    invoke-custom { v0, v1, v2 } callsite (III)V ConstantBootstraps.nullConstant { Lowner/Type }
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
	void arithmeticSecondSourceIsARegisterOrAnIntegerLiteral() {
		// An identifier names a register and a number is its value, so each is accepted where the other is not.
		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    add-int v0 v1 v2
				    add-int v0 v1 3
				    add-int v0 v1 -3
				    add-int v0 v1 2147483647
				    add-int v0 v1 -2147483648
				    return-void
				  }
				}
				""");

		// The value must be an int. A wider integer is refused rather than wrapped, and a floating-point number is refused
		// rather than truncated to an integer.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    add-int v0 v1 2147483648
				    return-void
				  }
				}
				""", "integer literal from -2147483648 through 2147483647");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    add-int v0 v1 1.5
				    return-void
				  }
				}
				""", "integer literal");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    add-int v0 v1 "three"
				    return-void
				  }
				}
				""", "register or integer literal");
	}

	@Test
	void registerOnlyPositionsRefuseNumbers() {
		// A number in a register position is refused, so no slot is ever allocated under a name such as "3".
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    sub-int v0 v1 3
				    return-void
				  }
				}
				""", "Expected register");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    move 3 v0
				    return-void
				  }
				}
				""", "Expected register");
	}

	@Test
	void reverseSubtractionTakesOnlyAnIntegerLiteral() {
		assertDalvikOk("""
				.method public static test ()V {
				  code: {
				    rsub-int v0 v1 -5
				    rsub-int v0 v1 -2147483648
				    return-void
				  }
				}
				""");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    rsub-int v0 v1 v2
				    return-void
				  }
				}
				""", "Expected integer literal");
	}

	@Test
	void retiredFilledArrayRangeSpellingIsAnUnknownInstruction() {
		// The range form is chosen from the registers, so filled-new-array takes one spelling for every length.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array/range { v0, v1 } [I
				    return-void
				  }
				}
				""", "Unknown instruction: filled-new-array/range");
	}

	@Test
	void retiredArithmeticSpellingsAreUnknownInstructions() {
		// The literal and two-address spellings were replaced by the ordinary mnemonics, which select the encoding.
		for (String mnemonic : List.of("add-int/lit8", "add-int/lit16", "rsub-int/lit8", "add-int/2addr")) {
			assertErrorContains("""
					.method public static test ()V {
					  code: {
					    %s v0 v1 3
					    return-void
					  }
					}
					""".formatted(mnemonic), "Unknown instruction: " + mnemonic);
		}
	}

	@Test
	void filledNewArrayAcceptsOnlySingleWordArrayTypes() {
		// The dex specification requires the contents to be single-word: "no arrays of long or double,
		// but reference types are acceptable". A long or double element needs two registers, which the
		// register fields cannot express, so the type is rejected instead of encoding overlapping
		// elements. The rule does not depend on how many registers the list names.
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

		// A longer list is subject to the same rule.
		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array { v0, v1, v2, v3 } [J
				    return-void
				  }
				}
				""", "contents must be single-word");

		assertErrorContains("""
				.method public static test ()V {
				  code: {
				    filled-new-array { v0, v1, v2, v3 } [D
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

			// A longer list shares the same accepted domain.
			assertDalvikOk("""
					.method public static test ()V {
					  code: {
					    filled-new-array { v0, v1, v2, v3 } %s
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

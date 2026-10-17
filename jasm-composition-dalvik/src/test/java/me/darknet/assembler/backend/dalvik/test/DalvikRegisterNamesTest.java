package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that printed bodies name parameters and locals by their source names, keep those names through a DEX round
 * trip, and warn about numeric references to named slots without blocking output.
 */
class DalvikRegisterNamesTest {
	private static final int DEX_VERSION = 35;

	private static final String SUM = """
			.super java/lang/Object
			.class public Example {
			    .method public static sum (II)I {
			        registers: 4,
			        parameters: { first, second },
			        code: {
			            add-int total first second
			            return total
			        }
			    }
			}
			""";

	private static final String INSTANCE = """
			.super java/lang/Object
			.class public Example {
			    .method public count (I)I {
			        registers: 3,
			        parameters: { value },
			        code: {
			            iget total this sample/Owner.count I
			            add-int total total value
			            return total
			        }
			    }
			}
			""";

	private static final String WIDE = """
			.super java/lang/Object
			.class public Example {
			    .method public static widen (JI)J {
			        registers: 7,
			        parameters: { wide, count },
			        code: {
			            int-to-long extra count
			            add-long total wide extra
			            return-wide total
			        }
			    }
			}
			""";

	private static final String ARRAY_ELEMENT = """
			.super java/lang/Object
			.class public Example {
			    .method public static fetch ()V {
			        registers: 3,
			        code: {
			            aget element list position
			            return-void
			        }
			    }
			}
			""";

	private static final String UNNAMED = """
			.super java/lang/Object
			.class public Example {
			    .method public static plain (II)I {
			        registers: 3,
			        code: {
			            add-int v0 v1 v2
			            return v0
			        }
			    }
			}
			""";

	private static final String CALL = """
			.super java/lang/Object
			.class public Example {
			    .method public static call (I)I {
			        registers: 3,
			        parameters: { value },
			        code: {
			            invoke-static { value } java/lang/Math.abs (I)I
			            move-result result
			            return result
			        }
			    }
			}
			""";

	private static final String LOOP = """
			.super java/lang/Object
			.class public Example {
			    .method public static loop (I)I {
			        registers: 3,
			        parameters: { limit },
			        code: {
			            const sum 0
			            const index 0
			        Loop:
			            if-ge index limit Done
			            add-int sum sum index
			            add-int index index 1
			            goto Loop
			        Done:
			            return sum
			        }
			    }
			}
			""";

	private static final String MIX = """
			.super java/lang/Object
			.class public Example {
			    .method public static mix (JLjava/lang/Object;)I {
			        registers: 8,
			        parameters: { wide, receiver },
			        code: {
			            invoke-static { wide } java/lang/Long.hashCode (J)I
			            move-result count
			            const-wide big 5L
			            invoke-static { big } java/lang/Long.hashCode (J)I
			            move-result other
			            add-int total count other
			            return total
			        }
			    }
			}
			""";

	@Test
	void parametersAndLocalsPrintBySourceNameInTheirSlots() throws Exception {
		String printed = printDecoded(SUM);
		assertEquals(List.of("A:", "add-int total first second", "return total", "B:"), body(printed, "sum"), printed);
		assertTrue(printed.contains("parameters: { first, second }"), printed);
	}

	@Test
	void receiverPrintsAsThis() throws Exception {
		String printed = printDecoded(INSTANCE);
		assertEquals(List.of("A:", "iget total this sample/Owner.count I", "add-int total total value", "return total",
				"B:"), body(printed, "count"), printed);
	}

	@Test
	void wideParameterAndWideLocalPrintTheirLowWords() throws Exception {
		String printed = printDecoded(WIDE);
		assertEquals(List.of("A:", "int-to-long extra count", "add-long total wide extra", "return-wide total", "B:"),
				body(printed, "widen"), printed);
	}

	@Test
	void arrayElementNamesFollowTheCompilerAllocationOrder() throws Exception {
		String printed = printDecoded(ARRAY_ELEMENT);
		assertEquals(List.of("A:", "aget element list position", "return-void", "B:"), body(printed, "fetch"), printed);
	}

	@Test
	void unnamedParametersKeepTheirNumericBody() throws Exception {
		String printed = printDecoded(UNNAMED);
		assertFalse(printed.contains("parameters:"), printed);
		assertEquals(List.of("A:", "add-int v0 v1 v2", "return v0", "B:"), body(printed, "plain"), printed);
	}

	@Test
	void namedLocalIsRecordedWithoutTypeOverTheWholeMethod() throws Exception {
		DexFile file = roundTrip(compile(SUM).definitions());
		Code code = file.definitions().getFirst().getMethods().values().stream()
				.filter(method -> method.getName().equals("sum")).findFirst().orElseThrow().getCode();
		List<DebugInformation.LocalVariable> locals = code.getDebugInfo().locals();
		assertEquals(1, locals.size());
		assertEquals(0, locals.getFirst().register());
		assertEquals("total", locals.getFirst().name());
		assertNull(locals.getFirst().type());
		assertEquals(0, locals.getFirst().start().position());
	}

	@Test
	void samplesRoundTripWithIdenticalTextInstructionsAndNames() throws Exception {
		for (String source : List.of(SUM, INSTANCE, WIDE, ARRAY_ELEMENT, UNNAMED, CALL, LOOP, MIX)) {
			DexFile first = roundTrip(compile(source).definitions());
			String printed = printClass(first);
			DexFile second = roundTrip(compile(printed).definitions());
			assertEquals(printed, printClass(second), printed);
			assertEquals(dumpMethods(first), dumpMethods(second), printed);
			assertEquals(nameDump(first), nameDump(second), printed);
		}
	}

	@Test
	void numericRegistersPrintSlotNumbersAndRecompileWithoutWarnings() throws Exception {
		DexFile file = roundTrip(compile(SUM).definitions());
		ClassDefinition definition = file.definitions().getFirst();
		String numeric = printNumeric(definition);
		assertFalse(numeric.contains("parameters:"), numeric);
		assertEquals(List.of("A:", "add-int v0 v2 v3", "return v0", "B:"), body(numeric, "sum"), numeric);

		CompileResult recompiled = compile(numeric);
		assertEquals(List.of(), recompiled.warnings(), numeric);
		assertEquals(dumpMethods(file), dumpMethods(roundTrip(recompiled.definitions())), numeric);
	}

	@Test
	void numericReferenceToParameterWarnsAndStillWritesDex() throws Exception {
		CompileResult result = compile("""
				.super java/lang/Object
				.class public Example {
				    .method public static sum (II)I {
				        registers: 4,
				        parameters: { first, second },
				        code: {
				            add-int total v2 second
				            return total
				        }
				    }
				}
				""");
		assertEquals(List.of("Numeric register v2 is the slot of parameter 'first'; write 'first' instead"),
				messages(result.warnings()));
		assertEquals(DiagnosticCode.NUMERIC_REFERENCE_TO_NAMED_SLOT, result.warnings().getFirst().code());
		assertEquals(1, roundTrip(result.definitions()).definitions().size());
	}

	@Test
	void explicitSlotIsNeverClaimedByALocalSoItDoesNotWarn() throws Exception {
		// The compiler reserves v0 for the explicit operand before it names any local, so total takes v1 and v0 stays numeric.
		CompileResult result = compile("""
				.super java/lang/Object
				.class public Example {
				    .method public static local (I)I {
				        registers: 3,
				        parameters: { value },
				        code: {
				            const total 1
				            add-int total v0 value
				            return total
				        }
				    }
				}
				""");
		assertTrue(result.warnings().isEmpty(), messages(result.warnings()).toString());
		assertEquals(List.of("A:", "const total #0x00000001", "add-int total v0 value", "return total", "B:"),
				body(printClass(roundTrip(result.definitions())), "local"));
	}

	@Test
	void numericHighWordOfWideParameterWarns() {
		CompileResult result = compile("""
				.super java/lang/Object
				.class public Example {
				    .method public static high (JI)J {
				        registers: 4,
				        parameters: { wide, count },
				        code: {
				            const v2 0
				            return-wide wide
				        }
				    }
				}
				""");
		assertEquals(List.of("Numeric register v2 is the high word of wide parameter 'wide'"), messages(result.warnings()));
	}

	@Test
	void wideNumericOperandCoveringANamedSlotWarns() {
		CompileResult result = compile("""
				.super java/lang/Object
				.class public Example {
				    .method public static covered (I)V {
				        registers: 4,
				        parameters: { size },
				        code: {
				            new-array v0 size [J
				            aget-wide v2 v0 size
				            return-void
				        }
				    }
				}
				""");
		assertEquals(List.of("Numeric wide register v2 covers v3, which is the slot of parameter 'size'"),
				messages(result.warnings()));
	}

	@Test
	void rawWordNamingAParameterSlotWarns() {
		CompileResult result = compile("""
				.super java/lang/Object
				.class public Example {
				    .method public static raw (II)I {
				        registers: 4,
				        parameters: { first, second },
				        code: {
				            invoke-static { words: { v2 } } java/lang/Math.abs (I)I
				            move-result v0
				            return v0
				        }
				    }
				}
				""");
		assertEquals(List.of("Numeric register v2 is the slot of parameter 'first'; write 'first' instead"),
				messages(result.warnings()));
	}

	@Test
	void receiverParameterAliasAndUnnamedSlotsDoNotWarn() {
		CompileResult receiver = compile("""
				.super java/lang/Object
				.class public Example {
				    .method public count (I)I {
				        registers: 3,
				        parameters: { value },
				        code: {
				            iget total this sample/Owner.count I
				            add-int total p0 value
				            return total
				        }
				    }
				}
				""");
		assertTrue(receiver.warnings().isEmpty(), messages(receiver.warnings()).toString());

		CompileResult unnamed = compile(UNNAMED);
		assertTrue(unnamed.warnings().isEmpty(), messages(unnamed.warnings()).toString());
	}

	private record CompileResult(List<ClassDefinition> definitions, List<Diagnostic> warnings) {}

	private static CompileResult compile(String source) {
		List<ClassDefinition> definitions = new ArrayList<>();
		List<Diagnostic> warnings = new ArrayList<>();
		TestUtils.processDalvik(source, TestUtils.options(),
				result -> definitions.add(((DalvikClassRepresentation) result.representation()).definition()),
				warnings::addAll);
		return new CompileResult(definitions, warnings);
	}

	private static DexFile roundTrip(List<ClassDefinition> definitions) throws IOException {
		return DalvikDexFixture.readDex(DalvikDexFixture.writeDex(definitions, DEX_VERSION));
	}

	private static String printDecoded(String source) throws IOException {
		return printClass(roundTrip(compile(source).definitions()));
	}

	private static String printClass(DexFile file) {
		return String.join("\n", DalvikDexFixture.printAll(file).values());
	}

	private static String printNumeric(ClassDefinition definition) {
		PrintContext<?> context = new PrintContext<>("\t");
		new DalvikClassPrinter(definition, true).print(context);
		return context.toString();
	}

	/**
	 * @return The trimmed body lines of the named method, which run from its {@code code: {} line to the closing brace.
	 */
	private static List<String> body(String printed, String method) {
		List<String> lines = new ArrayList<>();
		boolean inMethod = false;
		for (String line : printed.lines().map(String::trim).toList()) {
			if (line.startsWith(".method ") && line.contains(" " + method + " (")) {
				inMethod = true;
				continue;
			}
			if (!inMethod)
				continue;
			if (line.equals("}"))
				break;
			lines.add(line);
		}
		// The loop stops at the closing brace of the code block, so everything after its opening line is the body.
		List<String> body = lines.subList(lines.indexOf("code: {") + 1, lines.size());
		return body.stream().filter(line -> !line.isEmpty()).toList();
	}

	private static List<String> messages(List<Diagnostic> diagnostics) {
		return diagnostics.stream().map(Diagnostic::message).toList();
	}

	private static String dumpMethods(DexFile file) throws Exception {
		StringBuilder out = new StringBuilder();
		for (ClassDefinition definition : file.definitions()) {
			for (MethodMember method : sortedMethods(definition)) {
				if (method.getCode() == null)
					continue;
				out.append(method.getName()).append(method.getType().descriptor()).append('\n');
				out.append(dumpCode(method.getCode()));
			}
		}
		return out.toString();
	}

	private static String nameDump(DexFile file) {
		StringBuilder out = new StringBuilder();
		for (ClassDefinition definition : file.definitions()) {
			for (MethodMember method : sortedMethods(definition)) {
				if (method.getCode() == null || method.getCode().getDebugInfo() == null)
					continue;
				out.append(method.getName()).append(method.getType().descriptor()).append(' ');
				for (DebugInformation.LocalVariable local : method.getCode().getDebugInfo().locals())
					out.append(local.register()).append('=').append(local.name()).append(' ');
				out.append('\n');
			}
		}
		return out.toString();
	}

	private static List<MethodMember> sortedMethods(ClassDefinition definition) {
		List<MethodMember> methods = new ArrayList<>(definition.getMethods().values());
		methods.sort(Comparator.comparing((MethodMember method) -> method.getName() + method.getType().descriptor()));
		return methods;
	}

	/**
	 * @return The header and executable instructions of the code, with branch targets rendered by code offset.
	 */
	private static String dumpCode(Code code) throws IllegalAccessException {
		StringBuilder out = new StringBuilder();
		out.append("in=").append(code.getIn()).append(" out=").append(code.getOut())
				.append(" registers=").append(code.getRegisters()).append('\n');
		for (Instruction instruction : code.getInstructions()) {
			if (instruction instanceof Label)
				continue;
			render(out, instruction, 0);
			out.append('\n');
		}
		return out.toString();
	}

	private static void render(StringBuilder out, Object value, int depth) throws IllegalAccessException {
		if (value == null) {
			out.append("null");
		} else if (value instanceof Label label) {
			out.append('@').append(label.position());
		} else if (value instanceof String || value instanceof Number || value instanceof Boolean
				|| value instanceof Character || value instanceof Enum<?>) {
			out.append(value);
		} else if (value instanceof Collection<?> collection) {
			out.append('[');
			boolean first = true;
			for (Object element : collection) {
				if (!first)
					out.append(", ");
				first = false;
				render(out, element, depth + 1);
			}
			out.append(']');
		} else if (value instanceof Map<?, ?> map) {
			out.append('{');
			boolean first = true;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (!first)
					out.append(", ");
				first = false;
				render(out, entry.getKey(), depth + 1);
				out.append(':');
				render(out, entry.getValue(), depth + 1);
			}
			out.append('}');
		} else if (value.getClass().isArray()) {
			out.append('[');
			for (int i = 0; i < Array.getLength(value); i++) {
				if (i > 0)
					out.append(", ");
				render(out, Array.get(value, i), depth + 1);
			}
			out.append(']');
		} else if (value.getClass().getName().startsWith("java.") || depth > 8) {
			out.append(value.getClass().getSimpleName());
		} else {
			out.append(value.getClass().getSimpleName()).append('{');
			boolean first = true;
			for (Class<?> type = value.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
				for (Field field : type.getDeclaredFields()) {
					if (Modifier.isStatic(field.getModifiers()))
						continue;
					field.setAccessible(true);
					if (!first)
						out.append(", ");
					first = false;
					out.append(field.getName()).append('=');
					render(out, field.get(value), depth + 1);
				}
			}
			out.append('}');
		}
	}
}

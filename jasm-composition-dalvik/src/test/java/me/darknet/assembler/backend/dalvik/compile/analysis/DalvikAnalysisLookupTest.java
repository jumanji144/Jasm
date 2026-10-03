package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.FieldReference;
import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.analysis.registry.BasicFieldValueLookup;
import me.darknet.assembler.analysis.registry.BasicMethodValueLookup;
import me.darknet.assembler.analysis.registry.FieldValueLookup;
import me.darknet.assembler.analysis.registry.MethodValueLookup;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompilerOptions;
import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.instructions.MoveResultInstruction;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikAnalysisLookupTest {
	@Test
	void resolvesBasicMathAbsValue() {
		DalvikCompilerOptions options = new DalvikCompilerOptions();
		assertInstanceOf(BasicMethodValueLookup.class, options.getMethodValueLookup());

		Compilation compilation = compile("""
				.super java/lang/Object
				.class public LookupDefaults {
				    .method public static mathAbs ()I {
				        registers: 2,
				        code: {
				            const v1 -12
				            invoke-static { v1 } java/lang/Math.abs (I)I
				            move-result v0
				            return v0
				        }
				    }
				}
				""", options);

		MethodMember method = compilation.method("mathAbs", "()I");
		DalvikAnalysisResults results = compilation.results("mathAbs", "()I");
		MoveResultInstruction consumer = instruction(method, MoveResultInstruction.class);
		assertEquals(new Value.KnownIntValue(12), results.getStateBefore(consumer).pendingResult());
		assertSame(DalvikRegisterState.Undefined.INSTANCE, results.getStateBefore(consumer).slot(0));
		assertEquals(new Value.KnownIntValue(12), valueBeforeReturn(compilation, method));
		assertEquals(new Value.KnownIntValue(12), terminalValue(results, method));
		assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
	}

	@Test
	void resolvesBasicStringToStringValue() {
		DalvikCompilerOptions options = new DalvikCompilerOptions();
		assertInstanceOf(BasicMethodValueLookup.class, options.getMethodValueLookup());

		Compilation compilation = compile("""
				.super java/lang/Object
				.class public LookupDefaults {
				    .method public static stringToString ()Ljava/lang/String; {
				        registers: 2,
				        code: {
				            const-string v1 "known-string"
				            invoke-virtual { v1 } java/lang/String.toString ()Ljava/lang/String;
				            move-result-object v0
				            return-object v0
				        }
				    }
				}
				""", options);

		MethodMember method = compilation.method("stringToString", "()Ljava/lang/String;");
		DalvikAnalysisResults results = compilation.results("stringToString", "()Ljava/lang/String;");
		MoveResultInstruction consumer = instruction(method, MoveResultInstruction.class);
		assertEquals(Values.valueOfString("known-string"), results.getStateBefore(consumer).pendingResult());
		assertEquals(Values.valueOfString("known-string"), terminalValue(results, method));
		assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
	}

	@Test
	void resolvesBasicMathPiValueAndWideTail() {
		DalvikCompilerOptions options = new DalvikCompilerOptions();
		assertInstanceOf(BasicFieldValueLookup.class, options.getFieldValueLookup());

		Compilation compilation = compile("""
				.super java/lang/Object
				.class public LookupDefaults {
				    .method public static mathPi ()D {
				        registers: 2,
				        code: {
				            sget-wide v0 java/lang/Math.PI D
				            return-wide v0
				        }
				    }
				}
				""", options);

		MethodMember method = compilation.method("mathPi", "()D");
		DalvikAnalysisResults results = compilation.results("mathPi", "()D");
		DalvikRegisterState state = results.getStateBefore(instruction(method, ReturnInstruction.class));
		assertEquals(new Value.KnownDoubleValue(Math.PI), value(state, 0));
		assertEquals(new DalvikRegisterState.WideTail(0), state.slot(1));
		assertEquals(new Value.KnownDoubleValue(Math.PI), terminalValue(results, method));
		assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
	}

	@Test
	void keepsMissingMethodUnknownFallbackAndPendingResult() {
		DalvikCompilerOptions options = new DalvikCompilerOptions();
		assertInstanceOf(BasicMethodValueLookup.class, options.getMethodValueLookup());

		Compilation compilation = compile("""
				.super java/lang/Object
				.class public LookupDefaults {
				    .method public static missingMethod ()I {
				        registers: 2,
				        code: {
				            const v1 7
				            invoke-static { v1 } sample/Owner.missing (I)I
				            move-result v0
				            return v0
				        }
				    }
				}
				""", options);

		MethodMember method = compilation.method("missingMethod", "()I");
		DalvikAnalysisResults results = compilation.results("missingMethod", "()I");
		MoveResultInstruction consumer = instruction(method, MoveResultInstruction.class);
		assertEquals(Values.INT_VALUE, results.getStateBefore(consumer).pendingResult());
		assertEquals(Values.INT_VALUE, valueBeforeReturn(compilation, method));
		assertEquals(Values.INT_VALUE, terminalValue(results, method));
		assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
	}

	@Test
	void keepsMissingStaticFieldUnknownFallbackAndWideTail() {
		DalvikCompilerOptions options = new DalvikCompilerOptions();
		assertInstanceOf(BasicFieldValueLookup.class, options.getFieldValueLookup());

		Compilation compilation = compile("""
				.super java/lang/Object
				.class public LookupDefaults {
				    .method public static missingStaticField ()J {
				        registers: 2,
				        code: {
				            sget-wide v0 sample/Owner.missing J
				            return-wide v0
				        }
				    }
				}
				""", options);

		MethodMember method = compilation.method("missingStaticField", "()J");
		DalvikAnalysisResults results = compilation.results("missingStaticField", "()J");
		DalvikRegisterState state = results.getStateBefore(instruction(method, ReturnInstruction.class));
		assertEquals(Values.LONG_VALUE, value(state, 0));
		assertEquals(new DalvikRegisterState.WideTail(0), state.slot(1));
		assertEquals(Values.LONG_VALUE, terminalValue(results, method));
		assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
	}

	@Test
	void keepsMissingInstanceFieldUnknownArrayFallback() {
		DalvikCompilerOptions options = new DalvikCompilerOptions();
		assertInstanceOf(BasicFieldValueLookup.class, options.getFieldValueLookup());

		Compilation compilation = compile("""
				.super java/lang/Object
				.class public LookupDefaults {
				    .method public static missingInstanceField ()[Ljava/lang/String; {
				        registers: 2,
				        code: {
				            const-string v0 "receiver"
				            iget-object v1 v0 sample/Owner.missing [Ljava/lang/String;
				            return-object v1
				        }
				    }
				}
				""", options);

		MethodMember method = compilation.method("missingInstanceField", "()[Ljava/lang/String;");
		DalvikAnalysisResults results = compilation.results("missingInstanceField", "()[Ljava/lang/String;");
		DalvikRegisterState state = results.getStateBefore(instruction(method, ReturnInstruction.class));
		Value unknownArray = Values.valueOfArray(new ArrayDescriptor(new ClassDescriptor("java/lang/String")));
		assertEquals(unknownArray, value(state, 1));
		assertEquals(unknownArray, terminalValue(results, method));
		assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
	}

	@Test
	void configuredLookupsReplaceDefaultsAndReceiveLogicalArgumentsAndContexts() {
		List<List<Value>> combinedArguments = new ArrayList<>();
		List<Value.ObjectValue> concatContexts = new ArrayList<>();
		List<List<Value>> concatArguments = new ArrayList<>();
		List<Value.ObjectValue> fieldContexts = new ArrayList<>();
		boolean[] absUsedStaticContext = {false};
		boolean[] piUsedStaticContext = {false};
		boolean[] missingMethodCalled = {false};
		boolean[] missingStaticFieldCalled = {false};
		boolean[] missingInstanceFieldCalled = {false};

		MethodValueLookup methodLookup = (method, context, parameters) -> {
			if (method.owner().equals("java/lang/Math") && method.name().equals("abs") && method.descriptor().equals("(I)I")) {
				absUsedStaticContext[0] = context == null;
				return Values.valueOf(91);
			}
			if (method.owner().equals("sample/Owner") && method.name().equals("combine") && method.descriptor().equals("(IJ)I")) {
				combinedArguments.add(List.copyOf(parameters));
				if (context != null || parameters.size() != 2
						|| !(parameters.get(0) instanceof Value.KnownIntValue intValue)
						|| !(parameters.get(1) instanceof Value.KnownLongValue longValue))
					return null;
				return Values.valueOf(intValue.value() + (int) longValue.value());
			}
			if (method.owner().equals("java/lang/String") && method.name().equals("concat")
					&& method.descriptor().equals("(Ljava/lang/String;)Ljava/lang/String;")) {
				if (!(context instanceof Value.KnownStringValue receiver) || parameters.size() != 1
						|| !(parameters.getFirst() instanceof Value.KnownStringValue argument))
					return null;
				concatContexts.add(receiver);
				concatArguments.add(List.copyOf(parameters));
				return Values.valueOfString(receiver.value() + argument.value());
			}
			if (method.owner().equals("sample/Owner") && method.name().equals("missing") && method.descriptor().equals("(I)I")) {
				missingMethodCalled[0] = true;
				return null;
			}
			return null;
		};
		FieldValueLookup fieldLookup = (field, context) -> {
			if (field.owner().equals("java/lang/Math") && field.name().equals("PI") && field.descriptor().equals("D")) {
				piUsedStaticContext[0] = context == null;
				return Values.valueOf(6.25D);
			}
			if (field.owner().equals("sample/Owner") && field.name().equals("answer") && field.descriptor().equals("I")) {
				if (context != null)
					fieldContexts.add(context);
				return context == null ? null : Values.valueOf(73);
			}
			if (field.owner().equals("sample/Owner") && field.name().equals("missingStatic") && context == null) {
				missingStaticFieldCalled[0] = true;
				return null;
			}
			if (field.owner().equals("sample/Owner") && field.name().equals("missingInstance") && context != null) {
				missingInstanceFieldCalled[0] = true;
				return null;
			}
			return null;
		};

		DalvikCompilerOptions options = new DalvikCompilerOptions().withMethodValueLookup(methodLookup);
		assertSame(methodLookup, options.getMethodValueLookup());
		assertInstanceOf(BasicFieldValueLookup.class, options.getFieldValueLookup());
		options.withFieldValueLookup(fieldLookup);
		assertSame(methodLookup, options.getMethodValueLookup());
		assertSame(fieldLookup, options.getFieldValueLookup());

		Compilation compilation = compile("""
				.super java/lang/Object
				.class public LookupConfigured {
				    .method public static overriddenAbs ()I {
				        registers: 2,
				        code: {
				            const v1 -5
				            invoke-static { v1 } java/lang/Math.abs (I)I
				            move-result v0
				            return v0
				        }
				    }
				    .method public static overriddenPi ()D {
				        registers: 2,
				        code: {
				            sget-wide v0 java/lang/Math.PI D
				            return-wide v0
				        }
				    }
				    .method public static combineList ()I {
				        registers: 4,
				        code: {
				            const v0 5
				            const-wide v1 9
				            invoke-static { v0, v1, v2 } sample/Owner.combine (IJ)I
				            move-result v3
				            return v3
				        }
				    }
				    .method public static combineRange ()I {
				        registers: 4,
				        code: {
				            const v0 5
				            const-wide v1 9
				            invoke-static/range { v0, v2 } sample/Owner.combine (IJ)I
				            move-result v3
				            return v3
				        }
				    }
				    .method public static concatStrings ()Ljava/lang/String; {
				        registers: 3,
				        code: {
				            const-string v0 "base-"
				            const-string v1 "suffix"
				            invoke-virtual { v0, v1 } java/lang/String.concat (Ljava/lang/String;)Ljava/lang/String;
				            move-result-object v2
				            return-object v2
				        }
				    }
				    .method public static instanceField ()I {
				        registers: 2,
				        code: {
				            const-string v0 "field-receiver"
				            iget v1 v0 sample/Owner.answer I
				            return v1
				        }
				    }
				    .method public static missingMethod ()I {
				        registers: 2,
				        code: {
				            const v1 7
				            invoke-static { v1 } sample/Owner.missing (I)I
				            move-result v0
				            return v0
				        }
				    }
				    .method public static missingStaticField ()I {
				        registers: 1,
				        code: {
				            sget v0 sample/Owner.missingStatic I
				            return v0
				        }
				    }
				    .method public static missingInstanceField ()I {
				        registers: 2,
				        code: {
				            const-string v0 "field-receiver"
				            iget v1 v0 sample/Owner.missingInstance I
				            return v1
				        }
				    }
				    .method public static unknownCombine (IJ)I {
				        registers: 4,
				        parameters: { input, wide },
				        code: {
				            invoke-static { input, wide, v2 } sample/Owner.combine (IJ)I
				            move-result v3
				            return v3
				        }
				    }
				}
				""", options);

		MethodMember absMethod = compilation.method("overriddenAbs", "()I");
		DalvikAnalysisResults absResults = compilation.results("overriddenAbs", "()I");
		MoveResultInstruction absConsumer = instruction(absMethod, MoveResultInstruction.class);
		assertEquals(new Value.KnownIntValue(91), absResults.getStateBefore(absConsumer).pendingResult());
		assertEquals(new Value.KnownIntValue(91), terminalValue(absResults, absMethod));
		assertTrue(absUsedStaticContext[0]);

		MethodMember piMethod = compilation.method("overriddenPi", "()D");
		DalvikAnalysisResults piResults = compilation.results("overriddenPi", "()D");
		DalvikRegisterState piState = piResults.getStateBefore(instruction(piMethod, ReturnInstruction.class));
		assertEquals(new Value.KnownDoubleValue(6.25D), value(piState, 0));
		assertEquals(new DalvikRegisterState.WideTail(0), piState.slot(1));
		assertTrue(piUsedStaticContext[0]);

		assertEquals(new Value.KnownIntValue(14), terminalValue(compilation.results("combineList", "()I"), compilation.method("combineList", "()I")));
		assertEquals(new Value.KnownIntValue(14), terminalValue(compilation.results("combineRange", "()I"), compilation.method("combineRange", "()I")));
		assertEquals(List.of(List.of(new Value.KnownIntValue(5), new Value.KnownLongValue(9L)),
				List.of(new Value.KnownIntValue(5), new Value.KnownLongValue(9L))), combinedArguments);

		MethodMember concatMethod = compilation.method("concatStrings", "()Ljava/lang/String;");
		DalvikAnalysisResults concatResults = compilation.results("concatStrings", "()Ljava/lang/String;");
		assertEquals(Values.valueOfString("base-suffix"), terminalValue(concatResults, concatMethod));
		assertEquals(List.of(Values.valueOfString("base-")), concatContexts);
		assertEquals(List.of(List.of(Values.valueOfString("suffix"))), concatArguments);

		MethodMember instanceFieldMethod = compilation.method("instanceField", "()I");
		DalvikAnalysisResults instanceFieldResults = compilation.results("instanceField", "()I");
		assertEquals(new Value.KnownIntValue(73), terminalValue(instanceFieldResults, instanceFieldMethod));
		assertEquals(List.of(Values.valueOfString("field-receiver")), fieldContexts);

		MethodMember missingMethod = compilation.method("missingMethod", "()I");
		DalvikAnalysisResults missingMethodResults = compilation.results("missingMethod", "()I");
		MoveResultInstruction missingConsumer = instruction(missingMethod, MoveResultInstruction.class);
		assertTrue(missingMethodCalled[0]);
		assertEquals(Values.INT_VALUE, missingMethodResults.getStateBefore(missingConsumer).pendingResult());
		assertEquals(Values.INT_VALUE, terminalValue(missingMethodResults, missingMethod));

		assertTrue(missingStaticFieldCalled[0]);
		assertTrue(missingInstanceFieldCalled[0]);
		assertEquals(Values.INT_VALUE, terminalValue(compilation.results("missingStaticField", "()I"),
				compilation.method("missingStaticField", "()I")));
		assertEquals(Values.INT_VALUE, terminalValue(compilation.results("missingInstanceField", "()I"),
				compilation.method("missingInstanceField", "()I")));

		MethodMember unknownCombine = compilation.method("unknownCombine", "(IJ)I");
		DalvikAnalysisResults unknownCombineResults = compilation.results("unknownCombine", "(IJ)I");
		MoveResultInstruction unknownCombineConsumer = instruction(unknownCombine, MoveResultInstruction.class);
		assertEquals(Values.INT_VALUE, unknownCombineResults.getStateBefore(unknownCombineConsumer).pendingResult());
		assertEquals(2, combinedArguments.size(), "Unknown inputs must skip the custom method lookup");

		for (DalvikAnalysisResults results : compilation.result().analysisLookup().getAllResults().values())
			assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
	}

	private static Compilation compile(String source, DalvikCompilerOptions options) {
		List<ASTElement> parsed = DiagnosticAssertions.requireSuccess(
				AssemblyParseFixture.processDeclarations("<dalvik-analysis-lookup-test>", source, DalvikTargetContext.INSTANCE),
				"Dalvik lookup source should parse");
		ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
				SemanticProcessor.process(parsed, DalvikTargetContext.INSTANCE),
				"Dalvik lookup source should process");
		Outcome<DalvikCompileResult> outcome = new DalvikCompiler().compile(unit, options);
		assertFalse(outcome.hasErrors(), "Unexpected compiler errors: " + outcome.errors());
		assertFalse(outcome.hasWarnings(), "Unexpected compiler warnings: " + outcome.warnings());
		DalvikCompileResult result = outcome.requireValue();
		DalvikClassRepresentation representation = assertInstanceOf(DalvikClassRepresentation.class, result.representation());
		return new Compilation(representation.definition(), result);
	}

	private static Value valueBeforeReturn(Compilation compilation, MethodMember method) {
		return value(compilation.results(method.getName(), method.getType().descriptor())
				.getStateBefore(instruction(method, ReturnInstruction.class)), 0);
	}

	private static Value terminalValue(DalvikAnalysisResults results, MethodMember method) {
		ReturnInstruction instruction = instruction(method, ReturnInstruction.class);
		Integer index = results.getInstructionIndex(instruction);
		assertNotNull(index);
		DalvikAnalysisResults.TerminalState terminal = results.getTerminalStates().get(index);
		assertNotNull(terminal, results.getFailures().toString());
		return terminal.value();
	}

	private static Value value(DalvikRegisterState state, int register) {
		return assertInstanceOf(DalvikRegisterState.ValueHead.class, state.slot(register)).value();
	}

	private static <T extends me.darknet.dex.tree.definitions.instructions.Instruction> T instruction(MethodMember method, Class<T> type) {
		return method.getCode().getInstructions().stream().filter(type::isInstance).map(type::cast).findFirst()
				.orElseThrow(() -> new AssertionError("Instruction not found: " + type.getSimpleName()));
	}

	private record Compilation(ClassDefinition definition, DalvikCompileResult result) {
		private MethodMember method(String name, String descriptor) {
			MethodMember method = definition.getMethod(name, descriptor);
			assertNotNull(method, name + descriptor);
			return method;
		}

		private DalvikAnalysisResults results(String name, String descriptor) {
			DalvikAnalysisResults results = result.analysisLookup().getResults(name, descriptor);
			assertNotNull(results, name + descriptor);
			return results;
		}
	}
}

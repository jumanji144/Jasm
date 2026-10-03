package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompilerOptions;
import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.PrimitiveType;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.instructions.FilledNewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveExceptionInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveResultInstruction;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikAnalysisResultFlowTest {
	@Test
	void transfersInvokeResultsUsingEffectiveCallSiteTypes() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public InvokeResultFlow {
				    .method public static narrow (I)I {
				        registers: 2,
				        parameters: { input },
				        code: {
				            invoke-static { input } java/lang/Math.abs (I)I
				        Result:
				            move-result v0
				            return v0
				        }
				    }
				    .method public static objectResult (I)Ljava/lang/String; {
				        registers: 2,
				        parameters: { input },
				        code: {
				            invoke-static { input } java/lang/String.valueOf (I)Ljava/lang/String;
				            move-result-object v0
				            return-object v0
				        }
				    }
				    .method public static wideResult ()J {
				        registers: 2,
				        code: {
				            invoke-static { } java/lang/System.nanoTime ()J
				            move-result-wide v0
				            return-wide v0
				        }
				    }
				    .method public static polymorphic (Ljava/lang/Object;)I {
				        registers: 2,
				        parameters: { receiver },
				        code: {
				            invoke-polymorphic { receiver } java/lang/invoke/MethodHandle.invokeExact ([Ljava/lang/Object;)Ljava/lang/Object; (Ljava/lang/Object;)I
				            move-result v0
				            return v0
				        }
				    }
				    .method public static custom ()Ljava/lang/String; {
				        registers: 1,
				        code: {
				            invoke-custom { } callsite ()Ljava/lang/String; ConstantBootstraps.nullConstant { }
				            move-result-object v0
				            return-object v0
				        }
				    }
				}
				""");

		// Each invoke exposes its typed result only to the matching move-result consumer.
		assertResultFlow(compilation, "narrow", "(I)I", Values.INT_VALUE, 0);
		assertResultFlow(compilation, "objectResult", "(I)Ljava/lang/String;", Values.valueOfInstance(new ClassDescriptor("java/lang/String")), 0);
		assertResultFlow(compilation, "wideResult", "()J", Values.LONG_VALUE, 0);
		assertResultFlow(compilation, "polymorphic", "(Ljava/lang/Object;)I", Values.INT_VALUE, 0);
		assertResultFlow(compilation, "custom", "()Ljava/lang/String;", Values.valueOfInstance(new ClassDescriptor("java/lang/String")), 0);

		// A polymorphic call-site prototype controls the result type, not the method-handle prototype.
		InvokeInstruction polymorphic = instruction(compilation.method("polymorphic", "(Ljava/lang/Object;)I").getCode(), InvokeInstruction.class);
		assertEquals("([Ljava/lang/Object;)Ljava/lang/Object;", polymorphic.methodType().descriptor());
		assertEquals("(Ljava/lang/Object;)I", polymorphic.type().descriptor());
		assertEquals(Values.INT_VALUE, compilation.results("polymorphic", "(Ljava/lang/Object;)I")
				.getStateBefore(instruction(compilation.method("polymorphic", "(Ljava/lang/Object;)I").getCode(),
						MoveResultInstruction.class)).pendingResult());
	}

	@Test
	void filledArraysAreTransientUntilMoveResultObject() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public FilledArrayFlow {
				    .method public static list ()[Ljava/lang/String; {
				        registers: 3,
				        code: {
				            const-string v0 "first"
				            const-string v1 "second"
				            filled-new-array { v0, v1 } [Ljava/lang/String;
				            move-result-object v2
				            return-object v2
				        }
				    }
				    .method public static range ()[I {
				        registers: 4,
				        code: {
				            const v1 1
				            const v2 2
				            const v3 3
				            filled-new-array/range { v1, v3 } [I
				            move-result-object v0
				            return-object v0
				        }
				    }
				}
				""");
		assertFilledResult(compilation, "list", "()[Ljava/lang/String;", new ArrayDescriptor(new ClassDescriptor("java/lang/String")), 2, 2);
		assertFilledResult(compilation, "range", "()[I", new ArrayDescriptor(PrimitiveType.INT), 3, 0);
	}

	@Test
	void voidResultClearsPreviousPendingResultAtConsumer() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public InvalidResultFlow {
				    .method public static voidClearsOldResult ()V {
				        registers: 2,
				        code: {
				            invoke-static { } java/lang/System.nanoTime ()J
				            invoke-static { } java/lang/System.gc ()V
				            move-result-wide v0
				            return-void
				        }
				    }
				}
				""");

		DalvikAnalysisResults results = compilation.results("voidClearsOldResult", "()V");
		MethodMember method = compilation.method("voidClearsOldResult", "()V");
		List<InvokeInstruction> invokes = instructions(method.getCode(), InvokeInstruction.class);
		MoveResultInstruction consumer = instruction(method.getCode(), MoveResultInstruction.class);
		assertEquals(Values.LONG_VALUE, results.getStateBefore(invokes.get(1)).pendingResult());
		assertNull(results.getStateBefore(consumer).pendingResult());
		assertFailureAt(results, consumer);
		assertUndefinedAfter(results, method.getCode(), consumer, 0, 1);
	}

	@Test
	void orphanResultFailsAtConsumerAndClearsDestination() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public InvalidResultFlow {
				    .method public static orphan ()V {
				        registers: 1,
				        code: {
				            move-result v0
				            return-void
				        }
				    }
				}
				""");

		MethodMember method = compilation.method("orphan", "()V");
		MoveResultInstruction consumer = instruction(method.getCode(), MoveResultInstruction.class);
		DalvikAnalysisResults results = compilation.results("orphan", "()V");
		assertFailureAt(results, consumer);
		assertUndefinedAfter(results, method.getCode(), consumer, 0);
	}

	@Test
	void wrongKindResultFailsAtConsumerAndClearsDestination() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public InvalidResultFlow {
				    .method public static wrongKind ()V {
				        registers: 2,
				        code: {
				            const v1 4
				            invoke-static { v1 } java/lang/Math.abs (I)I
				            move-result-object v0
				            return-void
				        }
				    }
				}
				""");

		MethodMember method = compilation.method("wrongKind", "()V");
		MoveResultInstruction consumer = instruction(method.getCode(), MoveResultInstruction.class);
		DalvikAnalysisResults results = compilation.results("wrongKind", "()V");
		assertEquals(new Value.KnownIntValue(4), results.getStateBefore(consumer).pendingResult());
		assertFailureAt(results, consumer);
		assertUndefinedAfter(results, method.getCode(), consumer, 0);
	}

	@Test
	void interveningInstructionDiscardsResultBeforeConsumer() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public InvalidResultFlow {
				    .method public static intervening ()V {
				        registers: 3,
				        code: {
				            const v2 4
				            invoke-static { v2 } java/lang/Math.abs (I)I
				            const v0 9
				            move-result v1
				            return-void
				        }
				    }
				}
				""");

		MethodMember method = compilation.method("intervening", "()V");
		MoveResultInstruction consumer = instruction(method.getCode(), MoveResultInstruction.class);
		DalvikAnalysisResults results = compilation.results("intervening", "()V");
		assertNull(results.getStateBefore(consumer).pendingResult());
		assertEquals(new Value.KnownIntValue(9), value(results.getStateBefore(consumer), 0));
		assertFailureAt(results, consumer);
		assertEquals(1, results.getFailures().size(), "The intervening instruction silently discards the result");
		assertUndefinedAfter(results, method.getCode(), consumer, 1);
	}

	@Test
	void joinedResultFailsWhenIncomingPathsDisagree() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public InvalidResultFlow {
				    .method public static join (I)V {
				        registers: 2,
				        parameters: { condition },
				        code: {
				            if-eqz condition Join
				            invoke-static { condition } java/lang/Math.abs (I)I
				        Join:
				            move-result v0
				            return-void
				        }
				    }
				}
				""");

		MethodMember method = compilation.method("join", "(I)V");
		MoveResultInstruction consumer = instruction(method.getCode(), MoveResultInstruction.class);
		DalvikAnalysisResults results = compilation.results("join", "(I)V");
		assertNull(results.getStateBefore(consumer).pendingResult());
		assertFailureAt(results, consumer);
		assertUndefinedAfter(results, method.getCode(), consumer, 0);
	}

	@Test
	void orphanExceptionFailsAtConsumerAndClearsDestination() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public InvalidResultFlow {
				    .method public static orphanException ()V {
				        registers: 1,
				        code: {
				            move-exception v0
				            return-void
				        }
				    }
				}
				""");

		MethodMember method = compilation.method("orphanException", "()V");
		MoveExceptionInstruction consumer = instruction(method.getCode(), MoveExceptionInstruction.class);
		DalvikAnalysisResults results = compilation.results("orphanException", "()V");
		assertFailureAt(results, consumer);
		assertUndefinedAfter(results, method.getCode(), consumer, 0);
	}

	@Test
	void delayedExceptionFailsAtHandlerConsumerAndClearsDestination() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public InvalidResultFlow {
				    .method public static delayedException (I)V {
				        registers: 3,
				        parameters: { input },
				        exceptions: { { Start, End, Handler, java/lang/Exception } },
				        code: {
				        Start:
				            invoke-static { input } java/lang/Math.abs (I)I
				            move-result v0
				        End:
				            return-void
				        Handler:
				            nop
				            move-exception v1
				            return-void
				        }
				    }
				}
				""");

		MethodMember method = compilation.method("delayedException", "(I)V");
		MoveExceptionInstruction consumer = instructions(method.getCode(), MoveExceptionInstruction.class).getFirst();
		DalvikAnalysisResults results = compilation.results("delayedException", "(I)V");
		assertFailureAt(results, consumer);
		assertUndefinedAfter(results, method.getCode(), consumer, 1);
	}

	@Test
	void exceptionalEdgesCarryCaughtValuesFromPreInvokeState() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public ExceptionResultFlow {
				    .method public static typed (I)V {
				        registers: 3,
				        parameters: { argument },
				        exceptions: { { Start, End, Handler, java/lang/Exception } },
				        code: {
				        Start:
				            invoke-static { argument } java/lang/Math.abs (I)I
				            move-result v0
				        End:
				            return-void
				        Handler:
				            move-exception v1
				            return-void
				        }
				    }
				    .method public static catchAll (I)V {
				        registers: 3,
				        parameters: { argument },
				        exceptions: { { Start2, End2, Handler2, * } },
				        code: {
				        Start2:
				            invoke-static { argument } java/lang/Math.abs (I)I
				            move-result v0
				        End2:
				            return-void
				        Handler2:
				            move-exception v1
				            return-void
				        }
				    }
				}
				""");

		assertCaughtException(compilation, "typed", new ClassDescriptor("java/lang/Exception"));
		assertCaughtException(compilation, "catchAll", new ClassDescriptor("java/lang/Throwable"));
	}

	@Test
	void overlappingProtectedRangesKeepEdgeOrderAndPerHandlerTypes() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public OverlappingExceptionFlow {
				    .method public static nested (I)V {
				        registers: 3,
				        parameters: { argument },
				        exceptions: {
				            { OuterStart, OuterEnd, RuntimeHandler, java/lang/RuntimeException },
				            { OuterStart, OuterEnd, CatchAllHandler, * },
				            { InnerStart, InnerEnd, InnerHandler, java/lang/Exception }
				        },
				        code: {
				        OuterStart:
				            invoke-static { argument } java/lang/Math.abs (I)I
				            move-result v0
				        InnerStart:
				            invoke-static { argument } java/lang/Math.abs (I)I
				            move-result v0
				        InnerEnd:
				        OuterEnd:
				            return-void
				        RuntimeHandler:
				            move-exception v1
				            return-void
				        CatchAllHandler:
				            move-exception v1
				            return-void
				        InnerHandler:
				            move-exception v1
				            return-void
				        }
				    }
				}
				""");

		MethodMember method = compilation.method("nested", "(I)V");
		DalvikAnalysisResults results = compilation.results("nested", "(I)V");
		List<InvokeInstruction> invokes = instructions(method.getCode(), InvokeInstruction.class);
		int secondInvokeIndex = results.getInstructionIndex(invokes.get(1));

		// Edge order follows the overlapping protected-range declarations and preserves each handler type.
		DalvikControlFlowGraph graph = DalvikControlFlowGraphBuilder.build(method.getCode(), results.getInstructionToSource());
		List<DalvikControlFlowGraph.ExceptionEdge> edges = graph.exceptionalSuccessors().get(secondInvokeIndex);
		assertEquals(3, edges.size());
		assertEquals(List.of(0, 0, 1), edges.stream().map(DalvikControlFlowGraph.ExceptionEdge::tryCatchOrder).toList());
		assertEquals(List.of(0, 1, 0), edges.stream().map(DalvikControlFlowGraph.ExceptionEdge::handlerOrder).toList());
		assertEquals(new ClassDescriptor("java/lang/RuntimeException"), edges.get(0).exceptionType());
		assertNull(edges.get(1).exceptionType());
		assertEquals(new ClassDescriptor("java/lang/Exception"), edges.get(2).exceptionType());

		// Each handler receives its declared caught value, and move-exception itself remains failure-free.
		for (DalvikControlFlowGraph.ExceptionEdge edge : edges) {
			MoveExceptionInstruction moveException = assertInstanceOf(MoveExceptionInstruction.class, method.getCode().getInstructions().get(edge.targetIndex()));
			ReturnInstruction handlerReturn = assertInstanceOf(ReturnInstruction.class, method.getCode().getInstructions().get(edge.targetIndex() + 1));
			ClassDescriptor expectedType = edge.exceptionType() == null
					? new ClassDescriptor("java/lang/Throwable")
					: edge.exceptionType();
			assertEquals(Values.valueOfInstance(expectedType), value(results.getStateBefore(handlerReturn), 1));
			assertFalse(results.getFailures().stream().anyMatch(failure -> failure.instructionIndex().equals(results.getInstructionIndex(moveException))));
		}
		assertTrue(results.getFailures().isEmpty());
	}

	private static void assertResultFlow(Compilation compilation, String name, String descriptor, Value expected, int destination) {
		MethodMember method = compilation.method(name, descriptor);
		DalvikAnalysisResults results = compilation.results(name, descriptor);
		MoveResultInstruction moveResult = instruction(method.getCode(), MoveResultInstruction.class);
		ReturnInstruction ret = instruction(method.getCode(), ReturnInstruction.class);
		DalvikRegisterState beforeResult = results.getStateBefore(moveResult);
		assertEquals(expected, beforeResult.pendingResult(), name);
		assertSame(DalvikRegisterState.Undefined.INSTANCE, beforeResult.slot(destination), name);
		DalvikRegisterState beforeReturn = results.getStateBefore(ret);
		assertNull(beforeReturn.pendingResult(), name);
		assertEquals(expected, value(beforeReturn, destination), name);
		assertEquals(expected, results.getTerminalStates().get(results.getInstructionIndex(ret)).value(), name);
		assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
	}

	private static void assertFilledResult(Compilation compilation, String name, String descriptor,
	                                      ArrayDescriptor arrayType, int length, int destination) {
		MethodMember method = compilation.method(name, descriptor);
		DalvikAnalysisResults results = compilation.results(name, descriptor);
		FilledNewArrayInstruction producer = instruction(method.getCode(), FilledNewArrayInstruction.class);
		MoveResultInstruction consumer = instruction(method.getCode(), MoveResultInstruction.class);
		ReturnInstruction ret = instruction(method.getCode(), ReturnInstruction.class);
		DalvikRegisterState beforeConsumer = results.getStateBefore(consumer);
		Value.KnownLengthArrayValue array = assertInstanceOf(Value.KnownLengthArrayValue.class, beforeConsumer.pendingResult());
		assertEquals(arrayType, array.arrayType());
		assertEquals(length, array.length());
		assertSame(DalvikRegisterState.Undefined.INSTANCE, beforeConsumer.slot(destination));
		assertNull(results.getStateBefore(producer).pendingResult());
		assertSame(DalvikRegisterState.Undefined.INSTANCE, results.getStateBefore(producer).slot(destination));
		DalvikRegisterState beforeReturn = results.getStateBefore(ret);
		assertNull(beforeReturn.pendingResult());
		assertEquals(array, value(beforeReturn, destination));
		assertEquals(array, results.getTerminalStates().get(results.getInstructionIndex(ret)).value());
		assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
	}

	private static void assertCaughtException(Compilation compilation, String name, ClassDescriptor exceptionType) {
		MethodMember method = compilation.method(name, "(I)V");
		DalvikAnalysisResults results = compilation.results(name, "(I)V");
		InvokeInstruction invoke = instruction(method.getCode(), InvokeInstruction.class);
		MoveResultInstruction moveResult = instruction(method.getCode(), MoveResultInstruction.class);
		MoveExceptionInstruction moveException = instruction(method.getCode(), MoveExceptionInstruction.class);
		List<ReturnInstruction> returns = instructions(method.getCode(), ReturnInstruction.class);
		DalvikRegisterState handlerInput = results.getStateBefore(moveException);
		assertSame(DalvikRegisterState.Undefined.INSTANCE, handlerInput.slot(0));
		assertEquals(Values.INT_VALUE, value(handlerInput, 2));
		assertNull(handlerInput.pendingResult());
		assertEquals(Values.INT_VALUE, results.getStateBefore(moveResult).pendingResult());
		assertEquals(Values.valueOfInstance(exceptionType), value(results.getStateBefore(returns.getLast()), 1));
		assertEquals(Values.valueOfInstance(exceptionType), value(
				results.getTerminalStates().get(results.getInstructionIndex(returns.getLast())).registerState(), 1));
		assertTrue(results.getFailures().isEmpty(), results.getFailures().toString());
		assertNotNull(results.getSource(invoke));
	}

	private static void assertFailureAt(DalvikAnalysisResults results, Instruction consumer) {
		Integer instructionIndex = results.getInstructionIndex(consumer);
		DalvikAnalysisFailure failure = results.getFailures().stream()
				.filter(candidate -> candidate.instructionIndex().equals(instructionIndex))
				.findFirst().orElseThrow(() -> new AssertionError("Missing failure at " + consumer));
		assertEquals(DalvikAnalysisFailure.FailureKind.UNSUPPORTED_TRANSFER, failure.kind());
		assertNotNull(failure.sourceLocation());
		assertNotNull(results.getSource(consumer));
	}

	private static void assertUndefinedAfter(DalvikAnalysisResults results, Code code,
	                                       Instruction consumer, int... registers) {
		int consumerIndex = results.getInstructionIndex(consumer);
		int nextIndex = -1;
		for (int index = consumerIndex + 1; index < code.getInstructions().size(); index++) {
			if (!(code.getInstructions().get(index) instanceof me.darknet.dex.tree.definitions.instructions.Label)) {
				nextIndex = index;
				break;
			}
		}
		assertTrue(nextIndex >= 0, "Expected an executable instruction after consumer");
		DalvikRegisterState state = results.getStateBefore(nextIndex);
		assertNotNull(state);
		assertNull(state.pendingResult());
		for (int register : registers)
			assertSame(DalvikRegisterState.Undefined.INSTANCE, state.slot(register));
	}

	private static Value value(DalvikRegisterState state, int register) {
		return assertInstanceOf(DalvikRegisterState.ValueHead.class, state.slot(register)).value();
	}

	private static <T extends Instruction> T instruction(Code code, Class<T> type) {
		return code.getInstructions().stream().filter(type::isInstance).map(type::cast).findFirst()
				.orElseThrow(() -> new AssertionError("Instruction not found: " + type.getSimpleName()));
	}

	private static <T extends Instruction> List<T> instructions(Code code, Class<T> type) {
		return code.getInstructions().stream().filter(type::isInstance).map(type::cast).toList();
	}

	private static Compilation compile(String source) {
		List<ASTElement> parsed = DiagnosticAssertions.requireSuccess(
				AssemblyParseFixture.processDeclarations("<dalvik-result-flow-test>", source, DalvikTargetContext.INSTANCE),
				"Stage 5 Dalvik source should parse");
		ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
				SemanticProcessor.process(parsed, DalvikTargetContext.INSTANCE), "Stage 5 Dalvik source should process");
		Outcome<DalvikCompileResult> outcome = new DalvikCompiler().compile(unit, new DalvikCompilerOptions());
		assertFalse(outcome.hasErrors(), "Unexpected compiler errors: " + outcome.errors());
		assertFalse(outcome.hasWarnings(), "Unexpected compiler warnings: " + outcome.warnings());
		DalvikCompileResult result = outcome.requireValue();
		DalvikClassRepresentation representation = assertInstanceOf(DalvikClassRepresentation.class, result.representation());
		return new Compilation(representation.definition(), result);
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

package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.registry.BasicFieldValueLookup;
import me.darknet.assembler.analysis.registry.BasicMethodValueLookup;
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
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.CodeBuilder;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.instructions.*;
import me.darknet.dex.tree.type.Types;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikAnalysisValueTest {
	@Test
	void analyzesIntegerArithmetic() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4IntegerArithmetic {
				    .method public static sum ()I {
				        registers: 3,
				        code: {
				            const v0 7
				            const v1 5
				            add-int v2 v0 v1
				            return v2
				        }
				    }
				}
				""");

		MethodMember sum = compilation.method("sum", "()I");
		DalvikAnalysisResults sumResults = compilation.results("sum", "()I");
		ReturnInstruction sumReturn = instruction(sum.getCode(), ReturnInstruction.class);
		assertEquals(new Value.KnownIntValue(12), value(sumResults.getStateBefore(sumReturn), 2));
		assertEquals(new Value.KnownIntValue(12), terminal(sumResults, sum).value());
		assertTrue(sumResults.getFailures().isEmpty());
	}

	@Test
	void analyzesWideAndFloatingArithmetic() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4WideFloatingArithmetic {
				    .method public static longValue ()J {
				        registers: 4,
				        code: {
				            const-wide v0 7L
				            const-wide v2 5L
				            add-long v0 v0 v2
				            return-wide v0
				        }
				    }
				    .method public static floating ()F {
				        registers: 3,
				        code: {
				            const v0 1.5f
				            const v1 2.25f
				            mul-float v2 v0 v1
				            return v2
				        }
				    }
				    .method public static doubleArithmetic ()D {
				        registers: 4,
				        code: {
				            const-wide v0 1.5
				            const-wide v2 2.5
				            add-double v0 v0 v2
				            return-wide v0
				        }
				    }
				}
				""");

		assertEquals(Values.valueOf(12L), terminal(compilation.results("longValue", "()J"),
				compilation.method("longValue", "()J")).value());
		assertEquals(Values.valueOf(3.375f), terminal(compilation.results("floating", "()F"),
				compilation.method("floating", "()F")).value());
		assertEquals(Values.valueOf(4.0d), terminal(compilation.results("doubleArithmetic", "()D"),
				compilation.method("doubleArithmetic", "()D")).value());
	}

	@Test
	void preservesDoubleConstant() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4DoubleConstant {
				    .method public static doubleValue ()D {
				        registers: 2,
				        code: {
				            const-wide v0 2.5
				            return-wide v0
				        }
				    }
				}
				""");

		assertEquals(Values.valueOf(2.5d), terminal(compilation.results("doubleValue", "()D"),
				compilation.method("doubleValue", "()D")).value());
	}

	@Test
	void preservesNarrowAndObjectMoves() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4Moves {
				    .method public static narrowMoves ()I {
				        registers: 3,
				        code: {
				            const v0 13
				            move v1 v0
				            move v2 v1
				            return v2
				        }
				    }
				    .method public static objectZero ()Ljava/lang/Object; {
				        registers: 2,
				        code: {
				            const v0 0
				            move-object v1 v0
				            return-object v1
				        }
				    }
				}
				""");

		MethodMember narrowMoves = compilation.method("narrowMoves", "()I");
		assertEquals(new Value.KnownIntValue(13), terminal(compilation.results("narrowMoves", "()I"), narrowMoves).value());
		MethodMember objectZero = compilation.method("objectZero", "()Ljava/lang/Object;");
		DalvikAnalysisResults zeroResults = compilation.results("objectZero", "()Ljava/lang/Object;");
		MoveObjectInstruction moveObject = instruction(objectZero.getCode(), MoveObjectInstruction.class);
		assertSame(DalvikZeroValue.INSTANCE, value(zeroResults.getStateBefore(moveObject), 0));
		assertEquals(Values.NULL_VALUE, terminal(zeroResults, objectZero).value());
	}

	@Test
	void preservesZeroSentinelTypingAtConsumers() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4ZeroSentinels {
				    .method public static intZero ()I {
				        registers: 1,
				        code: {
				            const v0 0
				            return v0
				        }
				    }
				    .method public static wideZero ()J {
				        registers: 2,
				        code: {
				            const-wide v0 0L
				            return-wide v0
				        }
				    }
				    .method public static intZeroArithmetic ()I {
				        registers: 2,
				        code: {
				            const v0 0
				            add-int v1 v0 v0
				            return v1
				        }
				    }
				}
				""");

		// Narrow const-zero keeps its sentinel until an operation establishes the int category.
		MethodMember intZero = compilation.method("intZero", "()I");
		assertSame(DalvikZeroValue.INSTANCE, value(compilation.results("intZero", "()I")
				.getStateBefore(instruction(intZero.getCode(), ReturnInstruction.class)), 0));
		assertEquals(new Value.KnownLongValue(0), terminal(compilation.results("wideZero", "()J"),
				compilation.method("wideZero", "()J")).value());
		assertEquals(Values.INT_0, terminal(compilation.results("intZeroArithmetic", "()I"),
				compilation.method("intZeroArithmetic", "()I")).value());
	}

	@Test
	void preservesTypedStringAndClassConstants() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4TypedConstants {
				    .method public static stringValue ()Ljava/lang/String; {
				        registers: 1,
				        code: {
				            const-string v0 "known"
				            return-object v0
				        }
				    }
				    .method public static classValue ()Ljava/lang/Class; {
				        registers: 1,
				        code: {
				            const-class v0 Ljava/lang/String;
				            return-object v0
				        }
				    }
				}
				""");

		assertEquals(Values.valueOfString("known"), terminal(compilation.results("stringValue", "()Ljava/lang/String;"),
				compilation.method("stringValue", "()Ljava/lang/String;")).value());
		assertEquals(Values.valueOfInstance(new ClassDescriptor("java/lang/Class")),
				terminal(compilation.results("classValue", "()Ljava/lang/Class;"),
						compilation.method("classValue", "()Ljava/lang/Class;")).value());
	}

	@Test
	void preservesRawFloatingConstantPayloads() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4FloatingPayloads {
				    .method public static floatPayload ()F {
				        registers: 1,
				        code: {
				            const v0 #0x7FC01234
				            return v0
				        }
				    }
				    .method public static doublePayload ()D {
				        registers: 2,
				        code: {
				            const-wide v0 #0x7FF8000000000001
				            return-wide v0
				        }
				    }
				}
				""");

		Value.KnownFloatValue floatNan = assertInstanceOf(Value.KnownFloatValue.class,
				terminal(compilation.results("floatPayload", "()F"), compilation.method("floatPayload", "()F")).value());
		Value.KnownDoubleValue doubleNan = assertInstanceOf(Value.KnownDoubleValue.class,
				terminal(compilation.results("doublePayload", "()D"), compilation.method("doublePayload", "()D")).value());
		assertEquals(0x7FC01234, Float.floatToRawIntBits(floatNan.value()));
		assertEquals(0x7FF8000000000001L, Double.doubleToRawLongBits(doubleNan.value()));
	}

	@Test
	void evaluatesLiteralArithmetic() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4LiteralArithmetic {
				    .method public static literals ()I {
				        registers: 4,
				        code: {
				            const v0 4
				            mul-int/lit8 v1 v0 3
				            rsub-int/lit8 v2 v1 20
				            const v3 2
				            div-int/2addr v2 v3
				            return v2
				        }
				    }
				}
				""");

		assertEquals(new Value.KnownIntValue(4), terminal(compilation.results("literals", "()I"),
				compilation.method("literals", "()I")).value());
	}

	@Test
	void convertsPrimitiveValues() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4Conversion {
				    .method public static conversion ()I {
				        registers: 4,
				        code: {
				            const v0 258
				            int-to-byte v1 v0
				            neg-int v2 v1
				            int-to-long v2 v0
				            long-to-int v3 v2
				            return v3
				        }
				    }
				}
				""");

		MethodMember conversion = compilation.method("conversion", "()I");
		DalvikAnalysisResults results = compilation.results("conversion", "()I");
		assertEquals(new Value.KnownIntValue(258), terminal(results, conversion).value());
		UnaryInstruction intToLong = conversion.getCode().getInstructions().stream()
				.filter(UnaryInstruction.class::isInstance).map(UnaryInstruction.class::cast)
				.filter(instruction -> instruction.opcode() == Opcodes.INT_TO_LONG).findFirst().orElseThrow();
		assertEquals(new Value.KnownIntValue(2), value(results.getStateBefore(intToLong), 1));
		assertEquals(new Value.KnownIntValue(-2), value(results.getStateBefore(intToLong), 2));
	}

	@Test
	void comparesNaNWithBothFloatingComparisonModes() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4NaNComparison {
				    .method public static nanCompare ()I {
				        registers: 4,
				        code: {
				            const v0 #0x7FC00001
				            const v1 0.0f
				            cmpl-float v2 v0 v1
				            cmpg-float v3 v0 v1
				            return v2
				        }
				    }
				}
				""");

		MethodMember nanCompare = compilation.method("nanCompare", "()I");
		DalvikAnalysisResults nanResults = compilation.results("nanCompare", "()I");

		// CMPL yields -1 for NaN, while CMPG writes +1 to the other destination.
		assertEquals(new Value.KnownIntValue(-1), terminal(nanResults, nanCompare).value());
		assertEquals(new Value.KnownIntValue(1), value(nanResults.getStateBefore(instruction(nanCompare.getCode(), ReturnInstruction.class)), 3));
	}

	@Test
	void wrapsIntegerOverflow() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4IntegerOverflow {
				    .method public static overflow ()I {
				        registers: 3,
				        code: {
				            const v0 2147483647
				            const v1 1
				            add-int v2 v0 v1
				            return v2
				        }
				    }
				}
				""");

		assertEquals(new Value.KnownIntValue(Integer.MIN_VALUE), terminal(compilation.results("overflow", "()I"),
				compilation.method("overflow", "()I")).value());
	}

	@Test
	void masksIntegerShiftDistance() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4IntegerShift {
				    .method public static shift ()I {
				        registers: 3,
				        code: {
				            const v0 1
				            const v1 32
				            shl-int v2 v0 v1
				            return v2
				        }
				    }
				}
				""");

		assertEquals(new Value.KnownIntValue(1), terminal(compilation.results("shift", "()I"),
				compilation.method("shift", "()I")).value());
	}

	@Test
	void masksLongShiftDistance() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4LongShift {
				    .method public static shift ()J {
				        registers: 5,
				        code: {
				            const-wide v0 1L
				            const v2 64
				            shl-long v3 v0 v2
				            return-wide v3
				        }
				    }
				}
				""");

		assertEquals(new Value.KnownLongValue(1), terminal(compilation.results("shift", "()J"),
				compilation.method("shift", "()J")).value());
	}

	@Test
	void preservesSignedZeroAcrossArithmeticAndUnaryOperations() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4SignedZero {
				    .method public static negativeZeroArithmetic ()F {
				        registers: 3,
				        code: {
				            const v0 -0.0f
				            const v1 -0.0f
				            add-float v2 v0 v1
				            return v2
				        }
				    }
				    .method public static negativeZeroUnary ()F {
				        registers: 2,
				        code: {
				            const v0 0.0f
				            neg-float v1 v0
				            return v1
				        }
				    }
				    .method public static negativeZeroDouble ()D {
				        registers: 4,
				        code: {
				            const-wide v0 0.0
				            neg-double v2 v0
				            return-wide v2
				        }
				    }
				}
				""");

		Value.KnownFloatValue negativeZeroAdd = assertInstanceOf(Value.KnownFloatValue.class,
				terminal(compilation.results("negativeZeroArithmetic", "()F"),
						compilation.method("negativeZeroArithmetic", "()F")).value());
		Value.KnownFloatValue negativeZeroNegate = assertInstanceOf(Value.KnownFloatValue.class,
				terminal(compilation.results("negativeZeroUnary", "()F"),
						compilation.method("negativeZeroUnary", "()F")).value());
		Value.KnownDoubleValue negativeZeroDouble = assertInstanceOf(Value.KnownDoubleValue.class,
				terminal(compilation.results("negativeZeroDouble", "()D"),
						compilation.method("negativeZeroDouble", "()D")).value());
		assertEquals(0x80000000, Float.floatToRawIntBits(negativeZeroAdd.value()));
		assertEquals(0x80000000, Float.floatToRawIntBits(negativeZeroNegate.value()));
		assertEquals(0x8000000000000000L, Double.doubleToRawLongBits(negativeZeroDouble.value()));
	}

	@Test
	void keepsDivisionByZeroAsTypedUnknown() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4DivisionByZero {
				    .method public static divisionByZero ()I {
				        registers: 3,
				        code: {
				            const v0 9
				            const v1 0
				            div-int v2 v0 v1
				            return v2
				        }
				    }
				}
				""");

		assertEquals(Values.INT_VALUE, terminal(compilation.results("divisionByZero", "()I"),
				compilation.method("divisionByZero", "()I")).value());
	}

	@Test
	void comparesKnownLongs() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4LongComparison {
				    .method public static compareLong ()I {
				        registers: 5,
				        code: {
				            const-wide v0 -1L
				            const-wide v2 1L
				            cmp-long v4 v0 v2
				            return v4
				        }
				    }
				}
				""");

		assertEquals(new Value.KnownIntValue(-1), terminal(compilation.results("compareLong", "()I"),
				compilation.method("compareLong", "()I")).value());
	}

	@Test
	void keepsUnknownParameterArithmeticUnknown() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4UnknownOperand {
				    .method public static unknownOperand (I)I {
				        registers: 2,
				        parameters: { input },
				        code: {
				            const v0 8
				            add-int v1 input v0
				            return v1
				        }
				    }
				}
				""");

		assertEquals(Values.INT_VALUE, terminal(compilation.results("unknownOperand", "(I)I"),
				compilation.method("unknownOperand", "(I)I")).value());
	}

	@Test
	void handlesOverlappingWideMoves() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4OverlappingWideMove {
				    .method public static overlappingWideMove ()J {
				        registers: 3,
				        code: {
				            const-wide v0 11L
				            move-wide v1 v0
				            return-wide v1
				        }
				    }
				}
				""");

		MethodMember overlap = compilation.method("overlappingWideMove", "()J");
		DalvikAnalysisResults overlapResults = compilation.results("overlappingWideMove", "()J");
		DalvikRegisterState overlapState = overlapResults.getStateBefore(instruction(overlap.getCode(), ReturnInstruction.class));
		assertSame(DalvikRegisterState.Undefined.INSTANCE, overlapState.slot(0));
		assertEquals(new Value.KnownLongValue(11), value(overlapState, 1));
		assertEquals(new DalvikRegisterState.WideTail(1), overlapState.slot(2));
	}

	@Test
	void clearsOverwrittenWideWords() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4OverwrittenWideWords {
				    .method public static overwriteWideWords ()V {
				        registers: 4,
				        code: {
				            const-wide v0 11L
				            const v1 6
				            const-wide v2 13L
				            const v2 9
				            return-void
				        }
				    }
				}
				""");

		MethodMember overwrite = compilation.method("overwriteWideWords", "()V");
		DalvikRegisterState overwriteState = compilation.results("overwriteWideWords", "()V")
				.getStateBefore(instruction(overwrite.getCode(), ReturnInstruction.class));
		assertSame(DalvikRegisterState.Undefined.INSTANCE, overwriteState.slot(0));
		assertEquals(new Value.KnownIntValue(6), value(overwriteState, 1));
		assertEquals(new Value.KnownIntValue(9), value(overwriteState, 2));
		assertSame(DalvikRegisterState.Undefined.INSTANCE, overwriteState.slot(3));
	}

	@Test
	void joinsEqualConstantsAndKeepsConditionalSuccessorsReachable() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4SameJoin {
				    .method public static sameJoin ()I {
				        registers: 2,
				        code: {
				            const v0 0
				            if-eqz v0 Same
				            const v1 7
				            goto Join
				        Same:
				            const v1 7
				        Join:
				            return v1
				        }
				    }
				}
				""");

		MethodMember sameJoin = compilation.method("sameJoin", "()I");
		DalvikAnalysisResults sameResults = compilation.results("sameJoin", "()I");
		assertEquals(new Value.KnownIntValue(7), terminal(sameResults, sameJoin).value());

		// The constant branch condition does not remove either successor from the analysis.
		int branchIndex = indexOf(sameJoin.getCode(), instruction(sameJoin.getCode(), BranchZeroInstruction.class));
		DalvikControlFlowGraph graph = DalvikControlFlowGraphBuilder.build(sameJoin.getCode(), sameResults.getInstructionToSource());
		List<Integer> successors = graph.normalSuccessors().get(branchIndex);
		assertEquals(2, successors.size());
		for (int successor : successors)
			assertNotNull(sameResults.getStateBefore(successor), "Known conditional successor must remain reachable");
	}

	@Test
	void joinsConflictingConstantsAsUnknown() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4ConflictingJoin {
				    .method public static differentJoin ()I {
				        registers: 2,
				        code: {
				            const v0 0
				            if-eqz v0 Other
				            const v1 5
				            goto Join
				        Other:
				            const v1 6
				        Join:
				            return v1
				        }
				    }
				}
				""");

		assertEquals(Values.INT_VALUE, terminal(compilation.results("differentJoin", "()I"),
				compilation.method("differentJoin", "()I")).value());
	}

	@Test
	void rejectsPrimitiveReferenceCategoryMerge() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4IncompatibleCategory {
				    .method public static categoryJoin ()V {
				        registers: 2,
				        code: {
				            const v0 0
				            if-eqz v0 Reference
				            const v1 2
				            goto Join
				        Reference:
				            const-string v1 "ref"
				        Join:
				            return-void
				        }
				    }
				}
				""");

		assertIncompatibleMerge(compilation, "categoryJoin", "()V", 1);
	}

	@Test
	void rejectsIncompatibleWideCategoryMerge() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4IncompatibleWideCategory {
				    .method public static wideCategoryJoin ()V {
				        registers: 4,
				        code: {
				            const v0 0
				            if-eqz v0 DoublePath
				            const-wide v2 1L
				            goto Join
				        DoublePath:
				            const-wide v2 1.0
				        Join:
				            return-void
				        }
				    }
				}
				""");

		assertIncompatibleMerge(compilation, "wideCategoryJoin", "()V", 2, 3);
	}

	@Test
	void rejectsDistinctUninitializedAllocationMerge() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4AllocationJoin {
				    .method public static allocationJoin ()V {
				        registers: 2,
				        code: {
				            const v1 0
				            if-eqz v1 Second
				            new-instance v0 Ljava/lang/Object;
				            goto Join
				        Second:
				            new-instance v0 Ljava/lang/Object;
				        Join:
				            return-void
				        }
				    }
				}
				""");

		// Separate allocation sites remain incompatible even when they have the same class type.
		assertIncompatibleMerge(compilation, "allocationJoin", "()V", 0);
	}

	@Test
	void joinsDifferentNaNPayloadsAsUnknownFloat() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4DifferingNaN {
				    .method public static differingNaNPayloads ()F {
				        registers: 2,
				        code: {
				            const v0 0
				            if-eqz v0 Other
				            const v1 #0x7FC00001
				            goto Join
				        Other:
				            const v1 #0x7FC00002
				        Join:
				            return v1
				        }
				    }
				}
				""");

		assertEquals(Values.FLOAT_VALUE, terminal(compilation.results("differingNaNPayloads", "()F"),
				compilation.method("differingNaNPayloads", "()F")).value());
	}

	@Test
	void reachesFixedPointForSingleLoopCarriedValue() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4LoopJoin {
				    .method public static loop ()I {
				        registers: 1,
				        code: {
				            const v0 0
				        Loop:
				            add-int/lit8 v0 v0 1
				            if-ltz v0 Loop
				            return v0
				        }
				    }
				}
				""");

		assertEquals(Values.INT_VALUE, terminal(compilation.results("loop", "()I"),
				compilation.method("loop", "()I")).value());
	}

	@Test
	void analyzesReferenceCastsAndAllocations() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4References {
				    .method public static references ()Ljava/lang/Object; {
				        registers: 4,
				        code: {
				            const v0 0
				            check-cast v0 Ljava/lang/String;
				            instance-of v1 v0 Ljava/lang/String;
				            new-instance v2 Ljava/lang/Object;
				            check-cast v2 Ljava/lang/String;
				            instance-of v3 v2 Ljava/lang/String;
				            return-object v0
				        }
				    }
				}
				""");

		MethodMember references = compilation.method("references", "()Ljava/lang/Object;");
		DalvikAnalysisResults referenceResults = compilation.results("references", "()Ljava/lang/Object;");
		List<CheckCastInstruction> casts = references.getCode().getInstructions().stream()
				.filter(CheckCastInstruction.class::isInstance).map(CheckCastInstruction.class::cast).toList();
		List<InstanceOfInstruction> instanceOfs = references.getCode().getInstructions().stream()
				.filter(InstanceOfInstruction.class::isInstance).map(InstanceOfInstruction.class::cast).toList();
		assertEquals(Values.NULL_VALUE, value(referenceResults.getStateBefore(instanceOfs.getFirst()), 0));

		NewInstanceInstruction allocation = instruction(references.getCode(), NewInstanceInstruction.class);
		assertEquals(Values.INT_0, value(referenceResults.getStateBefore(allocation), 1));
		assertEquals(Values.INT_VALUE, value(referenceResults.getStateBefore(
				instruction(references.getCode(), ReturnInstruction.class)), 3));

		Value.UninitializedReferenceValue allocationValue = assertInstanceOf(Value.UninitializedReferenceValue.class,
				value(referenceResults.getStateBefore(casts.getLast()), 2));
		assertEquals(new ClassDescriptor("java/lang/Object"), allocationValue.owner());
		assertEquals(referenceResults.getInstructionIndex(allocation), allocationValue.allocationIdentity());
		assertEquals(Values.NULL_VALUE, terminal(referenceResults, references).value());
	}

	@Test
	void analyzesKnownArrayLoadsAndSizes() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4Arrays {
				    .method public static arrays ()V {
				        registers: 9,
				        code: {
				            const v0 4
				            new-array v1 v0 [I
				            array-length v2 v1
				            aput v0 v1 v0
				            aget v3 v1 v0
				            fill-array-data v1 { 1, 2, 3, 4 }
				            new-array v4 v0 [Ljava/lang/String;
				            aget-object v5 v4 v0
				            new-array v6 v0 [J
				            aget-wide v7 v6 v0
				            return-void
				        }
				    }
				}
				""");

		MethodMember arrays = compilation.method("arrays", "()V");
		DalvikAnalysisResults results = compilation.results("arrays", "()V");
		DalvikRegisterState state = results.getStateBefore(instruction(arrays.getCode(), ReturnInstruction.class));
		assertEquals(DalvikAnalysisResults.TerminalKind.RETURN, terminal(results, arrays).kind());
		assertNull(terminal(results, arrays).value());
		assertEquals(new Value.KnownIntValue(4), value(state, 2));
		assertEquals(Values.INT_VALUE, value(state, 3), "Stores must not refine later array loads");
		assertEquals(Values.valueOfInstance(new ClassDescriptor("java/lang/String")), value(state, 5));
		assertEquals(Values.LONG_VALUE, value(state, 7));
		assertEquals(new DalvikRegisterState.WideTail(7), state.slot(8));
	}

	@Test
	void analyzesArraysWithUnknownSizeConservatively() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4UnknownArraySize {
				    .method public static unknownSize (I)V {
				        registers: 3,
				        parameters: { size },
				        code: {
				            new-array v0 size [J
				            aget-wide v1 v0 size
				            return-void
				        }
				    }
				}
				""");

		MethodMember method = compilation.method("unknownSize", "(I)V");
		DalvikRegisterState state = compilation.results("unknownSize", "(I)V")
				.getStateBefore(instruction(method.getCode(), ReturnInstruction.class));
		assertEquals(Values.valueOfArray(new ArrayDescriptor(PrimitiveType.LONG)), value(state, 0));
		assertEquals(Values.LONG_VALUE, value(state, 1));
		assertEquals(new DalvikRegisterState.WideTail(1), state.slot(2));
	}

	@Test
	void analyzesFieldReadsAndWrites() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4Fields {
				    .method public static fields ()V {
				        registers: 5,
				        code: {
				            const-string v0 "receiver"
				            sget-wide v1 sample/Owner.value J
				            sget v4 sample/Owner.count I
				            iget-object v3 v0 sample/Owner.reference Ljava/lang/String;
				            sput-wide v1 sample/Owner.value J
				            iput-object v3 v0 sample/Owner.reference Ljava/lang/String;
				            return-void
				        }
				    }
				}
				""");

		MethodMember fields = compilation.method("fields", "()V");
		DalvikRegisterState fieldState = compilation.results("fields", "()V")
				.getStateBefore(instruction(fields.getCode(), ReturnInstruction.class));
		assertEquals(Values.LONG_VALUE, value(fieldState, 1));
		assertEquals(Values.INT_VALUE, value(fieldState, 4));
		assertEquals(Values.valueOfInstance(new ClassDescriptor("java/lang/String")), value(fieldState, 3));
	}

	@Test
	void recordsObjectReturnTerminal() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4ObjectReturn {
				    .method public static objectReturn ()Ljava/lang/Object; {
				        registers: 1,
				        code: {
				            const-string v0 "return"
				            return-object v0
				        }
				    }
				}
				""");

		assertEquals(Values.valueOfString("return"), terminal(compilation.results("objectReturn", "()Ljava/lang/Object;"),
				compilation.method("objectReturn", "()Ljava/lang/Object;")).value());
	}

	@Test
	void recordsThrownParameterTerminal() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4ThrowParameter {
				    .method public static throwParameter (Ljava/lang/Throwable;)V {
				        registers: 1,
				        parameters: { thrown },
				        code: {
				            throw thrown
				        }
				    }
				}
				""");

		DalvikAnalysisResults results = compilation.results("throwParameter", "(Ljava/lang/Throwable;)V");
		MethodMember method = compilation.method("throwParameter", "(Ljava/lang/Throwable;)V");
		assertEquals(DalvikAnalysisResults.TerminalKind.THROW, terminal(results, method).kind());
		assertEquals(Values.valueOfInstance(new ClassDescriptor("java/lang/Throwable")), terminal(results, method).value());
	}

	@Test
	void preservesExceptionHandlerPreState() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4ExceptionalPreState {
				    .method public static exceptional ([I)V {
				        registers: 2,
				        parameters: { values },
				        exceptions: {
				            { Start, End, Handler, java/lang/Exception }
				        },
				        code: {
				        Start:
				            array-length v0 values
				        End:
				            return-void
				        Handler:
				            move-exception v1
				            return-void
				        }
				    }
				}
				""");

		MethodMember exceptional = compilation.method("exceptional", "([I)V");
		DalvikAnalysisResults exceptionalResults = compilation.results("exceptional", "([I)V");
		MoveExceptionInstruction moveException = instruction(exceptional.getCode(), MoveExceptionInstruction.class);
		assertSame(DalvikRegisterState.Undefined.INSTANCE,
				exceptionalResults.getStateBefore(moveException).slot(0),
				"Handler input must be the pre-array-length state");
		assertNull(exceptionalResults.getStateBefore(moveException).pendingResult());
		ReturnInstruction handlerReturn = exceptional.getCode().getInstructions().stream()
				.filter(ReturnInstruction.class::isInstance).map(ReturnInstruction.class::cast).toList().getLast();
		assertEquals(Values.valueOfInstance(new ClassDescriptor("java/lang/Exception")),
				value(exceptionalResults.getStateBefore(handlerReturn), 1));
		assertFalse(exceptionalResults.getFailures().stream().anyMatch(failure ->
				failure.instructionIndex().equals(exceptionalResults.getInstructionIndex(moveException))));
	}

	@Test
	void recordsUnsupportedStageBoundaryTransfersAndSourceLocations() {
		Compilation compilation = compile("""
				.super java/lang/Object
				.class public Stage4Boundary {
				    .method public static invokeResult ()I {
				        registers: 2,
				        code: {
				            const v1 12
				            invoke-static { v1 } java/lang/Math.abs (I)I
				            move-result v0
				            return v0
				        }
				    }
				    .method public static invalidResult ()V {
				        registers: 2,
				        code: {
				            const v1 12
				            invoke-static { v1 } java/lang/Math.abs (I)I
				            move-result-object v0
				            return-void
				        }
				    }
				}
				""");
		MethodMember method = compilation.method("invokeResult", "()I");
		DalvikAnalysisResults results = compilation.results("invokeResult", "()I");
		InvokeInstruction invoke = instruction(method.getCode(), InvokeInstruction.class);
		MoveResultInstruction moveResult = instruction(method.getCode(), MoveResultInstruction.class);
		ReturnInstruction returnInstruction = instruction(method.getCode(), ReturnInstruction.class);
		DalvikRegisterState beforeResult = results.getStateBefore(moveResult);
		assertEquals(new Value.KnownIntValue(12), beforeResult.pendingResult());
		assertSame(DalvikRegisterState.Undefined.INSTANCE, beforeResult.slot(0));
		assertTrue(results.getFailures().isEmpty());
		DalvikRegisterState beforeReturn = results.getStateBefore(returnInstruction);
		assertNull(beforeReturn.pendingResult());
		assertEquals(new Value.KnownIntValue(12), value(beforeReturn, 0));
		assertEquals(new Value.KnownIntValue(12), results.getTerminalStates().get(results.getInstructionIndex(returnInstruction)).value());
		assertTrue(results.getInstructionToSource().containsKey(invoke));
		assertTrue(results.getInstructionToSource().containsKey(moveResult));

		MethodMember invalidMethod = compilation.method("invalidResult", "()V");
		DalvikAnalysisResults invalidResults = compilation.results("invalidResult", "()V");
		MoveResultInstruction invalidConsumer = instruction(invalidMethod.getCode(), MoveResultInstruction.class);
		ReturnInstruction invalidReturn = instruction(invalidMethod.getCode(), ReturnInstruction.class);
		DalvikAnalysisFailure resultFailure = invalidResults.getFailures().stream()
				.filter(failure -> failure.instructionIndex().equals(invalidResults.getInstructionIndex(invalidConsumer)))
				.findFirst().orElseThrow();
		assertEquals(DalvikAnalysisFailure.FailureKind.UNSUPPORTED_TRANSFER, resultFailure.kind());
		assertNotNull(resultFailure.sourceLocation());
		assertNotNull(invalidResults.getSource(invalidConsumer));
		assertNull(invalidResults.getStateBefore(invalidReturn).pendingResult());
		assertSame(DalvikRegisterState.Undefined.INSTANCE, invalidResults.getStateBefore(invalidReturn).slot(0));
	}

	@Test
	void missingConstSourceIsAnAnalysisFailureOnly() {
		MethodMember method = new MethodMember("constant", Types.methodTypeFromDescriptor("()I"), 0x0008);
		CodeBuilder builder = new CodeBuilder();
		ConstInstruction constant = new ConstInstruction(0, 19);
		ReturnInstruction ret = new ReturnInstruction(0);
		builder.add(constant);
		builder.add(ret);
		Code code = builder.arguments(0, 0).registers(1).build();
		DalvikAnalysisResults results = DalvikAnalysisEngine.analyze(
				new MethodReference("MissingSource", "constant", "()I"), method, code, Map.of(),
				me.darknet.assembler.compiler.EmptyInheritanceChecker.INSTANCE,
				new BasicMethodValueLookup(), new BasicFieldValueLookup());
		assertEquals(DalvikAnalysisFailure.FailureKind.UNSUPPORTED_TRANSFER, results.getFailures().getFirst().kind());
		assertSame(DalvikRegisterState.Undefined.INSTANCE, results.getStateBefore(ret).slot(0));
		assertTrue(results.getTerminalStates().isEmpty());
	}

	private static void assertIncompatibleMerge(Compilation compilation, String name, String descriptor, int... registers) {
		DalvikAnalysisResults results = compilation.results(name, descriptor);
		MethodMember method = compilation.method(name, descriptor);
		DalvikRegisterState state = results.getStateBefore(instruction(method.getCode(), ReturnInstruction.class));
		for (int register : registers)
			assertSame(DalvikRegisterState.Undefined.INSTANCE, state.slot(register), name + " v" + register);
		assertTrue(results.getFailures().stream().anyMatch(failure ->
				failure.kind() == DalvikAnalysisFailure.FailureKind.INCOMPATIBLE_MERGE));
	}

	private static Value value(DalvikRegisterState state, int register) {
		return assertInstanceOf(DalvikRegisterState.ValueHead.class, state.slot(register)).value();
	}

	private static DalvikAnalysisResults.TerminalState terminal(DalvikAnalysisResults results, MethodMember method) {
		Instruction instruction = method.getCode().getInstructions().stream()
				.filter(candidate -> candidate instanceof ReturnInstruction || candidate instanceof ThrowInstruction)
				.findFirst().orElseThrow(() -> new AssertionError("Terminal instruction not found"));
		Integer index = results.getInstructionIndex(instruction);
		assertNotNull(index);
		DalvikAnalysisResults.TerminalState terminal = results.getTerminalStates().get(index);
		assertNotNull(terminal, results.getFailures().toString());
		return terminal;
	}

	private static <T extends Instruction> T instruction(Code code, Class<T> type) {
		return code.getInstructions().stream().filter(type::isInstance).map(type::cast).findFirst()
				.orElseThrow(() -> new AssertionError("Instruction not found: " + type.getSimpleName()));
	}

	private static int indexOf(Code code, Instruction target) {
		for (int index = 0; index < code.getInstructions().size(); index++)
			if (code.getInstructions().get(index) == target)
				return index;
		throw new AssertionError("Instruction is absent from code");
	}

	private static Compilation compile(String source) {
		List<ASTElement> parsed = DiagnosticAssertions.requireSuccess(
				AssemblyParseFixture.processDeclarations("<dalvik-analysis-value-test>", source, DalvikTargetContext.INSTANCE),
				"Stage 4 Dalvik source should parse");
		ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
				SemanticProcessor.process(parsed, DalvikTargetContext.INSTANCE),
				"Stage 4 Dalvik source should process");
		Outcome<DalvikCompileResult> outcome = new DalvikCompiler().compile(unit, new DalvikCompilerOptions());
		assertTrue(outcome.errors().stream().allMatch(diagnostic ->
					diagnostic.phase() == DiagnosticPhase.OUTPUT_VERIFICATION &&
						diagnostic.code() == DiagnosticCode.ANALYSIS_FAILURE),
				"Unexpected compiler errors: " + outcome.errors());
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

package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.query.AssemblyQueries;
import me.darknet.assembler.query.AssemblyUtils;
import me.darknet.assembler.query.LabelInfo;
import me.darknet.assembler.query.LabelQueryResult;
import me.darknet.assembler.query.LabelReferenceKind;
import me.darknet.assembler.query.LabelUsage;
import me.darknet.assembler.query.VariableAccessKind;
import me.darknet.assembler.query.VariableInfo;
import me.darknet.assembler.query.VariableQueryResult;
import me.darknet.assembler.query.VariableUsage;
import me.darknet.assembler.query.resolution.LabelDeclarationResolution;
import me.darknet.assembler.query.resolution.LabelReferenceResolution;
import me.darknet.assembler.query.resolution.Resolution;
import me.darknet.assembler.query.resolution.TypeReferenceResolution;
import me.darknet.assembler.query.resolution.VariableDeclarationResolution;
import me.darknet.assembler.query.resolution.VariableReferenceResolution;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.util.Location;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AssemblyUtils} and {@link AssemblyQueries}.
 * <br>
 * I merged these together awhile ago when they were more tightly coupled but now
 * the utils are more generic and I don't really feel like breaking those back out.
 */
class AssemblyUtilsAndQueriesTest {
	@Test
	void resolvesVariableDeclarationsAndReferences() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        parameters: { alpha },
				        code: {
				        Start:
				            iload alpha
				            istore beta
				            iinc beta 1
				            goto End
				        End:
				            return
				        }
				    }
				}
				""";
		List<ASTElement> ast = processed(source);
		ASTMethod method = onlyMethod(ast);

		VariableQueryResult variables = AssemblyQueries.variables(method);
		assertEquals(2, variables.declarations().size());
		assertEquals(3, variables.usages().size());

		VariableInfo parameter = variables.declarations().stream()
				.filter(VariableInfo::parameter)
				.findFirst()
				.orElseThrow();
		assertEquals("alpha", parameter.identity().name());

		VariableInfo inferred = variables.declarations().stream()
				.filter(info -> !info.parameter())
				.findFirst()
				.orElseThrow();
		assertEquals("beta", inferred.identity().name());
		assertEquals(2, variables.writesOf(inferred.identity()).size());

		ASTIdentifier alpha = method.parameters().getFirst();
		Resolution declarationResolution = AssemblyQueries.resolveAt(ast, alpha.range().start());
		assertInstanceOf(VariableDeclarationResolution.class, declarationResolution);

		ASTIdentifier beta = variables.usages().stream()
				.filter(usage -> "beta".equals(usage.name()) && usage.kind() == VariableAccessKind.INCREMENT)
				.map(VariableUsage::reference)
				.findFirst()
				.orElseThrow();
		Resolution referenceResolution = AssemblyQueries.resolveAt(ast, beta.range().start());
		VariableReferenceResolution variableReferenceResolution =
				assertInstanceOf(VariableReferenceResolution.class, referenceResolution);
		assertEquals(inferred.identity(), variableReferenceResolution.usage().identity());
	}

	@Test
	void marksDuplicateVariableNamesAsAmbiguous() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        parameters: { same, same },
				        code: {
				            iload same
				            return
				        }
				    }
				}
				""";
		ASTMethod method = onlyMethod(processed(source));

		VariableQueryResult variables = AssemblyQueries.variables(method);
		assertEquals(2, variables.declarations().size());
		assertTrue(variables.usages().getFirst().ambiguous());
		assertNull(variables.usages().getFirst().identity());
	}

	@Test
	void collectsLabelsAcrossFlowExceptionsAndSwitches() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        exceptions: { { Start, End, Handler, java/lang/Exception } },
				        code: {
				        Start:
				            goto Handler
				        Switch:
				            tableswitch {
				                min: 10,
				                max: 11,
				                default: End,
				                cases: { Handler, End }
				            }
				        End:
				            return
				        Handler:
				            return
				        }
				    }
				}
				""";
		ASTMethod method = onlyMethod(processed(source));

		LabelQueryResult labels = AssemblyQueries.labels(method);
		assertEquals(4, labels.declarations().size());
		assertEquals(7, labels.usages().size());
		assertEquals(2, labels.usages().stream().filter(usage -> usage.kind() == LabelReferenceKind.SWITCH_CASE).count());
		assertEquals(1, labels.usages().stream().filter(usage -> usage.kind() == LabelReferenceKind.SWITCH_DEFAULT).count());
		assertTrue(labels.usages().stream().allMatch(LabelUsage::resolved));
	}

	@Test
	void variableQueryResultFiltersReadsAndWritesByIdentity() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        parameters: { alpha },
				        code: {
				            iload alpha
				            istore beta
				            iinc beta 1
				            ret alpha
				            return
				        }
				    }
				}
				""";
		ASTMethod method = onlyMethod(processed(source));

		VariableQueryResult variables = AssemblyQueries.variables(method);
		VariableInfo alpha = variables.declarations().stream()
				.filter(info -> "alpha".equals(info.identity().name()))
				.findFirst()
				.orElseThrow();
		VariableInfo beta = variables.declarations().stream()
				.filter(info -> "beta".equals(info.identity().name()))
				.findFirst()
				.orElseThrow();

		List<VariableUsage> alphaReads = variables.readsOf(alpha.identity());
		List<VariableUsage> alphaWrites = variables.writesOf(alpha.identity());
		List<VariableUsage> betaReads = variables.readsOf(beta.identity());
		List<VariableUsage> betaWrites = variables.writesOf(beta.identity());

		assertEquals(2, alphaReads.size());
		assertTrue(alphaWrites.isEmpty());
		assertTrue(betaReads.isEmpty());
		assertEquals(2, betaWrites.size());
		assertTrue(betaWrites.stream().anyMatch(usage -> usage.kind() == VariableAccessKind.WRITE));
		assertTrue(betaWrites.stream().anyMatch(usage -> usage.kind() == VariableAccessKind.INCREMENT));
		assertEquals(2, variables.usagesOf("beta").size());
	}

	@Test
	void labelQueryResultFindsDeclarationsUsagesAndReferences() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        exceptions: { { Start, End, Handler, java/lang/Exception } },
				        code: {
				        Start:
				            goto Handler
				        Switch:
				            tableswitch {
				                min: 10,
				                max: 11,
				                default: End,
				                cases: { Handler, End }
				            }
				        End:
				            return
				        Handler:
				            return
				        }
				    }
				}
				""";
		ASTMethod method = onlyMethod(processed(source));

		LabelQueryResult labels = AssemblyQueries.labels(method);
		LabelInfo handler = labels.declarationOf("Handler");
		assertNotNull(handler);
		assertEquals(1, labels.declarationsOf("Handler").size());

		LabelUsage flowToHandler = labels.usages().stream()
				.filter(usage -> "Handler".equals(usage.name()) && usage.kind() == LabelReferenceKind.FLOW)
				.findFirst()
				.orElseThrow();

		assertEquals(handler, labels.declarationOf(flowToHandler));
		assertEquals(3, labels.usagesOf("Handler").size());

		List<LabelUsage> handlerReferences = labels.referencesTo(handler);
		assertEquals(3, handlerReferences.size());
		assertTrue(handlerReferences.stream().anyMatch(usage -> usage.kind() == LabelReferenceKind.HANDLER));
		assertTrue(handlerReferences.stream().anyMatch(usage -> usage.kind() == LabelReferenceKind.FLOW));
		assertTrue(handlerReferences.stream().anyMatch(usage -> usage.kind() == LabelReferenceKind.SWITCH_CASE));
	}

	@Test
	void marksMissingAndDuplicateLabels() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        code: {
				        Dup:
				            goto Missing
				        Dup:
				            goto Dup
				            return
				        }
				    }
				}
				""";
		ASTMethod method = onlyMethod(processed(source));

		LabelQueryResult labels = AssemblyQueries.labels(method);
		assertEquals(2, labels.declarations().size());
		assertTrue(labels.declarations().stream().allMatch(LabelInfo::duplicate));
		assertEquals(2, labels.declarationsOf("Dup").size());

		LabelUsage missing = labels.usages().stream()
				.filter(usage -> "Missing".equals(usage.name()))
				.findFirst()
				.orElseThrow();
		assertFalse(missing.resolved());
		assertFalse(missing.ambiguous());
		assertNull(labels.declarationOf(missing));
		assertEquals(List.of(missing), labels.usagesOf("Missing"));
		assertTrue(labels.referencesTo(labels.declarations().getFirst()).stream().allMatch(LabelUsage::ambiguous));

		LabelUsage duplicate = labels.usages().stream()
				.filter(usage -> "Dup".equals(usage.name()))
				.findFirst()
				.orElseThrow();
		assertTrue(duplicate.ambiguous());
	}

	@Test
	void resolvesLineAndColumnSelections() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        code: {
				        A:
				            goto A
				            return
				        }
				    }
				}
				""";
		List<ASTElement> ast = processed(source);
		Resolution resolution = AssemblyQueries.resolveAt(ast, 4, 9);
		assertInstanceOf(LabelDeclarationResolution.class, resolution);
	}

	@Test
	void supportsJvmFormatAwareOverloads() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        code: {
				        A:
				            goto A
				            return
				        }
				    }
				}
				""";
		List<ASTElement> ast = processed(source);
		ASTMethod method = onlyMethod(ast);
		LabelQueryResult labels = AssemblyQueries.labels(method, BytecodeFormat.JVM);
		assertEquals(1, labels.declarationsOf("A").size());

		Resolution resolution = AssemblyQueries.resolveAt(ast, 5, 18, BytecodeFormat.JVM);
		assertInstanceOf(LabelReferenceResolution.class, resolution);
	}

	@Test
	void collectsDalvikLabelsAcrossFlowAndSwitchInstructions() {
		String source = """
				.method public static demo ()V {
				  code: {
				  Start:
				    if-eq v0 v1 Handler
				    if-nez v0 End
				    packed-switch { first: 5, targets: { Start, Handler } }
				    sparse-switch { 7: End, 8: Handler }
				    goto End
				  Handler:
				    return-void
				  End:
				    return-void
				  }
				}
				""";
		List<ASTElement> ast = processed(source, BytecodeFormat.DALVIK);
		ASTMethod method = onlyMethod(ast);

		LabelQueryResult labels = AssemblyQueries.labels(method, BytecodeFormat.DALVIK);
		assertEquals(3, labels.declarations().size());
		assertEquals(7, labels.usages().size());
		assertTrue(labels.usages().stream().allMatch(LabelUsage::resolved));
		assertEquals(4, labels.usages().stream().filter(usage -> usage.kind() == LabelReferenceKind.SWITCH_CASE).count());

		ASTInstruction ifEq = method.code().instructions().stream()
				.filter(instruction -> "if-eq".equals(instruction.identifier().content()))
				.findFirst()
				.orElseThrow();
		ASTIdentifier handlerReference = ifEq.argument(2, ASTIdentifier.class);
		Resolution resolution = AssemblyQueries.resolveAt(ast, handlerReference.range().start(), BytecodeFormat.DALVIK);
		LabelReferenceResolution labelReferenceResolution = assertInstanceOf(LabelReferenceResolution.class, resolution);
		assertEquals("Handler", labelReferenceResolution.usage().name());
	}

	@Test
	void resolvesDalvikTypeReferences() {
		String source = """
				.method public static demo ()V {
				  code: {
				    const-class v0 Ljava/lang/String;
				    check-cast v0 Ljava/lang/Object;
				    instance-of v0 v1 Ljava/lang/CharSequence;
				    new-instance v0 Ljava/lang/StringBuilder;
				    new-array v0 v1 [I
				    filled-new-array { v0 } [I
				    return-void
				  }
				}
				""";
		List<ASTElement> ast = processed(source, BytecodeFormat.DALVIK);
		ASTMethod method = onlyMethod(ast);

		assertTypeResolution(ast, method.code().instructions().get(0).argument(1, ASTIdentifier.class));
		assertTypeResolution(ast, method.code().instructions().get(1).argument(1, ASTIdentifier.class));
		assertTypeResolution(ast, method.code().instructions().get(3).argument(1, ASTIdentifier.class));
		assertTypeResolution(ast, method.code().instructions().get(4).argument(2, ASTIdentifier.class));
	}

	@Test
	void classifiesJvmAndDalvikInstructionKinds() {
		assertTrue(AssemblyUtils.isFlowControlInstruction(BytecodeFormat.JVM, "goto_w"));
		assertTrue(AssemblyUtils.isFlowControlInstruction(BytecodeFormat.DALVIK, "if-eq"));
		assertFalse(AssemblyUtils.isFlowControlInstruction(BytecodeFormat.DALVIK, "tableswitch"));

		assertTrue(AssemblyUtils.isSwitchInstruction(BytecodeFormat.JVM, "lookupswitch"));
		assertTrue(AssemblyUtils.isSwitchInstruction(BytecodeFormat.DALVIK, "packed-switch"));
		assertFalse(AssemblyUtils.isSwitchInstruction(BytecodeFormat.JVM, "goto"));

		assertTrue(AssemblyUtils.isTypeReferenceInstruction(BytecodeFormat.JVM, "multianewarray"));
		assertTrue(AssemblyUtils.isTypeReferenceInstruction(BytecodeFormat.DALVIK, "new-array"));
		assertFalse(AssemblyUtils.isTypeReferenceInstruction(BytecodeFormat.DALVIK, "goto"));

		assertTrue(AssemblyUtils.isVariableReferenceInstruction("aload"));
		assertTrue(AssemblyUtils.isVariableReferenceInstruction("iinc"));
		assertFalse(AssemblyUtils.isVariableReferenceInstruction("goto"));
	}

	@Test
	void picksElementsAndFindsInstructions() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        code: {
				        Start:
				            goto End
				            tableswitch {
				                min: 1,
				                max: 2,
				                default: End,
				                cases: { Start, End }
				            }
				        End:
				            return
				        }
				    }
				}
				""";
		List<ASTElement> ast = processed(source);
		ASTMethod method = onlyMethod(ast);
		ASTLabel start = AssemblyUtils.findLabelDeclaration(method, "Start");
		assertNotNull(start);

		ASTInstruction switchInstruction = method.code().instructions().stream()
				.filter(instruction -> "tableswitch".equals(instruction.identifier().content()))
				.findFirst()
				.orElseThrow();
		ASTObject switchObject = switchInstruction.argumentObject(0);
		ASTIdentifier defaultCase = switchObject.value("default");
		Location defaultLocation = defaultCase.location();

		assertSame(start.identifier(), AssemblyUtils.pickElementAt(ast, start.range().start()));
		assertSame(start, AssemblyUtils.findInstructionAt(ast, start.range().start()));
		assertSame(defaultCase, AssemblyUtils.pickElementAt(ast, defaultCase.range().start()));
		assertSame(defaultCase, AssemblyUtils.pickElementAt(ast, defaultLocation.line(), defaultLocation.column()));
		assertSame(switchInstruction, AssemblyUtils.findInstructionAt(ast, defaultCase.range().start()));
		assertSame(switchInstruction, AssemblyUtils.findInstructionAt(ast, defaultLocation.line(), defaultLocation.column()));
	}

	@Test
	void getsLabelDeclarationViaCompatibilityHelper() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        code: {
				        Start:
				            goto End
				        End:
				            return
				        }
				    }
				}
				""";
		ASTMethod method = onlyMethod(processed(source));

		ASTLabel start = AssemblyUtils.getLabelDeclaration(method, "Start");
		ASTLabel end = AssemblyUtils.getLabelDeclaration(method, "End");
		ASTLabel missing = AssemblyUtils.getLabelDeclaration(method, "Missing");

		assertNotNull(start);
		assertEquals("Start", start.identifier().literal());
		assertNotNull(end);
		assertEquals("End", end.identifier().literal());
		assertNull(missing);
	}

	@Test
	void findsInstructionViaCompatibilityHelper() {
		String source = """
				.class public Example {
				    .method public demo ()V {
				        code: {
				        Start:
				            goto End
				            tableswitch {
				                min: 1,
				                max: 2,
				                default: End,
				                cases: { Start, End }
				            }
				        End:
				            return
				        }
				    }
				}
				""";
		List<ASTElement> ast = processed(source);
		ASTMethod method = onlyMethod(ast);

		ASTInstruction gotoInstruction = method.code().instructions().stream()
				.filter(instruction -> "goto".equals(instruction.identifier().content()))
				.findFirst()
				.orElseThrow();
		ASTInstruction switchInstruction = method.code().instructions().stream()
				.filter(instruction -> "tableswitch".equals(instruction.identifier().content()))
				.findFirst()
				.orElseThrow();
		ASTObject switchObject = switchInstruction.argumentObject(0);
		ASTIdentifier defaultCase = switchObject.value("default");

		ASTInstruction foundGoto = AssemblyUtils.findInstruction(ast, gotoInstruction.range().start(),
				gotoInstruction.location().line());
		ASTInstruction foundSwitch = AssemblyUtils.findInstruction(ast, defaultCase.range().start(),
				defaultCase.location().line());
		ASTInstruction missing = AssemblyUtils.findInstruction(ast, -1, 1);

		assertSame(gotoInstruction, foundGoto);
		assertSame(switchInstruction, foundSwitch);
		assertNull(missing);
	}

	private static void assertTypeResolution(List<ASTElement> ast, ASTIdentifier typeIdentifier) {
		Resolution resolution = AssemblyQueries.resolveAt(ast, typeIdentifier.range().start(), BytecodeFormat.DALVIK);
		TypeReferenceResolution typeReferenceResolution = assertInstanceOf(TypeReferenceResolution.class, resolution);
		assertSame(typeIdentifier, typeReferenceResolution.type());
	}

	private static ASTMethod onlyMethod(List<ASTElement> ast) {
		ASTElement first = ast.getFirst();
		if (first instanceof ASTMethod method)
			return method;
		return (ASTMethod) ((ASTClass) first).contents().getFirst();
	}

	private static List<ASTElement> processed(String source) {
		return processed(source, BytecodeFormat.JVM);
	}

	private static List<ASTElement> processed(String source, BytecodeFormat format) {
		Result<List<ASTElement>> result = AssemblyParseFixture.processDeclarations(
				"AssemblyQueriesTest.jasm",
				source,
				format
		);
		if (result.hasErr())
			fail(result.errors().toString());
		return result.get();
	}
}

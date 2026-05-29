package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.query.AssemblyQueries;
import me.darknet.assembler.query.LabelInfo;
import me.darknet.assembler.query.LabelQueryResult;
import me.darknet.assembler.query.LabelReferenceKind;
import me.darknet.assembler.query.LabelUsage;
import me.darknet.assembler.query.VariableAccessKind;
import me.darknet.assembler.query.VariableInfo;
import me.darknet.assembler.query.VariableQueryResult;
import me.darknet.assembler.query.VariableUsage;
import me.darknet.assembler.query.resolution.LabelDeclarationResolution;
import me.darknet.assembler.query.resolution.Resolution;
import me.darknet.assembler.query.resolution.VariableDeclarationResolution;
import me.darknet.assembler.query.resolution.VariableReferenceResolution;
import me.darknet.assembler.test.AssemblyParseFixture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssemblyQueriesTest {
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
		LabelInfo handler = labels.declarations().stream()
				.filter(info -> "Handler".equals(info.name()))
				.findFirst()
				.orElseThrow();
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

	private static ASTMethod onlyMethod(List<ASTElement> ast) {
		return (ASTMethod) ((ASTClass) ast.getFirst()).contents().getFirst();
	}

	private static List<ASTElement> processed(String source) {
		Result<List<ASTElement>> result = AssemblyParseFixture.processDeclarations(
				"AssemblyQueriesTest.jasm",
				source,
				me.darknet.assembler.parser.BytecodeFormat.JVM
		);
		if (result.hasErr()) {
			fail(result.errors().toString());
		}
		return result.get();
	}
}

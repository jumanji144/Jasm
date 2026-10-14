package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Round-trips each Dalvik sample through the printer, parser, semantic processor, and compiler.
 */
class SampleTests {
	@Test
	void allSampleRoundTrip() throws IOException {
		List<DalvikSampleFixture.DexSample> samples = DalvikSampleFixture.allDexSamples();
		assertFalse(samples.isEmpty(), "No vendored Dalvik samples were discovered");

		for (DalvikSampleFixture.DexSample sample : samples) {
			DexFile file = DalvikDexFixture.readDex(sample.read());
			assertFalse(file.definitions().isEmpty(), sample.name() + " declares no classes");

			for (ClassDefinition definition : file.definitions()) {
				String label = sample.name() + " " + definition.getType().internalName();
				String printed = DalvikDexFixture.print(definition);

				// Reparse the printed source and ensure it has no errors.
				Outcome<List<ASTElement>> reparsed = AssemblyParseFixture.processDeclarations(
						"<" + sample.name() + ">", printed, DalvikTargetContext.INSTANCE);
				assertFalse(reparsed.hasErrors(), label
						+ " printed source does not reparse:\n"
						+ DiagnosticAssertions.formatErrors(reparsed.errors()) + "\n" + printed);

				// Process the reparsed source and ensure it has no errors.
				ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
						SemanticProcessor.process(reparsed.requireValue(), DalvikTargetContext.INSTANCE),
						label + " printed source does not process");
				Outcome<DalvikCompileResult> compiled = new DalvikCompiler().compile(unit, TestUtils.options());
				assertFalse(compiled.errors().stream().anyMatch(diagnostic ->
						diagnostic.phase() != DiagnosticPhase.OUTPUT_VERIFICATION ||
							diagnostic.code() != DiagnosticCode.ANALYSIS_FAILURE), label
						+ " printed source has unexpected compiler errors:\n"
						+ DiagnosticAssertions.formatErrors(compiled.errors()) + "\n" + printed);
				assertNotNull(compiled.requireValue().representation(), label + " produced no representation");
			}
		}
	}
}

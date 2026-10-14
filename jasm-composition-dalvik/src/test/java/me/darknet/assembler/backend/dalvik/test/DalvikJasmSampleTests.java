package me.darknet.assembler.backend.dalvik.test;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DalvikJasmSampleTests {
	// TODO: Dalvik is more complex than JVM in terms of us round-tripping the source.
	//  So for now we'll just test that the samples compile, and not that they round-trip back to the same source.

	@Test
	void allJasmSamplesCompile() {
		List<DalvikJasmSampleFixture.DalvikTextSample> samples = DalvikJasmSampleFixture.allJasmSamples();
		assertFalse(samples.isEmpty(), "No Dalvik JASM source samples were discovered");

		for (DalvikJasmSampleFixture.DalvikTextSample sample : samples) {
			String source = sample.read();
			assertDoesNotThrow(
					() -> TestUtils.processDalvik(source, TestUtils.options(),
							result -> assertNotNull(result.representation())),
					() -> "Failed to process Dalvik JASM sample " + sample.name() + ":\n" + source
			);
		}
	}
}

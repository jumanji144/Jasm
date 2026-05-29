package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.test.BinarySampleFixture;
import me.darknet.assembler.test.JvmDecompilationFixture;
import me.darknet.assembler.test.JvmDisassemblyFixture;
import me.darknet.assembler.test.JvmRoundTripFixture;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static me.darknet.assembler.test.SourceNormalization.normalize;
import static org.junit.jupiter.api.Assertions.*;

class JvmRoundTripTest {
    @ParameterizedTest
    @MethodSource("validSamples")
    void all(BinarySampleFixture.JvmTextSample sample) {
        String source = sample.read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.version(21);

        var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);
        if (source.contains("SKIP-ROUND-TRIP-EQUALITY")) {
            return;
        }

        assertEquals(
                normalize(source),
                normalize(roundTrip.disassembledSource()),
                "There was an unexpected difference in unmodified class: " + sample.name()
        );
    }

    @Test
    @Disabled
    void kotlinSr2c() {
        String source = validSamples().stream()
                .filter(sample -> sample.name().contains("KKKSample"))
                .findFirst()
                .orElseThrow()
                .read();
        JvmRoundTripFixture.roundTripJvm(source, new TestJvmCompilerOptions());
    }

    @Test
    @Disabled
    void kotlinSrc() {
        String source = validSamples().stream()
                .filter(sample -> sample.name().contains("KotlinSample"))
                .findFirst()
                .orElseThrow()
                .read();
        JvmRoundTripFixture.roundTripJvm(source, new TestJvmCompilerOptions());
    }

    @Test
    void kotlin() {
        byte[] raw = BinarySampleFixture.binarySample("ExtrasConfig.sample").read();
        String source = JvmDisassemblyFixture.disassembleJvm(raw);
        JvmRoundTripFixture.roundTripJvm(source, new TestJvmCompilerOptions());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "u000d.sample", "u000a.sample", "u0009.sample", "u2028.sample",
            "u002c.sample", "u002e.sample", "u0000.sample", "u0001.sample",
            "u0002.sample", "u0003.sample", "u0004.sample", "u0005.sample",
            "u0006.sample", "u0007.sample", "u0008.sample", "u0009.sample",
            "u000a.sample", "u000b.sample", "u000c.sample", "u000d.sample",
            "u000e.sample", "u000f.sample", "u0010.sample", "u0011.sample",
            "u0012.sample", "u0013.sample", "u0014.sample", "u2000.sample",
            "u2001.sample", "u2002.sample", "u2003.sample", "u2004.sample",
            "u2005.sample", "u2006.sample", "u2007.sample", "u2008.sample",
            "u2009.sample", "u200a.sample",
    })
    void unicodeEscapeRoundTrip(String name) {
        BinarySampleFixture.BinarySample sample = BinarySampleFixture.binarySample(name);
        String source = JvmDisassemblyFixture.disassembleJvm(sample.read());
        var roundTrip = JvmRoundTripFixture.roundTripJvm(source, new TestJvmCompilerOptions());

        assertEquals(
                normalize(source),
                normalize(roundTrip.disassembledSource()),
                "There was an unexpected difference in unmodified class: " + sample.name()
        );
    }

    @Test
    void supportInfinity() {
        String source = BinarySampleFixture.jvmSample("Example-infinity.jasm").read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(ValuedJvmAnalysisEngine::new);

        var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);
        assertEquals(
                normalize(source.replace("InfinityD", "Infinity").replace("+", "")),
                normalize(roundTrip.disassembledSource())
        );
    }

    @Test
    void supportInfinityInWholeNumberRepresentation() {
        byte[] raw = BinarySampleFixture.binarySample("InfinityFloat.sample").read();

        String source1 = JvmDisassemblyFixture.disassembleJvm(raw, ctx -> ctx.setForceWholeNumberRepresentation(true));
        String source2 = JvmDisassemblyFixture.disassembleJvm(raw, ctx -> ctx.setForceWholeNumberRepresentation(false));

        assertEquals(source1, source2);
    }

    @Test
    void supportNan() {
        String source = BinarySampleFixture.jvmSample("Example-nan.jasm").read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(ValuedJvmAnalysisEngine::new);

        var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);
        assertEquals(normalize(source.replace("NaND", "NaN")), normalize(roundTrip.disassembledSource()));
    }

    @Test
    void handlePrimitiveWidening() {
        byte[] raw = BinarySampleFixture.binarySample("TextFormatConfig.sample").read();
        String source = JvmDisassemblyFixture.disassembleJvm(raw);
        assertTrue(source.contains("iload shortenPath"));
        assertTrue(source.contains("iload escape"));
        assertTrue(source.contains("iload maxLength"));

        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(ValuedJvmAnalysisEngine::new);
        var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);
        assertTrue(roundTrip.disassembledSource().contains("iload shortenPath"));
        assertTrue(roundTrip.disassembledSource().contains("iload escape"));
        assertTrue(roundTrip.disassembledSource().contains("iload maxLength"));
    }

    @Test
    void kotlinVariableGarbageIHateKotlin() {
        byte[] raw = BinarySampleFixture.binarySample("KotlinVarScoping.sample").read();
        String source = JvmDisassemblyFixture.disassembleJvm(raw);
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.engineProvider(ValuedJvmAnalysisEngine::new);
        var roundTrip = JvmRoundTripFixture.roundTripJvm(source, options);
        
        // Seriously this language is such a disaster when you look at the generated code.
        //
        // Anyways, we shouldn't be breaking the code to the point of decompilation failures.
        // Historically this has happened due to the variable name picking in the disassembler.
        // It would pick improperly scoped variables, which when reassembled, result in invalid code
        // that decompilers couldn't handle.
        String decompileOriginal = JvmDecompilationFixture.decompile(raw);
        String decompileRound = roundTrip.compilation().requireDecompilation();
        assertFalse(decompileOriginal.contains("This method has failed to decompile"), "Original class failed to decompile, cannot test");
        assertFalse(decompileRound.contains("This method has failed to decompile"), "Round-tripped class failed to decompile, cannot test");
    }

    static List<BinarySampleFixture.JvmTextSample> validSamples() {
        return BinarySampleFixture.validJvmSamples();
    }
}

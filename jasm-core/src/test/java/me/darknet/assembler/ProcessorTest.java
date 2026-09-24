package me.darknet.assembler;

import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.test.FixtureTarget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessorTest {

    // TODO: This is more of a processor pipeline test than a test of the small 'Processor' helper class
    //  - Maybe move this someplace else? Or rename to 'ParserPipelineTest' or something?
    //  - If we rename it, that pipeline probably can have additional tests added to it.

    @Test
    public void testSynchronousResultProcessesDalvikSource() {
        var result = Processor.processSourceResult(
                ".class public Example {}",
                "<stdin>",
                FixtureTarget.DALVIK.context()
        );

        DiagnosticAssertions.requireSuccess(result, "Dalvik source should process successfully");
        assertEquals(1, result.requireValue().size());
    }

    @Test
    public void testTokenizerErrorsShortCircuitProcessing() {
        AtomicBoolean consumedAst = new AtomicBoolean(false);
        AtomicReference<List<Diagnostic>> parseErrors = new AtomicReference<>();

        Processor.processSource(
                "/",
                "<stdin>",
                ast -> consumedAst.set(true),
                parseErrors::set,
                FixtureTarget.JVM.context()
        );

        assertFalse(consumedAst.get());
        assertNotNull(parseErrors.get());
        assertFalse(parseErrors.get().isEmpty());
        DiagnosticAssertions.assertHasErrorCode(
                parseErrors.get(),
                DiagnosticCode.UNEXPECTED_TOKEN,
                "Trailing slash should report an unexpected-token diagnostic"
        );
        DiagnosticAssertions.assertPhase(
                parseErrors.get(),
                DiagnosticPhase.LEXER,
                "Trailing slash should be reported during lexing"
        );
    }
}

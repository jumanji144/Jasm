package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.parser.BytecodeFormat;
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
    public void testTokenizerErrorsShortCircuitProcessing() {
        AtomicBoolean consumedAst = new AtomicBoolean(false);
        AtomicReference<List<Error>> parseErrors = new AtomicReference<>();

        Processor.processSource(
                "/",
                "<stdin>",
                ast -> consumedAst.set(true),
                parseErrors::set,
                BytecodeFormat.DEFAULT
        );

        assertFalse(consumedAst.get());
        assertNotNull(parseErrors.get());
        assertFalse(parseErrors.get().isEmpty());
    }
}

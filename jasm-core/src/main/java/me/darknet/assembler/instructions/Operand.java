package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class Operand {

    private final Processor verifier;

    public Operand(Processor verifier) {
        this.verifier = verifier;
    }

    /**
     * Verify the operand.
     * @param context the parser context
     * @param element the element to verify
     * @see me.darknet.assembler.parser.processor.ASTProcessor.ParserContext#isNotType(ASTElement, ElementType, String)
     * @see me.darknet.assembler.parser.processor.ASTProcessor.ParserContext#isNull(Object, String, Location)
     * @see me.darknet.assembler.parser.processor.ASTProcessor.ParserContext#validateArray(ASTArray, ElementType, String, ASTElement)
     * @see me.darknet.assembler.parser.processor.ASTProcessor.ParserContext#validateElement(ASTElement, ElementType, String, ASTElement)
     * @see me.darknet.assembler.parser.processor.ASTProcessor.ParserContext#validateEmptyableElement(ASTElement, ElementType, String, ASTElement)
     * @see me.darknet.assembler.parser.processor.ASTProcessor.ParserContext#validateObject(ASTElement, String, ASTElement, String...)
     * @see me.darknet.assembler.parser.processor.ASTProcessor.ParserContext#throwUnexpectedElementError(String, ASTElement)
     * @see me.darknet.assembler.parser.processor.ASTProcessor.ParserContext#throwError(String, Location)
     */
    public void verify(ASTProcessor.ParserContext context, @NotNull ASTElement element) {
        verifier.accept(context, element);
    }

    @FunctionalInterface
    public interface Processor extends BiConsumer<ASTProcessor.ParserContext, ASTElement> {
    }

}

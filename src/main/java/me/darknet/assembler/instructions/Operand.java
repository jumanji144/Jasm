package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.parser.ASTProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class Operand {

	@FunctionalInterface
	public interface Processor extends BiConsumer<ASTProcessor.ParserContext, ASTElement> { }

	private final Processor verifier;

	public Operand(Processor verifier) {
		this.verifier = verifier;
	}

	public void verify(ASTProcessor.ParserContext context, @NotNull ASTElement element) {
		verifier.accept(context, element);
	}

}

package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Operand definition that verifies a source element and optionally resolves its typed value.
 */
public class Operand {
	private final Processor verifier;
	private final @Nullable OperandValueResolver resolver;

	public Operand(Processor verifier) {
		this(verifier, null);
	}

	/**
	 * @param verifier
	 * 		verifier rejecting malformed operand shapes.
	 * @param resolver
	 * 		resolver producing a typed value, or {@code null} when the operand has no typed meaning.
	 */
	public Operand(@NotNull Processor verifier, @Nullable OperandValueResolver resolver) {
		this.verifier = Objects.requireNonNull(verifier, "verifier");
		this.resolver = resolver;
	}

	/**
	 * Verify the operand.
	 *
	 * @param context
	 * 		the parser context
	 * @param element
	 * 		the element to verify
	 *
	 * @see me.darknet.assembler.parser.processor.ProcessorContext#isNotType(ASTElement, ElementType, String)
	 * @see me.darknet.assembler.parser.processor.ProcessorContext#isNull(Object, String, Location)
	 * @see me.darknet.assembler.parser.processor.ProcessorContext#validateArray(ASTArray, ElementType, String, ASTElement)
	 * @see me.darknet.assembler.parser.processor.ProcessorContext#validateElement(ASTElement, ElementType, String, ASTElement)
	 * @see me.darknet.assembler.parser.processor.ProcessorContext#validateEmptyableElement(ASTElement, ElementType, String, ASTElement)
	 * @see me.darknet.assembler.parser.processor.ProcessorContext#validateObject(ASTElement, String, ASTElement, String...)
	 * @see me.darknet.assembler.parser.processor.ProcessorContext#throwUnexpectedElementError(String, ASTElement)
	 * @see me.darknet.assembler.parser.processor.ProcessorContext#throwError(String, Location)
	 */
	public void verify(ProcessorContext context, @NotNull ASTElement element) {
		verifier.accept(context, element);
	}

	/**
	 * @return Verifier declaring the accepted source shapes for this operand.
	 */
	public Processor verifier() {
		return verifier;
	}

	/**
	 * @param context
	 * 		processor context receiving diagnostics.
	 * @param element
	 * 		verified source element for this operand.
	 * @return Typed value, or {@code null} when no resolver is declared or it reports an error.
	 */
	public @Nullable OperandValue resolve(ProcessorContext context, @NotNull ASTElement element) {
		return resolver == null ? null : resolver.resolve(context, element);
	}

	@FunctionalInterface
	public interface Processor extends BiConsumer<ProcessorContext, ASTElement> {}
}

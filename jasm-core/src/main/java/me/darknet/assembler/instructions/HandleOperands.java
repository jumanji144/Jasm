package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.descriptor.DescriptorParser;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.parser.processor.ProcessorContext;

import java.util.List;

/**
 * Shared validation for JVM-shaped method handles accepted by bundled targets.
 */
public final class HandleOperands {
    public static final Operand HANDLE = new Operand(HandleOperands::verify);

    private HandleOperands() {}

    /**
     * Reports malformed handle shapes through the processor context.
     *
     * @param context
     * 		Processor context receiving validation diagnostics.
     * @param element
     * 		Candidate handle element.
     */
    public static void verify(ProcessorContext context, ASTElement element) {
        verifyAndReport(context, element);
    }

    /**
     * Validates a handle while retaining the historical invalid-shape result for compound constants.
     *
     * @param context
     * 		Processor context receiving validation diagnostics.
     * @param element
     * 		Candidate handle element.
     *
     * @return {@code true} when validation reported an invalid handle.
     */
    public static boolean verifyAndReport(ProcessorContext context, ASTElement element) {
        if (element instanceof ASTIdentifier identifier) {
            Handle handle = Handle.HANDLE_SHORTCUTS.get(identifier.content());
            if (handle != null)
                return false;

            context.throwUnexpectedElementError("handle or short-handle", element);
            return true;
        }

        if (context.isNotType(element, ElementType.ARRAY, "handle"))
            return true;

        ASTArray array = (ASTArray) element;
        List<ASTIdentifier> values = context.validateArray(array, ElementType.IDENTIFIER, "handle element", element);
        if (array.values().size() != 3 || values.size() != 3) {
            context.throwUnexpectedElementError("kind, name and descriptor", element);
            return true;
        }

        Handle.Kind kind = Handle.Kind.from(values.getFirst().content());
        if (kind == null) {
            context.throwUnexpectedElementError("kind", values.getFirst());
            return true;
        }

        String descriptor = values.get(2).content();
        boolean valid = kind.isField()
                ? DescriptorParser.isValidFieldDescriptor(descriptor)
                : DescriptorParser.isValidMethodDescriptor(descriptor);
        if (!valid) {
            context.throwUnexpectedElementError(
                    kind.isField() ? "valid field descriptor" : "valid method descriptor", values.get(2));
            return true;
        }
        return false;
    }
}

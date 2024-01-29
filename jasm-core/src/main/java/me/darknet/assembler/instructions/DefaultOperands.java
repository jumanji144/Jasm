package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.util.DescriptorUtil;

public enum DefaultOperands implements Operands {

    STRING((context, element) -> context.isNotType(element, ElementType.STRING, "string literal")),
    INTEGER((context, element) -> {
        if (!context.isNotType(element, ElementType.NUMBER, "number literal")) {
            // verify that it's an integer
            ASTNumber number = (ASTNumber) element;
            if (number.isFloatingPoint()) {
                context.throwUnexpectedElementError("integer literal", number);
            }
        }
    }),
    NUMBER((context, element) -> context.isNotType(element, ElementType.NUMBER, "number literal")),
    IDENTIFIER((context, element) -> context.isNotType(element, ElementType.IDENTIFIER, "identifier")),
    LITERAL((context, element) -> {
        // literals can be: number or identifier
        if (element.type() != ElementType.NUMBER && element.type() != ElementType.IDENTIFIER)
            context.throwUnexpectedElementError("literal", element);
    }),
    LABEL((context, element) -> context.isNotType(element, ElementType.IDENTIFIER, "label")),
    METHOD_DESCRIPTOR((context, element) -> {
        // method descriptor can be: method or array
        if (context.isNotType(element, ElementType.IDENTIFIER, "method descriptor"))
            return;

        boolean valid = DescriptorUtil.isValidMethodDescriptor(element.content());
        if (!valid)
            context.throwUnexpectedElementError("method descriptor", element);

    }),
    FIELD_DESCRIPTOR((context, element) -> {
        // field descriptor can be: field or array
        if (context.isNotType(element, ElementType.IDENTIFIER, "field descriptor"))
            return;

        boolean valid = DescriptorUtil.isValidFieldDescriptor(element.content());
        if (!valid)
            context.throwUnexpectedElementError("field descriptor", element);
    }),
    DESCRIPTOR((context, element) -> {
        // descriptor can be: method or field or array
        if (context.isNotType(element, ElementType.IDENTIFIER, "descriptor"))
            return;

        boolean valid = DescriptorUtil.isValidMethodDescriptor(element.content());
        if (!valid)
            valid = DescriptorUtil.isValidFieldDescriptor(element.content());

        if (!valid)
            context.throwUnexpectedElementError("descriptor", element);
    });

    private final Operand operand;

    DefaultOperands(Operand.Processor operand) {
        this.operand = new Operand(operand);
    }

    @Override
    public Operand getOperand() {
        return operand;
    }

}

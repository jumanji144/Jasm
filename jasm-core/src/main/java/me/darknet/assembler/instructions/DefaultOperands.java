package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.descriptor.DescriptorParser;

/**
 * Standard operand definitions used by instruction metadata.
 */
public enum DefaultOperands implements Operands {
    STRING((context, element) -> context.isNotType(element, ElementType.STRING, "string literal")),
    INTEGER((context, element) -> {
        if (!context.isNotType(element, ElementType.NUMBER, "number literal")) {
            ASTNumber number = (ASTNumber) element;
            if (number.isFloatingPoint())
                context.throwUnexpectedElementError("integer literal", number);
            try {
                number.asInt();
            } catch (NumberFormatException e) {
                context.throwIllegalArgumentStateError("not a valid integer literal", number);
            }
        }
    }),
    NUMBER((context, element) -> {
        if (element.type() == ElementType.IDENTIFIER && isSpecialNumberSpelling(element.content()))
            return;
        if (context.isNotType(element, ElementType.NUMBER, "number literal"))
            return;
        ASTNumber number = (ASTNumber) element;
        try {
            number.number();
        } catch (NumberFormatException e) {
            context.throwUnexpectedElementError("valid number literal", number);
        }
    }),
    IDENTIFIER((context, element) -> context.isNotType(element, ElementType.IDENTIFIER, "identifier")),
    VARIABLE_NAME((context, element) -> {
        if (element.type() == ElementType.IDENTIFIER)
            return;
        if (context.isNotType(element, ElementType.NUMBER, "local variable name or index"))
            return;
        ASTNumber number = (ASTNumber) element;
        if (number.isFloatingPoint())
            context.throwUnexpectedElementError("local variable name or index", number);
    }),
    LITERAL((context, element) -> {
        if (element.type() != ElementType.NUMBER && element.type() != ElementType.IDENTIFIER)
            context.throwUnexpectedElementError("literal", element);
    }),
    LABEL((context, element) -> context.isNotType(element, ElementType.IDENTIFIER, "label")),
    METHOD_DESCRIPTOR((context, element) -> {
        if (context.isNotType(element, ElementType.IDENTIFIER, "method descriptor"))
            return;
        if (!DescriptorParser.isValidMethodDescriptor(element.content()))
            context.throwUnexpectedElementError("valid method descriptor", element);
    }),
    FIELD_DESCRIPTOR((context, element) -> {
        if (context.isNotType(element, ElementType.IDENTIFIER, "field descriptor"))
            return;
        if (!DescriptorParser.isValidFieldDescriptor(element.content()))
            context.throwUnexpectedElementError("valid field descriptor", element);
    }),
    DESCRIPTOR((context, element) -> {
        if (context.isNotType(element, ElementType.IDENTIFIER, "descriptor"))
            return;
        boolean valid = DescriptorParser.isValidMethodDescriptor(element.content())
                || DescriptorParser.isValidFieldDescriptor(element.content());
        if (!valid)
            context.throwUnexpectedElementError("valid descriptor", element);
    }),
    MEMBER_PATH(LITERAL.getOperand().verifier(), OperandValues::memberPath);

    private final Operand operand;

    DefaultOperands(Operand.Processor verifier) {
        this(verifier, null);
    }

    DefaultOperands(Operand.Processor verifier, OperandValueResolver resolver) {
        this.operand = new Operand(verifier, resolver);
    }

    @Override
    public Operand getOperand() {
        return operand;
    }

    private static boolean isSpecialNumberSpelling(String content) {
        return switch (content.toLowerCase()) {
            case "nan", "nand", "nanf",
                    "infinity", "+infinity", "-infinity",
                    "infinityd", "+infinityd", "-infinityd",
                    "infinityf", "+infinityf", "-infinityf" -> true;
            default -> false;
        };
    }
}

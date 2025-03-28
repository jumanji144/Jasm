package me.darknet.assembler.instructions.dalvik;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.Operands;
import me.darknet.assembler.instructions.jvm.JvmOperands;
import me.darknet.assembler.parser.processor.ASTProcessor;

public enum DalvikOperands implements Operands {

    CONSTANT(DalvikOperands::verifyConstant),
    CLASS_TYPE((context, element) -> {
        // class type can be: class or array
        if (context.isNotType(element, ElementType.IDENTIFIER, "class type"))
            return;

        char first = element.content().charAt(0);
        switch (first) {
            case 'L', '[' -> {
            }
            default -> context.throwUnexpectedElementError("class or array descriptor", element);
        }
    }),
    METHOD_TYPE((context, element) -> {
        // method type can be: method or array
        if (context.isNotType(element, ElementType.IDENTIFIER, "method type"))
            return;

        char first = element.content().charAt(0);
        switch (first) {
            case '(', '[' -> {
            }
            default -> context.throwUnexpectedElementError("method or array descriptor", element);
        }
    }),
    HANDLE(JvmOperands::verifyHandle),
    ARGS_ARRAY((context, element) -> {
        // args array can be: register or array
        ASTArray array = context.validateEmptyableElement(element, ElementType.ARRAY, "args array", element);
        for (ASTElement value : array.values()) {
            if (context.isNull(value, "args array element", array.location()))
                return;
            assert value != null;
            DalvikOperands.verifyConstant(context, element);
        }
    }),
    REGISTER_ARRAY((context, element) -> {
        // register array can be: register or array
        ASTArray array = context.validateEmptyableElement(element, ElementType.ARRAY, "register array", element);
        for (ASTElement value : array.values()) {
            if (context.isNull(value, "register array element", array.location()))
                return;
            assert value != null;
            if(value.type() != ElementType.IDENTIFIER)
                context.throwUnexpectedElementError("register", value);
        }
    }),
    DATA_ARRAY((context, element) -> {
        // data array can be: number or array
        ASTArray array = context.validateEmptyableElement(element, ElementType.ARRAY, "data array", element);
        for (ASTElement value : array.values()) {
            if (context.isNull(value, "data array element", array.location()))
                return;
            assert value != null;
            if(value.type() != ElementType.NUMBER)
                context.throwUnexpectedElementError("number", value);
        }
    }),
    PACKED_SWITCH((context, element) -> {
        ASTObject object = context.validateObject(element, "packed switch", element, "first", "targets");

        if (object == null)
            return;

        // start, end should be numbers
        if (context.validateCorrect(object.value("first"), ElementType.NUMBER, "number", object))
            return;

        ASTNumber min = object.value("min");

        if (min.isFloatingPoint())
            context.throwUnexpectedElementError("integer literal", min);

        // targets should be array
        ASTArray array = context.validateEmptyableElement(object.value("targets"), ElementType.ARRAY, "targets", object);
        if (array == null)
            return;

        context.validateArray(array, ElementType.IDENTIFIER, "label", element);
    }),
    SPARSE_SWITCH((context, element) -> {
        // lookup switch can be: default label, pairs
        if (context.isNotType(element, ElementType.OBJECT, "sparse switch"))
            return;

        ASTObject object = (ASTObject) element;

        // cases should be identifier
        for (ASTElement elem : object.values().elements()) {
            if (context.isNotType(elem, ElementType.IDENTIFIER, "identifier"))
                return;
        }
    });

    private final Operand operand;

    DalvikOperands(Operand.Processor operand) {
        this.operand = new Operand(operand);
    }

    @Override
    public Operand getOperand() {
        return operand;
    }

    static void verifyConstant(ASTProcessor.ParserContext ctx, ASTElement element) {
        switch (element.type()) {
            case NUMBER, STRING, CHARACTER -> {
            }
            case IDENTIFIER -> {
                // must be class or method type
                char first = element.content().charAt(0);
                // TODO: maybe replace with actual descriptor verification?
                switch (first) {
                    case 'L', '(', '[' -> {
                        return;
                    }
                }
                ctx.throwUnexpectedElementError("class, method or array descriptor", element);
            }
            case ARRAY -> // only handle
                    JvmOperands.verifyHandle(ctx, element);
        }
    }
}

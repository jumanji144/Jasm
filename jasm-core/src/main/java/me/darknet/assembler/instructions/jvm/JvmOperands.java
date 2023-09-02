package me.darknet.assembler.instructions.jvm;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.Operands;
import me.darknet.assembler.parser.processor.ASTProcessor;

import java.util.List;

public enum JvmOperands implements Operands {

    CONSTANT(JvmOperands::verifyConstant),
    TABLE_SWITCH((context, element) -> {
        // table switch can be: default label, low value, high value and labels
        ASTObject object = context.validateObject(element, "table switch", element, "min", "max", "default", "cases");

        if (object == null)
            return;

        // start, end should be numbers
        if (context.isNotType(object.values().get("min"), ElementType.NUMBER, "number"))
            return;
        if (context.isNotType(object.values().get("max"), ElementType.NUMBER, "number"))
            return;

        // default should be identifier
        if (context.isNotType(object.values().get("default"), ElementType.IDENTIFIER, "identifier"))
            return;

        // cases should be array
        if (context.isNotType(object.values().get("cases"), ElementType.ARRAY, "array"))
            return;
        context.validateArray(object.values().get("cases"), ElementType.IDENTIFIER, "label", element);

    }),
    LOOKUP_SWITCH((context, element) -> {
        // lookup switch can be: default label, pairs
        if (context.isNotType(element, ElementType.OBJECT, "lookup switch"))
            return;

        ASTObject object = (ASTObject) element;
        if (object.values().size() < 1) {
            context.throwUnexpectedElementError("default label", element);
            return;
        }

        // default should be identifier
        if (context.isNotType(object.values().get("default"), ElementType.IDENTIFIER, "identifier"))
            return;
        for (ASTElement elem : object.values().elements()) {
            if (context.isNotType(elem, ElementType.IDENTIFIER, "identifier"))
                return;
        }
    }),
    HANDLE(JvmOperands::verifyHandle),
    ARGS((context, element) -> {
        ASTArray array = context.validateEmptyableElement(element, ElementType.ARRAY, "args", element);
        for (ASTElement value : array.values()) {
            if (context.isNull(value, "args element", array.location()))
                return;
            assert value != null;
            JvmOperands.verifyConstant(context, value);
        }
    }),
    TYPE(((context, element) -> {
        context.isNotType(element, ElementType.IDENTIFIER, "type");
    })),
    NEW_ARRAY_TYPE(((context, element) -> {
        if (context.isNotType(element, ElementType.IDENTIFIER, "new array type"))
            return;
        ASTIdentifier identifier = (ASTIdentifier) element;
        switch (identifier.content()) {
            case "boolean", "byte", "char", "short", "int", "float", "long", "double" -> {}
            default ->
                    context.throwUnexpectedElementError("boolean, byte, char, short, int, float, long or double", element);
        }
    }));

    private final Operand operand;

    JvmOperands(Operand.Processor operand) {
        this.operand = new Operand(operand);
    }

    public static void verifyConstant(ASTProcessor.ParserContext context, ASTElement element) {
        switch (element.type()) {
            case NUMBER:
            case STRING:
                break;
            case IDENTIFIER: {
                if (!element.content().startsWith("L") && !element.content().startsWith("(")) {
                    context.throwUnexpectedElementError("class or method type", element);
                }
                break;
            }
            case ARRAY: {
                verifyHandle(context, element);
                break;
            }
            default:
                context.throwUnexpectedElementError("constant", element);
        }
    }

    public static void verifyHandle(ASTProcessor.ParserContext context, ASTElement element) {
        if (context.isNotType(element, ElementType.ARRAY, "handle"))
            return;

        ASTArray array = (ASTArray) element;
        List<ASTIdentifier> values = context.validateArray(array, ElementType.IDENTIFIER, "handle element", element);
        if (values.size() != 3) {
            context.throwUnexpectedElementError("kind, name and descriptor", element);
            return;
        }
        // first should be kind
        if (!Handle.KINDS.containsKey(values.get(0).content())) {
            context.throwUnexpectedElementError("kind", values.get(0));
        }
    }

    @Override
    public Operand getOperand() {
        return operand;
    }
}

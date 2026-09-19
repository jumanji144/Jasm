package me.darknet.assembler.instructions.dalvik;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.Operands;
import me.darknet.assembler.instructions.jvm.JvmOperands;
import me.darknet.assembler.parser.processor.ProcessorContext;

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
        if (array == null)
            return;
        for (ASTElement value : array.values()) {
            if (context.isNull(value, "args array element", array.location()))
                continue;
            DalvikOperands.verifyConstant(context, value);
        }
    }),
    REGISTER_ARRAY((context, element) -> {
        // register array can be: register or array
        ASTArray array = context.validateEmptyableElement(element, ElementType.ARRAY, "register array", element);
        if (array == null)
            return;
        for (ASTElement value : array.values()) {
            if (context.isNull(value, "register array element", array.location()))
                continue;
            if(value.type() != ElementType.IDENTIFIER)
                context.throwUnexpectedElementError("register", value);
        }
    }),
    DATA_ARRAY(DalvikOperands::verifyDataArray),
    PACKED_SWITCH((context, element) -> {
        ASTObject object = context.validateObject(element, "packed switch", element, "first", "targets");

        if (object == null)
            return;

        // start, end should be numbers
        if (context.validateCorrect(object.value("first"), ElementType.NUMBER, "number", object))
            return;

        ASTNumber min = object.value("first");

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
        for (var pair : object.values().pairs()) {
            ASTElement elem = pair.second();
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

    static void verifyDataArray(ProcessorContext context, ASTElement element) {
        if (element == null)
            return;

        if (element.type() == ElementType.ARRAY) {
            ASTArray array = (ASTArray) element;
            for (ASTElement value : array.values()) {
                if (context.isNull(value, "data array element", array.location())) {
                    continue;
                }
                if (value.type() != ElementType.NUMBER) {
                    context.throwUnexpectedElementError("number", value);
                }
            }
            return;
        }
        if (element.type() != ElementType.OBJECT) {
            context.throwUnexpectedElementError("data array or payload object", element);
            return;
        }

        ASTObject payload = context.validateObject(element, "data payload", element, "width", "values");
        if (payload == null) {
            return;
        }
        ASTNumber width = context.validateElement(payload.value("width"), ElementType.NUMBER, "data element width", payload);
        if (width != null && (width.isFloatingPoint() || (width.asInt() != 1 && width.asInt() != 2 && width.asInt() != 4 && width.asInt() != 8))) {
            context.throwUnexpectedElementError("element width 1, 2, 4, or 8", width);
        }
        ASTArray values = context.validateEmptyableElement(payload.value("values"), ElementType.ARRAY, "data payload values", payload);
        if (values != null) {
            for (ASTElement value : values.values()) {
                if (context.isNull(value, "data payload value", values.location())) {
                    continue;
                }
                if (value.type() != ElementType.NUMBER) {
                    context.throwUnexpectedElementError("number", value);
                }
            }
        }
    }

    private static void verifyConstant(ProcessorContext ctx, ASTElement element) {
        switch (element.type()) {
            case NUMBER, STRING, CHARACTER, BOOL, ENUM -> {}
            case DECLARATION -> verifyDeclarationConstant(ctx, element);
            case IDENTIFIER -> {
                if ("null".equalsIgnoreCase(element.content()))
                    return;

                // must be class or method type
                char first = element.content().charAt(0);
                // TODO: maybe replace with actual descriptor verification?
                switch (first) {
                    case 'L', '(', '[' -> {
                        return;
                    }
                    default -> {
                        // Bare identifiers are internal-name type constants in Dalvik argument arrays.
                        return;
                    }
                }
            }
            case ARRAY -> // only handle
                    JvmOperands.verifyHandle(ctx, element);
        }
    }

    private static void verifyDeclarationConstant(ProcessorContext ctx, ASTElement element) {
        if (!(element instanceof ASTDeclaration declaration) || declaration.keyword() == null) {
            ctx.throwUnexpectedElementError("enum or member constant", element);
            return;
        }

        String keyword = declaration.keyword().content();
        int size = declaration.elements().size();
        boolean validEnum = ".enum".equals(keyword) && (size == 2 || size == 3);
        boolean validMember = ".member".equals(keyword) && size == 3;
        if (!validEnum && !validMember) {
            ctx.throwUnexpectedElementError("enum or member constant", element);
            return;
        }

        for (ASTElement value : declaration.elements()) {
            if (contextIsNotIdentifier(ctx, value, element)) {
                return;
            }
        }
    }

    private static boolean contextIsNotIdentifier(ProcessorContext ctx, ASTElement value, ASTElement parent) {
        if (value == null || value.type() != ElementType.IDENTIFIER) {
            ctx.throwUnexpectedElementError("constant identifier", value == null ? parent : value);
            return true;
        }
        return false;
    }
}

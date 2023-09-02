package me.darknet.assembler.helper;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;

public record Constant(Type type, Object value) {

    /**
     * Create a new constant from element
     *
     * @param element
     *                the element, must be verified before
     *
     * @return the constant
     */
    public static Constant from(ASTElement element) {
        return switch (element.type()) {
            case NUMBER:
                ASTNumber number = (ASTNumber) element;
                yield new Constant(Constant.Type.Number, number.number());
            case STRING: new Constant(Constant.Type.String, element.value());
            case IDENTIFIER:
                ASTIdentifier identifier = (ASTIdentifier) element;
                if (identifier.content().startsWith("L")) { // is class
                    yield new Constant(Constant.Type.ClassType, identifier.content());
                } else {
                    // must be method `(` or method type `L`
                    yield new Constant(Constant.Type.MethodType, identifier.content());
                }
            case ARRAY: {
                ASTArray array = (ASTArray) element;
                yield new Constant(Constant.Type.MethodHandle, Handle.from(array));
            }
            default:
                throw new IllegalStateException("Unexpected value: " + element.type());
        };
    }

    public enum Type {
        String,
        Number,
        ClassType,
        MethodType,
        MethodHandle
    }

}

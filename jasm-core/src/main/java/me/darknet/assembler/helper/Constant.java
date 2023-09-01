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
        return switch (element.getType()) {
            case NUMBER:
                ASTNumber number = (ASTNumber) element;
                yield new Constant(Constant.Type.Number, number.getNumber());
            case STRING: new Constant(Constant.Type.String, element.getValue());
            case IDENTIFIER:
                ASTIdentifier identifier = (ASTIdentifier) element;
                if (identifier.getContent().startsWith("L")) { // is class
                    yield new Constant(Constant.Type.ClassType, identifier.getContent());
                } else {
                    // must be method `(` or method type `L`
                    yield new Constant(Constant.Type.MethodType, identifier.getContent());
                }
            case ARRAY: {
                ASTArray array = (ASTArray) element;
                yield new Constant(Constant.Type.MethodHandle, Handle.from(array));
            }
            default:
                throw new IllegalStateException("Unexpected value: " + element.getType());
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

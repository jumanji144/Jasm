package me.darknet.assembler.compile;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.dex.tree.definitions.constant.*;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.Types;

public class ConstantMapper {

    public static Handle methodHandleFromHandle(me.darknet.assembler.helper.Handle handle) {
        var split = handle.name().split("\\.");
        String className = split[0];
        String methodName = split[1];

        InstanceType owner = Types.instanceTypeFromInternalName(className);
        MethodType methodType = Types.methodTypeFromDescriptor(handle.descriptor());

        return new Handle(handle.kind().ordinal() + 1, owner, methodName, methodType);
    }

    public static Handle methodHandleFromArray(ASTArray array) {
        me.darknet.assembler.helper.Handle.Kind kind = me.darknet.assembler.helper.Handle.Kind.from(array.values().getFirst().content());
        String name = array.<ASTIdentifier>value(1).literal();
        String descriptor = array.<ASTIdentifier>value(2).literal();

        var split = name.split("\\.");
        String className = split[0];
        String methodName = split[1];

        InstanceType owner = Types.instanceTypeFromInternalName(className);
        MethodType methodType = Types.methodTypeFromDescriptor(descriptor);

        return new Handle(kind.ordinal() + 1, owner, methodName, methodType);
    }

    public static Constant fromConstant(ASTElement constant) {
        return switch (constant) {
            case ASTCharacter character -> new CharConstant(character.content().charAt(0));
            case ASTNumber number -> {
                if (number.isFloatingPoint()) {
                    if (number.isWide()) {
                        yield new DoubleConstant(number.asDouble());
                    } else {
                        yield new FloatConstant(number.asFloat());
                    }
                } else {
                    if (number.isWide()) {
                        yield new LongConstant(number.asLong());
                    } else {
                        yield new IntConstant(number.asInt());
                    }
                }
            }
            case ASTString string -> new StringConstant(string.content());
            case ASTIdentifier identifier -> {
                char first = identifier.content().charAt(0);
                yield switch (first) {
                    case 'L' -> {
                        // if last is `;` then it's a class type, if not could be a short handle
                        char last = identifier.content().charAt(identifier.content().length() - 1);
                        if(last == ';') {
                            yield new TypeConstant(Types.instanceTypeFromDescriptor(identifier.literal()));
                        } else {
                            me.darknet.assembler.helper.Handle handle = me.darknet.assembler.helper.Handle.HANDLE_SHORTCUTS.get(identifier.literal());
                            if (handle != null) {
                                yield new HandleConstant(methodHandleFromHandle(handle));
                            }
                            throw new IllegalStateException("Unexpected value: " + first);
                        }
                    }
                    case '(' -> new TypeConstant(Types.methodTypeFromDescriptor(identifier.literal()));
                    case '[' -> new TypeConstant(Types.arrayTypeFromDescriptor(identifier.literal()));
                    default -> switch (identifier.literal().toLowerCase()) {
                        case "true" -> new BoolConstant(true);
                        case "false" -> new BoolConstant(false);
                        case "nan", "nand" -> new DoubleConstant(Double.NaN);
                        case "nanf" -> new FloatConstant(Float.NaN);
                        case "+infinity", "+infinityd", "infinity", "infinityd"
                                -> new DoubleConstant(Double.POSITIVE_INFINITY);
                        case "+infinityf", "infinityf" -> new FloatConstant(Float.POSITIVE_INFINITY);
                        case "-infinity", "-infinityd" -> new DoubleConstant(Double.NEGATIVE_INFINITY);
                        case "-infinityf" -> new FloatConstant(Float.NEGATIVE_INFINITY);
                        default -> {
                            // maybe is a short handle
                            me.darknet.assembler.helper.Handle handle = me.darknet.assembler.helper.Handle.HANDLE_SHORTCUTS.get(identifier.literal());
                            if (handle != null) {
                                yield new HandleConstant(methodHandleFromHandle(handle));
                            }
                            throw new IllegalStateException("Unexpected value: " + first);
                        }
                    };
                };
            }
            case ASTArray array -> {
                ASTElement last = array.values().getLast();
                yield switch(last) {
                    case ASTIdentifier identifier -> new HandleConstant(methodHandleFromArray(array));
                    default -> throw new IllegalStateException("Unexpected value: " + last);
                };
            }
            default -> throw new IllegalStateException("Unexpected value: " + constant);
        };
    }

}

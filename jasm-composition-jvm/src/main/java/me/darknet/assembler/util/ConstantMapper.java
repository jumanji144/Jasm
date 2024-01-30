package me.darknet.assembler.util;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.helper.Handle;

import dev.xdark.blw.constant.*;
import dev.xdark.blw.type.*;

import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.List;

public class ConstantMapper {

    public static MethodHandle methodHandleFromArray(ASTArray array) {
        Handle.Kind kind = Handle.Kind.from(array.values().get(0).content());
        String name = array.<ASTIdentifier>value(1).literal();
        String descriptor = array.<ASTIdentifier>value(2).literal();

        var split = name.split("\\.");
        String className = split[0];
        String methodName = split[1];

        ObjectType owner = Types.instanceTypeFromInternalName(className);

        // Despite the naming conventions, you can have a method handle to a field.
        // Field types can be arrays, classes, or primitives.
        // To keep things simple in this case we'll use type-reader directly rather than chaining
        //  conditional type util calls.
        Type methodType = kind.isField() ? new TypeReader(descriptor).read() : Types.methodType(descriptor);

        // TODO: ITF
        return new MethodHandle(kind.ordinal() + 1, owner, methodName, methodType, false);
    }

    public static MethodHandle methodHandleFromHandle(Handle handle) {
        var split = handle.name().split("\\.");
        String className = split[0];
        String methodName = split[1];

        ObjectType owner = Types.instanceTypeFromInternalName(className);

        Type methodType = handle.kind().isField() ?
                new TypeReader(handle.descriptor()).read() : Types.methodType(handle.descriptor());

        return new MethodHandle(handle.kind().ordinal() + 1, owner, methodName, methodType, false);
    }

    public static ConstantDynamic constantDynamicFromArray(ASTArray array) {
        String name = array.<ASTIdentifier>value(0).literal();
        String descriptor = array.<ASTIdentifier>value(1).literal();

        MethodHandle bootstrapMethod = methodHandleFromArray(array.value(2));

        ASTElement argsArray = array.value(3);
        ASTArray args = argsArray instanceof ASTArray ? (ASTArray) argsArray : ASTEmpty.EMPTY_ARRAY;

        List<Constant> constantArgs = args.values().stream().map(ConstantMapper::fromConstant).toList();

        ClassType type = new TypeReader(descriptor).requireClassType();

        return new ConstantDynamic(name, type, bootstrapMethod, constantArgs);
    }

    public static Constant fromConstant(ASTElement element) {
        return switch (element.type()) {
            case CHARACTER -> {
                ASTCharacter character = (ASTCharacter) element;
                assert character.content() != null;
                yield new OfInt(character.content().charAt(0));
            }
            case NUMBER -> {
                ASTNumber number = (ASTNumber) element;
                if (number.isFloatingPoint()) {
                    if (number.isWide()) {
                        yield new OfDouble(number.asDouble());
                    } else {
                        yield new OfFloat(number.asFloat());
                    }
                } else {
                    if (number.isWide()) {
                        yield new OfLong(number.asLong());
                    } else {
                        yield new OfInt(number.asInt());
                    }
                }
            }
            case STRING -> new OfString(element.value().content());
            case IDENTIFIER -> {
                ASTIdentifier identifier = (ASTIdentifier) element;
                assert identifier.content() != null;
                char first = identifier.content().charAt(0);
                yield switch (first) {
                    case 'L' -> new OfType(Types.instanceTypeFromDescriptor(identifier.literal()));
                    case '(' -> new OfType(Types.methodType(identifier.literal()));
                    case '[' -> new OfType(Types.arrayTypeFromDescriptor(identifier.literal()));
                    default -> switch (identifier.literal().toLowerCase()) {
                        case "true" -> new OfInt(1);
                        case "false" -> new OfInt(0);
                        case "nan", "nand" -> new OfDouble(Double.NaN);
                        case "nanf" -> new OfFloat(Float.NaN);
                        case "+infinity", "+infinityd", "infinity", "infinityd"
                                -> new OfDouble(Double.POSITIVE_INFINITY);
                        case "+infinityf", "infinityf" -> new OfFloat(Float.POSITIVE_INFINITY);
                        case "-infinity", "-infinityd" -> new OfDouble(Double.NEGATIVE_INFINITY);
                        case "-infinityf" -> new OfFloat(Float.NEGATIVE_INFINITY);
                        default -> {
                            // maybe is a short handle
                            Handle handle = Handle.HANDLE_SHORTCUTS.get(identifier.literal());
                            if (handle != null) {
                                yield new OfMethodHandle(methodHandleFromHandle(handle));
                            }
                            throw new IllegalStateException("Unexpected value: " + first);
                        }
                    };
                };
            }
            case ARRAY -> {
                ASTArray array = (ASTArray) element;
                ASTElement last = array.values().get(array.values().size() - 1);
                assert last != null;
                yield switch (last.type()) {
                    case ARRAY, EMPTY -> new OfDynamic(constantDynamicFromArray(array));
                    case IDENTIFIER -> new OfMethodHandle(methodHandleFromArray(array));
                    default -> throw new IllegalStateException("Unexpected value: " + last.type());
                };
            }
            default -> throw new IllegalStateException("Unexpected value: " + element.type());
        };
    }

}

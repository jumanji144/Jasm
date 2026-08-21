package me.darknet.assembler.util;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTCharacter;
import me.darknet.assembler.ast.primitive.ASTEmpty;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.helper.Handle;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Type;

import java.util.List;

public class ConstantMapper {

    public static org.objectweb.asm.Handle methodHandleFromArray(ASTArray array) {
        Handle.Kind kind = Handle.Kind.from(array.values().getFirst().content());
        String name = array.<ASTIdentifier>value(1).literal();
        String descriptor = array.<ASTIdentifier>value(2).literal();

        int split = name.lastIndexOf('.');
        String owner = name.substring(0, split);
        String methodName = name.substring(split + 1);
        return new org.objectweb.asm.Handle(kind.ordinal() + 1, owner, methodName, descriptor,
                kind == Handle.Kind.INVOKE_INTERFACE);
    }

    public static org.objectweb.asm.Handle methodHandleFromHandle(Handle handle) {
        int split = handle.name().lastIndexOf('.');
        String owner = handle.name().substring(0, split);
        String methodName = handle.name().substring(split + 1);
        return new org.objectweb.asm.Handle(handle.kind().ordinal() + 1, owner, methodName, handle.descriptor(),
                handle.kind() == Handle.Kind.INVOKE_INTERFACE);
    }

    public static ConstantDynamic constantDynamicFromArray(ASTArray array) {
        ASTElement nameElement = array.value(0);
        ASTElement typeElement = array.value(1);

        String name = nameElement instanceof ASTIdentifier ident ? ident.literal() :
                nameElement instanceof ASTNumber number ? number.content() : null;
        String descriptor = typeElement instanceof ASTIdentifier ident ? ident.literal() : null;
        if (name == null || descriptor == null) {
            throw new IllegalStateException("Invalid condy, name or type not an identifier");
        }

        org.objectweb.asm.Handle bootstrapMethod = methodHandleFromArray(array.value(2));
        ASTElement argsArray = array.value(3);
        ASTArray args = argsArray instanceof ASTArray ? (ASTArray) argsArray : ASTEmpty.EMPTY_ARRAY;
        Object[] constantArgs = args.values().stream().map(ConstantMapper::fromConstant).toArray();
        return new ConstantDynamic(name, descriptor, bootstrapMethod, constantArgs);
    }

    public static Object fromConstant(ASTElement element) {
        return switch (element.type()) {
            case CHARACTER -> {
                ASTCharacter character = (ASTCharacter) element;
                String content = character.content();
                if (content == null || content.isEmpty()) {
                    throw new IllegalStateException("Character constant is missing content");
                }
                yield (int) content.charAt(0);
            }
            case NUMBER -> {
                ASTNumber number = (ASTNumber) element;
                if (number.isFloatingPoint()) {
                    if (number.isWide()) {
                        yield number.asDouble();
                    }
                    yield number.asFloat();
                }
                if (number.isWide()) {
                    yield number.asLong();
                }
                yield number.asInt();
            }
            case STRING -> element.value().content();
            case IDENTIFIER -> mapIdentifier((ASTIdentifier) element);
            case ARRAY -> {
                ASTArray array = (ASTArray) element;
                ASTElement last = array.values().getLast();
                if (last == null) {
                    throw new IllegalStateException("Array constant is missing its trailing discriminator element");
                }
                yield switch (last.type()) {
                    case ARRAY, EMPTY -> constantDynamicFromArray(array);
                    case IDENTIFIER -> methodHandleFromArray(array);
                    default -> throw new IllegalStateException("Unexpected value: " + last.type());
                };
            }
            default -> throw new IllegalStateException("Unexpected value: " + element.type());
        };
    }

    private static Object mapIdentifier(ASTIdentifier identifier) {
        String content = identifier.content();
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("Identifier constant is missing content");
        }

        char first = content.charAt(0);
        return switch (first) {
            case 'L' -> {
                if (content.charAt(content.length() - 1) == ';') {
                    yield Type.getType(identifier.literal());
                }
                Handle handle = Handle.HANDLE_SHORTCUTS.get(identifier.literal());
                if (handle != null) {
                    yield methodHandleFromHandle(handle);
                }
                throw new IllegalStateException("Unexpected value: " + first);
            }
            case '(' -> Type.getMethodType(identifier.literal());
            case '[' -> Type.getType(identifier.literal());
            default -> switch (identifier.literal().toLowerCase()) {
                case "true" -> 1;
                case "false" -> 0;
                case "nan", "nand" -> Double.NaN;
                case "nanf" -> Float.NaN;
                case "+infinity", "+infinityd", "infinity", "infinityd" -> Double.POSITIVE_INFINITY;
                case "+infinityf", "infinityf" -> Float.POSITIVE_INFINITY;
                case "-infinity", "-infinityd" -> Double.NEGATIVE_INFINITY;
                case "-infinityf" -> Float.NEGATIVE_INFINITY;
                default -> {
                    Handle handle = Handle.HANDLE_SHORTCUTS.get(identifier.literal());
                    if (handle != null) {
                        yield methodHandleFromHandle(handle);
                    }
                    throw new IllegalStateException("Unexpected value: " + first);
                }
            };
        };
    }
}

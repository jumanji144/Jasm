package me.darknet.assembler.compile;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.dex.tree.definitions.MemberIdentifier;
import me.darknet.dex.tree.definitions.constant.*;
import me.darknet.dex.tree.type.ClassType;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.TypeParser;
import me.darknet.dex.tree.type.Types;

public class DalvikConstantMapper {

    public static Handle methodHandleFromHandle(me.darknet.assembler.helper.Handle handle) {
        var split = handle.name().split("\\.");
        String className = split[0];
        String methodName = split[1];

        InstanceType owner = Types.instanceTypeFromInternalName(className);
        MethodType methodType = Types.methodTypeFromDescriptor(handle.descriptor());

        return new Handle(dexHandleKind(handle.kind()), owner, methodName, methodType);
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

        return new Handle(dexHandleKind(kind), owner, methodName, methodType);
    }

    private static int dexHandleKind(me.darknet.assembler.helper.Handle.Kind kind) {
        return switch (kind) {
            case GET_STATIC -> Handle.KIND_STATIC_GET;
            case PUT_STATIC -> Handle.KIND_STATIC_PUT;
            case GET_FIELD -> Handle.KIND_INSTANCE_GET;
            case PUT_FIELD -> Handle.KIND_INSTANCE_PUT;
            case INVOKE_VIRTUAL -> Handle.KIND_INVOKE_INSTANCE;
            case INVOKE_STATIC -> Handle.KIND_INVOKE_STATIC;
            case INVOKE_SPECIAL -> Handle.KIND_INVOKE_DIRECT;
            case NEW_INVOKE_SPECIAL -> Handle.KIND_INVOKE_CONSTRUCTOR;
            case INVOKE_INTERFACE -> Handle.KIND_INVOKE_INTERFACE;
        };
    }

    public static Constant fromConstant(ASTElement constant) {
        return switch (constant) {
            case ASTBool bool -> new BoolConstant(bool.bool());
            case ASTCharacter character -> new CharConstant(character.content().charAt(0));
            case ASTEnum enumValue -> {
                InstanceType owner = Types.instanceTypeFromInternalName(enumValue.enumOwner().literal());
                yield new EnumConstant(owner, new MemberIdentifier(
                        enumValue.enumFieldName().literal(), owner
                ));
            }
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
                        // if last is ';' then it's a class type, if not could be a short handle
                        char last = identifier.content().charAt(identifier.content().length() - 1);
                        if(last == ';') {
                            yield new TypeConstant(Types.instanceTypeFromDescriptor(identifier.literal()));
                        } else {
                            me.darknet.assembler.helper.Handle handle = me.darknet.assembler.helper.Handle.HANDLE_SHORTCUTS.get(identifier.literal());
                            if (handle != null) {
                                yield new HandleConstant(methodHandleFromHandle(handle));
                            }
                            if (identifier.literal().contains("/")) {
                                yield new TypeConstant(Types.instanceTypeFromInternalName(identifier.literal()));
                            }
                            throw new IllegalStateException("Unexpected value: " + first);
                        }
                    }
                    case '(' -> new TypeConstant(Types.methodTypeFromDescriptor(identifier.literal()));
                    case '[' -> new TypeConstant(Types.arrayTypeFromDescriptor(identifier.literal()));
                    default -> switch (identifier.literal().toLowerCase()) {
                        case "null" -> NullConstant.INSTANCE;
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
                            yield new TypeConstant(Types.instanceTypeFromInternalName(identifier.literal()));
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
            case ASTDeclaration declaration -> memberConstantFromDeclaration(declaration);
            default -> throw new IllegalStateException("Unexpected value: " + constant);
        };
    }

    private static Constant memberConstantFromDeclaration(ASTDeclaration declaration) {
        ASTIdentifier keyword = declaration.keyword();
        if (keyword == null) {
            throw new IllegalStateException("Expected .enum or .member constant");
        }
        return switch (keyword.content()) {
            case ".enum" -> enumConstantFromDeclaration(declaration);
            case ".member" -> memberConstantFromMemberDeclaration(declaration);
            default -> throw new IllegalStateException("Expected .enum or .member constant");
        };
    }

    private static Constant enumConstantFromDeclaration(ASTDeclaration declaration) {
        if (declaration.elements().size() < 2 || declaration.elements().size() > 3
                || !(declaration.element(0) instanceof ASTIdentifier ownerName)
                || !(declaration.element(1) instanceof ASTIdentifier fieldName)) {
            throw new IllegalStateException("Expected .enum owner name descriptor constant");
        }
        InstanceType owner = Types.instanceTypeFromInternalName(ownerName.literal());
        return new EnumConstant(owner, new MemberIdentifier(fieldName.literal(), owner));
    }

    private static Constant memberConstantFromMemberDeclaration(ASTDeclaration declaration) {
        if (declaration.elements().size() != 3
                || !(declaration.element(0) instanceof ASTIdentifier ownerName)
                || !(declaration.element(1) instanceof ASTIdentifier memberName)
                || !(declaration.element(2) instanceof ASTIdentifier descriptor)) {
            throw new IllegalStateException("Expected .member owner name descriptor constant");
        }

        InstanceType owner = Types.instanceTypeFromInternalName(ownerName.literal());
        if (descriptor.literal().startsWith("(")) {
            return new MemberConstant(owner, new MemberIdentifier(
                    memberName.literal(), Types.methodTypeFromDescriptor(descriptor.literal())
            ));
        }
        ClassType memberType = new TypeParser(descriptor.literal()).requireClassType();
        return new MemberConstant(owner, new MemberIdentifier(memberName.literal(), memberType));
    }

}

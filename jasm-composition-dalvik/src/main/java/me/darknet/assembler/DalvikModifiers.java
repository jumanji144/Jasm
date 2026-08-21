package me.darknet.assembler;

import me.darknet.assembler.visitor.Modifiers;
import me.darknet.dex.tree.definitions.AccessFlags;

import java.util.Map;

public class DalvikModifiers implements AccessFlags {

    public static final int CLASS = 1;
    public static final int METHOD = 2;
    public static final int FIELD = 3;

    private static final Map<Integer, String> UNIQUE_MODIFIER_NAMES = Map.ofEntries(
            Map.entry(ACC_PUBLIC, "public"),
            Map.entry(ACC_PRIVATE, "private"),
            Map.entry(ACC_PROTECTED, "protected"),
            Map.entry(ACC_STATIC, "static"),
            Map.entry(ACC_FINAL, "final"),
            Map.entry(ACC_NATIVE, "native"),
            Map.entry(ACC_INTERFACE, "interface"),
            Map.entry(ACC_ABSTRACT, "abstract"),
            Map.entry(ACC_STRICT, "strict"),
            Map.entry(ACC_SYNTHETIC, "synthetic"),
            Map.entry(ACC_ANNOTATION, "annotation"),
            Map.entry(ACC_ENUM, "enum"),
            Map.entry(ACC_CONSTRUCTOR, "constructor")
    );

    private final static Map<Integer, String> CLASS_MODIFIER_NAMES = Map.of();
    private final static Map<Integer, String> METHOD_MODIFIER_NAMES = Map.of(
            ACC_BRIDGE, "bridge",
            ACC_VARARGS, "varargs",
            ACC_CONSTRUCTOR, "constructor",
            ACC_SYNCHRONIZED, "synchronized",
            ACC_DECLARED_SYNCHRONIZED, "declared-synchronized",
            ACC_STRICT, "strict"
    );
    private final static Map<Integer, String> FIELD_MODIFIER_NAMES = Map.of(
            ACC_VOLATILE, "volatile",
            ACC_TRANSIENT, "transient"
    );

    public static String getModifierName(int modifier) {
        return UNIQUE_MODIFIER_NAMES.get(modifier);
    }

    public static String getClassModifierName(int modifier) {
        if (UNIQUE_MODIFIER_NAMES.containsKey(modifier)) {
            return UNIQUE_MODIFIER_NAMES.get(modifier);
        }
        return CLASS_MODIFIER_NAMES.get(modifier);
    }

    public static String getMethodModifierName(int modifier) {
        if (UNIQUE_MODIFIER_NAMES.containsKey(modifier)) {
            return UNIQUE_MODIFIER_NAMES.get(modifier);
        }
        return METHOD_MODIFIER_NAMES.get(modifier);
    }

    public static String getFieldModifierName(int modifier) {
        if (UNIQUE_MODIFIER_NAMES.containsKey(modifier)) {
            return UNIQUE_MODIFIER_NAMES.get(modifier);
        }
        return FIELD_MODIFIER_NAMES.get(modifier);
    }

    public static int getModifier(String name) {
        return switch (name) {
            case "public" -> ACC_PUBLIC;
            case "private" -> ACC_PRIVATE;
            case "protected" -> ACC_PROTECTED;
            case "static" -> ACC_STATIC;
            case "final" -> ACC_FINAL;
            case "native" -> ACC_NATIVE;
            case "abstract" -> ACC_ABSTRACT;
            case "interface" -> ACC_INTERFACE;
            case "synthetic" -> ACC_SYNTHETIC;
            case "strict" -> ACC_STRICT;
            case "annotation" -> ACC_ANNOTATION;
            case "enum" -> ACC_ENUM;
            case "synchronized" -> ACC_SYNCHRONIZED;
            case "bridge" -> ACC_BRIDGE;
            case "varargs" -> ACC_VARARGS;
            case "volatile" -> ACC_VOLATILE;
            case "transient" -> ACC_TRANSIENT;
            default -> 0;
        };
    }

    public static int getClassModifier(String name) {
        return switch (name) {
            case "public" -> ACC_PUBLIC;
            case "private" -> ACC_PRIVATE;
            case "protected" -> ACC_PROTECTED;
            case "static" -> ACC_STATIC;
            case "final" -> ACC_FINAL;
            case "native" -> ACC_NATIVE;
            case "abstract" -> ACC_ABSTRACT;
            case "interface" -> ACC_INTERFACE;
            case "synthetic" -> ACC_SYNTHETIC;
            case "strict" -> ACC_STRICT;
            case "annotation" -> ACC_ANNOTATION;
            case "enum" -> ACC_ENUM;
            default -> 0;
        };
    }

    public static int getMethodModifier(String name) {
        return switch (name) {
            case "public" -> ACC_PUBLIC;
            case "private" -> ACC_PRIVATE;
            case "protected" -> ACC_PROTECTED;
            case "static" -> ACC_STATIC;
            case "final" -> ACC_FINAL;
            case "native" -> ACC_NATIVE;
            case "abstract" -> ACC_ABSTRACT;
            case "interface" -> ACC_INTERFACE;
            case "synthetic" -> ACC_SYNTHETIC;
            case "strict" -> ACC_STRICT;
            case "annotation" -> ACC_ANNOTATION;
            case "enum" -> ACC_ENUM;
            case "synchronized" -> ACC_SYNCHRONIZED;
            case "bridge" -> ACC_BRIDGE;
            case "varargs" -> ACC_VARARGS;
            default -> 0;
        };
    }

    public static int getFieldModifier(String name) {
        return switch (name) {
            case "public" -> ACC_PUBLIC;
            case "private" -> ACC_PRIVATE;
            case "protected" -> ACC_PROTECTED;
            case "static" -> ACC_STATIC;
            case "final" -> ACC_FINAL;
            case "native" -> ACC_NATIVE;
            case "abstract" -> ACC_ABSTRACT;
            case "interface" -> ACC_INTERFACE;
            case "synthetic" -> ACC_SYNTHETIC;
            case "strict" -> ACC_STRICT;
            case "annotation" -> ACC_ANNOTATION;
            case "enum" -> ACC_ENUM;
            case "volatile" -> ACC_VOLATILE;
            case "transient" -> ACC_TRANSIENT;
            default -> 0;
        };
    }

    public static int modifier(String modifier, int type) {
        return switch (type) {
            case 0 -> getModifier(modifier);
            case 1 -> getClassModifier(modifier);
            case 2 -> getMethodModifier(modifier);
            case 3 -> getFieldModifier(modifier);
            default -> 0;
        };
    }

    public static String modifiers(int modifiers, int type) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            int flag = 1 << i;
            if ((modifiers & flag) != 0) {
                String modifierName = switch (type) {
                    case 0 -> getModifierName(flag);
                    case 1 -> getClassModifierName(flag);
                    case 2 -> getMethodModifierName(flag);
                    case 3 -> getFieldModifierName(flag);
                    default -> null;
                };
                if (modifierName != null) {
                    builder.append(modifierName).append(" ");
                }
            }
        }
        return builder.toString();
    }

    public static int getClassModifiers(Modifiers modifiers) {
        return getModifiers(modifiers, CLASS);
    }

    public static int getFieldModifiers(Modifiers modifiers) {
        return getModifiers(modifiers, FIELD);
    }

    public static int getMethodModifiers(Modifiers modifiers) {
        return getModifiers(modifiers, METHOD);
    }

    public static int getModifiers(Modifiers modifiers, int type) {
        return modifiers.modifiers().stream().map(it -> modifier(it.content(), type)).reduce(0, (a, b) -> a | b);
    }

}

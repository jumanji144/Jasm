package me.darknet.assembler.util;

import java.util.Map;

import static dev.xdark.blw.classfile.AccessFlag.*;

public class BlwModifiers {

    public static final int CLASS = 1;
    public static final int METHOD = 2;
    public static final int FIELD = 3;

    private static final Map<Integer, String> UNIQUE_MODIFIER_NAMES = Map.ofEntries(
            Map.entry(ACC_PUBLIC, "public"), Map.entry(ACC_PRIVATE, "private"), Map.entry(ACC_PROTECTED, "protected"),
            Map.entry(ACC_STATIC, "static"), Map.entry(ACC_FINAL, "final"), Map.entry(ACC_NATIVE, "native"),
            Map.entry(ACC_ABSTRACT, "abstract"), Map.entry(ACC_INTERFACE, "interface"),
            Map.entry(ACC_SYNTHETIC, "synthetic"), Map.entry(ACC_STRICT, "strict"),
            Map.entry(ACC_ANNOTATION, "annotation"), Map.entry(ACC_ENUM, "enum")
    );

    private static final Map<Integer, String> CLASS_MODIFIER_NAMES = Map
            .ofEntries(Map.entry(ACC_SUPER, "super"), Map.entry(ACC_MODULE, "module"));

    private static final Map<Integer, String> METHOD_MODIFIER_NAMES = Map.ofEntries(
            Map.entry(ACC_SYNCHRONIZED, "synchronized"), Map.entry(ACC_BRIDGE, "bridge"),
            Map.entry(ACC_VARARGS, "varargs")
    );

    private static final Map<Integer, String> FIELD_MODIFIER_NAMES = Map
            .ofEntries(Map.entry(ACC_VOLATILE, "volatile"), Map.entry(ACC_TRANSIENT, "transient"));

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
            case "super" -> ACC_SUPER;
            case "module" -> ACC_MODULE;
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
            case "super" -> ACC_SUPER;
            case "module" -> ACC_MODULE;
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

    public static int modifiers(String modifiers, int type) {
        String[] split = modifiers.split(" ");
        int result = 0;
        for (String modifier : split) {
            result |= switch (type) {
                case 0 -> getModifier(modifier);
                case 1 -> getClassModifier(modifier);
                case 2 -> getMethodModifier(modifier);
                case 3 -> getFieldModifier(modifier);
                default -> 0;
            };
        }

        return result;
    }

}

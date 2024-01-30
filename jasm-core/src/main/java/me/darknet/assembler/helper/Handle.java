package me.darknet.assembler.helper;

import me.darknet.assembler.ast.primitive.ASTArray;

import org.jetbrains.annotations.Contract;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.TypeDescriptor;
import java.util.HashMap;
import java.util.Map;

public record Handle(Kind kind, String name, String descriptor) {

    public static Map<String, Kind> KINDS = Map.of(
            "getfield", Kind.GET_FIELD, "getstatic", Kind.GET_STATIC, "putfield", Kind.PUT_FIELD, "putstatic",
            Kind.PUT_STATIC, "invokevirtual", Kind.INVOKE_VIRTUAL, "invokestatic", Kind.INVOKE_STATIC, "invokespecial",
            Kind.INVOKE_SPECIAL, "newinvokespecial", Kind.NEW_INVOKE_SPECIAL, "invokeinterface", Kind.INVOKE_INTERFACE
    );

    /**
     * Table of shortcut handles
     * <ul>
     *     <li>LambdaMetaFactory.metafactory -> {@link java.lang.invoke.LambdaMetafactory#metafactory(java.lang.invoke.MethodHandles.Lookup, String, java.lang.invoke.MethodType, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, java.lang.invoke.MethodType)}</li>
     *     <li>LambdaMetaFactory.altMetafactory -> {@link java.lang.invoke.LambdaMetafactory#altMetafactory(java.lang.invoke.MethodHandles.Lookup, String, java.lang.invoke.MethodType, Object...)}</li>
     *     <li>ConstantBootstraps.nullConstant -> {@link java.lang.invoke.ConstantBootstraps#nullConstant(java.lang.invoke.MethodHandles.Lookup, String, Class)}</li>
     *     <li>ConstantBootstraps.primitiveClass -> {@link java.lang.invoke.ConstantBootstraps#primitiveClass(java.lang.invoke.MethodHandles.Lookup, String, Class)}</li>
     *     <li>ConstantBootstraps.enumConstant -> {@link java.lang.invoke.ConstantBootstraps#enumConstant(MethodHandles.Lookup, String, Class)}</li>
     *     <li>ConstantBootstraps.getStaticFinal -> {@link java.lang.invoke.ConstantBootstraps#getStaticFinal(java.lang.invoke.MethodHandles.Lookup, String, Class, Class)}</li>
     *     <li>ConstantBootstraps.invoke -> {@link java.lang.invoke.ConstantBootstraps#invoke(MethodHandles.Lookup, String, Class, MethodHandle, Object...)}</li>
     *     <li>ConstantBootstraps.fieldVarHandle -> {@link java.lang.invoke.ConstantBootstraps#fieldVarHandle(MethodHandles.Lookup, String, Class, Class, Class)}</li>
     *     <li>ConstantBootstraps.staticFieldVarHandle -> {@link java.lang.invoke.ConstantBootstraps#staticFieldVarHandle(MethodHandles.Lookup, String, Class, Class, Class)}</li>
     *     <li>ConstantBootstraps.arrayVarHandle -> {@link java.lang.invoke.ConstantBootstraps#arrayVarHandle(MethodHandles.Lookup, String, Class, Class)}</li>
     *     <li>ConstantBootstraps.explicitCast -> {@link java.lang.invoke.ConstantBootstraps#explicitCast(MethodHandles.Lookup, String, Class, Object)}</li>
     *     <li>SwitchBootstraps.enumSwitch -> {@link java.lang.runtime.SwitchBootstraps#enumSwitch(MethodHandles.Lookup, String, MethodType, Object...)}</li>
     *     <li>SwitchBootstraps.stringSwitch -> {@link java.lang.runtime.SwitchBootstraps#typeSwitch(MethodHandles.Lookup, String, MethodType, Object...)}</li>
     *     <li>ObjectMethods.bootstrap -> {@link java.lang.runtime.ObjectMethods#bootstrap(MethodHandles.Lookup, String, TypeDescriptor, Class, String, MethodHandle...)}
     * </ul>
     */
    public static Map<String, Handle> HANDLE_SHORTCUTS = Map.ofEntries(
            Map.entry("LambdaMetafactory.metafactory", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/LambdaMetafactory.metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;")),
            Map.entry("LambdaMetafactory.altMetafactory", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/LambdaMetafactory.altMetafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;")),
            Map.entry("ConstantBootstraps.nullConstant", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/ConstantBootstraps.nullConstant", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;")),
            Map.entry("ConstantBootstraps.primitiveClass", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/ConstantBootstraps.primitiveClass", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Class;")),
            Map.entry("ConstantBootstraps.enumConstant", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/ConstantBootstraps.enumConstant", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Enum;")),
            Map.entry("ConstantBootstraps.getStaticFinal", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/ConstantBootstraps.getStaticFinal", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/Object;")),
            Map.entry("ConstantBootstraps.invoke", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/ConstantBootstraps.invoke", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;")),
            Map.entry("ConstantBootstraps.fieldVarHandle", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/ConstantBootstraps.fieldVarHandle", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;")),
            Map.entry("ConstantBootstraps.staticFieldVarHandle", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/ConstantBootstraps.staticFieldVarHandle", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;")),
            Map.entry("ConstantBootstraps.arrayVarHandle", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/ConstantBootstraps.arrayVarHandle", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;")),
            Map.entry("ConstantBootstraps.explicitCast", new Handle(Kind.INVOKE_STATIC, "java/lang/invoke/ConstantBootstraps.explicitCast", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;")),
            Map.entry("SwitchBootstraps.enumSwitch", new Handle(Kind.INVOKE_STATIC, "java/lang/runtime/SwitchBootstraps.enumSwitch", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/Object;")),
            Map.entry("SwitchBootstraps.stringSwitch", new Handle(Kind.INVOKE_STATIC, "java/lang/runtime/SwitchBootstraps.stringSwitch", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/Object;")),
            Map.entry("ObjectMethods.bootstrap", new Handle(Kind.INVOKE_STATIC, "java/lang/runtime/ObjectMethods.bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;"))
    );

    /**
     * Reverse lookup for {@link #HANDLE_SHORTCUTS}
     */
    public static Map<String, String> SHORTCUT_LOOKUP = new HashMap<>();

    @Contract(pure = true)
    public static Handle from(ASTArray array) {
        // assert that the array has 3 elements
        Handle.Kind kind = KINDS.get(array.values().get(0).content());
        String name = array.values().get(1).content();
        String descriptor = array.values().get(2).content();

        return new Handle(kind, name, descriptor);
    }

    public enum Kind {
        GET_FIELD,
        GET_STATIC,
        PUT_FIELD,
        PUT_STATIC,
        INVOKE_VIRTUAL,
        INVOKE_STATIC,
        INVOKE_SPECIAL,
        NEW_INVOKE_SPECIAL,
        INVOKE_INTERFACE;

        public boolean isField() {
            return ordinal() <= PUT_STATIC.ordinal();
        }

        public static Kind from(String name) {
            return KINDS.get(name);
        }
    }

    static {
        HANDLE_SHORTCUTS.forEach((key, value) -> SHORTCUT_LOOKUP.put(value.name() + value.descriptor(), key));
    }

}

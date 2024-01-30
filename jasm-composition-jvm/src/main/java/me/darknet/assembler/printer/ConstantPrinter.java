package me.darknet.assembler.printer;

import dev.xdark.blw.constant.*;
import dev.xdark.blw.type.ConstantDynamic;
import dev.xdark.blw.type.MethodHandle;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.Type;
import me.darknet.assembler.helper.Handle;

import java.util.Map;

record ConstantPrinter(PrintContext<?> ctx) implements ConstantSink {

    private static final Map<Integer, String> HANDLE_TYPES = Map.of(
            1, "getfield", 2, "getstatic", 3, "putfield", 4, "putstatic", 5, "invokevirtual", 6, "invokestatic", 7,
            "invokespecial", 8, "newinvokespecial", 9, "invokeinterface"
    );

    public static void printMethodHandle(MethodHandle handle, PrintContext<?> ctx) {
        String shortHandle = Handle.SHORTCUT_LOOKUP.get(handle.owner().internalName() + "." + handle.name() + handle.type().descriptor());
        if (shortHandle != null) {
            ctx.literal(shortHandle);
            return;
        }
        var array = ctx.array();
        array.print(HANDLE_TYPES.get(handle.kind())).arg().literal(handle.owner().internalName()).literal(".")
                .literal(handle.name()).arg().literal(handle.type().descriptor()).end();
    }

    @Override
    public void acceptString(OfString value) {
        ctx.string(value.value());
    }

    @Override
    public void acceptMethodHandle(OfMethodHandle value) {
        printMethodHandle(value.value(), ctx);
    }

    @Override
    public void acceptType(OfType value) {
        Type type = value.value();
        if (type instanceof ObjectType ct) {
            ctx.print("L").literal(ct.internalName()).print(";");
        } else {
            ctx.literal(type.descriptor());
        }
    }

    @Override
    public void acceptDynamic(OfDynamic value) {
        ConstantDynamic dynamic = value.value();
        var array = ctx.array();
        array.literal(dynamic.name()).arg().literal(dynamic.type().descriptor()).arg();
        printMethodHandle(dynamic.methodHandle(), ctx);
        var bsmArray = array.arg().array();
        ConstantPrinter printer = new ConstantPrinter(bsmArray);
        bsmArray.print(dynamic.args(), (__, arg) -> arg.accept(printer));
        bsmArray.end();
        array.end();
    }

    @Override
    public void acceptLong(OfLong value) {
        ctx.print(String.valueOf(value.value())).print("L");
    }

    @Override
    public void acceptDouble(OfDouble value) {
        String content = Double.toString(value.value());
        ctx.print(content);

        // Skip 'D' suffix for things like 'NaN' where it is implied
        if (!content.matches("[\\D]+"))
            ctx.print("D");
    }

    @Override
    public void acceptInt(OfInt value) {
        ctx.print(String.valueOf(value.value()));
    }

    @Override
    public void acceptFloat(OfFloat value) {
        ctx.print(String.valueOf(value.value())).print("F");
    }

}

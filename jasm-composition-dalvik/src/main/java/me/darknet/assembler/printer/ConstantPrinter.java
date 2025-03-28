package me.darknet.assembler.printer;

import me.darknet.assembler.util.EscapeUtil;
import me.darknet.dex.tree.definitions.MemberIdentifier;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.constant.*;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.Type;

import java.util.List;
import java.util.Map;

import static me.darknet.assembler.printer.DalvikAnnotationPrinter.VISIBILITY_INTERNAL;

public class ConstantPrinter {

    private static final Map<Integer, String> HANDLE_TYPES = Map.of(
            Handle.KIND_STATIC_GET, "getstatic", Handle.KIND_STATIC_PUT, "putstatic",
            Handle.KIND_INSTANCE_GET, "getfield", Handle.KIND_INSTANCE_PUT, "putfield",
            Handle.KIND_INVOKE_INSTANCE, "invokevirtual", Handle.KIND_INVOKE_STATIC, "invokestatic",
            Handle.KIND_INVOKE_DIRECT, "invokedirect", Handle.KIND_INVOKE_INTERFACE, "invokeinterface",
            Handle.KIND_INVOKE_CONSTRUCTOR, "invokeconstructor"
    );

    public static void printHandle(Handle handle, PrintContext<?> ctx) {
        String owner = handle.owner().internalName();
        String name = handle.name();
        String descriptor = handle.type().descriptor();
        String shortHandle = me.darknet.assembler.helper.Handle.SHORTCUT_LOOKUP.get(owner + "." + name + descriptor);
        if (shortHandle != null) {
            // We are intentionally using append because our 'short handle' is safe and does not need to be escaped
            ctx.append(shortHandle);
            return;
        }
        var array = ctx.array();
        String kind = HANDLE_TYPES.get(handle.kind());
        array.print(kind).arg().literal(owner).append(".").literal(name).arg().literal(descriptor).end();
    }

    private static void printAnnotation(PrintContext<?> ctx, AnnotationPart part) {
        var printer = new DalvikAnnotationPrinter(new Annotation(VISIBILITY_INTERNAL, part));
        printer.print(ctx);
    }

    public static void printConstant(PrintContext<?> ctx, Constant constant) {
        switch (constant) {
            case AnnotationConstant(AnnotationPart part) -> printAnnotation(ctx, part);
            case ArrayConstant(List<Constant> constants) -> {
                var array = ctx.array();
                array.print(constants, ConstantPrinter::printConstant);
                array.end();
            }
            case BoolConstant(boolean value) -> ctx.print(Boolean.toString(value));
            case ByteConstant(byte value) -> ctx.print(Byte.toString(value));
            case CharConstant(char value) -> {
                String str = String.valueOf(value);
                ctx.print("'").print(EscapeUtil.escapeString(str)).print("'");
            }
            case EnumConstant(InstanceType owner, MemberIdentifier field) -> ctx.element(".enum")
                    .literal(owner.internalName()).print(" ")
                    .literal(field.name()).print(" ")
                    .literal(field.descriptor());
            case FloatConstant(float value) -> ctx.print(value + "F");
            case DoubleConstant(double value) -> {
                String content = Double.toString(value);
                ctx.print(content);

                // Skip 'D' suffix for things like 'NaN' where it is implied
                if (!content.matches("\\D+"))
                    ctx.print("D");
            }
            case IntConstant(int value) -> ctx.print(Integer.toString(value));
            case LongConstant(long value) -> ctx.print(value + "L");
            case ShortConstant(short value) -> ctx.print(Short.toString(value));
            case StringConstant(String value) -> ctx.string(value);
            case TypeConstant(Type type) -> ctx.literal(type.descriptor());
            case HandleConstant(Handle handle) -> printHandle(handle, ctx);
            case MemberConstant(InstanceType owner, MemberIdentifier member) -> ctx.literal(owner.internalName())
                    .print(" ")
                    .literal(member.name())
                    .print(" ")
                    .literal(member.descriptor());

            default -> throw new IllegalStateException("Unexpected value: " + constant);
        }
    }

}

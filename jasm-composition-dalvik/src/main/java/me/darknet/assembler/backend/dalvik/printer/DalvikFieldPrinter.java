package me.darknet.assembler.backend.dalvik.printer;

import me.darknet.assembler.printer.*;

import me.darknet.dex.tree.definitions.FieldMember;
import me.darknet.dex.tree.definitions.constant.Constant;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for printing field definitions in Dalvik assembly format.
 */
public class DalvikFieldPrinter implements FieldPrinter {

    private final FieldMember definition;
    private final DalvikMemberPrinter memberPrinter;
    private final String ownerInternalName;

    public DalvikFieldPrinter(FieldMember definition) {
        this(definition, null);
    }

    public DalvikFieldPrinter(FieldMember definition, String ownerInternalName) {
        this.definition = definition;
        this.ownerInternalName = ownerInternalName;
        this.memberPrinter = new DalvikMemberPrinter(definition, definition, DalvikMemberPrinter.Type.FIELD);
    }

    @Override
    public @Nullable AnnotationPrinter annotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter visibleAnnotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter invisibleAnnotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public void print(PrintContext<?> ctx) {
        if (ownerInternalName != null)
            ctx.printDisassemblyComments(new DisassemblyCommentSite.FieldSite(
                    ownerInternalName, definition.getName(), definition.getType().descriptor()));
        memberPrinter.printAttributes(ctx);
        if (definition.getSignature() != null) {
            ctx.begin().element(".signature").string(definition.getSignature()).next();
        }
        memberPrinter.printDeclaration(ctx).literal(definition.getName()).print(" ").literal(definition.getType().descriptor())
                .print(" ");
        Constant constant = definition.getStaticValue();
        if (constant != null) {
            ctx.print("{value: ");
            DalvikConstantPrinter.printConstant(ctx, constant);
            ctx.print("}");
        }
    }
}

package me.darknet.assembler.printer;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.TypeAnnotationNode;

public class JvmTypeAnnotationPrinter extends JvmAnnotationPrinter {
    private final TypeAnnotationNode annotation;

    protected JvmTypeAnnotationPrinter(TypeAnnotationNode annotation, boolean visible) {
        super(annotation, visible);
        this.annotation = annotation;
    }

    protected JvmTypeAnnotationPrinter(TypeAnnotationNode annotation) {
        super(annotation);
        this.annotation = annotation;
    }

    @Override
    public void print(PrintContext<?> ctx) {
        String token = visible == null ? ".type-annotation" :
                visible ? ".type-visible-annotation" : ".type-invisible-annotation";

        ctx.begin().element(token).literal(Type.getType(annotation.desc).getInternalName()).print(" ");

        var wrapper = ctx.object();
        var location = wrapper.literalValue("location").object();
        location.literalValue("ref").literal("0b" + Integer.toBinaryString(annotation.typeRef)).next();
        location.literalValue("path").literal(annotation.typePath == null ? "_" : annotation.typePath.toString());
        location.end();
        location.next();

        var entries = wrapper.literalValue("values").object();
        if (annotation.values != null && !annotation.values.isEmpty()) {
            printEntries(entries, annotation.values);
        }
        entries.end();
        wrapper.end();
    }
}

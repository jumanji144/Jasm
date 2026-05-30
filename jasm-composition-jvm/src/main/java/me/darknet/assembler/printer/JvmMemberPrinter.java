package me.darknet.assembler.printer;

import me.darknet.assembler.util.JvmModifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.List;
import java.util.function.Function;

public final class JvmMemberPrinter {
    private final List<AnnotationNode> visibleAnnotations;
    private final List<AnnotationNode> invisibleAnnotations;
    private final List<TypeAnnotationNode> visibleTypeAnnotations;
    private final List<TypeAnnotationNode> invisibleTypeAnnotations;
    private final String signature;
    private final Integer access;
    private final Type type;

    public JvmMemberPrinter(@NotNull ClassNode node, @NotNull Type type) {
        this(node.visibleAnnotations, node.invisibleAnnotations, node.visibleTypeAnnotations, node.invisibleTypeAnnotations,
                node.signature, node.access, type);
    }

    public JvmMemberPrinter(@NotNull FieldNode node, @NotNull Type type) {
        this(node.visibleAnnotations, node.invisibleAnnotations, node.visibleTypeAnnotations, node.invisibleTypeAnnotations,
                node.signature, node.access, type);
    }

    public JvmMemberPrinter(@NotNull MethodNode node, @NotNull Type type) {
        this(node.visibleAnnotations, node.invisibleAnnotations, node.visibleTypeAnnotations, node.invisibleTypeAnnotations,
                node.signature, node.access, type);
    }

    public JvmMemberPrinter(@NotNull RecordComponentNode node) {
        this(node.visibleAnnotations, node.invisibleAnnotations, node.visibleTypeAnnotations, node.invisibleTypeAnnotations,
                node.signature, null, Type.CLASS);
    }

    private JvmMemberPrinter(@Nullable List<AnnotationNode> visibleAnnotations,
                             @Nullable List<AnnotationNode> invisibleAnnotations,
                             @Nullable List<TypeAnnotationNode> visibleTypeAnnotations,
                             @Nullable List<TypeAnnotationNode> invisibleTypeAnnotations,
                             @Nullable String signature,
                             @Nullable Integer access,
                             @NotNull Type type) {
        this.visibleAnnotations = visibleAnnotations == null ? List.of() : visibleAnnotations;
        this.invisibleAnnotations = invisibleAnnotations == null ? List.of() : invisibleAnnotations;
        this.visibleTypeAnnotations = visibleTypeAnnotations == null ? List.of() : visibleTypeAnnotations;
        this.invisibleTypeAnnotations = invisibleTypeAnnotations == null ? List.of() : invisibleTypeAnnotations;
        this.signature = signature;
        this.access = access;
        this.type = type;
    }

    public void printAttributes(PrintContext<?> ctx) {
        printAnnos(ctx, visibleAnnotations, annotation -> JvmAnnotationPrinter.forTopLevelAnno(annotation, true));
        printAnnos(ctx, invisibleAnnotations, annotation -> JvmAnnotationPrinter.forTopLevelAnno(annotation, false));
        printAnnos(ctx, visibleTypeAnnotations, annotation -> JvmAnnotationPrinter.forTopLevelAnno(annotation, true));
        printAnnos(ctx, invisibleTypeAnnotations, annotation -> JvmAnnotationPrinter.forTopLevelAnno(annotation, false));
        if (signature != null) {
            ctx.begin().element(".signature").string(signature).next();
        }
    }

    private <A extends AnnotationNode> void printAnnos(PrintContext<?> ctx, List<A> annotations,
                                                       Function<A, JvmAnnotationPrinter> printerFunction) {
        for (A annotation : annotations) {
            printerFunction.apply(annotation).print(ctx);
            ctx.next();
        }
    }

    public PrintContext<?> printDeclaration(PrintContext<?> ctx) {
        if (access != null) {
            String elementName = switch (type) {
                case CLASS -> ".class";
                case FIELD -> ".field";
                case METHOD -> ".method";
            };
            int modifierType = switch (type) {
                case CLASS -> JvmModifiers.CLASS;
                case FIELD -> JvmModifiers.FIELD;
                case METHOD -> JvmModifiers.METHOD;
            };
            return ctx.begin().element(elementName).print(JvmModifiers.modifiers(access, modifierType));
        }
        return ctx;
    }

    public @Nullable AnnotationPrinter printAnnotation(int index) {
        int visibleCount = visibleAnnotations.size();
        if (index < visibleCount) {
            return new JvmAnnotationPrinter(visibleAnnotations.get(index), true);
        }
        int invisibleIndex = index - visibleCount;
        if (invisibleIndex < invisibleAnnotations.size()) {
            return new JvmAnnotationPrinter(invisibleAnnotations.get(invisibleIndex), false);
        }
        return null;
    }

    public @Nullable AnnotationPrinter printVisibleAnnotation(int index) {
        if (index < visibleAnnotations.size()) {
            return new JvmAnnotationPrinter(visibleAnnotations.get(index), true);
        }
        return null;
    }

    public @Nullable AnnotationPrinter printInvisibleAnnotation(int index) {
        if (index < invisibleAnnotations.size()) {
            return new JvmAnnotationPrinter(invisibleAnnotations.get(index), false);
        }
        return null;
    }

    public enum Type {
        CLASS,
        FIELD,
        METHOD
    }
}


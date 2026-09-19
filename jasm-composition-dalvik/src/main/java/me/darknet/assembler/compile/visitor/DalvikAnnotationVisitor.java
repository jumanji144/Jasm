package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.assembler.compile.DalvikConstantMapper;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.constant.AnnotationConstant;
import me.darknet.dex.tree.definitions.constant.ArrayConstant;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds a DEX annotation from JASM annotation elements.
 */
final class DalvikAnnotationVisitor implements ASTAnnotationVisitor {
    static final byte BUILD = Annotation.VISIBILITY_BUILD;
    static final byte RUNTIME = Annotation.VISIBILITY_RUNTIME;
    static final byte SYSTEM = Annotation.VISIBILITY_SYSTEM;

    private final byte visibility;
    private final InstanceType type;
    private final Consumer<Annotation> sink;
    private final Map<String, Constant> elements = new LinkedHashMap<>();

    DalvikAnnotationVisitor(byte visibility, @NotNull ASTIdentifier classType, Consumer<Annotation> sink) {
        this(visibility, Types.instanceTypeFromInternalName(classType.literal()), sink);
    }

    private DalvikAnnotationVisitor(byte visibility, InstanceType type, Consumer<Annotation> sink) {
        this.visibility = visibility;
        this.type = type;
        this.sink = sink;
    }

    @Override
    public void visitValue(ASTIdentifier name, ASTValue value) {
        elements.put(name.literal(), DalvikConstantMapper.fromConstant(value));
    }

    @Override
    public void visitTypeValue(ASTIdentifier name, ASTIdentifier className) {
        elements.put(name.literal(), DalvikConstantMapper.fromConstant(className));
    }

    @Override
    public void visitEnumValue(ASTIdentifier name, ASTIdentifier className, ASTIdentifier enumName) {
        elements.put(name.literal(), DalvikConstantMapper.fromConstant(new ASTEnum(className, enumName)));
    }

    @Override
    public ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier name, ASTIdentifier className) {
        return new DalvikAnnotationVisitor(
                RUNTIME,
                className,
                annotation -> elements.put(name.literal(), new AnnotationConstant(annotation.annotation()))
        );
    }

    @Override
    public ASTAnnotationArrayVisitor visitArrayValue(ASTIdentifier name) {
        return new DalvikAnnotationArrayVisitor(
                values -> elements.put(name.literal(), new ArrayConstant(values))
        );
    }

    @Override
    public void visitEnd() {
        sink.accept(new Annotation(visibility, new AnnotationPart(type, new LinkedHashMap<>(elements))));
    }
}

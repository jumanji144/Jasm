package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTEnum;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.backend.dalvik.compile.DalvikConstantMapper;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.dex.tree.definitions.constant.AnnotationConstant;
import me.darknet.dex.tree.definitions.constant.ArrayConstant;
import me.darknet.dex.tree.definitions.constant.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds DEX constants from an annotation array.
 */
final class DalvikAnnotationArrayVisitor implements ASTAnnotationArrayVisitor {
    private final List<Constant> values = new ArrayList<>();
    private final Consumer<List<Constant>> sink;

    DalvikAnnotationArrayVisitor(Consumer<List<Constant>> sink) {
        this.sink = sink;
    }

    @Override
    public void visitValue(ASTValue value) {
        values.add(DalvikConstantMapper.fromConstant(value));
    }

    @Override
    public void visitTypeValue(ASTIdentifier className) {
        values.add(DalvikConstantMapper.fromConstant(className));
    }

    @Override
    public void visitEnumValue(ASTIdentifier className, ASTIdentifier enumName) {
        values.add(DalvikConstantMapper.fromConstant(new ASTEnum(className, enumName)));
    }

    @Override
    public ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier className) {
        return new DalvikAnnotationVisitor(
                DalvikAnnotationVisitor.RUNTIME,
                className,
                annotation -> values.add(new AnnotationConstant(annotation.annotation()))
        );
    }

    @Override
    public ASTAnnotationArrayVisitor visitArrayValue() {
        return new DalvikAnnotationArrayVisitor(this::addAllAsArray);
    }

    @Override
    public void visitEnd() {
        sink.accept(List.copyOf(values));
    }

    private void addAllAsArray(List<Constant> nested) {
        values.add(new ArrayConstant(nested));
    }
}

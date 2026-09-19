package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.DalvikModifiers;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import me.darknet.assembler.visitor.*;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.FieldMember;
import me.darknet.dex.tree.definitions.InnerClass;
import me.darknet.dex.tree.definitions.MemberIdentifier;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationProcessing;
import me.darknet.dex.tree.type.ClassType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.Type;
import me.darknet.dex.tree.type.TypeParser;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DalvikClassVisitor(ClassDefinition definition) implements ASTClassVisitor {
    @Override
    public void visitSuperClass(@Nullable ASTIdentifier superClass) {
        definition.setSuperClass(superClass == null ? null : Types.instanceTypeFromInternalName(superClass.literal()));
    }

    @Override
    public void visitInterface(@NotNull ASTIdentifier interfaceName) {
        definition.addInterface(Types.instanceTypeFromInternalName(interfaceName.literal()));
    }

    @Override
    public void visitSourceFile(@Nullable ASTString sourceFile) {
        definition.setSourceFile(sourceFile == null ? null : sourceFile.content());
    }

    @Override
    public void visitOuterClass(@Nullable ASTElement outerClass) {
        if (outerClass == null) {
            definition.setEnclosingClass(null);
            return;
        }
        if (!(outerClass instanceof ASTIdentifier identifier)) {
            throw new IllegalStateException("Expected an outer class identifier");
        }
        definition.setEnclosingClass(Types.instanceTypeFromInternalName(identifier.literal()));
    }

    @Override
    public void visitOuterMethod(@Nullable ASTOuterMethod outerMethod) {
        if (outerMethod == null) {
            definition.setEnclosingMethod(null);
            return;
        }
        definition.setEnclosingMethod(new MemberIdentifier(
                outerMethod.getMethodName().literal(),
                Types.methodTypeFromDescriptor(outerMethod.getMethodDesc().literal())
        ));
    }

    @Override
    public void visitPermittedSubclass(@NotNull ASTIdentifier subclass) {
        throw new IllegalStateException("Dalvik permitted-subclass metadata is not supported by the current dex tree");
    }

    @Override
    public void visitNestHost(@Nullable ASTIdentifier nestHost) {
        if (nestHost != null) {
            throw new IllegalStateException("Dalvik nest metadata is not supported by the current dex tree");
        }
    }

    @Override
    public void visitNestMember(@NotNull ASTIdentifier nestMember) {
        throw new IllegalStateException("Dalvik nest metadata is not supported by the current dex tree");
    }

    @Override
    public ASTRecordComponentVisitor visitRecordComponent(@NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor, @Nullable ASTString signature) {
        throw new IllegalStateException("Dalvik record components are not supported by the current dex tree");
    }

    @Override
    public void visitInnerClass(@NotNull Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass, @Nullable ASTIdentifier innerClass) {
        if (innerClass == null || outerClass == null) {
            throw new IllegalStateException("Dalvik inner classes require inner and outer class identifiers");
        }
        definition.addInnerClass(new InnerClass(
                innerClass.literal(),
                outerClass.literal(),
                name == null ? null : name.literal(),
                DalvikModifiers.getClassModifiers(modifiers)
        ));
    }

    @Override
    public ASTFieldVisitor visitField(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor) {
        ClassType type = new TypeParser(descriptor.literal()).requireClassType();
        FieldMember member = new FieldMember(name.literal(), type, DalvikModifiers.getFieldModifiers(modifiers));
        definition.putField(member);
        return new DalvikFieldVisitor(member);
    }

    @Override
    public ASTMethodVisitor visitMethod(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor) {
        Type parsedType = new TypeParser(descriptor.literal()).required();
        if (!(parsedType instanceof MethodType methodType)) {
            throw new IllegalStateException("Expected method descriptor: " + descriptor.literal());
        }

        MethodMember member = new MethodMember(name.literal(), methodType, DalvikModifiers.getMethodModifiers(modifiers));
        definition.putMethod(member);
        return new DalvikMethodVisitor(member);
    }

    @Override
    public void visitSignature(@Nullable ASTString signature) {
        definition.setSignature(signature == null ? null : signature.content());
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public ASTAnnotationVisitor visitVisibleAnnotation(@NotNull ASTIdentifier classType) {
        return new DalvikAnnotationVisitor(DalvikAnnotationVisitor.RUNTIME, classType, this::acceptAnnotation);
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleAnnotation(@NotNull ASTIdentifier classType) {
        return new DalvikAnnotationVisitor(DalvikAnnotationVisitor.BUILD, classType, this::acceptAnnotation);
    }

    @Override
    public ASTAnnotationVisitor visitSystemAnnotation(@NotNull ASTIdentifier classType) {
        return new DalvikAnnotationVisitor(DalvikAnnotationVisitor.SYSTEM, classType, this::acceptAnnotation);
    }

    @Override
    public ASTAnnotationVisitor visitVisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        throw new IllegalStateException("Dalvik type annotations are not supported by the current dex tree");
    }

    @Override
    public ASTAnnotationVisitor visitInvisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
        throw new IllegalStateException("Dalvik type annotations are not supported by the current dex tree");
    }

    private void acceptAnnotation(Annotation annotation) {
        if (annotation.visibility() == Annotation.VISIBILITY_SYSTEM) {
            switch (AnnotationProcessing.processAttribute(java.util.Map.of(), definition, annotation.annotation())) {
                case CONSUMED -> {
                    return;
                }
                case ERROR -> throw new IllegalStateException(
                        "Invalid class annotation: " + annotation.annotation().type().internalName()
                );
                case PRESERVE -> {
                }
            }
        }
        definition.addAnnotation(annotation);
    }
}

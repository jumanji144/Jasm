package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.DalvikModifiers;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.visitor.*;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.FieldMember;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.type.ClassType;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.Type;
import me.darknet.dex.tree.type.TypeParser;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.NotNull;

public class DalvikRootVisitor implements ASTRootVisitor {

    private ClassDefinition definition;

    public DalvikRootVisitor(ClassDefinition overlay) {
        this.definition = overlay;
    }

    public ClassDefinition getDefinition() {
        return definition;
    }

    @Override
    public ASTAnnotationVisitor visitAnnotation(ASTIdentifier name) {
        return null;
    }

    @Override
    public ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name) {
        int accessFlags = DalvikModifiers.getClassModifiers(modifiers);
        InstanceType type = Types.instanceTypeFromInternalName(name.literal());
        if (definition == null) {
            definition = new ClassDefinition(type, null, accessFlags);
        }
        return new DalvikClassVisitor(definition);
    }

    @Override
    public ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
        ClassDefinition definition = requireDefinition();
        ClassType type = new TypeParser(descriptor.literal()).requireClassType();
        FieldMember member = new FieldMember(name.literal(), type, DalvikModifiers.getFieldModifiers(modifiers));
        definition.putField(member);
        return new DalvikFieldVisitor(member);
    }

    @Override
    public ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
        ClassDefinition definition = requireDefinition();
        Type parsedType = new TypeParser(descriptor.literal()).required();
        if (!(parsedType instanceof MethodType methodType)) {
            throw new IllegalStateException("Expected method descriptor: " + descriptor.literal());
        }

        MethodMember member = new MethodMember(name.literal(), methodType, DalvikModifiers.getMethodModifiers(modifiers));
        definition.putMethod(member);
        return new DalvikMethodVisitor(member);
    }

    private @NotNull ClassDefinition requireDefinition() {
        if (definition == null) {
            throw new IllegalStateException("Top-level Dalvik members require an overlay class");
        }
        return definition;
    }
}

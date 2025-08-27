package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.DalvikModifiers;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.visitor.*;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.Types;

public class DalvikRootVisitor implements ASTRootVisitor {

    private ClassDefinition definition;

    public DalvikRootVisitor(ClassDefinition overlay) {
        this.definition = overlay;
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
        return null;
    }

    @Override
    public ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
        return null;
    }
}

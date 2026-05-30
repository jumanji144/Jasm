package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;

public interface ASTRootVisitor {

    // TODO: This method is never called in our tests.
    //  - I think we had the idea to allow independent annotation editing
    //    without the need to associate them with something else but we never implemented it.
    ASTAnnotationVisitor visitAnnotation(ASTIdentifier name);

    ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name);

    ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor);

    ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor);

}

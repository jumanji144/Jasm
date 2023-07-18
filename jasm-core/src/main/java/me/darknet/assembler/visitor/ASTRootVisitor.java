package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;

public interface ASTRootVisitor {

	ASTAnnotationVisitor visitAnnotation(ASTIdentifier name);

	ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name);

	ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor);

	ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor);

}

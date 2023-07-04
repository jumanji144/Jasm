package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;

public interface ASTClassVisitor extends ASTDeclarationVisitor {

	void visitSuperClass(ASTIdentifier superClass);

	void visitInterface(ASTIdentifier interfaceName);

	void visitSourceFile(ASTString sourceFile);

}

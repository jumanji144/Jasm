package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

public class DelegatingClassGroupVisitor implements ClassGroupVisitor{
	private final ClassGroupVisitor delegate;

	public DelegatingClassGroupVisitor(ClassGroupVisitor delegate) {
		this.delegate = delegate;
	}

	@Override
	public void visitExtends(ExtendsGroup group) {
		delegate.visitExtends(group);
	}

	@Override
	public void visitImplements(ImplementsGroup group) {
		delegate.visitImplements(group);
	}

	@Override
	public void visitAnnotation(AnnotationGroup annotation) throws AssemblerException {
		delegate.visitAnnotation(annotation);
	}

	@Override
	public void visitSignature(SignatureGroup signature) throws AssemblerException {
		delegate.visitSignature(signature);
	}

	@Override
	public void visitVersion(VersionGroup version) throws AssemblerException {
		delegate.visitVersion(version);
	}

	@Override
	public void visitSourceFile(SourceFileGroup sourceFile) throws AssemblerException {
		delegate.visitSourceFile(sourceFile);
	}

	@Override
	public void visitAttribute(ClassAttributeGroup group) throws AssemblerException {
		delegate.visitAttribute(group);
	}

	@Override
	public void visitBegin() throws AssemblerException {
		delegate.visitBegin();
	}

	@Override
	public void visit(Group group) throws AssemblerException {
		delegate.visit(group);
	}

	@Override
	public void visitEnd() throws AssemblerException {
		delegate.visitEnd();
	}
}

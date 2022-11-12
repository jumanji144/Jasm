package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.AnnotationGroup;
import me.darknet.assembler.parser.groups.FieldAttributeGroup;
import me.darknet.assembler.parser.groups.SignatureGroup;

public class DelegatingFieldGroupVisitor implements FieldGroupVisitor{
	private final FieldGroupVisitor delegate;

	public DelegatingFieldGroupVisitor(FieldGroupVisitor delegate) {
		this.delegate = delegate;
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
	public void visitAttribute(FieldAttributeGroup group) throws AssemblerException {
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

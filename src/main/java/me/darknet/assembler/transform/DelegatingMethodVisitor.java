package me.darknet.assembler.transform;

import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

public class DelegatingMethodVisitor implements MethodGroupVisitor {
	private final MethodGroupVisitor delegate;

	public DelegatingMethodVisitor(MethodGroupVisitor delegate) {
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
	public void visitThrows(ThrowsGroup thrw) throws AssemblerException {
		delegate.visitThrows(thrw);
	}

	@Override
	public void visitLabel(LabelGroup label) throws AssemblerException {
		delegate.visitLabel(label);
	}

	@Override
	public void visitLookupSwitchInsn(LookupSwitchGroup lookupSwitch) throws AssemblerException {
		delegate.visitLookupSwitchInsn(lookupSwitch);
	}

	@Override
	public void visitTableSwitchInsn(TableSwitchGroup tableSwitch) throws AssemblerException {
		delegate.visitTableSwitchInsn(tableSwitch);
	}

	@Override
	public void visitCatch(CatchGroup catchGroup) throws AssemblerException {
		delegate.visitCatch(catchGroup);
	}

	@Override
	public void visitVarInsn(int opcode, IdentifierGroup identifier) throws AssemblerException {
		delegate.visitVarInsn(opcode, identifier);
	}

	@Override
	public void visitDirectVarInsn(int opcode, int var) throws AssemblerException {
		delegate.visitDirectVarInsn(opcode, var);
	}

	@Override
	public void visitMethodInsn(int opcode, IdentifierGroup name, IdentifierGroup desc, boolean itf) throws AssemblerException {
		delegate.visitMethodInsn(opcode, name, desc, itf);
	}

	@Override
	public void visitFieldInsn(int opcode, IdentifierGroup name, IdentifierGroup desc) throws AssemblerException {
		delegate.visitFieldInsn(opcode, name, desc);
	}

	@Override
	public void visitJumpInsn(int opcode, LabelGroup label) throws AssemblerException {
		delegate.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLdcInsn(Group constant) throws AssemblerException {
		delegate.visitLdcInsn(constant);
	}

	@Override
	public void visitTypeInsn(int opcode, IdentifierGroup type) throws AssemblerException {
		delegate.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitIincInsn(IdentifierGroup var, int value) throws AssemblerException {
		delegate.visitIincInsn(var, value);
	}

	@Override
	public void visitIntInsn(int opcode, int value) throws AssemblerException {
		delegate.visitIntInsn(opcode, value);
	}

	@Override
	public void visitLineNumber(NumberGroup line, IdentifierGroup label) throws AssemblerException {
		delegate.visitLineNumber(line, label);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) throws AssemblerException {
		delegate.visitMultiANewArrayInsn(desc, dims);
	}

	@Override
	public void visitInvokeDynamicInstruction(String identifier, IdentifierGroup descriptor, HandleGroup handle, ArgsGroup args) throws AssemblerException {
		delegate.visitInvokeDynamicInstruction(identifier, descriptor, handle, args);
	}

	@Override
	public void visitInsn(int opcode) throws AssemblerException {
		delegate.visitInsn(opcode);
	}

	@Override
	public void visitExpr(ExprGroup expr) throws AssemblerException {
		delegate.visitExpr(expr);
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

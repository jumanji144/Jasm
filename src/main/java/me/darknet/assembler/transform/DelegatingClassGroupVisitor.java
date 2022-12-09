package me.darknet.assembler.transform;

import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.annotation.AnnotationGroup;
import me.darknet.assembler.parser.groups.attributes.*;
import me.darknet.assembler.parser.groups.module.ModuleGroup;
import me.darknet.assembler.parser.groups.record.RecordGroup;

public class DelegatingClassGroupVisitor implements ClassGroupVisitor{
	private final ClassGroupVisitor delegate;

	public DelegatingClassGroupVisitor(ClassGroupVisitor delegate) {
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
	public void visitVersion(VersionGroup version) throws AssemblerException {
		delegate.visitVersion(version);
	}

	@Override
	public void visitSourceFile(SourceFileGroup sourceFile) throws AssemblerException {
		delegate.visitSourceFile(sourceFile);
	}

	@Override
	public void visitInnerClass(InnerClassGroup innerClass) throws AssemblerException {
		delegate.visitInnerClass(innerClass);
	}

	@Override
	public void visitNestHost(NestHostGroup nestHost) throws AssemblerException {
		delegate.visitNestHost(nestHost);
	}

	@Override
	public void visitNestMember(NestMemberGroup nestMember) throws AssemblerException {
		delegate.visitNestMember(nestMember);
	}

	@Override
	public void visitPermittedSubclass(PermittedSubclassGroup permittedSubclass) throws AssemblerException {
		delegate.visitPermittedSubclass(permittedSubclass);
	}

	@Override
	public void visitDeprecated(DeprecatedGroup deprecated) throws AssemblerException {
		delegate.visitDeprecated(deprecated);
	}

	@Override
	public void visitModule(ModuleGroup module) throws AssemblerException {
		delegate.visitModule(module);
	}

	@Override
	public void visitRecord(RecordGroup record) throws AssemblerException {
		delegate.visitRecord(record);
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

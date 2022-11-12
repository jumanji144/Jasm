package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

public abstract class AbstractTopLevelGroupVisitor implements TopLevelGroupVisitor {
	private final AttributeStore attributeStore = new AttributeStore();

	protected AttributeStore getAttributeStore() {
		return attributeStore;
	}

	@Override
	public void visitBegin() throws AssemblerException {
		// no-op
	}

	@Override
	public void visitEnd() throws AssemblerException {
		// no-op
	}

	@Override
	public void visit(Group group) throws AssemblerException {
		// Record all attribute-like groups
		if (group instanceof AttributeGroup) {
			attributeStore.recordGroup((AttributeGroup) group);
		}
	}

	public abstract ClassGroupVisitor visitImplClass(AccessModsGroup accessMods, IdentifierGroup identifier) throws AssemblerException;

	public abstract FieldGroupVisitor visitImplField(FieldDeclarationGroup decl) throws AssemblerException;

	public abstract MethodGroupVisitor visitImplMethod(MethodDeclarationGroup decl) throws AssemblerException;

	@Override
	public final ClassGroupVisitor visitClass(AccessModsGroup accessMods, IdentifierGroup identifier) throws AssemblerException {
		// When the visitation is over, add all recorded attributes
		ClassGroupVisitor delegate = visitImplClass(accessMods, identifier);
		return new DelegatingClassGroupVisitor(delegate) {
			@Override
			public void visitEnd() throws AssemblerException {
				attributeStore.accept(delegate);
				attributeStore.clear();
				super.visitEnd();
			}
		};
	}

	@Override
	public final FieldGroupVisitor visitField(FieldDeclarationGroup decl) throws AssemblerException {
		// When the visitation is over, add all recorded attributes
		FieldGroupVisitor delegate = visitImplField(decl);
		return new DelegatingFieldGroupVisitor(delegate) {
			@Override
			public void visitEnd() throws AssemblerException {
				attributeStore.accept(delegate);
				attributeStore.clear();
				super.visitEnd();
			}
		};
	}

	@Override
	public final MethodGroupVisitor visitMethod(MethodDeclarationGroup decl) throws AssemblerException {
		// When the visitation is over, add all recorded attributes
		MethodGroupVisitor delegate = visitImplMethod(decl);
		return new DelegatingMethodVisitor(delegate) {
			@Override
			public void visitEnd() throws AssemblerException {
				attributeStore.accept(delegate);
				attributeStore.clear();
				super.visitEnd();
			}
		};
	}
}

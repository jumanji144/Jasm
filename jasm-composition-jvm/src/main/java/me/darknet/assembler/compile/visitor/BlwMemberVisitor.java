package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.Member;
import dev.xdark.blw.classfile.MemberBuilder;
import dev.xdark.blw.type.Type;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;
import org.jetbrains.annotations.Nullable;

public class BlwMemberVisitor<T extends Type, M extends Member<T>> implements ASTDeclarationVisitor {
	private final MemberBuilder<T, M, ?> builder;

	public BlwMemberVisitor(MemberBuilder<T, M, ?> builder) {
		this.builder = builder;
	}

	@Override
	public ASTAnnotationVisitor visitAnnotation(ASTIdentifier classType) {
		return new BlwAnnotationVisitor(
				builder.putVisibleRuntimeAnnotation(Types.instanceTypeFromInternalName(classType.literal())).child()
		);
	}

	@Override
	public void visitSignature(@Nullable ASTString signature) {
		builder.signature(signature.content());
	}

	@Override
	public void visitEnd() {
	}
}

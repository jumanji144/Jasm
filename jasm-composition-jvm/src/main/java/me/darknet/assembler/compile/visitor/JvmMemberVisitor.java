package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTDeclarationVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

/**
 * Shared visitor for JVM declarations, including signatures and runtime annotations.
 */
public abstract class JvmMemberVisitor implements ASTDeclarationVisitor {
	protected abstract void setSignature(@NotNull String signature);

	protected abstract void setDeprecated();

	protected abstract @NotNull AnnotationNode addRuntimeAnnotation(boolean visible, @NotNull String descriptor);

	protected abstract @NotNull TypeAnnotationNode addRuntimeTypeAnnotation(boolean visible, int typeRef,
	                                                                        @Nullable TypePath typePath,
	                                                                        @NotNull String descriptor);

	@Override
	public ASTAnnotationVisitor visitAnnotation(@NotNull AnnotationVisibility visibility, @NotNull ASTIdentifier classType) {
		return new JvmAnnotationVisitor(addRuntimeAnnotation(visibility == AnnotationVisibility.VISIBLE,
				Type.getObjectType(classType.literal()).getDescriptor()));
	}

	@Override
	public ASTAnnotationVisitor visitTypeAnnotation(@NotNull AnnotationVisibility visibility, @NotNull ASTIdentifier classType,
	                                                @NotNull ASTNumber typeRef, @Nullable ASTIdentifier typePath) {
		return new JvmAnnotationVisitor(addRuntimeTypeAnnotation(
				visibility == AnnotationVisibility.VISIBLE,
				typeRef.asInt(),
				parseTypePath(typePath),
				Type.getObjectType(classType.literal()).getDescriptor()
		));
	}

	@Override
	public void visitSignature(@Nullable ASTString signature) {
		if (signature != null)
			setSignature(signature.content());
	}

	@Override
	public void visitDeprecated() {
		setDeprecated();
	}

	@Override
	public void visitEnd() {
	}

	private static @Nullable TypePath parseTypePath(@Nullable ASTIdentifier typePath) {
		if (typePath == null)
			return null;
		String content = typePath.content();
		return "_".equals(content) ? null : TypePath.fromString(content);
	}
}

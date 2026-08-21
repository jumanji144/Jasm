package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.util.ConstantMapper;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;

public class JvmFieldVisitor extends JvmMemberVisitor implements ASTFieldVisitor {
	private final FieldNode field;

	public JvmFieldVisitor(FieldNode field) {
		this.field = field;
	}

	@Override
	protected void setSignature(@NotNull String signature) {
		field.signature = signature;
	}

	@Override
	protected void setDeprecated() {
		field.access |= Opcodes.ACC_DEPRECATED;
	}

	@Override
	protected @NotNull AnnotationNode addRuntimeAnnotation(boolean visible, @NotNull String descriptor) {
		AnnotationNode annotation = new AnnotationNode(descriptor);
		if (visible) {
			if (field.visibleAnnotations == null) {
				field.visibleAnnotations = new ArrayList<>();
			}
			field.visibleAnnotations.add(annotation);
		} else {
			if (field.invisibleAnnotations == null) {
				field.invisibleAnnotations = new ArrayList<>();
			}
			field.invisibleAnnotations.add(annotation);
		}
		return annotation;
	}

	@Override
	protected @NotNull TypeAnnotationNode addRuntimeTypeAnnotation(boolean visible, int typeRef,
	                                                               @Nullable TypePath typePath,
	                                                               @NotNull String descriptor) {
		TypeAnnotationNode annotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
		if (visible) {
			if (field.visibleTypeAnnotations == null) {
				field.visibleTypeAnnotations = new ArrayList<>();
			}
			field.visibleTypeAnnotations.add(annotation);
		} else {
			if (field.invisibleTypeAnnotations == null) {
				field.invisibleTypeAnnotations = new ArrayList<>();
			}
			field.invisibleTypeAnnotations.add(annotation);
		}
		return annotation;
	}

	@Override
	public void visitValue(ASTElement value) {
		if (value != null) {
			field.value = ConstantMapper.fromConstant(value);
		}
	}
}


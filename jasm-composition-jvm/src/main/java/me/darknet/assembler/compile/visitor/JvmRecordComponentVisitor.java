package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTRecordComponentVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;

public class JvmRecordComponentVisitor implements ASTRecordComponentVisitor, JvmAnnotationElementAdapter {
	private final RecordComponentNode component;

	public JvmRecordComponentVisitor(RecordComponentNode component) {
		this.component = component;
	}

	@Override
	public ASTAnnotationVisitor visitVisibleAnnotation(@NotNull ASTIdentifier classType) {
		return new JvmAnnotationVisitor(addRuntimeAnnotation(true, classType.literal()));
	}

	@Override
	public ASTAnnotationVisitor visitInvisibleAnnotation(@NotNull ASTIdentifier classType) {
		return new JvmAnnotationVisitor(addRuntimeAnnotation(false, classType.literal()));
	}

	@Override
	public ASTAnnotationVisitor visitVisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef,
	                                                       @Nullable ASTIdentifier typePath) {
		return new JvmAnnotationVisitor(addRuntimeTypeAnnotation(true, classType.literal(), typeRef.asInt(), typePath));
	}

	@Override
	public ASTAnnotationVisitor visitInvisibleTypeAnnotation(@NotNull ASTIdentifier classType, @NotNull ASTNumber typeRef,
	                                                         @Nullable ASTIdentifier typePath) {
		return new JvmAnnotationVisitor(addRuntimeTypeAnnotation(false, classType.literal(), typeRef.asInt(), typePath));
	}

	private @NotNull AnnotationNode addRuntimeAnnotation(boolean visible, @NotNull String internalName) {
		AnnotationNode annotation = new AnnotationNode(Type.getObjectType(internalName).getDescriptor());
		if (visible) {
			if (component.visibleAnnotations == null) {
				component.visibleAnnotations = new ArrayList<>();
			}
			component.visibleAnnotations.add(annotation);
		} else {
			if (component.invisibleAnnotations == null) {
				component.invisibleAnnotations = new ArrayList<>();
			}
			component.invisibleAnnotations.add(annotation);
		}
		return annotation;
	}

	private @NotNull TypeAnnotationNode addRuntimeTypeAnnotation(boolean visible, @NotNull String internalName,
	                                                             int typeRef, @Nullable ASTIdentifier typePath) {
		TypeAnnotationNode annotation = new TypeAnnotationNode(typeRef,
				parseTypePath(typePath),
				Type.getObjectType(internalName).getDescriptor());
		if (visible) {
			if (component.visibleTypeAnnotations == null) {
				component.visibleTypeAnnotations = new ArrayList<>();
			}
			component.visibleTypeAnnotations.add(annotation);
		} else {
			if (component.invisibleTypeAnnotations == null) {
				component.invisibleTypeAnnotations = new ArrayList<>();
			}
			component.invisibleTypeAnnotations.add(annotation);
		}
		return annotation;
	}

	private static @Nullable TypePath parseTypePath(@Nullable ASTIdentifier typePath) {
		if (typePath == null) {
			return null;
		}
		String content = typePath.content();
		return "_".equals(content) ? null : TypePath.fromString(content);
	}
}


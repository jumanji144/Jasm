package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.builder.JvmClassBuilder;
import me.darknet.assembler.util.AnnotationKind;
import me.darknet.assembler.util.JvmModifiers;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTClassVisitor;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.assembler.visitor.ASTRootVisitor;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public record JvmRootVisitor(JvmClassBuilder builder, JvmCompilerOptions options) implements ASTRootVisitor {
	@Override
	public ASTAnnotationVisitor visitAnnotation(ASTIdentifier name) {
		String path = options.annotationPath();
		if (path == null || path.isBlank()) {
			throw new IllegalStateException("Annotation target path was not specified");
		}

		String[] parts = path.split("\\.");
		if (parts.length < 2) {
			throw new IllegalStateException("Invalid annotation target path: " + path);
		}

		int last = parts.length - 1;
		int index = Integer.parseInt(parts[last]);
		AnnotationKind kind = AnnotationKind.from(parts[last - 1]);
		AnnotationNode annotation = selectAnnotationTarget(parts, index, kind, Type.getObjectType(name.literal()).getDescriptor());
		return new JvmAnnotationVisitor(annotation);
	}

	@Override
	public ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name) {
		builder.accessFlags(JvmModifiers.getClassModifiers(modifiers));
		builder.type(name.literal());
		return new JvmClassVisitor(options, builder);
	}

	@Override
	public ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
		return new JvmFieldVisitor(builder.putField(JvmModifiers.getFieldModifiers(modifiers), name.literal(), descriptor.literal()));
	}

	@Override
	public ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
		int accessFlags = JvmModifiers.getMethodModifiers(modifiers);
		return new JvmMethodVisitor(
				options,
				Type.getObjectType(builder.type()),
				Type.getMethodType(descriptor.literal()),
				builder.putMethod(accessFlags, name.literal(), descriptor.literal()),
				analysisResults -> builder.setMethodAnalysis(name.literal(), descriptor.literal(), analysisResults)
		);
	}

	private @NotNull AnnotationNode selectAnnotationTarget(@NotNull String[] parts, int index, @NotNull AnnotationKind kind,
	                                                       @NotNull String descriptor) {
		if (parts.length >= 5) {
			String target = parts[parts.length - 5];
			String member = parts[parts.length - 4];
			String memberDescriptor = parts[parts.length - 3];
			return switch (target) {
				case "field" -> installFieldAnnotation(member, memberDescriptor, kind, index, descriptor);
				case "method" -> installMethodAnnotation(member, memberDescriptor, kind, index, descriptor);
				default -> installClassAnnotation(kind, index, descriptor);
			};
		}
		return installClassAnnotation(kind, index, descriptor);
	}

	private @NotNull AnnotationNode installClassAnnotation(@NotNull AnnotationKind kind, int index, @NotNull String descriptor) {
		return installAnnotation(listForClass(kind), index, descriptor);
	}

	private @NotNull AnnotationNode installFieldAnnotation(@NotNull String name, @NotNull String descriptor,
	                                                       @NotNull AnnotationKind kind, int index,
	                                                       @NotNull String annotationDescriptor) {
		FieldNode field = builder.field(name, descriptor);
		if (field == null) {
			throw new IllegalStateException("Unexpected missing field data: " + name);
		}
		List<AnnotationNode> annotations = kind == AnnotationKind.VIS_ANNO ? field.visibleAnnotations : field.invisibleAnnotations;
		AnnotationNode annotation = installAnnotation(annotations, index, annotationDescriptor);
		if (kind == AnnotationKind.VIS_ANNO) {
			field.visibleAnnotations = annotations;
		} else {
			field.invisibleAnnotations = annotations;
		}
		return annotation;
	}

	private @NotNull AnnotationNode installMethodAnnotation(@NotNull String name, @NotNull String descriptor,
	                                                        @NotNull AnnotationKind kind, int index,
	                                                        @NotNull String annotationDescriptor) {
		MethodNode method = builder.method(name, descriptor);
		if (method == null) {
			throw new IllegalStateException("Unexpected missing method data: " + name);
		}
		builder.markMethodModified(name, descriptor);
		List<AnnotationNode> annotations = kind == AnnotationKind.VIS_ANNO ? method.visibleAnnotations : method.invisibleAnnotations;
		AnnotationNode annotation = installAnnotation(annotations, index, annotationDescriptor);
		if (kind == AnnotationKind.VIS_ANNO) {
			method.visibleAnnotations = annotations;
		} else {
			method.invisibleAnnotations = annotations;
		}
		return annotation;
	}

	private @NotNull List<AnnotationNode> listForClass(@NotNull AnnotationKind kind) {
		if (kind == AnnotationKind.VIS_ANNO) {
			if (builder.node().visibleAnnotations == null) {
				builder.node().visibleAnnotations = new ArrayList<>();
			}
			return builder.node().visibleAnnotations;
		}
		if (builder.node().invisibleAnnotations == null) {
			builder.node().invisibleAnnotations = new ArrayList<>();
		}
		return builder.node().invisibleAnnotations;
	}

	private static @NotNull AnnotationNode installAnnotation(@Nullable List<AnnotationNode> list, int index,
	                                                         @NotNull String descriptor) {
		List<AnnotationNode> annotations = list == null ? new ArrayList<>() : list;
		AnnotationNode annotation = new AnnotationNode(descriptor);
		if (index >= 0 && index < annotations.size()) {
			annotations.set(index, annotation);
		} else {
			annotations.add(annotation);
		}
		return annotation;
	}
}

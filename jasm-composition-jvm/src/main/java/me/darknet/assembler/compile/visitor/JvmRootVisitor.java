package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.builder.JvmClassBuilder;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.processing.ProcessedMethod;
import me.darknet.assembler.util.JvmModifiers;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTClassVisitor;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.assembler.visitor.ASTRootVisitor;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Root visitor for JVM declarations.
 */
public final class JvmRootVisitor implements ASTRootVisitor {
	private final JvmClassBuilder builder;
	private final JvmCompilerOptions options;
	private final @NotNull DiagnosticSink sink;
	private @Nullable JvmClassVisitor classVisitor;

	public JvmRootVisitor(JvmClassBuilder builder, JvmCompilerOptions options, @NotNull DiagnosticSink sink) {
		this.builder = builder;
		this.options = options;
		this.sink = sink;
	}

	/**
	 * Applies class-only attributes that are not part of the neutral visitor protocol.
	 */
	public void applyJvmClassAttributes(@NotNull ASTClass source) {
		if (classVisitor == null)
			return;
		classVisitor.visitVersion(source.getVersion());
		classVisitor.visitSourceDebugExtension(source.getSourceDebugExtension());
	}

	@Override
	public @Nullable ASTAnnotationVisitor visitAnnotation(@NotNull ASTAnnotation annotation) {
		String path = options.getAnnotationPath();
		if (path == null || path.isBlank()) {
			sink.error(DiagnosticCode.MALFORMED_DECLARATION,
					"Annotation target path was not specified", annotation.location());
			return null;
		}

		String[] parts = path.split("\\.");
		if (parts.length < 2) {
			sink.error(DiagnosticCode.MALFORMED_DECLARATION,
					"Invalid annotation target path: " + path, annotation.location());
			return null;
		}

		int last = parts.length - 1;
		int index;
		try {
			index = Integer.parseInt(parts[last]);
		} catch (NumberFormatException ex) {
			sink.error(DiagnosticCode.MALFORMED_DECLARATION,
					"Invalid annotation target index: " + parts[last], annotation.location());
			return null;
		}

		String kind = parts[last - 1];
		boolean visible = visibleAnnotationKind(kind);
		boolean typeAnnotation = annotation.isTypeAnnotation() || typeAnnotationKind(kind);
		String descriptor = Type.getObjectType(annotation.getClassType().literal()).getDescriptor();
		try {
			AnnotationNode installed = selectAnnotationTarget(parts, index, visible, typeAnnotation, descriptor,
					annotation.getTypeRef(), annotation.getTypePath());
			return new JvmAnnotationVisitor(installed);
		} catch (RuntimeException ex) {
			sink.error(DiagnosticCode.MALFORMED_DECLARATION,
					"Invalid annotation target: " + ex.getMessage(), annotation.location());
			return null;
		}
	}

	@Override
	public ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name) {
		builder.accessFlags(JvmModifiers.getClassModifiers(modifiers));
		builder.type(name.literal());
		classVisitor = new JvmClassVisitor(options, builder, sink);
		return classVisitor;
	}

	@Override
	public ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
		return new JvmFieldVisitor(builder.putField(JvmModifiers.getFieldModifiers(modifiers), name.literal(), descriptor.literal()));
	}

	@Override
	public ASTMethodVisitor visitMethod(@NotNull ASTMethod source, @NotNull ProcessedMethod processed) {
		ASTIdentifier name = source.getName();
		ASTIdentifier descriptor = source.getDescriptor();
		int accessFlags = JvmModifiers.getMethodModifiers(source.getModifiers());
		MethodNode existingMethod = builder.method(name.literal(), descriptor.literal());
		boolean hadPriorLocalVariables = existingMethod != null
				&& existingMethod.localVariables != null
				&& !existingMethod.localVariables.isEmpty();
		return new JvmMethodVisitor(
				options,
				Type.getObjectType(builder.type()),
				Type.getMethodType(descriptor.literal()),
				builder.putMethod(accessFlags, name.literal(), descriptor.literal()),
				hadPriorLocalVariables,
				analysisResults -> builder.setMethodAnalysis(name.literal(), descriptor.literal(), analysisResults),
				sink
		);
	}

	private @NotNull AnnotationNode selectAnnotationTarget(@NotNull String[] parts, int index, boolean visible,
	                                                       boolean typeAnnotation, @NotNull String descriptor,
	                                                       @Nullable me.darknet.assembler.ast.primitive.ASTNumber typeRef,
	                                                       @Nullable ASTIdentifier typePath) {
		if (parts.length >= 5) {
			String target = parts[parts.length - 5];
			String member = parts[parts.length - 4];
			String memberDescriptor = parts[parts.length - 3];
			return switch (target) {
				case "field" -> installFieldAnnotation(member, memberDescriptor, visible, typeAnnotation, index, descriptor, typeRef, typePath);
				case "method" -> installMethodAnnotation(member, memberDescriptor, visible, typeAnnotation, index, descriptor, typeRef, typePath);
				default -> installClassAnnotation(visible, typeAnnotation, index, descriptor, typeRef, typePath);
			};
		}
		return installClassAnnotation(visible, typeAnnotation, index, descriptor, typeRef, typePath);
	}

	private @NotNull AnnotationNode installClassAnnotation(boolean visible, boolean typeAnnotation, int index,
	                                                      @NotNull String descriptor,
	                                                      @Nullable me.darknet.assembler.ast.primitive.ASTNumber typeRef,
	                                                      @Nullable ASTIdentifier typePath) {
		List<? extends AnnotationNode> list = listForClass(visible, typeAnnotation);
		AnnotationNode annotation = typeAnnotation
				? new TypeAnnotationNode(typeRef == null ? 0 : typeRef.asInt(), parseTypePath(typePath), descriptor)
				: new AnnotationNode(descriptor);
		insert(list, index, annotation);
		return annotation;
	}

	private @NotNull AnnotationNode installFieldAnnotation(@NotNull String name, @NotNull String descriptor,
	                                                      boolean visible, boolean typeAnnotation, int index,
	                                                      @NotNull String annotationDescriptor,
	                                                      @Nullable me.darknet.assembler.ast.primitive.ASTNumber typeRef,
	                                                      @Nullable ASTIdentifier typePath) {
		FieldNode field = builder.field(name, descriptor);
		if (field == null)
			throw new IllegalStateException("Unexpected missing field data: " + name);
		if (typeAnnotation) {
			if (visible) {
				if (field.visibleTypeAnnotations == null) field.visibleTypeAnnotations = new ArrayList<>();
				TypeAnnotationNode annotation = new TypeAnnotationNode(typeRef == null ? 0 : typeRef.asInt(),
						parseTypePath(typePath), annotationDescriptor);
				insert(field.visibleTypeAnnotations, index, annotation);
				return annotation;
			}
			if (field.invisibleTypeAnnotations == null) field.invisibleTypeAnnotations = new ArrayList<>();
			TypeAnnotationNode annotation = new TypeAnnotationNode(typeRef == null ? 0 : typeRef.asInt(),
					parseTypePath(typePath), annotationDescriptor);
			insert(field.invisibleTypeAnnotations, index, annotation);
			return annotation;
		}
		List<AnnotationNode> annotations = visible ? field.visibleAnnotations : field.invisibleAnnotations;
		if (annotations == null)
			annotations = new ArrayList<>();
		AnnotationNode annotation = new AnnotationNode(annotationDescriptor);
		insert(annotations, index, annotation);
		if (visible) field.visibleAnnotations = annotations;
		else field.invisibleAnnotations = annotations;
		return annotation;
	}

	private @NotNull AnnotationNode installMethodAnnotation(@NotNull String name, @NotNull String descriptor,
	                                                       boolean visible, boolean typeAnnotation, int index,
	                                                       @NotNull String annotationDescriptor,
	                                                       @Nullable me.darknet.assembler.ast.primitive.ASTNumber typeRef,
	                                                       @Nullable ASTIdentifier typePath) {
		MethodNode method = builder.method(name, descriptor);
		if (method == null)
			throw new IllegalStateException("Unexpected missing method data: " + name);
		builder.markMethodModified(name, descriptor);
		if (typeAnnotation) {
			if (visible) {
				if (method.visibleTypeAnnotations == null) method.visibleTypeAnnotations = new ArrayList<>();
				TypeAnnotationNode annotation = new TypeAnnotationNode(typeRef == null ? 0 : typeRef.asInt(),
						parseTypePath(typePath), annotationDescriptor);
				insert(method.visibleTypeAnnotations, index, annotation);
				return annotation;
			}
			if (method.invisibleTypeAnnotations == null) method.invisibleTypeAnnotations = new ArrayList<>();
			TypeAnnotationNode annotation = new TypeAnnotationNode(typeRef == null ? 0 : typeRef.asInt(),
					parseTypePath(typePath), annotationDescriptor);
			insert(method.invisibleTypeAnnotations, index, annotation);
			return annotation;
		}
		List<AnnotationNode> annotations = visible ? method.visibleAnnotations : method.invisibleAnnotations;
		if (annotations == null)
			annotations = new ArrayList<>();
		AnnotationNode annotation = new AnnotationNode(annotationDescriptor);
		insert(annotations, index, annotation);
		if (visible) method.visibleAnnotations = annotations;
		else method.invisibleAnnotations = annotations;
		return annotation;
	}

	private @NotNull List<? extends AnnotationNode> listForClass(boolean visible, boolean typeAnnotation) {
		if (typeAnnotation) {
			if (visible) {
				if (builder.node().visibleTypeAnnotations == null) builder.node().visibleTypeAnnotations = new ArrayList<>();
				return builder.node().visibleTypeAnnotations;
			}
			if (builder.node().invisibleTypeAnnotations == null) builder.node().invisibleTypeAnnotations = new ArrayList<>();
			return builder.node().invisibleTypeAnnotations;
		}
		if (visible) {
			if (builder.node().visibleAnnotations == null) builder.node().visibleAnnotations = new ArrayList<>();
			return builder.node().visibleAnnotations;
		}
		if (builder.node().invisibleAnnotations == null) builder.node().invisibleAnnotations = new ArrayList<>();
		return builder.node().invisibleAnnotations;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void insert(@Nullable List<? extends AnnotationNode> list, int index, @NotNull AnnotationNode annotation) {
		if (list == null)
			throw new IllegalStateException("Annotation list was not initialized");
		List raw = list;
		if (index >= 0 && index < raw.size()) raw.set(index, annotation);
		else raw.add(annotation);
	}

	private static boolean visibleAnnotationKind(@Nullable String name) {
		if (name == null || "VIS_ANNO".equalsIgnoreCase(name) || "vis-a".equalsIgnoreCase(name))
			return true;
		if ("VIS_TYPE_ANNO".equalsIgnoreCase(name) || "vis-type-a".equalsIgnoreCase(name)
				|| "INVIS_ANNO".equalsIgnoreCase(name) || "invis-a".equalsIgnoreCase(name)
				|| "INVIS_TYPE_ANNO".equalsIgnoreCase(name) || "invis-type-a".equalsIgnoreCase(name))
			return false;
		return true;
	}

	private static boolean typeAnnotationKind(@Nullable String name) {
		return "VIS_TYPE_ANNO".equalsIgnoreCase(name) || "vis-type-a".equalsIgnoreCase(name)
				|| "INVIS_TYPE_ANNO".equalsIgnoreCase(name) || "invis-type-a".equalsIgnoreCase(name);
	}

	private static @Nullable org.objectweb.asm.TypePath parseTypePath(@Nullable ASTIdentifier typePath) {
		if (typePath == null)
			return null;
		String content = typePath.content();
		return "_".equals(content) ? null : org.objectweb.asm.TypePath.fromString(content);
	}
}

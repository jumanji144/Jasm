package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.builder.JvmClassBuilder;
import me.darknet.assembler.util.JvmModifiers;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTClassVisitor;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.assembler.visitor.ASTRecordComponentVisitor;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;

public class JvmClassVisitor implements ASTClassVisitor {
	private final JvmClassBuilder builder;
	private final JvmCompilerOptions options;

	public JvmClassVisitor(JvmCompilerOptions options, JvmClassBuilder builder) {
		this.options = options;
		this.builder = builder;
	}

	@Override
	public void visitSuperClass(@Nullable ASTIdentifier superClass) {
		if (superClass != null) {
			builder.setSuperClass(superClass.literal());
		}
	}

	@Override
	public void visitInterface(@NotNull ASTIdentifier interfaceName) {
		builder.addInterface(interfaceName.literal());
	}

	@Override
	public void visitSourceFile(@Nullable ASTString sourceFile) {
		builder.setSourceFile(sourceFile == null ? null : sourceFile.content());
	}

	@Override
	public void visitSourceDebugExtension(@Nullable ASTString sourceDebugExtension) {
		builder.setSourceDebugExtension(sourceDebugExtension == null ? null : sourceDebugExtension.content());
	}

	@Override
	public void visitOuterClass(@Nullable ASTElement outerClass) {
		builder.setOuterClass(outerClass == null ? null : outerClass.content());
	}

	@Override
	public void visitOuterMethod(@Nullable ASTOuterMethod outerMethod) {
		if (outerMethod == null) {
			return;
		}
		String outerClass = builder.getOuterClass();
		if (outerClass == null) {
			throw new IllegalStateException("Must visit outer class attribute before visiting outer method");
		}
		builder.setOuterMethod(outerClass, outerMethod.getMethodName().content(), outerMethod.getMethodDesc().content());
	}

	@Override
	public void visitPermittedSubclass(@NotNull ASTIdentifier subclass) {
		builder.addPermittedSubclass(subclass.literal());
	}

	@Override
	public void visitNestHost(@Nullable ASTIdentifier nestHost) {
		builder.setNestHost(nestHost == null ? null : nestHost.literal());
	}

	@Override
	public void visitNestMember(@NotNull ASTIdentifier nestMember) {
		builder.addNestMember(nestMember.literal());
	}

	@Override
	public ASTRecordComponentVisitor visitRecordComponent(@NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor,
	                                                      @Nullable ASTString signature) {
		return new JvmRecordComponentVisitor(
				builder.putRecordComponent(name.literal(), descriptor.literal(), signature == null ? null : signature.content())
		);
	}

	@Override
	public void visitInnerClass(@NotNull Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass,
	                            @Nullable ASTIdentifier innerClass) {
		builder.addInnerClass(new InnerClassNode(
				innerClass == null ? null : innerClass.literal(),
				outerClass == null ? null : outerClass.literal(),
				name == null ? null : name.literal(),
				JvmModifiers.getClassModifiers(modifiers)
		));
	}

	@Override
	public ASTFieldVisitor visitField(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name,
	                                  @NotNull ASTIdentifier descriptor) {
		return new JvmFieldVisitor(builder.putField(JvmModifiers.getFieldModifiers(modifiers), name.literal(), descriptor.literal()));
	}

	@Override
	public ASTMethodVisitor visitMethod(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name,
	                                    @NotNull ASTIdentifier descriptor) {
		int accessFlags = JvmModifiers.getMethodModifiers(modifiers);
		var existingMethod = builder.method(name.literal(), descriptor.literal());
		boolean hadPriorLocalVariables = existingMethod != null
				&& existingMethod.localVariables != null
				&& !existingMethod.localVariables.isEmpty();
		return new JvmMethodVisitor(
				options,
				Type.getObjectType(builder.type()),
				Type.getMethodType(descriptor.literal()),
				builder.putMethod(accessFlags, name.literal(), descriptor.literal()),
				hadPriorLocalVariables,
				analysisResults -> builder.setMethodAnalysis(name.literal(), descriptor.literal(), analysisResults)
		);
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

	@Override
	public void visitSignature(@Nullable ASTString signature) {
		if (signature != null) {
			builder.signature(signature.content());
		}
	}

	@Override
	public void visitDeprecated() {
		builder.node().access |= Opcodes.ACC_DEPRECATED;
	}

	@Override
	public void visitEnd() {
	}

	private @NotNull AnnotationNode addRuntimeAnnotation(boolean visible, @NotNull String internalName) {
		AnnotationNode annotation = new AnnotationNode(Type.getObjectType(internalName).getDescriptor());
		if (visible) {
			if (builder.node().visibleAnnotations == null) {
				builder.node().visibleAnnotations = new ArrayList<>();
			}
			builder.node().visibleAnnotations.add(annotation);
		} else {
			if (builder.node().invisibleAnnotations == null) {
				builder.node().invisibleAnnotations = new ArrayList<>();
			}
			builder.node().invisibleAnnotations.add(annotation);
		}
		return annotation;
	}

	private @NotNull TypeAnnotationNode addRuntimeTypeAnnotation(boolean visible, @NotNull String internalName,
	                                                             int typeRef, @Nullable ASTIdentifier typePath) {
		TypeAnnotationNode annotation = new TypeAnnotationNode(typeRef,
				parseTypePath(typePath),
				Type.getObjectType(internalName).getDescriptor());
		if (visible) {
			if (builder.node().visibleTypeAnnotations == null) {
				builder.node().visibleTypeAnnotations = new ArrayList<>();
			}
			builder.node().visibleTypeAnnotations.add(annotation);
		} else {
			if (builder.node().invisibleTypeAnnotations == null) {
				builder.node().invisibleTypeAnnotations = new ArrayList<>();
			}
			builder.node().invisibleTypeAnnotations.add(annotation);
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

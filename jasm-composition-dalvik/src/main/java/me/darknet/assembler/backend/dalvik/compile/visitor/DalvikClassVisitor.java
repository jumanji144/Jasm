package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.processing.ProcessedMethod;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTClassVisitor;
import me.darknet.assembler.visitor.ASTFieldVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.assembler.visitor.ASTRecordComponentVisitor;
import me.darknet.assembler.visitor.Modifiers;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.FieldMember;
import me.darknet.dex.tree.definitions.InnerClass;
import me.darknet.dex.tree.definitions.MemberIdentifier;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.RecordComponent;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationProcessing;
import me.darknet.dex.tree.type.ClassType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.Type;
import me.darknet.dex.tree.type.TypeParser;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class DalvikClassVisitor implements ASTClassVisitor {
	private final ClassDefinition definition;
	private final DiagnosticSink sink;

	public DalvikClassVisitor(ClassDefinition definition, DiagnosticSink sink) {
		this.definition = definition;
		this.sink = sink;
	}

	public ClassDefinition definition() {
		return definition;
	}

	public DiagnosticSink sink() {
		return sink;
	}

	@Override
	public void visitSuperClass(@Nullable ASTIdentifier superClass) {
		definition.setSuperClass(superClass == null ? null : Types.instanceTypeFromInternalName(superClass.literal()));
	}

	@Override
	public void visitInterface(@NotNull ASTIdentifier interfaceName) {
		definition.addInterface(Types.instanceTypeFromInternalName(interfaceName.literal()));
	}

	@Override
	public void visitSourceFile(@Nullable ASTString sourceFile) {
		definition.setSourceFile(sourceFile == null ? null : sourceFile.content());
	}

	@Override
	public void visitOuterClass(@Nullable ASTElement outerClass) {
		if (outerClass == null) {
			definition.setEnclosingClass(null);
			return;
		}
		if (!(outerClass instanceof ASTIdentifier identifier))
			throw new IllegalStateException("Expected an outer class identifier");
		definition.setEnclosingClass(Types.instanceTypeFromInternalName(identifier.literal()));
	}

	@Override
	public void visitOuterMethod(@Nullable ASTOuterMethod outerMethod) {
		if (outerMethod == null) {
			definition.setEnclosingMethod(null);
			return;
		}
		definition.setEnclosingMethod(new MemberIdentifier(
				outerMethod.getMethodName().literal(),
				Types.methodTypeFromDescriptor(outerMethod.getMethodDesc().literal())
		));
	}

	@Override
	public void visitPermittedSubclass(@NotNull ASTIdentifier subclass) {
		definition.addPermittedSubclass(Types.instanceTypeFromInternalName(subclass.literal()));
	}

	@Override
	public void visitNestHost(@Nullable ASTIdentifier nestHost) {
		definition.setNestHost(nestHost == null ? null : Types.instanceTypeFromInternalName(nestHost.literal()));
	}

	@Override
	public void visitNestMember(@NotNull ASTIdentifier nestMember) {
		definition.addNestMember(Types.instanceTypeFromInternalName(nestMember.literal()));
	}

	@Override
	public ASTRecordComponentVisitor visitRecordComponent(@NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor, @Nullable ASTString signature) {
		ClassType type = new TypeParser(descriptor.literal()).requireClassType();
		List<Annotation> annotations = new ArrayList<>();
		definition.addRecordComponent(new RecordComponent(name.literal(), type,
				signature == null ? null : signature.content(), annotations));
		return new DalvikRecordComponentVisitor(annotations);
	}

	@Override
	public void visitInnerClass(@NotNull Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass, @Nullable ASTIdentifier innerClass) {
		if (innerClass == null || outerClass == null) {
			ASTIdentifier located = innerClass != null ? innerClass : outerClass != null ? outerClass : name;
			sink.error(DiagnosticCode.MALFORMED_DECLARATION,
					"Dalvik inner classes require inner and outer class identifiers",
					located == null ? null : located.location());
			return;
		}
		definition.addInnerClass(new InnerClass(
				innerClass.literal(),
				outerClass.literal(),
				name == null ? null : name.literal(),
				DalvikModifiers.getClassModifiers(modifiers)
		));
	}

	@Override
	public ASTFieldVisitor visitField(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor) {
		ClassType type = new TypeParser(descriptor.literal()).requireClassType();
		FieldMember member = new FieldMember(name.literal(), type, DalvikModifiers.getFieldModifiers(modifiers));
		definition.putField(member);
		return new DalvikFieldVisitor(member);
	}

	@Override
	public ASTMethodVisitor visitMethod(@NotNull ASTMethod source, @NotNull ProcessedMethod processed) {
		Modifiers modifiers = source.getModifiers();
		ASTIdentifier name = source.getName();
		ASTIdentifier descriptor = source.getDescriptor();
		Type parsedType = new TypeParser(descriptor.literal()).required();
		if (!(parsedType instanceof MethodType methodType)) {
			throw new IllegalStateException("Expected method descriptor: " + descriptor.literal());
		}

		MethodMember member = new MethodMember(name.literal(), methodType, DalvikModifiers.getMethodModifiers(modifiers));
		definition.putMethod(member);
		return new DalvikMethodVisitor(member, processed, sink);
	}

	@Override
	public void visitSignature(@Nullable ASTString signature) {
		definition.setSignature(signature == null ? null : signature.content());
	}

	@Override
	public void visitEnd() {
		// no-op
	}

	@Override
	public ASTAnnotationVisitor visitAnnotation(@NotNull AnnotationVisibility visibility, @NotNull ASTIdentifier classType) {
		byte mapped = switch (visibility) {
			case VISIBLE -> DalvikAnnotationVisitor.RUNTIME;
			case INVISIBLE -> DalvikAnnotationVisitor.BUILD;
			case SYSTEM -> DalvikAnnotationVisitor.SYSTEM;
		};
		return new DalvikAnnotationVisitor(mapped, classType, this::acceptAnnotation);
	}

	@Override
	public ASTAnnotationVisitor visitTypeAnnotation(@NotNull AnnotationVisibility visibility,
	                                                @NotNull ASTIdentifier classType,
	                                                @NotNull ASTNumber typeRef,
	                                                @Nullable ASTIdentifier typePath) {
		throw new IllegalStateException("Dalvik type annotations are not supported by the current dex tree");
	}

	private void acceptAnnotation(Annotation annotation) {
		if (annotation.visibility() == Annotation.VISIBILITY_SYSTEM) {
			switch (AnnotationProcessing.processAttribute(java.util.Map.of(), definition, annotation.annotation())) {
				case CONSUMED -> {
					return;
				}
				case ERROR -> throw new IllegalStateException("Invalid class annotation: " + annotation.annotation().type().internalName());
				case PRESERVE -> {}
			}
		}
		definition.addAnnotation(annotation);
	}
}

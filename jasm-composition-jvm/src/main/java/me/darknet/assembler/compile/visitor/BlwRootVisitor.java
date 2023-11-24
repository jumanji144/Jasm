package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.annotation.AnnotationBuilder;
import dev.xdark.blw.classfile.*;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.TypeReader;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.builder.BlwReplaceClassBuilder;
import me.darknet.assembler.util.BlwModifiers;
import me.darknet.assembler.util.CastUtil;
import me.darknet.assembler.visitor.*;

public record BlwRootVisitor(BlwReplaceClassBuilder builder, JvmCompilerOptions options) implements ASTRootVisitor {

	@Override
	public ASTAnnotationVisitor visitAnnotation(ASTIdentifier name) {
		// parse annotation path
		var path = options.annotationPath().split("\\.");

		// TODO: blw rewrite removed usage of this index
		//  make some test cases to validate behavior does not change
		int index = Integer.parseInt(path[path.length - 1]);

		InstanceType type = builder.type();

		AnnotationBuilder<?> nested = switch (path.length) {
			case 2 -> builder.putVisibleRuntimeAnnotation(type).child();
			case 5 -> {
				String member = path[3];
				String descriptor = path[4];
				yield switch (path[2]) {
					case "field" -> {
						FieldBuilder<Field, ?> fieldBuilder = builder.getFieldBuilder(member, descriptor);
						if (fieldBuilder != null) yield fieldBuilder.putVisibleRuntimeAnnotation(type).child();
						throw new IllegalStateException("Unexpected missing field data: " + member);
					}
					case "method" -> {
						MethodBuilder<Method, ?> methodBuilder = builder.getMethodBuilder(member, descriptor);
						if (methodBuilder != null) yield methodBuilder.putVisibleRuntimeAnnotation(type).child();
						throw new IllegalStateException("Unexpected missing method data: " + member);
					}
					default -> throw new IllegalStateException("Unexpected value: " + path[2]);
				};
			}
			default -> throw new IllegalStateException("Unexpected value: " + path.length);
		};
		return new BlwAnnotationVisitor(nested);
	}

	@Override
	public ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name) {
		int accessFlags = BlwModifiers.getClassModifiers(modifiers);
		builder.accessFlags(accessFlags);
		builder.type(Types.instanceTypeFromInternalName(name.literal()));
		return new BlwClassVisitor(options, builder);
	}

	@Override
	public ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
		int accessFlags = BlwModifiers.getFieldModifiers(modifiers);
		return new BlwFieldVisitor(
				builder.putField(accessFlags, name.literal(), new TypeReader(descriptor.literal()).requireClassType()).child()
		);
	}

	@Override
	public ASTMethodVisitor visitMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
		int accessFlags = BlwModifiers.getMethodModifiers(modifiers);
		MethodType type = Types.methodType(descriptor.literal());
		return new BlwMethodVisitor(
				options, builder.type(), type,
				(accessFlags & AccessFlag.ACC_STATIC) == AccessFlag.ACC_STATIC,
				CastUtil.cast(builder.putMethod(accessFlags, name.literal(), type).child()),
				analysisResults -> builder.setMethodAnalysis(name.literal(), type, analysisResults)
		);
	}
}

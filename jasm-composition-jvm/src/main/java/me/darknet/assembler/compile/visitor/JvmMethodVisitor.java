package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.compile.MethodVariableLayout;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.error.ErrorCollectionException;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.Pair;
import me.darknet.assembler.util.VarNaming;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class JvmMethodVisitor extends JvmMemberVisitor implements JvmAnnotationElementAdapter, ASTMethodVisitor {
	private final MethodNode method;
	private final JvmCompilerOptions options;
	private final Consumer<AnalysisResults> analysisResultsConsumer;
	private final MethodVariableLayout variableLayout;
	private final Type methodType;
	private final boolean isStatic;

	public JvmMethodVisitor(JvmCompilerOptions options, Type ownerType, Type methodType,
	                        MethodNode method, Consumer<AnalysisResults> analysisResultsConsumer) {
		this.options = options;
		this.methodType = methodType;
		this.isStatic = (method.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
		this.method = method;
		this.analysisResultsConsumer = analysisResultsConsumer;
		this.variableLayout = MethodVariableLayout.fromAst(ownerType, methodType, isStatic);
	}

	@Override
	protected void setSignature(@NotNull String signature) {
		method.signature = signature;
	}

	@Override
	protected void setDeprecated() {
		method.access |= Opcodes.ACC_DEPRECATED;
	}

	@Override
	protected @NotNull AnnotationNode addRuntimeAnnotation(boolean visible, @NotNull String descriptor) {
		AnnotationNode annotation = new AnnotationNode(descriptor);
		if (visible) {
			if (method.visibleAnnotations == null) {
				method.visibleAnnotations = new ArrayList<>();
			}
			method.visibleAnnotations.add(annotation);
		} else {
			if (method.invisibleAnnotations == null) {
				method.invisibleAnnotations = new ArrayList<>();
			}
			method.invisibleAnnotations.add(annotation);
		}
		return annotation;
	}

	@Override
	protected @NotNull TypeAnnotationNode addRuntimeTypeAnnotation(boolean visible, int typeRef,
	                                                               @Nullable TypePath typePath,
	                                                               @NotNull String descriptor) {
		TypeAnnotationNode annotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
		if (visible) {
			if (method.visibleTypeAnnotations == null) {
				method.visibleTypeAnnotations = new ArrayList<>();
			}
			method.visibleTypeAnnotations.add(annotation);
		} else {
			if (method.invisibleTypeAnnotations == null) {
				method.invisibleTypeAnnotations = new ArrayList<>();
			}
			method.invisibleTypeAnnotations.add(annotation);
		}
		return annotation;
	}

	@Override
	public void visitParameter(int index, ASTIdentifier name) {
		variableLayout.setSourceParameterName(index, name.literal());

		ParameterNode methodParameter = variableLayout.createMethodParameter(index);
		if (methodParameter != null) {
			if (method.parameters == null)
				method.parameters = new ArrayList<>();
			method.parameters.add(methodParameter);
		}
	}

	@Override
	public void visitDeclaredException(@NotNull ASTIdentifier exceptionType) {
		if (method.exceptions == null)
			method.exceptions = new ArrayList<>();
		method.exceptions.add(exceptionType.literal());
	}

	@Override
	public void visitAnnotationDefaultValue(ASTElement defaultValue) {
		AnnotationNode wrapper = new AnnotationNode("LAnnotationDefault;");
		ErrorCollector collector = new ErrorCollector();
		ASTAnnotationVisitor.accept(new JvmAnnotationVisitor(wrapper),
				Collections.singleton(new Pair<>(ASTIdentifier.STUB, defaultValue)),
				collector);
		if (collector.hasErr()) {
			throw new ErrorCollectionException("Failed building array element from ast", collector);
		}
		method.annotationDefault = wrapper.values == null || wrapper.values.size() < 2 ? null : wrapper.values.get(1);
	}

	@Override
	public ASTJvmInstructionVisitor visitJvmCode(@NotNull ErrorCollector collector) {
		return new JvmCodeVisitor(options, collector, method, variableLayout.createParameterLocals()) {
			@Override
			public void visitEnd() {
				super.visitEnd();
				if (analysisResultsConsumer != null) {
					analysisResultsConsumer.accept(getAnalysisResults());
				}
			}
		};
	}

	@Override
	public ASTAnnotationVisitor visitVisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType) {
		return new JvmAnnotationVisitor(addParameterAnnotation(true, index, classType.literal()));
	}

	@Override
	public ASTAnnotationVisitor visitInvisibleParameterAnnotation(int index, @NotNull ASTIdentifier classType) {
		return new JvmAnnotationVisitor(addParameterAnnotation(false, index, classType.literal()));
	}

	@Override
	public void visitEnd() {
	}

	private @NotNull AnnotationNode addParameterAnnotation(boolean visible, int parameterIndex, @NotNull String internalName) {
		AnnotationNode annotation = new AnnotationNode(Type.getObjectType(internalName).getDescriptor());
		int parameterCount = Math.max(methodType.getArgumentTypes().length, parameterIndex + 1);
		if (visible) {
			method.visibleParameterAnnotations = ensureParameterAnnotations(method.visibleParameterAnnotations, parameterCount);
			if (method.visibleParameterAnnotations[parameterIndex] == null) {
				method.visibleParameterAnnotations[parameterIndex] = new ArrayList<>();
			}
			method.visibleParameterAnnotations[parameterIndex].add(annotation);
		} else {
			method.invisibleParameterAnnotations = ensureParameterAnnotations(method.invisibleParameterAnnotations, parameterCount);
			if (method.invisibleParameterAnnotations[parameterIndex] == null) {
				method.invisibleParameterAnnotations[parameterIndex] = new ArrayList<>();
			}
			method.invisibleParameterAnnotations[parameterIndex].add(annotation);
		}
		return annotation;
	}

	@SuppressWarnings("unchecked")
	private static List<AnnotationNode>[] ensureParameterAnnotations(@Nullable List<AnnotationNode>[] current, int size) {
		if (current != null && current.length >= size) {
			return current;
		}
		List<AnnotationNode>[] target = new List[size];
		if (current != null) {
			System.arraycopy(current, 0, target, 0, current.length);
		}
		return target;
	}
}

package me.darknet.assembler.backend.jvm.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.backend.jvm.compile.JvmCompilerOptions;
import me.darknet.assembler.backend.jvm.compile.JvmVariableEmissionFilter;
import me.darknet.assembler.backend.jvm.compile.JvmVariableMode;
import me.darknet.assembler.backend.jvm.compile.analysis.AnalysisException;
import me.darknet.assembler.backend.jvm.compile.analysis.AnalysisResults;
import me.darknet.assembler.backend.jvm.compile.analysis.Local;
import me.darknet.assembler.backend.jvm.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.backend.jvm.compile.analysis.VarCache;
import me.darknet.assembler.backend.jvm.instructions.JvmLowering;
import me.darknet.assembler.backend.jvm.instructions.LookupSwitchPayload;
import me.darknet.assembler.backend.jvm.instructions.TableSwitchPayload;
import me.darknet.assembler.backend.jvm.visitor.ASTJvmInstructionVisitor;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.instructions.SemanticInstruction;
import me.darknet.assembler.backend.jvm.util.ConstantMapper;
import me.darknet.assembler.backend.jvm.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Visits JVM instruction syntax and emits ASM instructions to a target method node.
 */
public class JvmCodeVisitor implements ASTJvmInstructionVisitor, Opcodes {
	private final MethodNode method;
	private final DiagnosticSink sink;
	private final Map<String, LabelNode> nameToLabel = new HashMap<>();
	private final List<Local> parameters;
	private final VarCache varCache = new VarCache();
	private final Set<String> definedLabels = new HashSet<>();
	private final Set<AbstractInsnNode> emittedInstructions = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<ASTInstruction, List<String>> referencedLabels = new IdentityHashMap<>();
	private final MethodAnalysisResult analysisResult = new MethodAnalysisResult();
	private final JvmVariableMode variableTableMode;
	private final JvmVariableEmissionFilter variableFilter;
	private final boolean hadPriorLocalVariables;
	private SemanticInstruction currentInstructionAst;
	private int opcode;

	public JvmCodeVisitor(JvmCompilerOptions options, DiagnosticSink sink, MethodNode method,
	                      List<Local> parameters, boolean hadPriorLocalVariables) {
		this.method = method;
		this.sink = sink;
		this.parameters = parameters;
		this.variableTableMode = options.getVariableTableMode();
		this.hadPriorLocalVariables = hadPriorLocalVariables;
		this.variableFilter = options.getVariableFilter();
		parameters.stream().filter(Objects::nonNull).forEach(param -> {
			VarCache.Variable parameterVar = varCache.getOrCreate(param.name(), param.index(), param.size() > 1);
			parameterVar.updateTypeHint(param.type());
		});
	}

	@NotNull
	public AnalysisResults getAnalysisResults() {
		return analysisResult;
	}

	private @NotNull LabelNode getOrCreateLabel(String name) {
		return nameToLabel.computeIfAbsent(name, ignored -> new LabelNode());
	}

	@Override
	public void visitInstruction(@NotNull SemanticInstruction instruction) {
		currentInstructionAst = instruction;
		if (instruction.source() instanceof ASTLabel)
			return;
		if (instruction.lowering() instanceof JvmLowering(int op))
			opcode = op;
		else
			opcode = -1;
	}

	@Override
	public void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end, @NotNull ASTIdentifier handler,
	                           @NotNull ASTIdentifier type) {
		recordLabelReference(currentInstructionAst, start.content());
		recordLabelReference(currentInstructionAst, end.content());
		recordLabelReference(currentInstructionAst, handler.content());
		String typeName = type.literal();
		String internalName = typeName.equals("*") ? null : descriptorToInternalName(typeName);
		method.tryCatchBlocks.add(new TryCatchBlockNode(
				getOrCreateLabel(start.content()),
				getOrCreateLabel(end.content()),
				getOrCreateLabel(handler.content()),
				internalName
		));
	}

	@Override
	public void visitInsn() {
		add(new InsnNode(opcode));
	}

	@Override
	public void visitIntInsn(@NotNull ASTNumber operand) {
		add(new IntInsnNode(opcode, operand.asInt()));
	}

	@Override
	public void visitNewArrayInsn(@NotNull ASTIdentifier type) {
		int operand = switch (type.content()) {
			case "boolean" -> T_BOOLEAN;
			case "char" -> T_CHAR;
			case "float" -> T_FLOAT;
			case "double" -> T_DOUBLE;
			case "byte" -> T_BYTE;
			case "short" -> T_SHORT;
			case "int" -> T_INT;
			case "long" -> T_LONG;
			default -> throw new IllegalStateException("Unexpected value: " + type.content());
		};
		add(new IntInsnNode(NEWARRAY, operand));
	}

	@Override
	public void visitLdcInsn(@NotNull ASTElement constant) {
		add(new LdcInsnNode(ConstantMapper.fromConstant(constant)));
	}

	@Override
	public void visitVarInsn(@NotNull ASTIdentifier var) {
		String name = var.literal();
		boolean wide = opcode == LSTORE || opcode == DSTORE || opcode == LLOAD || opcode == DLOAD;
		int index = varCache.getOrCreate(name, wide);
		var variable = varCache.getFirstByIndex(index);
		if (variable != null) {
			variable.updateTypeHint(switch (opcode) {
				case ILOAD, ISTORE -> Type.INT_TYPE;
				case LLOAD, LSTORE -> Type.LONG_TYPE;
				case FLOAD, FSTORE -> Type.FLOAT_TYPE;
				case DLOAD, DSTORE -> Type.DOUBLE_TYPE;
				case ALOAD, ASTORE -> JvmTypeUtils.OBJECT;
				default -> null;
			});
		}
		add(new VarInsnNode(opcode, index));
	}

	@Override
	public void visitIincInsn(@NotNull ASTIdentifier var, @NotNull ASTNumber increment) {
		int index = varCache.getOrCreate(var.literal(), false);
		var variable = varCache.getFirstByIndex(index);
		if (variable != null) {
			variable.updateTypeHint(Type.INT_TYPE);
		}
		add(new IincInsnNode(index, increment.asInt()));
	}

	@Override
	public void visitJumpInsn(@NotNull ASTIdentifier label) {
		recordLabelReference(currentInstructionAst, label.content());
		add(new JumpInsnNode(opcode, getOrCreateLabel(label.content())));
	}

	@Override
	public void visitTypeInsn(@NotNull ASTIdentifier type) {
		String literal = type.literal();
		if (opcode == NEW) {
			add(new TypeInsnNode(opcode, adaptDescToInternalName("new", literal)));
			return;
		}
		if (opcode == CHECKCAST || opcode == INSTANCEOF || opcode == ANEWARRAY) {
			add(new TypeInsnNode(opcode, adaptDescToInternalNameOrArray(literal)));
			return;
		}
		throw new IllegalStateException("Unexpected value: " + opcode);
	}

	@Override
	public void visitLookupSwitchInsn(@NotNull LookupSwitchPayload payload) {
		recordLabelReference(currentInstructionAst, payload.defaultLabel());
		List<LookupSwitchKey> entries = new ArrayList<>();
		for (Map.Entry<Integer, String> entry : payload.caseLabels().entrySet()) {
			recordLabelReference(currentInstructionAst, entry.getValue());
			entries.add(new LookupSwitchKey(entry.getKey(), getOrCreateLabel(entry.getValue())));
		}
		Collections.sort(entries);
		add(new LookupSwitchInsnNode(
				getOrCreateLabel(payload.defaultLabel()),
				entries.stream().mapToInt(entry -> entry.value).toArray(),
				entries.stream().map(entry -> entry.label).toArray(LabelNode[]::new)
		));
	}

	@Override
	public void visitTableSwitchInsn(@NotNull TableSwitchPayload payload) {
		recordLabelReference(currentInstructionAst, payload.defaultLabel());
		List<LabelNode> labels = new ArrayList<>();
		for (String caseLabel : payload.caseLabels()) {
			recordLabelReference(currentInstructionAst, caseLabel);
			labels.add(getOrCreateLabel(caseLabel));
		}
		add(new TableSwitchInsnNode(payload.min(), payload.min() + labels.size() - 1,
				getOrCreateLabel(payload.defaultLabel()), labels.toArray(LabelNode[]::new)));
	}

	@Override
	public void visitFieldInsn(@NotNull MemberPath path, @NotNull ASTIdentifier descriptor) {
		add(new FieldInsnNode(opcode, path.owner(), path.name(), descriptor.literal()));
	}

	@Override
	public void visitMethodInsn(@NotNull MemberPath path, @NotNull ASTIdentifier descriptor, boolean itf) {
		add(new MethodInsnNode(opcode, path.owner(), path.name(), descriptor.literal(), itf));
	}

	@Override
	public void visitInvokeDynamicInsn(@NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor, @NotNull ASTElement bsm, @NotNull ASTArray bsmArgs) {
		Handle handle;
		if (bsm instanceof ASTIdentifier identifier) {
			handle = Handle.HANDLE_SHORTCUTS.get(identifier.content());
		} else if (bsm instanceof ASTArray array) {
			handle = Handle.from(array);
		} else {
			throw new IllegalStateException("Unexpected value: " + bsm);
		}
		add(new InvokeDynamicInsnNode(
				name.literal(),
				descriptor.literal(),
				ConstantMapper.methodHandleFromHandle(handle),
				bsmArgs.values().stream().filter(Objects::nonNull).map(ConstantMapper::fromConstant).toArray()
		));
	}

	@Override
	public void visitMultiANewArrayInsn(@NotNull ASTIdentifier descriptor, @NotNull ASTNumber numDimensions) {
		String literal = descriptor.literal();
		String actualDescriptor = literal.startsWith("[") ? literal : '[' + asFieldDescriptor(literal);
		if (!literal.startsWith("[")) {
			sink.warning(DiagnosticCode.UNEXPECTED_ELEMENT,
					"Expected array type, got class name", currentInstructionAst.location());
		}
		add(new MultiANewArrayInsnNode(actualDescriptor, numDimensions.asInt()));
	}

	@Override
	public void visitLabel(@NotNull ASTIdentifier label) {
		String labelName = label.content();
		if (definedLabels.add(labelName)) {
			LabelNode node = getOrCreateLabel(labelName);
			add(node);
			// Labels can be dropped from the final class when nothing references
			// them, so record the source mapping here while the node still exists.
			if (currentInstructionAst != null && currentInstructionAst.source() instanceof ASTLabel astLabel)
				analysisResult.recordLabelMapping(astLabel, node);
		} else {
			sink.error(DiagnosticCode.DUPLICATE_LABEL, "Label '" + labelName + "' already defined",
					label.location());
		}
	}

	@Override
	public void visitLineNumber(@NotNull ASTNumber line) {
		LabelNode label = new LabelNode();
		add(label);
		LineNumberNode lineNode = new LineNumberNode(line.asInt(), label);
		add(lineNode);

		// Line markers survive into the final class, but recording here keeps the
		// source mapping next to the same pass that creates the node.
		if (currentInstructionAst != null)
			analysisResult.recordLineNumberMapping(currentInstructionAst.source(), lineNode);
	}

	@Override
	public void visitEnd() {
		validateReferencedLabels();

		// The rest of the logic here is just emitting local variable metadata.
		// If we don't care about that we're done.
		if (variableTableMode == JvmVariableMode.NEVER_WRITE)
			return;
		if (variableTableMode == JvmVariableMode.WRITE_IF_ALREADY_PRESENT && !hadPriorLocalVariables)
			return;

		boolean needsLocalVariableTable = varCache.vars()
				.filter(variable -> variable.getIndex() >= parameters.size())
				.anyMatch(variable -> variable.getTypeHint() != null);
		LabelNode begin = ensureLeadingBoundaryLabel();
		LabelNode end = ensureTrailingBoundaryLabel();
		if (method.localVariables == null)
			method.localVariables = new ArrayList<>();

		for (Local parameter : parameters) {
			if (parameter == null)
				continue;

			int index = parameter.index();
			String name = parameter.name();
			String descriptor = parameter.safeType().getDescriptor();

			if (!variableFilter.canEmit(index, name, Type.getType(descriptor)))
				continue;

			method.localVariables.add(new LocalVariableNode(
					name,
					descriptor,
					null,
					begin,
					end,
					index
			));
			var parameterVar = varCache.getFirstByIndex(index);
			if (parameterVar != null)
				parameterVar.updateFirstAssignedOffset(-1);
		}

		if (needsLocalVariableTable) {
			int parameterSlots = parameters.size();
			varCache.vars()
					.filter(variable -> variable.getIndex() >= parameterSlots)
					.filter(variable -> variable.getTypeHint() != null)
					.filter(variable -> variableFilter.canEmit(variable.getIndex(), variable.getName(), variable.getTypeHint()))
					.forEach(variable -> method.localVariables.add(new LocalVariableNode(
							variable.getName(),
							variable.getTypeHint().getDescriptor(),
							null,
							begin,
							end,
							variable.getIndex()
					)));
		}
	}

	private void add(@NotNull AbstractInsnNode instruction) {
		if (emittedInstructions.add(instruction)) {
			method.instructions.add(instruction);

			// Only record non-metadata instructions for analysis.
			// Inclusion of metadata instructions can cause mismatches in AST <-> bytecode mapping.
			if (currentInstructionAst != null && instruction.getOpcode() >= 0)
				analysisResult.recordExecutableInstruction(currentInstructionAst.source());
		} else {
			sink.error(DiagnosticCode.INVALID_EMISSION,
					"Instruction emitted/visited multiple times: " + instruction.getClass().getSimpleName(),
					currentInstructionAst == null ? null : currentInstructionAst.location());
		}
	}

	private void recordLabelReference(@Nullable SemanticInstruction instruction, @NotNull String labelName) {
		if (instruction == null)
			return;
		referencedLabels.computeIfAbsent(instruction.source(), ignored -> new ArrayList<>()).add(labelName);
	}

	private void validateReferencedLabels() {
		for (Map.Entry<ASTInstruction, List<String>> entry : referencedLabels.entrySet()) {
			for (String labelName : entry.getValue()) {
				if (definedLabels.contains(labelName))
					continue;

				ASTInstruction instruction = entry.getKey();
				analysisResult.setAnalysisFailure(new AnalysisException(
						AnalysisException.FailureKind.INVALID_CONTROL_FLOW,
						"Target for branch instruction does not exist: " + labelName
				));
				sink.error(DiagnosticCode.MISSING_LABEL, "Target for branch instruction does not exist: " + labelName, instruction.location());
				return;
			}
		}
	}

	/**
	 * For lots of analysis, you'll encounter problems if you have stuff like this:
	 * <pre>{@code
	 *  ldc "foo"
	 *  astore x // Written to before any label
	 * A:
	 *  aload x
	 *  areturn
	 * B:
	 * }</pre>
	 * This ensures that doesn't happen by emitting a leading label if none exist.
	 *
	 * @return Label at start of the method.
	 */
	private @NotNull LabelNode ensureLeadingBoundaryLabel() {
		if (method.instructions.getFirst() instanceof LabelNode label)
			return label;

		LabelNode label = new LabelNode();
		if (method.instructions.getFirst() == null) {
			add(label);
		} else {
			method.instructions.insert(label);
		}
		return label;
	}

	/**
	 * For lots of analysis, you'll encounter problems if you have stuff like this:
	 * <pre>{@code
	 * A:
	 *  aload x
	 *  return
	 *  // No end label
	 * }</pre>
	 * This ensures that doesn't happen by emitting a trailing label if none exist.
	 *
	 * @return Label at end of the method.
	 */
	private @NotNull LabelNode ensureTrailingBoundaryLabel() {
		if (method.instructions.getLast() instanceof LabelNode label)
			return label;

		LabelNode label = new LabelNode();
		add(label);
		return label;
	}

	private static @NotNull String adaptDescToInternalName(@NotNull String op, @NotNull String desc) {
		char first = desc.charAt(0);
		if (first == 'L' && desc.charAt(desc.length() - 1) == ';')
			return desc.substring(1, desc.length() - 1);

		if (first == '[')
			throw new IllegalStateException("Cannot use '" + op + "' to allocate an array type");

		return desc;
	}

	private static @NotNull String adaptDescToInternalNameOrArray(@NotNull String desc) {
		if (desc.charAt(0) == 'L' && desc.charAt(desc.length() - 1) == ';') {
			return desc.substring(1, desc.length() - 1);
		}
		return desc;
	}

	private static @NotNull String descriptorToInternalName(@NotNull String descriptor) {
		if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
			return Type.getType(descriptor).getInternalName();
		}
		return descriptor;
	}

	private static @NotNull String asFieldDescriptor(@NotNull String literal) {
		if (literal.isEmpty()) {
			throw new IllegalStateException("Descriptor must not be empty");
		}
		char first = literal.charAt(0);
		if ("VZCBSIFJD[".indexOf(first) >= 0) {
			return literal;
		}
		if (first == 'L' && literal.charAt(literal.length() - 1) == ';') {
			return literal;
		}
		return 'L' + literal + ';';
	}

	private record LookupSwitchKey(int value, LabelNode label) implements Comparable<LookupSwitchKey> {
		@Override
		public int compareTo(@NotNull LookupSwitchKey other) {
			return Integer.compare(value, other.value);
		}
	}
}

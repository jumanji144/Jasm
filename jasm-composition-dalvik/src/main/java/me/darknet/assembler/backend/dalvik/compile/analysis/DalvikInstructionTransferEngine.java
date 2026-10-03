package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.FieldReference;
import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.analysis.registry.FieldValueLookup;
import me.darknet.assembler.analysis.registry.MethodValueLookup;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.backend.dalvik.util.DalvikTypeUtils;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.DescriptorParser;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.assembler.descriptor.Descriptors;
import me.darknet.assembler.descriptor.MethodDescriptor;
import me.darknet.assembler.descriptor.PrimitiveType;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.instructions.ArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.ArrayLengthInstruction;
import me.darknet.dex.tree.definitions.instructions.Binary2AddrInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryLiteralInstruction;
import me.darknet.dex.tree.definitions.instructions.BranchInstruction;
import me.darknet.dex.tree.definitions.instructions.BranchZeroInstruction;
import me.darknet.dex.tree.definitions.instructions.CheckCastInstruction;
import me.darknet.dex.tree.definitions.instructions.CompareInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodHandleInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstStringInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstWideInstruction;
import me.darknet.dex.tree.definitions.instructions.FillArrayDataInstruction;
import me.darknet.dex.tree.definitions.instructions.FilledNewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.GotoInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceOfInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.definitions.instructions.MonitorInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveExceptionInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveObjectInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveResultInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveWideInstruction;
import me.darknet.dex.tree.definitions.instructions.NewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.NewInstanceInstruction;
import me.darknet.dex.tree.definitions.instructions.NopInstruction;
import me.darknet.dex.tree.definitions.instructions.PackedSwitchInstruction;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import me.darknet.dex.tree.definitions.instructions.SparseSwitchInstruction;
import me.darknet.dex.tree.definitions.instructions.StaticFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.ThrowInstruction;
import me.darknet.dex.tree.definitions.instructions.UnaryInstruction;
import me.darknet.dex.tree.simulation.ExecutionEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static me.darknet.assembler.descriptor.PrimitiveType.*;

/**
 * Engine for transferring Dalvik register states across individual instructions.
 */
final class DalvikInstructionTransferEngine implements ExecutionEngine {
	private final InheritanceChecker inheritanceChecker;
	private final MethodValueLookup methodValueLookup;
	private final FieldValueLookup fieldValueLookup;
	private final Map<Instruction, ASTInstruction> instructionToSource;
	private final FailureRecorder failureRecorder;
	private int instructionIndex;
	private Instruction instruction;
	private DalvikAnalysisFrame input;
	private DalvikAnalysisFrame output;
	private DalvikAnalysisResults.TerminalState terminalState;

	DalvikInstructionTransferEngine(@NotNull InheritanceChecker inheritanceChecker,
	                                @NotNull MethodValueLookup methodValueLookup,
	                                @NotNull FieldValueLookup fieldValueLookup,
	                                @NotNull Map<Instruction, ASTInstruction> instructionToSource,
	                                @NotNull FailureRecorder failureRecorder) {
		this.inheritanceChecker = inheritanceChecker;
		this.methodValueLookup = methodValueLookup;
		this.fieldValueLookup = fieldValueLookup;
		this.instructionToSource = instructionToSource;
		this.failureRecorder = failureRecorder;
	}

	@Override
	public void label(@NotNull Label label) {}

	@Override
	public void execute(@NotNull ArrayInstruction instruction) {
		int opcode = instruction.opcode();
		if (isArrayPut(opcode))
			return;
		if (!isArrayGet(opcode)) {
			failDestination(instruction.value(), opcode == Opcodes.AGET_WIDE,
					"unrecognized array opcode 0x" + Integer.toHexString(opcode));
			return;
		}
		if (opcode == Opcodes.AGET_WIDE) {
			Value array = arrayValue(instruction.array());
			if (!(array instanceof Value.ArrayValue arrayValue)
					|| !(arrayValue.arrayType().component() == LONG
					|| arrayValue.arrayType().component() == DOUBLE)) {
				failDestination(instruction.value(), true, "aget-wide element category is unknown");
				return;
			}
			write(instruction.value(), Values.valueOfPrimitive((PrimitiveType) arrayValue.arrayType().component()));
			return;
		}
		if (opcode == Opcodes.AGET_OBJECT) {
			Value array = arrayValue(instruction.array());
			if (array instanceof Value.ArrayValue arrayValue) {
				DescriptorType component = arrayValue.arrayType().component();
				if (component instanceof ClassDescriptor || component instanceof ArrayDescriptor) {
					writeDescriptorValue(instruction.value(), component);
					return;
				}
			}
			write(instruction.value(), Values.OBJECT_VALUE);
			return;
		}
		if (opcode == Opcodes.AGET || opcode == Opcodes.AGET_BOOLEAN || opcode == Opcodes.AGET_BYTE
				|| opcode == Opcodes.AGET_CHAR || opcode == Opcodes.AGET_SHORT) {
			write(instruction.value(), Values.INT_VALUE);
			return;
		}
		failDestination(instruction.value(), false, "unrecognized array get opcode 0x" + Integer.toHexString(opcode));
	}

	@Override
	public void execute(@NotNull ArrayLengthInstruction instruction) {
		Value array = readReference(instruction.array(), instruction.dest(), false);
		if (array == null)
			return;
		if (array instanceof Value.KnownLengthArrayValue known)
			write(instruction.dest(), Values.valueOf(known.length()));
		else
			write(instruction.dest(), Values.INT_VALUE);
	}

	@Override
	public void execute(@NotNull Binary2AddrInstruction instruction) {
		transferBinary(instruction.opcode(), instruction.a(), instruction.a(), instruction.b(), null);
	}

	@Override
	public void execute(@NotNull BinaryInstruction instruction) {
		transferBinary(instruction.opcode(), instruction.dest(), instruction.a(), instruction.b(), null);
	}

	@Override
	public void execute(@NotNull BinaryLiteralInstruction instruction) {
		transferBinary(instruction.opcode(), instruction.dest(), instruction.src(), -1, instruction.constant());
	}

	@Override
	public void execute(@NotNull BranchInstruction instruction) {}

	@Override
	public void execute(@NotNull BranchZeroInstruction instruction) {}

	@Override
	public void execute(@NotNull CheckCastInstruction instruction) {
		int register = instruction.register();
		Value reference = readReference(register, register, false);
		if (reference == null)
			return;
		if (reference instanceof Value.NullValue || reference instanceof Value.UninitializedReferenceValue) {
			write(register, reference);
			return;
		}
		try {
			writeDescriptorValue(register, DescriptorParser.parseFieldDescriptor(instruction.type().descriptor()));
		} catch (RuntimeException failure) {
			failDestination(register, false, "invalid check-cast type: " + failure.getMessage());
		}
	}

	@Override
	public void execute(@NotNull CompareInstruction instruction) {
		int opcode = instruction.opcode();
		PrimitiveType operandType = switch (opcode) {
			case Opcodes.CMP_LONG -> LONG;
			case Opcodes.CMPL_FLOAT, Opcodes.CMPG_FLOAT -> FLOAT;
			case Opcodes.CMPL_DOUBLE, Opcodes.CMPG_DOUBLE -> DOUBLE;
			default -> null;
		};
		if (operandType == null) {
			failDestination(instruction.dest(), false, "unrecognized comparison opcode 0x" + Integer.toHexString(opcode));
			return;
		}
		Value left = readTyped(instruction.a(), operandType, instruction.dest(), false);
		if (left == null)
			return;
		Value right = readTyped(instruction.b(), operandType, instruction.dest(), false);
		if (right == null)
			return;
		if (!left.isKnown() || !right.isKnown()) {
			write(instruction.dest(), Values.INT_VALUE);
			return;
		}
		int compared;
		if (operandType == LONG) {
			compared = Long.compare(((Value.KnownLongValue) left).value(), ((Value.KnownLongValue) right).value());
		} else if (operandType == FLOAT) {
			float a = ((Value.KnownFloatValue) left).value();
			float b = ((Value.KnownFloatValue) right).value();
			compared = Float.isNaN(a) || Float.isNaN(b)
					? (opcode == Opcodes.CMPL_FLOAT ? -1 : 1)
					: Float.compare(a, b);
		} else {
			double a = ((Value.KnownDoubleValue) left).value();
			double b = ((Value.KnownDoubleValue) right).value();
			compared = Double.isNaN(a) || Double.isNaN(b)
					? (opcode == Opcodes.CMPL_DOUBLE ? -1 : 1)
					: Double.compare(a, b);
		}
		write(instruction.dest(), Values.valueOf(compared));
	}

	@Override
	public void execute(@NotNull ConstInstruction instruction) {
		ASTElement literal = sourceLiteral();
		if (literal instanceof ASTNumber number) {
			try {
				if (number.isFloatingPoint())
					write(instruction.register(), new Value.KnownFloatValue(Float.intBitsToFloat(instruction.value())));
				else if (instruction.value() == 0)
					write(instruction.register(), DalvikZeroValue.INSTANCE);
				else
					write(instruction.register(), Values.valueOf(instruction.value()));
			} catch (RuntimeException failure) {
				failDestination(instruction.register(), false, "invalid const literal: " + failure.getMessage());
			}
			return;
		}
		if (literal instanceof ASTIdentifier) {
			write(instruction.register(), new Value.KnownFloatValue(Float.intBitsToFloat(instruction.value())));
			return;
		}
		failDestination(instruction.register(), false, "const has no compatible source literal");
	}

	@Override
	public void execute(@NotNull ConstTypeInstruction instruction) {
		write(instruction.register(), Values.valueOfInstance(new ClassDescriptor("java/lang/Class")));
	}

	@Override
	public void execute(@NotNull ConstWideInstruction instruction) {
		ASTElement literal = sourceLiteral();
		if (literal instanceof ASTNumber number) {
			try {
				if (number.isFloatingPoint())
					write(instruction.register(), new Value.KnownDoubleValue(Double.longBitsToDouble(instruction.value())));
				else
					write(instruction.register(), Values.valueOf(instruction.value()));
			} catch (RuntimeException failure) {
				failDestination(instruction.register(), true, "invalid const-wide literal: " + failure.getMessage());
			}
			return;
		}
		if (literal instanceof ASTIdentifier) {
			write(instruction.register(), new Value.KnownDoubleValue(Double.longBitsToDouble(instruction.value())));
			return;
		}
		failDestination(instruction.register(), true, "const-wide has no compatible source literal");
	}

	private @Nullable ASTElement sourceLiteral() {
		ASTInstruction source = instructionToSource.get(instruction);
		if (source == null || source.arguments().size() < 2)
			return null;
		return source.arguments().get(1);
	}

	@Override
	public void execute(@NotNull ConstStringInstruction instruction) {
		write(instruction.register(), Values.valueOfString(instruction.string()));
	}

	@Override
	public void execute(@NotNull ConstMethodHandleInstruction instruction) {
		write(instruction.destination(), Values.valueOfInstance(new ClassDescriptor("java/lang/invoke/MethodHandle")));
	}

	@Override
	public void execute(@NotNull ConstMethodTypeInstruction instruction) {
		write(instruction.destination(), Values.valueOfInstance(new ClassDescriptor("java/lang/invoke/MethodType")));
	}

	@Override
	public void execute(@NotNull FillArrayDataInstruction instruction) {}

	@Override
	public void execute(@NotNull FilledNewArrayInstruction instruction) {
		try {
			DescriptorType descriptor = DescriptorParser.parseFieldDescriptor(instruction.componentType().descriptor());
			if (!(descriptor instanceof ArrayDescriptor arrayType))
				throw new IllegalArgumentException("expected an array descriptor");

			int length;
			if (instruction.isRange()) {
				length = instruction.last() - instruction.first() + 1;
			} else {
				int[] registers = instruction.registers();
				if (registers == null)
					throw new IllegalArgumentException("register list is missing");
				length = registers.length;
			}

			if (length < 0)
				throw new IllegalArgumentException("negative array length");

			output.setPendingResult(Values.valueOfArray(arrayType, length));
		} catch (RuntimeException failure) {
			output.clearPendingResult();
			unsupported("invalid filled-new-array type: " + failure.getMessage());
		}
	}

	@Override
	public void execute(@NotNull GotoInstruction instruction) {}

	@Override
	public void execute(@NotNull InstanceFieldInstruction instruction) {
		transferField(instruction.opcode(), instruction.value(), instruction.owner().internalName(),
				instruction.name(), instruction.type().descriptor(), instruction.instance());
	}

	@Override
	public void execute(@NotNull InstanceOfInstruction instruction) {
		int destination = instruction.destination();
		Value reference = readReference(instruction.register(), destination, false);
		if (reference == null)
			return;
		write(destination, reference instanceof Value.NullValue ? Values.INT_0 : Values.INT_VALUE);
	}

	@Override
	public void execute(@NotNull InvokeCustomInstruction instruction) {
		produceInvokeResult(instruction.type().descriptor());
	}

	@Override
	public void execute(@NotNull InvokeInstruction instruction) {
		produceInvokeResult(instruction);
	}

	@Override
	public void execute(@NotNull MonitorInstruction instruction) {}

	@Override
	public void execute(@NotNull MoveExceptionInstruction instruction) {
		Value exception = input.pendingException();
		output.clearPendingException();
		if (exception instanceof Value.ObjectValue) {
			write(instruction.register(), exception);
			return;
		}
		failDestination(instruction.register(), false,
				exception == null ? "move-exception has no pending caught exception" : "move-exception value is not a reference");
	}

	@Override
	public void execute(@NotNull MoveInstruction instruction) {
		Value value = readSingle(instruction.from(), instruction.to(), false);
		if (value != null)
			write(instruction.to(), value);
	}

	@Override
	public void execute(@NotNull MoveObjectInstruction instruction) {
		Value value = readReference(instruction.from(), instruction.to(), false);
		if (value != null)
			write(instruction.to(), value);
	}

	@Override
	public void execute(@NotNull MoveResultInstruction instruction) {
		Value result = input.pendingResult();
		output.clearPendingResult();
		boolean wide = instruction.opcode() == Opcodes.MOVE_RESULT_WIDE;
		boolean compatible = switch (instruction.opcode()) {
			case Opcodes.MOVE_RESULT -> result instanceof Value.PrimitiveValue primitive && !primitive.isWide();
			case Opcodes.MOVE_RESULT_OBJECT -> result instanceof Value.ObjectValue || result instanceof Value.NullValue;
			case Opcodes.MOVE_RESULT_WIDE -> result != null && DalvikAnalysisFrame.isWide(result);
			default -> false;
		};
		if (!compatible) {
			String detail = result == null ? "move-result has no pending result" : "move-result result category does not match opcode";
			failDestination(instruction.to(), wide, detail);
			return;
		}
		write(instruction.to(), result);
	}

	@Override
	public void execute(@NotNull MoveWideInstruction instruction) {
		try {
			output.copyWide(instruction.to(), instruction.from());
		} catch (RuntimeException failure) {
			failDestination(instruction.to(), true, "move-wide has an invalid source or destination pair");
		}
	}

	@Override
	public void execute(@NotNull NewArrayInstruction instruction) {
		int destination = instruction.dest();
		Value size = readTyped(instruction.sizeRegister(), INT, destination, false);
		if (size == null)
			return;
		try {
			DescriptorType descriptor = DescriptorParser.parseFieldDescriptor(instruction.componentType().descriptor());
			if (!(descriptor instanceof ArrayDescriptor arrayType))
				throw new IllegalArgumentException("expected an array descriptor");
			Value array = size instanceof Value.KnownIntValue(int value) && value >= 0
					? Values.valueOfArray(arrayType, value)
					: Values.valueOfArray(arrayType);
			write(destination, array);
		} catch (RuntimeException failure) {
			failDestination(destination, false, "invalid new-array type: " + failure.getMessage());
		}
	}

	@Override
	public void execute(@NotNull NewInstanceInstruction instruction) {
		try {
			ClassDescriptor owner = new ClassDescriptor(instruction.type().internalName());
			write(instruction.dest(), new Value.UninitializedReferenceValue(owner, instructionIndex));
		} catch (RuntimeException failure) {
			failDestination(instruction.dest(), false, "invalid new-instance type: " + failure.getMessage());
		}
	}

	@Override
	public void execute(@NotNull NopInstruction instruction) {}

	@Override
	public void execute(@NotNull PackedSwitchInstruction instruction) {}

	@Override
	public void execute(@NotNull ReturnInstruction instruction) {
		if (instruction.opcode() == Opcodes.RETURN_VOID) {
			terminalState = new DalvikAnalysisResults.TerminalState(DalvikAnalysisResults.TerminalKind.RETURN,
					input.snapshot(), null);
			return;
		}
		int register = instruction.register();
		Value returned;
		if (instruction.opcode() == Opcodes.RETURN_WIDE) {
			returned = readWide(register, register, true);
		} else if (instruction.opcode() == Opcodes.RETURN_OBJECT) {
			returned = readReference(register, register, false);
		} else {
			returned = readSingle(register, register, false);
			if (returned != null && returned != DalvikZeroValue.INSTANCE
					&& (!(returned instanceof Value.PrimitiveValue primitive) || primitive.isWide())) {
				failDestination(register, false, "return requires a single-word primitive value");
				returned = null;
			}
		}
		if (returned != null)
			terminalState = new DalvikAnalysisResults.TerminalState(DalvikAnalysisResults.TerminalKind.RETURN,
					input.snapshot(), returned);
	}

	@Override
	public void execute(@NotNull SparseSwitchInstruction instruction) {}

	@Override
	public void execute(@NotNull StaticFieldInstruction instruction) {
		transferField(instruction.opcode(), instruction.value(), instruction.owner().internalName(),
				instruction.name(), instruction.type().descriptor(), null);
	}

	@Override
	public void execute(@NotNull ThrowInstruction instruction) {
		Value thrown = readReference(instruction.value(), instruction.value(), false);
		if (thrown == null)
			return;
		if (thrown instanceof Value.UninitializedReferenceValue) {
			failDestination(instruction.value(), false, "throw requires an initialized reference");
			return;
		}
		terminalState = new DalvikAnalysisResults.TerminalState(DalvikAnalysisResults.TerminalKind.THROW,
				input.snapshot(), thrown);
	}

	@Override
	public void execute(@NotNull UnaryInstruction instruction) {
		int opcode = instruction.opcode();
		UnarySpec spec = unarySpec(opcode);
		if (spec == null) {
			failDestination(instruction.dest(), opcodeIsWide(opcode), "unrecognized unary opcode 0x" + Integer.toHexString(opcode));
			return;
		}
		Value source = readTyped(instruction.source(), spec.inputType(), instruction.dest(), DalvikTypeUtils.isWideType(spec.outputType()));
		if (source == null)
			return;
		Value.PrimitiveValue primitive = (Value.PrimitiveValue) source;
		Value result = switch (spec.operation()) {
			case NEGATE -> {
				if (primitive instanceof Value.KnownFloatValue(float value))
					yield new Value.KnownFloatValue(-value);
				if (primitive instanceof Value.KnownDoubleValue(double value))
					yield new Value.KnownDoubleValue(-value);
				yield primitive.negate();
			}
			case NOT -> source instanceof Value.KnownIntValue(int value)
					? Values.valueOf(~value)
					: Values.INT_VALUE;
			case NOT_LONG -> source instanceof Value.KnownLongValue(long value)
					? Values.valueOf(~value)
					: Values.LONG_VALUE;
			case CAST -> castPrimitive(primitive, spec.castType());
		};
		write(instruction.dest(), result);
	}

	@Override
	public void execute(@NotNull Instruction instruction) {
		unsupported("unknown concrete instruction " + instruction.getClass().getSimpleName());
		output.invalidateAll();
	}

	/**
	 * Produces the pending result for an invoke-custom instruction without resolving a target method.
	 *
	 * @param descriptor
	 * 		The method descriptor of the invoked method.
	 */
	private void produceInvokeResult(@NotNull String descriptor) {
		try {
			DescriptorType returnType = DescriptorParser.parseMethodDescriptor(descriptor).returnType();
			output.setPendingResult(returnType == VOID ? null : Values.valueOf(returnType));
		} catch (RuntimeException failure) {
			output.clearPendingResult();
			unsupported("invalid invoke return type: " + failure.getMessage());
		}
	}

	/**
	 * Resolves a direct invoke when every register word can be grouped into known logical arguments.
	 *
	 * @param instruction
	 * 		The invoke instruction to resolve.
	 */
	private void produceInvokeResult(@NotNull InvokeInstruction instruction) {
		String descriptor = instruction.type().descriptor();
		MethodDescriptor methodDescriptor;
		try {
			methodDescriptor = DescriptorParser.parseMethodDescriptor(descriptor);
		} catch (RuntimeException failure) {
			output.clearPendingResult();
			unsupported("invalid invoke return type: " + failure.getMessage());
			return;
		}

		DescriptorType returnType = methodDescriptor.returnType();
		if (returnType == VOID) {
			output.setPendingResult(null);
			return;
		}

		Value result = Values.valueOf(returnType);
		InvocationArguments arguments = invocationArguments(instruction, methodDescriptor);
		if (arguments != null) {
			Value lookupResult = methodValueLookup.accept(
					new MethodReference(instruction.owner().internalName(), instruction.name(), descriptor),
					arguments.context(), arguments.parameters());
			if (lookupResult != null)
				result = lookupResult;
		}
		output.setPendingResult(result);
	}

	/**
	 * Groups the register words of an invoke instruction into logical arguments.
	 *
	 * @param instruction
	 * 		The invoke instruction to resolve.
	 * @param methodDescriptor
	 * 		The method descriptor of the invoked method.
	 *
	 * @return The grouped arguments, or {@code null} if the registers cannot be grouped into known logical arguments.
	 */
	private @Nullable InvocationArguments invocationArguments(@NotNull InvokeInstruction instruction,
	                                                          @NotNull MethodDescriptor methodDescriptor) {
		boolean staticInvoke = instruction.opcode() == Opcodes.INVOKE_STATIC || instruction.opcode() == Opcodes.INVOKE_STATIC_RANGE;
		long expectedWords = staticInvoke ? 0 : 1;
		for (DescriptorType parameter : methodDescriptor.parameters())
			expectedWords += parameter == LONG || parameter == DOUBLE ? 2 : 1;
		if (expectedWords > Integer.MAX_VALUE)
			return null;

		// If the instruction is a range invoke, the registers are implicit and we can only check the count.
		int[] registers = instruction.isRange() ? null : instruction.arguments();
		if (instruction.isRange()) {
			long actualWords = (long) instruction.last() - instruction.first() + 1;
			if (actualWords != expectedWords)
				return null;
		} else if (registers == null || registers.length != expectedWords) {
			return null;
		}

		// Read the receiver from the first register word if its a non-static invoke.
		int wordOffset = 0;
		Value.ObjectValue context = null;
		if (!staticInvoke) {
			Value receiver = readLookupSingle(argumentRegister(instruction, registers, wordOffset++));
			if (receiver == DalvikZeroValue.INSTANCE)
				receiver = Values.NULL_VALUE;
			if (!(receiver instanceof Value.ObjectValue objectValue) || !receiver.isKnown())
				return null;
			context = objectValue;
		}

		// Read each parameter from the next register words.
		List<Value> parameters = new ArrayList<>(methodDescriptor.parameters().size());
		for (DescriptorType parameter : methodDescriptor.parameters()) {
			int register = argumentRegister(instruction, registers, wordOffset);

			// Even in dalvik-land we cannot escape from wide types...
			if (parameter == LONG || parameter == DOUBLE) {
				// Sanity check that the next register is the tail of the wide value.
				int tailRegister = argumentRegister(instruction, registers, wordOffset + 1);
				if ((long) tailRegister != (long) register + 1)
					return null;

				// And check the value of the wide type is known and of the correct type.
				Value value = readLookupWide(register);
				if (!(value instanceof Value.PrimitiveValue primitive) || primitive.type() != parameter || !value.isKnown())
					return null;

				parameters.add(value);
				wordOffset += 2;
				continue;
			}

			// Normalize special case values.
			Value value = readLookupSingle(register);
			if (value == DalvikZeroValue.INSTANCE) {
				if (Descriptors.isIntLike(parameter))
					value = Values.INT_0;
				else if (Descriptors.isReferenceType(parameter))
					value = Values.NULL_VALUE;
				else
					return null;
			}

			// Check that the value is of the correct type and is known.
			if (Descriptors.isIntLike(parameter)) {
				if (!(value instanceof Value.IntValue))
					return null;
			} else if (parameter == FLOAT) {
				if (!(value instanceof Value.FloatValue))
					return null;
			} else if (Descriptors.isReferenceType(parameter)) {
				if (!(value instanceof Value.ObjectValue))
					return null;
			} else {
				return null;
			}
			if (!value.isKnown())
				return null;

			parameters.add(value);
			wordOffset++;
		}
		return new InvocationArguments(context, parameters);
	}

	/**
	 * Gets the register for a logical argument word in an invoke instruction.
	 *
	 * @param instruction
	 * 		The invoke instruction.
	 * @param registers
	 * 		The register list for the instruction, or {@code null} if the instruction is a range invoke.
	 * @param wordOffset
	 * 		The offset of the argument word to read.
	 *
	 * @return The register containing the argument word.
	 */
	private static int argumentRegister(@NotNull InvokeInstruction instruction, @Nullable int[] registers, int wordOffset) {
		return instruction.isRange() ? instruction.first() + wordOffset : registers[wordOffset];
	}

	/**
	 * @param register
	 * 		The register to read.
	 *
	 * @return The single value, or {@code null} if the value is not a known single primitive.
	 */
	private @Nullable Value readLookupSingle(int register) {
		try {
			return input.readSingle(register);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	/**
	 * @param register
	 * 		The register to read.
	 *
	 * @return The wide value, or {@code null} if the value is not a known wide primitive.
	 */
	private @Nullable Value readLookupWide(int register) {
		try {
			return input.readWide(register);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	/**
	 * Transfers a binary operation across a single instruction.
	 *
	 * @param opcode
	 * 		The opcode of the binary instruction.
	 * @param destination
	 * 		The destination register for the result.
	 * @param leftRegister
	 * 		The register containing the left operand.
	 * @param rightRegister
	 * 		The register containing the right operand, or {@code -1} if the right operand is a literal.
	 * @param literal
	 * 		The literal value for the right operand, or {@code null} if the right operand is in a register.
	 */
	private void transferBinary(int opcode, int destination, int leftRegister, int rightRegister,
	                            @Nullable Integer literal) {
		// Must have a known spec for the op.
		BinarySpec spec = binarySpec(opcode);
		if (spec == null) {
			failDestination(destination, opcodeIsWide(opcode), "unrecognized binary opcode 0x" + Integer.toHexString(opcode));
			return;
		}

		boolean wideDestination = DalvikTypeUtils.isWideType(spec.type());
		PrimitiveType leftType = spec.type();
		PrimitiveType rightType = spec.longShift() ? INT : spec.type();
		Value left;
		Value right;
		if (literal == null) {
			left = readTyped(leftRegister, leftType, destination, wideDestination);
			if (left == null)
				return;

			right = readTyped(rightRegister, rightType, destination, wideDestination);
			if (right == null)
				return;
		} else {
			Value source = readTyped(leftRegister, INT, destination, false);
			if (source == null)
				return;

			Value literalValue = Values.valueOf(literal);
			if (spec.reverse()) {
				left = literalValue;
				right = source;
			} else {
				left = source;
				right = literalValue;
			}
		}

		write(destination, foldBinary(spec, left, right));
	}

	/**
	 * Transfers the input register state across a single instruction.
	 *
	 * @param instructionIndex
	 * 		The index of the instruction in the method's instruction list.
	 * @param instruction
	 * 		The instruction to transfer across.
	 * @param input
	 * 		The input register state before the instruction executes.
	 *
	 * @return The output register state after the instruction executes,
	 * and any terminal state that may have been reached.
	 */
	@NotNull TransferResult transfer(int instructionIndex, @NotNull Instruction instruction,
	                                 @NotNull DalvikAnalysisFrame input) {
		this.instructionIndex = instructionIndex;
		this.instruction = instruction;
		this.input = input;
		this.output = input.copy();
		this.terminalState = null;

		// Clear any pending result or exception if the instruction is not a move-result or move-exception.
		if (!(instruction instanceof MoveResultInstruction))
			output.clearPendingResult();
		if (!(instruction instanceof MoveExceptionInstruction))
			output.clearPendingException();

		try {
			if (!DalvikInstructionDispatcher.dispatch(instruction, this)) {
				unsupported("unknown concrete instruction " + instruction.getClass().getSimpleName());
				output.invalidateAll();
			}
		} catch (RuntimeException failure) {
			unsupported("transfer failed: " + failure.getMessage());
			output.invalidateAll();
		}
		return new TransferResult(output, terminalState);
	}

	/**
	 * Records an unsupported transfer failure for the current instruction.
	 *
	 * @param detail
	 * 		Detail message for the unsupported transfer.
	 */
	private void unsupported(@NotNull String detail) {
		failureRecorder.record(instructionIndex, DalvikAnalysisFailure.FailureKind.UNSUPPORTED_TRANSFER, "Unsupported Dalvik transfer: " + detail);
	}

	/**
	 * Fails the destination register in the output frame, if possible.
	 *
	 * @param register
	 * 		The register to fail.
	 * @param wide
	 * 		Whether the register is wide.
	 * @param detail
	 * 		The detail message for the failure.
	 */
	private void failDestination(int register, boolean wide, String detail) {
		unsupported(detail);
		clearDestination(register, wide);
	}

	/**
	 * Clears the destination register in the output frame, if possible.
	 *
	 * @param register
	 * 		The register to clear.
	 * @param wide
	 * 		Whether the register is wide.
	 */
	private void clearDestination(int register, boolean wide) {
		try {
			if (wide)
				output.clearWide(register);
			else
				output.clear(register);
		} catch (RuntimeException failure) {
			unsupported("invalid destination v" + register + ": " + failure.getMessage());
		}
	}

	/**
	 * @param register
	 * 		The register to write to.
	 * @param value
	 * 		The value to write.
	 *
	 * @return {@code true} if the value was written successfully,
	 * {@code false} if the destination register is invalid.
	 */
	private boolean write(int register, @NotNull Value value) {
		try {
			if (DalvikAnalysisFrame.isWide(value))
				output.writeWide(register, value);
			else
				output.write(register, value);
			return true;
		} catch (RuntimeException failure) {
			failDestination(register, DalvikAnalysisFrame.isWide(value), "invalid destination v" + register + ": " + failure.getMessage());
			return false;
		}
	}

	/**
	 * @param register
	 * 		The register to read from.
	 * @param destination
	 * 		The destination register to write to (for error reporting).
	 * @param destinationWide
	 * 		Whether the destination register is wide (for error reporting).
	 *
	 * @return The single-word value in the given register,
	 * or {@code null} if the register doesn't contain a valid single-word value.
	 */
	private @Nullable Value readSingle(int register, int destination, boolean destinationWide) {
		try {
			return input.readSingle(register);
		} catch (RuntimeException failure) {
			failDestination(destination, destinationWide, "v" + register + " does not contain a valid single-word value");
			return null;
		}
	}

	/**
	 * @param register
	 * 		The register to read from.
	 * @param destination
	 * 		The destination register to write to (for error reporting).
	 * @param destinationWide
	 * 		Whether the destination register is wide (for error reporting).
	 *
	 * @return The wide value in the given register,
	 * or {@code null} if the register doesn't contain a valid wide value.
	 */
	private @Nullable Value readWide(int register, int destination, boolean destinationWide) {
		try {
			return input.readWide(register);
		} catch (RuntimeException failure) {
			failDestination(destination, destinationWide, "v" + register + " does not contain a valid wide value");
			return null;
		}
	}

	/**
	 * @param register
	 * 		The register to read from.
	 * @param expected
	 * 		The expected primitive type of the value in the register.
	 * @param destination
	 * 		The destination register to write to (for error reporting).
	 * @param destinationWide
	 * 		Whether the destination register is wide (for error reporting).
	 *
	 * @return The primitive value in the given register,
	 * or {@code null} if the register doesn't contain a valid primitive value of the expected type.
	 */
	private @Nullable Value readTyped(int register, @NotNull PrimitiveType expected, int destination, boolean destinationWide) {
		// Read the value from the register, using the appropriate method for wide or single-word types.
		Value value = DalvikTypeUtils.isWideType(expected)
				? readWide(register, destination, destinationWide)
				: readSingle(register, destination, destinationWide);
		if (value == null)
			return null;

		// If the value is the Dalvik zero value and the expected type is INT, treat it as a known int zero.
		if (value == DalvikZeroValue.INSTANCE && expected == INT)
			return Values.INT_0;

		// If the value is not a primitive value or its type does not match the expected type, fail.
		if (!(value instanceof Value.PrimitiveValue primitive) || primitive.type() != expected) {
			failDestination(destination, destinationWide, "v" + register + " has the wrong primitive category for " + expected);
			return null;
		}
		return value;
	}

	/**
	 * @param register
	 * 		The register to read from.
	 * @param destination
	 * 		The destination register to write to (for error reporting).
	 * @param destinationWide
	 * 		Whether the destination register is wide (for error reporting).
	 *
	 * @return The reference value in the given register, or {@code null} if the register doesn't contain a valid reference value.
	 */
	private @Nullable Value readReference(int register, int destination, boolean destinationWide) {
		// Check if the value is a reference type (object or array) or null. If it's a primitive, fail.
		Value value = readSingle(register, destination, destinationWide);
		if (value == null)
			return null;

		// If the value is the Dalvik zero value, treat it as a null reference.
		if (value == DalvikZeroValue.INSTANCE)
			return Values.NULL_VALUE;

		// If the value is an object value, return it.
		if (value instanceof Value.ObjectValue)
			return value;

		// This isn't a reference...
		failDestination(destination, destinationWide, "v" + register + " is not a reference value");
		return null;
	}

	/**
	 * @param destination
	 * 		The destination register to write to.
	 * @param type
	 * 		The type of the value to write.
	 *
	 * @return {@code true} if the value was written successfully,
	 * {@code false} if the destination register is invalid.
	 */
	private boolean writeDescriptorValue(int destination, @NotNull DescriptorType type) {
		return write(destination, Values.valueOf(type));
	}

	/**
	 * @param register
	 * 		The register to read the array value from.
	 *
	 * @return The array value in the given register, or {@code null} if the register doesn't contain a valid array value.
	 */
	private @Nullable Value arrayValue(int register) {
		DalvikRegisterState.Slot slot;
		try {
			slot = input.slot(register);
		} catch (RuntimeException ignored) {
			return null;
		}
		return slot instanceof DalvikRegisterState.ValueHead(Value value)
				&& !DalvikAnalysisFrame.isWide(value) ? value : null;
	}

	/**
	 * @param opcode
	 * 		The opcode of the array instruction.
	 *
	 * @return {@code true} if the array instruction is an array get, {@code false} otherwise.
	 */
	private static boolean isArrayGet(int opcode) {
		return switch (opcode) {
			case Opcodes.AGET, Opcodes.AGET_WIDE, Opcodes.AGET_OBJECT, Opcodes.AGET_BOOLEAN,
			     Opcodes.AGET_BYTE, Opcodes.AGET_CHAR, Opcodes.AGET_SHORT -> true;
			default -> false;
		};
	}

	/**
	 * @param opcode
	 * 		The opcode of the array instruction.
	 *
	 * @return {@code true} if the array instruction is an array put, {@code false} otherwise.
	 */
	private static boolean isArrayPut(int opcode) {
		return switch (opcode) {
			case Opcodes.APUT, Opcodes.APUT_WIDE, Opcodes.APUT_OBJECT, Opcodes.APUT_BOOLEAN,
			     Opcodes.APUT_BYTE, Opcodes.APUT_CHAR, Opcodes.APUT_SHORT -> true;
			default -> false;
		};
	}

	/**
	 * @param opcode
	 * 		The opcode of the binary instruction.
	 *
	 * @return {@code true} if the binary instruction produces a wide result, {@code false} otherwise.
	 */
	private static boolean opcodeIsWide(int opcode) {
		return switch (opcode) {
			case Opcodes.ADD_LONG, Opcodes.SUB_LONG, Opcodes.MUL_LONG, Opcodes.DIV_LONG, Opcodes.REM_LONG,
			     Opcodes.AND_LONG, Opcodes.OR_LONG, Opcodes.XOR_LONG, Opcodes.SHL_LONG, Opcodes.SHR_LONG,
			     Opcodes.USHR_LONG, Opcodes.ADD_LONG_2ADDR, Opcodes.SUB_LONG_2ADDR, Opcodes.MUL_LONG_2ADDR,
			     Opcodes.DIV_LONG_2ADDR, Opcodes.REM_LONG_2ADDR, Opcodes.AND_LONG_2ADDR, Opcodes.OR_LONG_2ADDR,
			     Opcodes.XOR_LONG_2ADDR, Opcodes.SHL_LONG_2ADDR, Opcodes.SHR_LONG_2ADDR, Opcodes.USHR_LONG_2ADDR ->
					true;
			default -> false;
		};
	}

	/**
	 * @param opcode
	 * 		The opcode of the binary instruction.
	 *
	 * @return Spec for the binary instruction, {@code null} if the opcode isn't recognized.
	 */
	private static @Nullable BinarySpec binarySpec(int opcode) {
		return switch (opcode) {
			case Opcodes.ADD_INT, Opcodes.ADD_INT_2ADDR, Opcodes.ADD_INT_LIT16, Opcodes.ADD_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.ADD, false, false);
			case Opcodes.SUB_INT, Opcodes.SUB_INT_2ADDR -> new BinarySpec(INT, BinaryOperation.SUBTRACT, false, false);
			case Opcodes.RSUB_INT, Opcodes.RSUB_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.SUBTRACT, false, true);
			case Opcodes.MUL_INT, Opcodes.MUL_INT_2ADDR, Opcodes.MUL_INT_LIT16, Opcodes.MUL_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.MULTIPLY, false, false);
			case Opcodes.DIV_INT, Opcodes.DIV_INT_2ADDR, Opcodes.DIV_INT_LIT16, Opcodes.DIV_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.DIVIDE, false, false);
			case Opcodes.REM_INT, Opcodes.REM_INT_2ADDR, Opcodes.REM_INT_LIT16, Opcodes.REM_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.REMAINDER, false, false);
			case Opcodes.AND_INT, Opcodes.AND_INT_2ADDR, Opcodes.AND_INT_LIT16, Opcodes.AND_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.AND, false, false);
			case Opcodes.OR_INT, Opcodes.OR_INT_2ADDR, Opcodes.OR_INT_LIT16, Opcodes.OR_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.OR, false, false);
			case Opcodes.XOR_INT, Opcodes.XOR_INT_2ADDR, Opcodes.XOR_INT_LIT16, Opcodes.XOR_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.XOR, false, false);
			case Opcodes.SHL_INT, Opcodes.SHL_INT_2ADDR, Opcodes.SHL_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.SHL, false, false);
			case Opcodes.SHR_INT, Opcodes.SHR_INT_2ADDR, Opcodes.SHR_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.SHR, false, false);
			case Opcodes.USHR_INT, Opcodes.USHR_INT_2ADDR, Opcodes.USHR_INT_LIT8 -> new BinarySpec(INT, BinaryOperation.USHR, false, false);
			case Opcodes.ADD_LONG, Opcodes.ADD_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.ADD, false, false);
			case Opcodes.SUB_LONG, Opcodes.SUB_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.SUBTRACT, false, false);
			case Opcodes.MUL_LONG, Opcodes.MUL_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.MULTIPLY, false, false);
			case Opcodes.DIV_LONG, Opcodes.DIV_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.DIVIDE, false, false);
			case Opcodes.REM_LONG, Opcodes.REM_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.REMAINDER, false, false);
			case Opcodes.AND_LONG, Opcodes.AND_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.AND, false, false);
			case Opcodes.OR_LONG, Opcodes.OR_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.OR, false, false);
			case Opcodes.XOR_LONG, Opcodes.XOR_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.XOR, false, false);
			case Opcodes.SHL_LONG, Opcodes.SHL_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.SHL, true, false);
			case Opcodes.SHR_LONG, Opcodes.SHR_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.SHR, true, false);
			case Opcodes.USHR_LONG, Opcodes.USHR_LONG_2ADDR -> new BinarySpec(LONG, BinaryOperation.USHR, true, false);
			case Opcodes.ADD_FLOAT, Opcodes.ADD_FLOAT_2ADDR -> new BinarySpec(FLOAT, BinaryOperation.ADD, false, false);
			case Opcodes.SUB_FLOAT, Opcodes.SUB_FLOAT_2ADDR -> new BinarySpec(FLOAT, BinaryOperation.SUBTRACT, false, false);
			case Opcodes.MUL_FLOAT, Opcodes.MUL_FLOAT_2ADDR -> new BinarySpec(FLOAT, BinaryOperation.MULTIPLY, false, false);
			case Opcodes.DIV_FLOAT, Opcodes.DIV_FLOAT_2ADDR -> new BinarySpec(FLOAT, BinaryOperation.DIVIDE, false, false);
			case Opcodes.REM_FLOAT, Opcodes.REM_FLOAT_2ADDR -> new BinarySpec(FLOAT, BinaryOperation.REMAINDER, false, false);
			case Opcodes.ADD_DOUBLE, Opcodes.ADD_DOUBLE_2ADDR -> new BinarySpec(DOUBLE, BinaryOperation.ADD, false, false);
			case Opcodes.SUB_DOUBLE, Opcodes.SUB_DOUBLE_2ADDR -> new BinarySpec(DOUBLE, BinaryOperation.SUBTRACT, false, false);
			case Opcodes.MUL_DOUBLE, Opcodes.MUL_DOUBLE_2ADDR -> new BinarySpec(DOUBLE, BinaryOperation.MULTIPLY, false, false);
			case Opcodes.DIV_DOUBLE, Opcodes.DIV_DOUBLE_2ADDR -> new BinarySpec(DOUBLE, BinaryOperation.DIVIDE, false, false);
			case Opcodes.REM_DOUBLE, Opcodes.REM_DOUBLE_2ADDR -> new BinarySpec(DOUBLE, BinaryOperation.REMAINDER, false, false);
			default -> null;
		};
	}

	/**
	 * Folds a binary operation on two known values into a single known value.
	 *
	 * @param spec
	 * 		The binary operation specification.
	 * @param left
	 * 		The left operand value.
	 * @param right
	 * 		The right operand value.
	 * @return The folded result value, or an unknown value if the operands are not known.
	 */
	private static @NotNull Value foldBinary(@NotNull BinarySpec spec, @NotNull Value left, @NotNull Value right) {
		if (spec.type() == INT) {
			if (!left.isKnown() || !right.isKnown())
				return Values.INT_VALUE;
			int a = ((Value.KnownIntValue) left).value();
			int b = ((Value.KnownIntValue) right).value();
			if ((spec.operation() == BinaryOperation.DIVIDE || spec.operation() == BinaryOperation.REMAINDER) && b == 0)
				return Values.INT_VALUE;
			return Values.valueOf(switch (spec.operation()) {
				case ADD -> a + b;
				case SUBTRACT -> a - b;
				case MULTIPLY -> a * b;
				case DIVIDE -> a / b;
				case REMAINDER -> a % b;
				case AND -> a & b;
				case OR -> a | b;
				case XOR -> a ^ b;
				case SHL -> a << (b & 0x1f);
				case SHR -> a >> (b & 0x1f);
				case USHR -> a >>> (b & 0x1f);
			});
		}
		if (spec.type() == LONG) {
			if (!left.isKnown() || !right.isKnown())
				return Values.LONG_VALUE;
			long a = ((Value.KnownLongValue) left).value();
			if (spec.longShift()) {
				int distance = ((Value.KnownIntValue) right).value() & 0x3f;
				return Values.valueOf(switch (spec.operation()) {
					case SHL -> a << distance;
					case SHR -> a >> distance;
					case USHR -> a >>> distance;
					default -> throw new IllegalStateException("Invalid long shift operation");
				});
			}
			long b = ((Value.KnownLongValue) right).value();
			if ((spec.operation() == BinaryOperation.DIVIDE || spec.operation() == BinaryOperation.REMAINDER) && b == 0)
				return Values.LONG_VALUE;
			return Values.valueOf(switch (spec.operation()) {
				case ADD -> a + b;
				case SUBTRACT -> a - b;
				case MULTIPLY -> a * b;
				case DIVIDE -> a / b;
				case REMAINDER -> a % b;
				case AND -> a & b;
				case OR -> a | b;
				case XOR -> a ^ b;
				default -> throw new IllegalStateException("Invalid long arithmetic operation");
			});
		}
		if (spec.type() == FLOAT) {
			if (!left.isKnown() || !right.isKnown())
				return Values.FLOAT_VALUE;
			float a = ((Value.KnownFloatValue) left).value();
			float b = ((Value.KnownFloatValue) right).value();
			float result = switch (spec.operation()) {
				case ADD -> a + b;
				case SUBTRACT -> a - b;
				case MULTIPLY -> a * b;
				case DIVIDE -> a / b;
				case REMAINDER -> a % b;
				default -> throw new IllegalStateException("Invalid float arithmetic operation");
			};
			return new Value.KnownFloatValue(result);
		}
		if (!left.isKnown() || !right.isKnown())
			return Values.DOUBLE_VALUE;

		double a = ((Value.KnownDoubleValue) left).value();
		double b = ((Value.KnownDoubleValue) right).value();
		double result = switch (spec.operation()) {
			case ADD -> a + b;
			case SUBTRACT -> a - b;
			case MULTIPLY -> a * b;
			case DIVIDE -> a / b;
			case REMAINDER -> a % b;
			default -> throw new IllegalStateException("Invalid double arithmetic operation");
		};
		return new Value.KnownDoubleValue(result);
	}

	/**
	 * Transfers a field value from the source register to the destination register based on the opcode.
	 * When the field is recognized in the value lookup, the corresponding value is written to the destination register.
	 *
	 * @param opcode
	 * 		The opcode representing the field operation.
	 * @param register
	 * 		The register involved in the field operation.
	 * @param descriptor
	 * 		The descriptor of the field type.
	 */
	private void transferField(int opcode, int register, @NotNull String owner, @NotNull String name,
	                           @NotNull String descriptor, @Nullable Integer instanceRegister) {
		// We only handle field gets since those write to registers.
		if (isFieldPut(opcode))
			return;
		if (!isFieldGet(opcode)) {
			failDestination(register, isWideFieldOpcode(opcode), "unrecognized field opcode 0x" + Integer.toHexString(opcode));
			return;
		}

		// Get the fallback value to put in the register for later.
		DescriptorType fieldType;
		Value fallback;
		try {
			fieldType = DescriptorParser.parseFieldDescriptor(descriptor);
			fallback = Values.valueOf(fieldType);
		} catch (RuntimeException failure) {
			failDestination(register, isWideFieldOpcode(opcode), "invalid field type: " + failure.getMessage());
			return;
		}

		// Look up the field value using the provided lookup function.
		// If it's not found then that's why we got our fallback.
		FieldReference reference = new FieldReference(owner, name, descriptor);
		Value lookupResult = null;
		if (instanceRegister == null) {
			lookupResult = fieldValueLookup.accept(reference, null);
		} else {
			Value receiver = readLookupSingle(instanceRegister);
			if (receiver == DalvikZeroValue.INSTANCE)
				receiver = Values.NULL_VALUE;
			if (receiver instanceof Value.ObjectValue objectValue && receiver.isKnown())
				lookupResult = fieldValueLookup.accept(reference, objectValue);
		}
		write(register, lookupResult == null ? fallback : lookupResult);
	}

	/**
	 * @param opcode
	 * 		The opcode to check.
	 * @return {@code true} if the opcode is a field get operation. Otherwise, {@code false}.
	 */
	private static boolean isFieldGet(int opcode) {
		return switch (opcode) {
			case Opcodes.SGET, Opcodes.SGET_WIDE, Opcodes.SGET_OBJECT, Opcodes.SGET_BOOLEAN, Opcodes.SGET_BYTE,
			     Opcodes.SGET_CHAR, Opcodes.SGET_SHORT, Opcodes.IGET, Opcodes.IGET_WIDE, Opcodes.IGET_OBJECT,
			     Opcodes.IGET_BOOLEAN, Opcodes.IGET_BYTE, Opcodes.IGET_CHAR, Opcodes.IGET_SHORT -> true;
			default -> false;
		};
	}

	/**
	 * @param opcode
	 * 		The opcode to check.
	 * @return {@code true} if the opcode is a field put operation. Otherwise, {@code false}.
	 */
	private static boolean isFieldPut(int opcode) {
		return switch (opcode) {
			case Opcodes.SPUT, Opcodes.SPUT_WIDE, Opcodes.SPUT_OBJECT, Opcodes.SPUT_BOOLEAN, Opcodes.SPUT_BYTE,
			     Opcodes.SPUT_CHAR, Opcodes.SPUT_SHORT, Opcodes.IPUT, Opcodes.IPUT_WIDE, Opcodes.IPUT_OBJECT,
			     Opcodes.IPUT_BOOLEAN, Opcodes.IPUT_BYTE, Opcodes.IPUT_CHAR, Opcodes.IPUT_SHORT -> true;
			default -> false;
		};
	}

	/**
	 * @param opcode
	 * 		The opcode to check.
	 * @return {@code true} if the opcode is a wide field access (get or put). Otherwise, {@code false}.
	 */
	private static boolean isWideFieldOpcode(int opcode) {
		return opcode == Opcodes.SGET_WIDE
				|| opcode == Opcodes.SPUT_WIDE
				|| opcode == Opcodes.IGET_WIDE
				|| opcode == Opcodes.IPUT_WIDE;
	}

	/**
	 * Casts a primitive value to the specified target type, if possible.
	 *
	 * @param source
	 * 		The source primitive value to cast.
	 * @param target
	 * 		The target primitive type to cast to. Must not be {@code null}.
	 *
	 * @return The cast primitive value.
	 *
	 * @throws IllegalStateException
	 * 		When the target type is {@code null}.
	 */
	private static @NotNull Value.PrimitiveValue castPrimitive(Value.PrimitiveValue source, @Nullable PrimitiveType target) {
		if (target == null)
			throw new IllegalStateException("Missing primitive conversion target");
		if (source instanceof Value.KnownFloatValue(float value) && target == DOUBLE)
			return new Value.KnownDoubleValue(value);
		if (source instanceof Value.KnownDoubleValue(double value) && target == FLOAT)
			return new Value.KnownFloatValue((float) value);
		return source.cast(target);
	}

	/**
	 * @param opcode
	 * 		The opcode to look up.
	 * @return The corresponding {@link UnarySpec} if the opcode is recognized. Otherwise, {@code null}.
	 */
	private static @Nullable UnarySpec unarySpec(int opcode) {
		return switch (opcode) {
			case Opcodes.NEG_INT -> new UnarySpec(INT, INT, UnaryOperation.NEGATE, null);
			case Opcodes.NOT_INT -> new UnarySpec(INT, INT, UnaryOperation.NOT, null);
			case Opcodes.NEG_LONG -> new UnarySpec(LONG, LONG, UnaryOperation.NEGATE, null);
			case Opcodes.NOT_LONG -> new UnarySpec(LONG, LONG, UnaryOperation.NOT_LONG, null);
			case Opcodes.NEG_FLOAT -> new UnarySpec(FLOAT, FLOAT, UnaryOperation.NEGATE, null);
			case Opcodes.NEG_DOUBLE -> new UnarySpec(DOUBLE, DOUBLE, UnaryOperation.NEGATE, null);
			case Opcodes.INT_TO_LONG -> new UnarySpec(INT, LONG, UnaryOperation.CAST, LONG);
			case Opcodes.INT_TO_FLOAT -> new UnarySpec(INT, FLOAT, UnaryOperation.CAST, FLOAT);
			case Opcodes.INT_TO_DOUBLE -> new UnarySpec(INT, DOUBLE, UnaryOperation.CAST, DOUBLE);
			case Opcodes.LONG_TO_INT -> new UnarySpec(LONG, INT, UnaryOperation.CAST, INT);
			case Opcodes.LONG_TO_FLOAT -> new UnarySpec(LONG, FLOAT, UnaryOperation.CAST, FLOAT);
			case Opcodes.LONG_TO_DOUBLE -> new UnarySpec(LONG, DOUBLE, UnaryOperation.CAST, DOUBLE);
			case Opcodes.FLOAT_TO_INT -> new UnarySpec(FLOAT, INT, UnaryOperation.CAST, INT);
			case Opcodes.FLOAT_TO_LONG -> new UnarySpec(FLOAT, LONG, UnaryOperation.CAST, LONG);
			case Opcodes.FLOAT_TO_DOUBLE -> new UnarySpec(FLOAT, DOUBLE, UnaryOperation.CAST, DOUBLE);
			case Opcodes.DOUBLE_TO_INT -> new UnarySpec(DOUBLE, INT, UnaryOperation.CAST, INT);
			case Opcodes.DOUBLE_TO_LONG -> new UnarySpec(DOUBLE, LONG, UnaryOperation.CAST, LONG);
			case Opcodes.DOUBLE_TO_FLOAT -> new UnarySpec(DOUBLE, FLOAT, UnaryOperation.CAST, FLOAT);
			case Opcodes.INT_TO_BYTE -> new UnarySpec(INT, INT, UnaryOperation.CAST, BYTE);
			case Opcodes.INT_TO_CHAR -> new UnarySpec(INT, INT, UnaryOperation.CAST, CHAR);
			case Opcodes.INT_TO_SHORT -> new UnarySpec(INT, INT, UnaryOperation.CAST, SHORT);
			default -> null;
		};
	}

	private record BinarySpec(PrimitiveType type, BinaryOperation operation, boolean longShift, boolean reverse) {}
	private record UnarySpec(PrimitiveType inputType, PrimitiveType outputType, UnaryOperation operation, @Nullable PrimitiveType castType) {}

	private enum BinaryOperation {ADD, SUBTRACT, MULTIPLY, DIVIDE, REMAINDER, AND, OR, XOR, SHL, SHR, USHR}
	private enum UnaryOperation {NEGATE, NOT, NOT_LONG, CAST}

	/**
	 * Represents the arguments for a method invocation, including the context (receiver) and parameters.
	 *
	 * @param context
	 * 		The receiver object for instance method invocations, or {@code null} for static method invocations.
	 * @param parameters
	 * 		The list of parameter values for the method invocation.
	 */
	private record InvocationArguments(@Nullable Value.ObjectValue context, @NotNull List<Value> parameters) {}

	/**
	 * Functional interface for recording analysis failures during the transfer process.
	 */
	@FunctionalInterface
	interface FailureRecorder {
		void record(int instructionIndex, DalvikAnalysisFailure.FailureKind kind, String message);
	}

	/**
	 * Represents the result of a transfer operation, including the updated output frame and any terminal state.
	 *
	 * @param output
	 * 		Updated output frame after the transfer operation.
	 * @param terminalState
	 * 		Final state of the analysis if the transfer operation resulted in a terminal condition. Otherwise, {@code null}.
	 */
	record TransferResult(@NotNull DalvikAnalysisFrame output,
	                      @Nullable DalvikAnalysisResults.TerminalState terminalState) {}
}

package me.darknet.assembler.backend.dalvik.printer;

import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.printer.AnnotationPrinter;
import me.darknet.assembler.printer.MethodPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
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
import me.darknet.dex.tree.simulation.StraightForwardSimulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DalvikMethodPrinter implements MethodPrinter {
	private final MethodMember definition;
	private final DalvikMemberPrinter memberPrinter;

	/**
	 * @param definition
	 * 		The method definition to print.
	 */
	public DalvikMethodPrinter(MethodMember definition) {
		this.definition = definition;
		this.memberPrinter = new DalvikMemberPrinter(definition, definition, DalvikMemberPrinter.Type.METHOD);
	}

	@Override
	public @Nullable AnnotationPrinter annotation(int index) {
		return memberPrinter.printAnnotation(index);
	}

	@Override
	public @Nullable AnnotationPrinter visibleAnnotation(int index) {
		return memberPrinter.printAnnotation(index);
	}

	@Override
	public @Nullable AnnotationPrinter invisibleAnnotation(int index) {
		return memberPrinter.printAnnotation(index);
	}

	private static String getLabelName(int index) {
		StringBuilder label = new StringBuilder();

		while (index >= 0) {
			label.insert(0, (char) ('A' + index % 26));
			index = (index / 26) - 1;
		}

		return label.toString();
	}

	@Override
	public void print(PrintContext<?> ctx) {
		memberPrinter.printAttributes(ctx);
		if (definition.getSignature() != null) {
			ctx.begin().element(".signature").string(definition.getSignature()).next();
		}
		var obj = memberPrinter.printDeclaration(ctx).literal(definition.getName()).print(" ")
				.literal(definition.getType().descriptor()).print(" ").object();

		var code = definition.getCode();
		Map<Label, String> labelNames = code == null ? Map.of() : getLabelNames(code);

		boolean hasPrior = false;
		int parameterCount = definition.getType().parameterTypes().size();
		List<String> parameterNames = parameterNamesForPrint(code);
		boolean allParametersNamed = parameterCount > 0 && parameterNames.stream()
				.allMatch(name -> name != null && !name.isEmpty());
		if (code != null) {
			obj.value("registers").print(String.valueOf(code.getRegisters()));
			hasPrior = true;
		}
		if (allParametersNamed) {
			if (hasPrior) {
				obj.next();
			}
			PrintContext.ArrayPrint parameters = obj.value("parameters").array();
			for (int i = 0; i < parameterCount; i++) {
				if (i > 0) {
					parameters.arg();
				}
				parameters.print(parameterNames.get(i));
			}
			parameters.end();
			hasPrior = true;

			List<List<Annotation>> parameterAnnotations = definition.getParameterAnnotations();
			if (parameterAnnotations.stream().anyMatch(annotations -> !annotations.isEmpty())) {
				obj.next();
				PrintContext.ObjectPrint parameterAnnotationObject = obj.value("parameter-annotations").object();
				int limit = Math.min(parameterAnnotations.size(), parameterCount);
				boolean firstEntry = true;
				for (int i = 0; i < limit; i++) {
					List<Annotation> annotations = parameterAnnotations.get(i);
					if (annotations.isEmpty()) {
						continue;
					}
					if (!firstEntry) {
						parameterAnnotationObject.next();
					}
					firstEntry = false;
					parameterAnnotationObject.literalValue(parameterNames.get(i));
					PrintContext.ArrayPrint annotationArray = parameterAnnotationObject.array();
					for (Annotation annotation : annotations) {
						new DalvikAnnotationPrinter(annotation).print(annotationArray);
					}
					annotationArray.end();
				}
				parameterAnnotationObject.end();
			}
		}

		if (code != null && !definition.getThrownTypes().isEmpty()) {
			obj.next();
			PrintContext.ArrayPrint thrownTypes = obj.value("throws").array();
			for (int i = 0; i < definition.getThrownTypes().size(); i++) {
				thrownTypes.literal(definition.getThrownTypes().get(i));
				if (i < definition.getThrownTypes().size() - 1) {
					thrownTypes.arg();
				}
			}
			thrownTypes.end();
		}

		if (definition.getDefaultValue() != null) {
			if (hasPrior) {
				obj.next();
			}
			DalvikConstantPrinter.printConstant(obj.value("default-value"), definition.getDefaultValue());
			hasPrior = true;
		}

		if (code != null && !code.tryCatch().isEmpty()) {
			if (hasPrior) obj.next();

			PrintContext.ArrayPrint exceptions = obj.value("exceptions").array();
			boolean firstHandler = true;
			for (var tryCatch : code.tryCatch()) {
				if (tryCatch.handlers().isEmpty()) {
					throw new IllegalStateException("Cannot print a try-catch entry without handlers");
				}
				for (var handler : tryCatch.handlers()) {
					if (!firstHandler) {
						exceptions.arg();
					}
					exceptions.array()
							.print(labelNames.get(tryCatch.begin())).arg()
							.print(labelNames.get(tryCatch.end())).arg()
							.print(labelNames.get(handler.handler())).arg()
							.literal(handler.exceptionType() == null ? "*" : handler.exceptionType().internalName())
							.end();
					firstHandler = false;
				}
			}
			exceptions.end();
			hasPrior = true;
		}

		if (code != null) {
			if (hasPrior) obj.next();
			var codeObj = obj.value("code").code();
			Map<Integer, String> registers = getRegisterNames(code, allParametersNamed ? parameterNames : List.of());
			DalvikCodePrinter printer = new DalvikCodePrinter(codeObj, registers, labelNames);
			StraightForwardSimulation simulation = new StraightForwardSimulation();
			simulation.execute(printer, code);
			codeObj.end();
		}

		obj.end();
	}

	/**
	 * @param code
	 * 		The code to extract label names from.
	 *
	 * @return A map of labels to their corresponding names.
	 */
	private static @NotNull Map<Label, String> getLabelNames(@NotNull Code code) {
		Map<Label, String> labelNames = new IdentityHashMap<>();
		int labelIndex = 0;
		for (var instruction : code.getInstructions()) {
			if (instruction instanceof Label label) {
				labelNames.put(label, getLabelName(labelIndex++));
			}
		}
		return labelNames;
	}

	/**
	 * @param code
	 * 		The code to extract parameter names from.
	 *
	 * @return A list of parameter names, in order, with null or empty strings for unnamed parameters.
	 */
	private @NotNull List<String> parameterNamesForPrint(@Nullable Code code) {
		int parameterCount = definition.getType().parameterTypes().size();
		if (parameterCount == 0)
			return List.of();

		List<String> memberNames = definition.getParameterNames() == null
				? List.of()
				: definition.getParameterNames();
		DebugInformation debug = code == null ? null : code.getDebugInfo();
		List<String> debugNames = debug == null || debug.parameterNames() == null
				? List.of()
				: debug.parameterNames();
		List<String> names = new ArrayList<>(parameterCount);
		for (int i = 0; i < parameterCount; i++) {
			String name = i < memberNames.size() ? memberNames.get(i) : null;
			if ((name == null || name.isEmpty()) && i < debugNames.size())
				name = debugNames.get(i);
			names.add(name);
		}
		return names;
	}

	/**
	 * @param code
	 * 		The code to extract register names from.
	 * @param emittedParameterNames
	 * 		The parameter names that have already been emitted, to avoid conflicts.
	 *
	 * @return A map of register indices to their corresponding names, with default names for unnamed registers.
	 */
	private @NotNull Map<Integer, String> getRegisterNames(@NotNull Code code,
	                                                       @NotNull List<String> emittedParameterNames) {
		Map<Integer, String> registers = new HashMap<>();
		for (int i = 0; i < code.getRegisters(); i++)
			registers.put(i, "v" + i);

		// Check if debug information is available and valid for the method's parameters.
		int registerCount = code.getRegisters();
		int parameterBase = registerCount - code.getIn();
		if (parameterBase < 0 || parameterBase > registerCount)
			return registers;
		DebugInformation debugInfo = code.getDebugInfo();
		if (debugInfo == null || debugInfo.locals() == null || debugInfo.locals().isEmpty())
			return registers;

		// Collect candidate register names from debug information, filtering out invalid or conflicting names.
		Map<Integer, String> candidates = new HashMap<>();
		for (DebugInformation.LocalVariable local : debugInfo.locals()) {
			if (local == null
					|| local.register() < 0
					|| local.register() >= parameterBase
					|| local.register() >= registerCount) {
				continue;
			}
			if (!candidates.containsKey(local.register()))
				candidates.put(local.register(), local.name());
		}
		if (candidates.isEmpty())
			return registers;

		// Fill reserved names with "this" and parameter names to avoid conflicts with candidate register names.
		Set<String> reservedNames = new HashSet<>();
		if ((definition.getAccess() & DalvikModifiers.ACC_STATIC) == 0)
			reservedNames.add("this");
		for (int i = 0; i < definition.getType().parameterTypes().size(); i++)
			reservedNames.add("p" + i);
		reservedNames.addAll(emittedParameterNames);

		// Build map of names to registers, checking for conflicts and invalid names.
		Map<String, Integer> candidateRegisters = new HashMap<>();
		for (Map.Entry<Integer, String> entry : candidates.entrySet()) {
			String name = entry.getValue();
			if (name == null || name.isEmpty() || isNumericRegisterName(name) || reservedNames.contains(name))
				return registers;
			Integer previous = candidateRegisters.putIfAbsent(name, entry.getKey());
			if (previous != null && previous.intValue() != entry.getKey())
				return registers;
		}

		// Allocate registers for the candidate names, ensuring that they do not conflict with existing register usage.
		RegisterAllocator allocator = new RegisterAllocator(parameterBase, registerCount, candidates);
		for (Instruction instruction : code.getInstructions()) {
			if (!consumeRegisterOperands(instruction, allocator)) {
				return registers;
			}
		}
		registers.putAll(candidates);
		return registers;
	}

	/**
	 * @param name
	 * 		The register name to check.
	 *
	 * @return {@code true} if the name is a numeric register name (Example: "v0", "v1"), {@code false} otherwise.
	 */
	private static boolean isNumericRegisterName(String name) {
		if (name.length() < 2 || name.charAt(0) != 'v')
			return false;
		for (int i = 1; i < name.length(); i++)
			if (!Character.isDigit(name.charAt(i)))
				return false;
		return true;
	}

	/**
	 * Consumes the register operands of an instruction using the provided allocator.
	 *
	 * @param instruction
	 * 		The instruction to consume register operands from.
	 * @param allocator
	 * 		The register allocator to use for consuming registers.
	 *
	 * @return {@code true} if all register operands were successfully consumed, {@code false} otherwise.
	 */
	private static boolean consumeRegisterOperands(@NotNull Instruction instruction,
	                                               @NotNull RegisterAllocator allocator) {
		// Instructions that do not use registers can be ignored.
		if (instruction instanceof Label || instruction instanceof GotoInstruction || instruction instanceof NopInstruction)
			return true;

		switch (instruction) {
			case ArrayInstruction array -> {
				return allocator.consume(array.value())
						&& allocator.consume(array.array())
						&& allocator.consume(array.index());
			}
			case ArrayLengthInstruction arrayLength -> {
				return allocator.consume(arrayLength.dest()) && allocator.consume(arrayLength.array());
			}
			case Binary2AddrInstruction binary -> {
				return allocator.consume(binary.a()) && allocator.consume(binary.b());
			}
			case BinaryInstruction binary -> {
				return allocator.consume(binary.dest())
						&& allocator.consume(binary.a())
						&& allocator.consume(binary.b());
			}
			case BinaryLiteralInstruction binary -> {
				return allocator.consume(binary.dest()) && allocator.consume(binary.src());
			}
			case BranchInstruction branch -> {
				return allocator.consume(branch.a()) && allocator.consume(branch.b());
			}
			case BranchZeroInstruction branch -> {
				return allocator.consume(branch.a());
			}
			case CheckCastInstruction checkCast -> {
				return allocator.consume(checkCast.register());
			}
			case CompareInstruction compare -> {
				return allocator.consume(compare.dest())
						&& allocator.consume(compare.a())
						&& allocator.consume(compare.b());
			}
			case ConstInstruction constant -> {
				return allocator.consume(constant.register());
			}
			case ConstTypeInstruction constant -> {
				return allocator.consume(constant.register());
			}
			case ConstWideInstruction constant -> {
				return allocator.consume(constant.register());
			}
			case ConstStringInstruction constant -> {
				return allocator.consume(constant.register());
			}
			case ConstMethodHandleInstruction constant -> {
				return allocator.consume(constant.destination());
			}
			case ConstMethodTypeInstruction constant -> {
				return allocator.consume(constant.destination());
			}
			case FillArrayDataInstruction fillArrayData -> {
				return allocator.consume(fillArrayData.array());
			}
			case FilledNewArrayInstruction filledNewArray -> {
				if (filledNewArray.isRange()) {
					return allocator.consume(filledNewArray.first())
							&& allocator.consume(filledNewArray.last());
				}
				int[] values = filledNewArray.registers();
				if (values == null) {
					return false;
				}
				for (int value : values) {
					if (!allocator.consume(value)) {
						return false;
					}
				}
				return true;
			}
			case InstanceFieldInstruction field -> {
				return allocator.consume(field.value()) && allocator.consume(field.instance());
			}
			case InstanceOfInstruction instanceOf -> {
				return allocator.consume(instanceOf.destination()) && allocator.consume(instanceOf.register());
			}
			case InvokeCustomInstruction invoke -> {
				if (invoke.isRange()) {
					return allocator.consume(invoke.first()) && allocator.consume(invoke.last());
				}
				int[] values = invoke.argumentRegisters();
				if (values == null) {
					return false;
				}
				for (int value : values) {
					if (!allocator.consume(value)) {
						return false;
					}
				}
				return true;
			}
			case InvokeInstruction invoke -> {
				if (invoke.isRange()) {
					return allocator.consume(invoke.first()) && allocator.consume(invoke.last());
				}
				int[] values = invoke.arguments();
				if (values == null) {
					return false;
				}
				for (int value : values) {
					if (!allocator.consume(value)) {
						return false;
					}
				}
				return true;
			}
			case MonitorInstruction monitor -> {
				return allocator.consume(monitor.register());
			}
			case MoveExceptionInstruction moveException -> {
				return allocator.consume(moveException.register());
			}
			case MoveInstruction move -> {
				return allocator.consume(move.to()) && allocator.consume(move.from());
			}
			case MoveObjectInstruction move -> {
				return allocator.consume(move.to()) && allocator.consume(move.from());
			}
			case MoveResultInstruction moveResult -> {
				return allocator.consume(moveResult.to());
			}
			case MoveWideInstruction move -> {
				return allocator.consume(move.to()) && allocator.consume(move.from());
			}
			case NewArrayInstruction newArray -> {
				return allocator.consume(newArray.dest()) && allocator.consume(newArray.sizeRegister());
			}
			case NewInstanceInstruction newInstance -> {
				return allocator.consume(newInstance.dest());
			}
			case PackedSwitchInstruction packedSwitch -> {
				return allocator.consume(packedSwitch.register());
			}
			case ReturnInstruction returnInstruction -> {
				return returnInstruction.opcode() == Opcodes.RETURN_VOID
						|| allocator.consume(returnInstruction.register());
			}
			case SparseSwitchInstruction sparseSwitch -> {
				return allocator.consume(sparseSwitch.register());
			}
			case StaticFieldInstruction field -> {
				return allocator.consume(field.value());
			}
			case ThrowInstruction throwInstruction -> {
				return allocator.consume(throwInstruction.value());
			}
			case UnaryInstruction unary -> {
				return allocator.consume(unary.dest()) && allocator.consume(unary.source());
			}
			default -> {}
		}
		return false;
	}

	/**
	 * Allocates registers for a method, ensuring that no two registers are assigned the same name and that all register names are valid.
	 */
	private static final class RegisterAllocator {
		private final int parameterBase;
		private final int registerCount;
		private final Map<Integer, String> candidates;
		private final Set<Integer> used = new HashSet<>();
		private final Map<String, Integer> assigned = new HashMap<>();
		private int next;

		private RegisterAllocator(int parameterBase, int registerCount, Map<Integer, String> candidates) {
			this.parameterBase = parameterBase;
			this.registerCount = registerCount;
			this.candidates = candidates;
			for (int register = parameterBase; register < registerCount; register++) {
				used.add(register);
			}
		}

		private boolean consume(int register) {
			if (register < 0 || register >= registerCount)
				return false;

			String name = candidates.get(register);
			if (name == null) {
				used.add(register);
				return true;
			}

			Integer existing = assigned.get(name);
			if (existing != null)
				return existing == register;

			int index = next;
			while (index < parameterBase && used.contains(index))
				index++;
			if (index >= parameterBase)
				return false;

			assigned.put(name, index);
			used.add(index);
			next = index + 1;
			return index == register;
		}
	}
}


package me.darknet.assembler.backend.dalvik.printer;

import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikConstantTypeResolver;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.parser.Tokenizer;
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
import me.darknet.dex.tree.type.ClassType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.darknet.assembler.descriptor.Descriptors.isWideType;

public class DalvikMethodPrinter implements MethodPrinter {
	private static final Tokenizer TOKENIZER = new Tokenizer();
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
			DalvikCodePrinter printer = new DalvikCodePrinter(codeObj, registers, labelNames,
					DalvikConstantTypeResolver.resolve(definition, code));
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

		// Use member names if available, otherwise use debug names, and fallback to "pN" for unnamed parameters.
		List<String> names = new ArrayList<>(parameterCount);
		Set<Integer> fallbackAliases = new HashSet<>();
		for (int i = 0; i < parameterCount; i++) {
			String name = i < memberNames.size() ? memberNames.get(i) : null;
			if ((name == null || name.isEmpty()) && i < debugNames.size())
				name = debugNames.get(i);
			if (name == null || name.isEmpty()) {
				names.add(null);
			} else if (isPrintableIdentifier(name)) {
				names.add(name);
			} else {
				names.add("p" + i);
				fallbackAliases.add(i);
			}
		}

		// Remove fallback names that conflict with other parameter names.
		for (int index : fallbackAliases) {
			String alias = names.get(index);
			for (int other = 0; other < parameterCount; other++) {
				if (other != index && alias.equals(names.get(other))) {
					names.set(index, null);
					break;
				}
			}
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

		// Collect registers that aren't usable (reserved for wide values) to avoid assigning names to them.
		Set<Integer> unusableRegisters = new HashSet<>();
		collectWideRegisters(code, unusableRegisters);

		// Fill reserved names with "this" and parameter names to avoid conflicts with candidate register names.
		Set<String> reservedNames = new HashSet<>();
		if ((definition.getAccess() & DalvikModifiers.ACC_STATIC) == 0)
			reservedNames.add("this");
		for (int i = 0; i < definition.getType().parameterTypes().size(); i++)
			reservedNames.add("p" + i);
		reservedNames.addAll(emittedParameterNames);

		// Build map of usable candidates, filtering out invalid or conflicting names.
		Map<Integer, String> usableCandidates = new HashMap<>();
		for (Map.Entry<Integer, String> entry : candidates.entrySet()) {
			String name = entry.getValue();
			if (!isPrintableIdentifier(name) || isNumericRegisterName(name))
				continue;
			if (reservedNames.contains(name))
				return registers;
			usableCandidates.put(entry.getKey(), name);
		}
		if (usableCandidates.isEmpty())
			return registers;

		// Check for duplicate names among usable candidates, which would prevent unique assignment.
		Map<String, Integer> candidateRegisters = new HashMap<>();
		for (Map.Entry<Integer, String> entry : usableCandidates.entrySet()) {
			Integer previous = candidateRegisters.putIfAbsent(entry.getValue(), entry.getKey());
			if (previous != null && previous.intValue() != entry.getKey())
				return registers;
		}

		// Filter usable candidates to exclude unusable registers, ensuring only valid assignments are made.
		Map<Integer, String> namedCandidates = new HashMap<>();
		for (Map.Entry<Integer, String> entry : usableCandidates.entrySet()) {
			if (!unusableRegisters.contains(entry.getKey()))
				namedCandidates.put(entry.getKey(), entry.getValue());
		}
		if (namedCandidates.isEmpty())
			return registers;

		// First pass: reserve registers for named candidates and unusable registers.
		RegisterAllocator allocator = new RegisterAllocator(parameterBase, registerCount, namedCandidates);
		for (Instruction instruction : code.getInstructions()) {
			if (!consumeRegisterOperands(instruction, allocator, true))
				return registers;
		}

		// Second pass: reserve unusable registers.
		for (int register : unusableRegisters) {
			if (!allocator.reserve(register))
				return registers;
		}

		// Third pass: consume register operands for all instructions.
		for (Instruction instruction : code.getInstructions()) {
			if (!consumeRegisterOperands(instruction, allocator, false))
				return registers;
		}
		registers.putAll(namedCandidates);
		return registers;
	}

	/**
	 * @param name
	 * 		The identifier to check.
	 *
	 * @return {@code true} if the identifier is printable and valid, {@code false} otherwise.
	 */
	private static boolean isPrintableIdentifier(@Nullable String name) {
		if (name == null || name.isEmpty())
			return false;

		var result = TOKENIZER.tokenize("<debug-name>", name);
		if (result.hasErrors())
			return false;

		List<Token> tokens = result.requireValue();
		return tokens.size() == 1
				&& tokens.getFirst().type() == TokenType.IDENTIFIER
				&& name.equals(tokens.getFirst().content());
	}

	/**
	 * Marks every register that participates in a wide instruction or wide argument.
	 *
	 * @param code
	 * 		The method code to inspect.
	 * @param unusableRegisters
	 * 		Registers that must retain their numeric names.
	 */
	private static void collectWideRegisters(@NotNull Code code, @NotNull Set<Integer> unusableRegisters) {
		for (Instruction instruction : code.getInstructions()) {
			switch (instruction) {
				case ArrayInstruction array -> {
					if (array.opcode() == Opcodes.AGET_WIDE || array.opcode() == Opcodes.APUT_WIDE)
						markWidePair(unusableRegisters, array.value());
				}
				case Binary2AddrInstruction binary -> markWideBinary2Addr(unusableRegisters, binary);
				case BinaryInstruction binary -> markWideBinary(unusableRegisters, binary);
				case CompareInstruction compare -> {
					if (compare.opcode() == Opcodes.CMP_LONG
							|| compare.opcode() == Opcodes.CMPL_DOUBLE
							|| compare.opcode() == Opcodes.CMPG_DOUBLE) {
						markWidePair(unusableRegisters, compare.a());
						markWidePair(unusableRegisters, compare.b());
					}
				}
				case ConstWideInstruction constant -> markWidePair(unusableRegisters, constant.register());
				case FilledNewArrayInstruction filledNewArray ->
						markWideFilledNewArray(unusableRegisters, filledNewArray);
				case InstanceFieldInstruction field -> {
					if (isWideType(field.type().descriptor()))
						markWidePair(unusableRegisters, field.value());
				}
				case InvokeCustomInstruction invoke -> markWideInvokeCustom(unusableRegisters, invoke);
				case InvokeInstruction invoke -> markWideInvoke(unusableRegisters, invoke);
				case MoveResultInstruction moveResult -> {
					if (moveResult.opcode() == Opcodes.MOVE_RESULT_WIDE)
						markWidePair(unusableRegisters, moveResult.to());
				}
				case MoveWideInstruction move -> {
					markWidePair(unusableRegisters, move.to());
					markWidePair(unusableRegisters, move.from());
				}
				case ReturnInstruction returnInstruction -> {
					if (returnInstruction.opcode() == Opcodes.RETURN_WIDE)
						markWidePair(unusableRegisters, returnInstruction.register());
				}
				case StaticFieldInstruction field -> {
					if (isWideType(field.type().descriptor()))
						markWidePair(unusableRegisters, field.value());
				}
				case UnaryInstruction unary -> markWideUnary(unusableRegisters, unary);
				default -> {}
			}
		}
	}

	/**
	 * Marks the registers used in a wide binary 2-address instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param instruction
	 * 		The binary 2-address instruction to inspect.
	 */
	private static void markWideBinary2Addr(@NotNull Set<Integer> registers,
	                                        @NotNull Binary2AddrInstruction instruction) {
		switch (instruction.opcode()) {
			case Opcodes.ADD_LONG_2ADDR,
			     Opcodes.SUB_LONG_2ADDR,
			     Opcodes.MUL_LONG_2ADDR,
			     Opcodes.DIV_LONG_2ADDR,
			     Opcodes.REM_LONG_2ADDR,
			     Opcodes.AND_LONG_2ADDR,
			     Opcodes.OR_LONG_2ADDR,
			     Opcodes.XOR_LONG_2ADDR,
			     Opcodes.ADD_DOUBLE_2ADDR,
			     Opcodes.SUB_DOUBLE_2ADDR,
			     Opcodes.MUL_DOUBLE_2ADDR,
			     Opcodes.DIV_DOUBLE_2ADDR,
			     Opcodes.REM_DOUBLE_2ADDR -> {
				markWidePair(registers, instruction.a());
				markWidePair(registers, instruction.b());
			}
			case Opcodes.SHL_LONG_2ADDR,
			     Opcodes.SHR_LONG_2ADDR,
			     Opcodes.USHR_LONG_2ADDR -> markWidePair(registers, instruction.a());
			default -> {}
		}
	}

	/**
	 * Marks the registers used in a wide binary instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param instruction
	 * 		The binary instruction to inspect.
	 */
	private static void markWideBinary(@NotNull Set<Integer> registers,
	                                   @NotNull BinaryInstruction instruction) {
		switch (instruction.opcode()) {
			case Opcodes.ADD_LONG,
			     Opcodes.SUB_LONG,
			     Opcodes.MUL_LONG,
			     Opcodes.DIV_LONG,
			     Opcodes.REM_LONG,
			     Opcodes.AND_LONG,
			     Opcodes.OR_LONG,
			     Opcodes.XOR_LONG,
			     Opcodes.ADD_DOUBLE,
			     Opcodes.SUB_DOUBLE,
			     Opcodes.MUL_DOUBLE,
			     Opcodes.DIV_DOUBLE,
			     Opcodes.REM_DOUBLE -> {
				markWidePair(registers, instruction.dest());
				markWidePair(registers, instruction.a());
				markWidePair(registers, instruction.b());
			}
			case Opcodes.SHL_LONG,
			     Opcodes.SHR_LONG,
			     Opcodes.USHR_LONG -> {
				markWidePair(registers, instruction.dest());
				markWidePair(registers, instruction.a());
			}
			default -> {}
		}
	}

	/**
	 * Marks the registers used in a wide unary instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param instruction
	 * 		The unary instruction to inspect.
	 */
	private static void markWideUnary(@NotNull Set<Integer> registers,
	                                  @NotNull UnaryInstruction instruction) {
		switch (instruction.opcode()) {
			case Opcodes.NEG_LONG,
			     Opcodes.NOT_LONG,
			     Opcodes.NEG_DOUBLE,
			     Opcodes.LONG_TO_DOUBLE,
			     Opcodes.DOUBLE_TO_LONG -> {
				markWidePair(registers, instruction.dest());
				markWidePair(registers, instruction.source());
			}
			case Opcodes.INT_TO_LONG,
			     Opcodes.INT_TO_DOUBLE,
			     Opcodes.FLOAT_TO_LONG,
			     Opcodes.FLOAT_TO_DOUBLE -> markWidePair(registers, instruction.dest());
			case Opcodes.LONG_TO_INT,
			     Opcodes.LONG_TO_FLOAT,
			     Opcodes.DOUBLE_TO_INT,
			     Opcodes.DOUBLE_TO_FLOAT -> markWidePair(registers, instruction.source());
			default -> {}
		}
	}

	/**
	 * Marks the registers used in a wide invoke instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param instruction
	 * 		The invoke instruction to inspect.
	 */
	private static void markWideInvoke(@NotNull Set<Integer> registers, @NotNull InvokeInstruction instruction) {
		int position = instruction.opcode() == Opcodes.INVOKE_STATIC ? 0 : 1;
		for (ClassType parameter : instruction.type().parameterTypes()) {
			if (isWideType(parameter.descriptor()))
				markWideArgument(registers, instruction, position);
			position += isWideType(parameter.descriptor()) ? 2 : 1;
		}
	}

	/**
	 * Marks the registers used in a wide invoke-custom instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param instruction
	 * 		The invoke-custom instruction to inspect.
	 */
	private static void markWideInvokeCustom(@NotNull Set<Integer> registers,
	                                         @NotNull InvokeCustomInstruction instruction) {
		int position = 0;
		for (ClassType parameter : instruction.type().parameterTypes()) {
			if (isWideType(parameter.descriptor()))
				markWideArgument(registers, instruction, position);
			position += isWideType(parameter.descriptor()) ? 2 : 1;
		}
	}

	/**
	 * Marks the registers used in a wide filled-new-array instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param instruction
	 * 		The filled-new-array instruction to inspect.
	 */
	private static void markWideFilledNewArray(@NotNull Set<Integer> registers,
	                                           @NotNull FilledNewArrayInstruction instruction) {
		if (!isWideArrayComponent(instruction.componentType().descriptor()))
			return;
		int wordCount = instruction.isRange()
				? instruction.last() - instruction.first() + 1
				: instruction.registers().length;
		for (int position = 0; position < wordCount; position += 2)
			markWideArgument(registers, instruction, position);
	}

	/**
	 * Marks the registers used in a wide argument of an invoke instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param instruction
	 * 		The invoke instruction to inspect.
	 * @param position
	 * 		The position of the argument to inspect.
	 */
	private static void markWideArgument(@NotNull Set<Integer> registers, @NotNull InvokeInstruction instruction,
	                                     int position) {
		int register = instruction.isRange()
				? instruction.first() + position
				: instruction.arguments() != null && position < instruction.arguments().length
				? instruction.arguments()[position]
				: -1;
		markWidePair(registers, register);
	}

	/**
	 * Marks the registers used in a wide argument of an invoke-custom instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param instruction
	 * 		The invoke-custom instruction to inspect.
	 * @param position
	 * 		The position of the argument to inspect.
	 */
	private static void markWideArgument(@NotNull Set<Integer> registers,
	                                     @NotNull InvokeCustomInstruction instruction, int position) {
		int register = instruction.isRange()
				? instruction.first() + position
				: instruction.argumentRegisters() != null && position < instruction.argumentRegisters().length
				? instruction.argumentRegisters()[position]
				: -1;
		markWidePair(registers, register);
	}

	/**
	 * Marks the registers used in a wide argument of a filled-new-array instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param instruction
	 * 		The filled-new-array instruction to inspect.
	 * @param position
	 * 		The position of the argument to inspect.
	 */
	private static void markWideArgument(@NotNull Set<Integer> registers,
	                                     @NotNull FilledNewArrayInstruction instruction, int position) {
		int register = instruction.isRange()
				? instruction.first() + position
				: instruction.registers() != null && position < instruction.registers().length
				? instruction.registers()[position]
				: -1;
		markWidePair(registers, register);
	}

	/**
	 * Marks a register and its adjacent register as used in a wide instruction.
	 *
	 * @param registers
	 * 		The set of registers to mark.
	 * @param register
	 * 		The register to mark.
	 */
	private static void markWidePair(@NotNull Set<Integer> registers, int register) {
		if (register >= 0) {
			registers.add(register);
			if (register < Integer.MAX_VALUE)
				registers.add(register + 1);
		}
	}

	/**
	 * @param descriptor
	 * 		The type descriptor to check.
	 *
	 * @return {@code true} if the descriptor represents a wide array component type (long[] or double[]), {@code false} otherwise.
	 */
	private static boolean isWideArrayComponent(@NotNull String descriptor) {
		return isWideType(descriptor)
				|| (descriptor.length() == 2 && descriptor.charAt(0) == '['
				&& isWideType(descriptor.substring(1)));
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
	 * @param reserveOnly
	 *        {@code true} if the allocator should only reserve registers without consuming them, {@code false} to consume them.
	 *
	 * @return {@code true} if all register operands were successfully consumed, {@code false} otherwise.
	 */
	private static boolean consumeRegisterOperands(@NotNull Instruction instruction,
	                                               @NotNull RegisterAllocator allocator,
	                                               boolean reserveOnly) {
		if (instruction instanceof Label || instruction instanceof GotoInstruction || instruction instanceof NopInstruction)
			return true;

		switch (instruction) {
			case ArrayInstruction array -> {
				return allocator.consume(array.value(), reserveOnly)
						&& allocator.consume(array.array(), reserveOnly)
						&& allocator.consume(array.index(), reserveOnly);
			}
			case ArrayLengthInstruction arrayLength -> {
				return allocator.consume(arrayLength.dest(), reserveOnly)
						&& allocator.consume(arrayLength.array(), reserveOnly);
			}
			case Binary2AddrInstruction binary -> {
				return allocator.consume(binary.a(), reserveOnly)
						&& allocator.consume(binary.b(), reserveOnly);
			}
			case BinaryInstruction binary -> {
				return allocator.consume(binary.dest(), reserveOnly)
						&& allocator.consume(binary.a(), reserveOnly)
						&& allocator.consume(binary.b(), reserveOnly);
			}
			case BinaryLiteralInstruction binary -> {
				return allocator.consume(binary.dest(), reserveOnly)
						&& allocator.consume(binary.src(), reserveOnly);
			}
			case BranchInstruction branch -> {
				return allocator.consume(branch.a(), reserveOnly)
						&& allocator.consume(branch.b(), reserveOnly);
			}
			case BranchZeroInstruction branch -> {
				return allocator.consume(branch.a(), reserveOnly);
			}
			case CheckCastInstruction checkCast -> {
				return allocator.consume(checkCast.register(), reserveOnly);
			}
			case CompareInstruction compare -> {
				return allocator.consume(compare.dest(), reserveOnly)
						&& allocator.consume(compare.a(), reserveOnly)
						&& allocator.consume(compare.b(), reserveOnly);
			}
			case ConstInstruction constant -> {
				return allocator.consume(constant.register(), reserveOnly);
			}
			case ConstTypeInstruction constant -> {
				return allocator.consume(constant.register(), reserveOnly);
			}
			case ConstWideInstruction constant -> {
				return allocator.consume(constant.register(), reserveOnly);
			}
			case ConstStringInstruction constant -> {
				return allocator.consume(constant.register(), reserveOnly);
			}
			case ConstMethodHandleInstruction constant -> {
				return allocator.consume(constant.destination(), reserveOnly);
			}
			case ConstMethodTypeInstruction constant -> {
				return allocator.consume(constant.destination(), reserveOnly);
			}
			case FillArrayDataInstruction fillArrayData -> {
				return allocator.consume(fillArrayData.array(), reserveOnly);
			}
			case FilledNewArrayInstruction filledNewArray -> {
				if (filledNewArray.isRange()) {
					if (reserveOnly)
						return allocator.reserveRange(filledNewArray.first(), filledNewArray.last());
					return allocator.consume(filledNewArray.first(), false)
							&& allocator.consume(filledNewArray.last(), false);
				}
				int[] values = filledNewArray.registers();
				if (values == null)
					return false;
				for (int value : values) {
					if (!allocator.consume(value, reserveOnly))
						return false;
				}
				return true;
			}
			case InstanceFieldInstruction field -> {
				return allocator.consume(field.value(), reserveOnly)
						&& allocator.consume(field.instance(), reserveOnly);
			}
			case InstanceOfInstruction instanceOf -> {
				return allocator.consume(instanceOf.destination(), reserveOnly)
						&& allocator.consume(instanceOf.register(), reserveOnly);
			}
			case InvokeCustomInstruction invoke -> {
				if (invoke.isRange()) {
					if (reserveOnly)
						return allocator.reserveRange(invoke.first(), invoke.last());
					return allocator.consume(invoke.first(), false)
							&& allocator.consume(invoke.last(), false);
				}
				int[] values = invoke.argumentRegisters();
				if (values == null)
					return false;
				for (int value : values) {
					if (!allocator.consume(value, reserveOnly))
						return false;
				}
				return true;
			}
			case InvokeInstruction invoke -> {
				if (invoke.isRange()) {
					if (reserveOnly)
						return allocator.reserveRange(invoke.first(), invoke.last());
					return allocator.consume(invoke.first(), false)
							&& allocator.consume(invoke.last(), false);
				}
				int[] values = invoke.arguments();
				if (values == null)
					return false;
				for (int value : values) {
					if (!allocator.consume(value, reserveOnly))
						return false;
				}
				return true;
			}
			case MonitorInstruction monitor -> {
				return allocator.consume(monitor.register(), reserveOnly);
			}
			case MoveExceptionInstruction moveException -> {
				return allocator.consume(moveException.register(), reserveOnly);
			}
			case MoveInstruction move -> {
				return allocator.consume(move.to(), reserveOnly)
						&& allocator.consume(move.from(), reserveOnly);
			}
			case MoveObjectInstruction move -> {
				return allocator.consume(move.to(), reserveOnly)
						&& allocator.consume(move.from(), reserveOnly);
			}
			case MoveResultInstruction moveResult -> {
				return allocator.consume(moveResult.to(), reserveOnly);
			}
			case MoveWideInstruction move -> {
				return allocator.consume(move.to(), reserveOnly)
						&& allocator.consume(move.from(), reserveOnly);
			}
			case NewArrayInstruction newArray -> {
				return allocator.consume(newArray.dest(), reserveOnly)
						&& allocator.consume(newArray.sizeRegister(), reserveOnly);
			}
			case NewInstanceInstruction newInstance -> {
				return allocator.consume(newInstance.dest(), reserveOnly);
			}
			case PackedSwitchInstruction packedSwitch -> {
				return allocator.consume(packedSwitch.register(), reserveOnly);
			}
			case ReturnInstruction returnInstruction -> {
				return returnInstruction.opcode() == Opcodes.RETURN_VOID
						|| allocator.consume(returnInstruction.register(), reserveOnly);
			}
			case SparseSwitchInstruction sparseSwitch -> {
				return allocator.consume(sparseSwitch.register(), reserveOnly);
			}
			case StaticFieldInstruction field -> {
				return allocator.consume(field.value(), reserveOnly);
			}
			case ThrowInstruction throwInstruction -> {
				return allocator.consume(throwInstruction.value(), reserveOnly);
			}
			case UnaryInstruction unary -> {
				return allocator.consume(unary.dest(), reserveOnly)
						&& allocator.consume(unary.source(), reserveOnly);
			}
			default -> {}
		}
		return false;
	}

	/**
	 * A simple register allocator that assigns names to registers based on candidates and usage.
	 */
	private static final class RegisterAllocator {
		private final int parameterBase;
		private final int registerCount;
		private final Map<Integer, String> candidates;
		private final Set<Integer> used = new HashSet<>();
		private final Map<String, Integer> assigned = new HashMap<>();
		private int next;

		/**
		 * @param parameterBase
		 * 		The base index for parameter registers.
		 * @param registerCount
		 * 		The total number of registers available.
		 * @param candidates
		 * 		A map of candidate register indices to their corresponding names.
		 */
		private RegisterAllocator(int parameterBase, int registerCount, @NotNull Map<Integer, String> candidates) {
			this.parameterBase = parameterBase;
			this.registerCount = registerCount;
			this.candidates = candidates;
			for (int register = parameterBase; register < registerCount; register++)
				used.add(register);
		}

		/**
		 * Consumes a register, assigning a name if it's a candidate and not already used.
		 *
		 * @param register
		 * 		The register index to consume.
		 * @param reserveOnly
		 *        {@code true} if the allocator should only reserve the register without consuming it, {@code false} to consume it.
		 *
		 * @return {@code true} if the register was successfully consumed or reserved, {@code false} otherwise.
		 */
		private boolean consume(int register, boolean reserveOnly) {
			if (reserveOnly)
				return reserve(register);
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

		/**
		 * @param register
		 * 		The register index to reserve.
		 *
		 * @return {@code true} if the register was successfully reserved, {@code false} otherwise.
		 */
		private boolean reserve(int register) {
			if (register < 0 || register >= registerCount)
				return false;
			if (!candidates.containsKey(register))
				used.add(register);
			return true;
		}

		/**
		 * @param first
		 * 		The first register index in the range.
		 * @param last
		 * 		The last register index in the range.
		 *
		 * @return {@code true} if the range was successfully reserved, {@code false} otherwise.
		 */
		private boolean reserveRange(int first, int last) {
			if (first < 0 || first >= registerCount || last < 0 || last >= registerCount || first > last)
				return false;
			if (!candidates.containsKey(first) && !candidates.containsKey(last)) {
				for (int register = first; register <= last; register++)
					used.add(register);
				return true;
			}
			return reserve(first) && reserve(last);
		}
	}
}


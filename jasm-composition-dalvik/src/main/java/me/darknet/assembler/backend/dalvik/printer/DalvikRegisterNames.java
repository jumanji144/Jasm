package me.darknet.assembler.backend.dalvik.printer;

import me.darknet.assembler.backend.dalvik.DalvikParameterNames;
import me.darknet.assembler.backend.dalvik.instructions.DalvikInvokeLayout;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.*;
import me.darknet.dex.tree.type.ClassType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.darknet.assembler.descriptor.Descriptors.isWideType;

/**
 * Chooses the names the method printer gives to register slots.
 * <p>
 * A parameter prints by its source name at its first slot, and the receiver prints as {@code this}. A named local prints
 * by its debug name only when compiling the printed body gives that name the same slot again. The compiler reserves
 * every slot written numerically, then gives each new name the first free slot below the parameters, in the order the
 * names first appear in the body. This class replays that allocation over the decoded instructions, in the order the
 * compiler resolves their operands, and keeps the local names only when every one of them lands on its recorded slot.
 * When every pooled local replays, the method keeps all of them. Otherwise locals are added greedily in debug-entry order,
 * and each one is kept only when the whole set still replays, so a printed body never recompiles to other slots.
 */
final class DalvikRegisterNames {
	private static final Tokenizer TOKENIZER = new Tokenizer();

	private DalvikRegisterNames() {}

	/**
	 * @param code
	 * 		Method body whose slots are named.
	 *
	 * @return Every slot under its numeric name.
	 */
	private static @NotNull Map<Integer, String> slotNames(@NotNull Code code) {
		Map<Integer, String> registers = new HashMap<>();
		for (int i = 0; i < code.getRegisters(); i++)
			registers.put(i, "v" + i);
		return registers;
	}

	/**
	 * @param code
	 * 		Method body whose slots are named.
	 * @param instance
	 * 		{@code true} when the method has a receiver, which is named {@code this}.
	 * @param parameters
	 * 		Parameter types of the method, in declaration order.
	 * @param parameterNames
	 * 		Source name of every parameter, or {@code null} when the body keeps parameter slots numeric. Callers pass the
	 * 		names only when every parameter has one.
	 *
	 * @return Printed name of each slot. A slot without a name keeps its numeric name.
	 */
	static @NotNull Map<Integer, String> resolve(@NotNull Code code, boolean instance,
	                                             @NotNull List<ClassType> parameters,
	                                             @NotNull List<String> parameterNames) {
		Map<Integer, String> registers = slotNames(code);
		int registerCount = code.getRegisters();
		int parameterBase = registerCount - code.getIn();
		if (parameterBase < 0 || parameterBase > registerCount)
			return registers;

		nameParameters(registers, instance, parameters, parameterNames, parameterBase);
		registers.putAll(localNames(code, instance, parameters, parameterNames, parameterBase, registerCount));
		return registers;
	}

	/**
	 * @param name
	 * 		The identifier to check.
	 *
	 * @return {@code true} if the identifier tokenizes as exactly one identifier spelled as given.
	 */
	static boolean isPrintableIdentifier(@Nullable String name) {
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
	 * Names the receiver and every parameter at the slot where the compiler maps it. A wide parameter's high word stays
	 * numeric.
	 *
	 * @param registers
	 * 		Printed name of each slot, which this method updates in place.
	 * @param instance
	 * 		{@code true} when the method has a receiver, which is named {@code this}.
	 * @param parameters
	 * 		Parameter types of the method, in declaration order.
	 * @param parameterNames
	 * 		Source name of each parameter, in declaration order.
	 * @param parameterBase
	 * 		Slot of the first incoming word, which is the receiver's slot when the method has one.
	 */
	private static void nameParameters(@NotNull Map<Integer, String> registers, boolean instance,
	                                   @NotNull List<ClassType> parameters, @NotNull List<String> parameterNames,
	                                   int parameterBase) {
		int slot = parameterBase;
		if (instance)
			registers.put(slot++, "this");
		for (int index = 0; index < parameters.size(); index++) {
			registers.put(slot, parameterNames.get(index));
			slot += isWideType(parameters.get(index).descriptor()) ? 2 : 1;
		}
	}

	/**
	 * @param code
	 * 		Method body whose locals are resolved.
	 * @param instance
	 * 		{@code true} when the method has a receiver, whose name {@code this} a local may not take.
	 * @param parameters
	 * 		Parameter types of the method, in declaration order. A local may not take the positional alias {@code pN} of
	 * 		any of them.
	 * @param parameterNames
	 * 		Source name of every parameter, or {@code null} when the body keeps parameter slots numeric. A local may not
	 * 		take one of these names.
	 * @param parameterBase
	 * 		Slot of the first parameter. Only slots below it can hold locals.
	 * @param registerCount
	 * 		Number of registers the method declares. Debug entries for slots at or beyond it are ignored.
	 *
	 * @return Local names that survive the replay, keyed by slot. Empty when no local can be replayed.
	 */
	private static @NotNull Map<Integer, String> localNames(@NotNull Code code, boolean instance,
	                                                        @NotNull List<ClassType> parameters,
	                                                        @NotNull List<String> parameterNames,
	                                                        int parameterBase, int registerCount) {
		DebugInformation debugInfo = code.getDebugInfo();
		if (debugInfo == null || debugInfo.locals() == null || debugInfo.locals().isEmpty())
			return Map.of();

		// The first entry for a slot names it, in debug-entry order. Only slots below the parameters are locals.
		Map<Integer, String> candidates = new LinkedHashMap<>();
		for (DebugInformation.LocalVariable local : debugInfo.locals()) {
			if (local == null
					|| local.register() < 0
					|| local.register() >= parameterBase
					|| local.register() >= registerCount)
				continue;
			candidates.putIfAbsent(local.register(), local.name());
		}

		// A candidate joins the pool when it can be printed and does not claim a name the body already binds. A name that an
		// earlier candidate holds is excluded too. Each candidate is excluded alone, so one bad name never disables the others.
		Set<String> reserved = reservedNames(instance, parameters, parameterNames);
		Map<Integer, String> pool = new LinkedHashMap<>();
		Set<String> claimed = new HashSet<>();
		for (Map.Entry<Integer, String> entry : candidates.entrySet()) {
			String name = entry.getValue();
			if (!isPrintableIdentifier(name) || DalvikParameterNames.isNumericRegisterName(name) || reserved.contains(name))
				continue;
			if (claimed.add(name))
				pool.put(entry.getKey(), name);
		}
		if (pool.isEmpty())
			return Map.of();

		List<Use> uses = new ArrayList<>();
		for (Instruction instruction : code.getInstructions()) {
			if (!collectUses(instruction, uses))
				return Map.of();
		}

		// When every pooled local replays, the method keeps all of them, as it always has.
		Map<Integer, String> all = allocate(uses, pool, parameterBase, registerCount);
		if (!all.isEmpty())
			return all;

		// Otherwise add locals greedily in debug-entry order, and keep each one only when the whole set still replays.
		Map<Integer, String> chosen = new LinkedHashMap<>();
		for (Map.Entry<Integer, String> entry : pool.entrySet()) {
			Map<Integer, String> trial = new LinkedHashMap<>(chosen);
			trial.put(entry.getKey(), entry.getValue());
			if (allocate(uses, trial, parameterBase, registerCount).containsKey(entry.getKey()))
				chosen = trial;
		}
		return chosen.isEmpty() ? Map.of() : allocate(uses, chosen, parameterBase, registerCount);
	}

	/**
	 * @param instance
	 * 		{@code true} when the method has a receiver, which the body binds as {@code this}.
	 * @param parameters
	 * 		Parameter types of the method, in declaration order.
	 * @param parameterNames
	 * 		The name printed for every parameter, which is its source name or its positional alias.
	 *
	 * @return The names the body already binds. A local may not take one of them.
	 */
	private static @NotNull Set<String> reservedNames(boolean instance, @NotNull List<ClassType> parameters,
	                                                  @NotNull List<String> parameterNames) {
		Set<String> reserved = new HashSet<>();
		if (instance)
			reserved.add("this");
		for (int index = 0; index < parameters.size(); index++)
			reserved.add("p" + index);
		reserved.addAll(parameterNames);
		return reserved;
	}

	/**
	 * Replays the compiler's allocation. Every slot a numeric operand names is reserved first, as the compiler reserves
	 * explicit registers. Each name then takes the first free slot from the start of the locals, in order of first use,
	 * and a wide name takes two free adjacent slots. A name keeps its local names only if every slot matches the recorded slot.
	 *
	 * @param uses
	 * 		Register operands of the method, in the order the compiler resolves them.
	 * @param names
	 * 		Candidate local names keyed by their recorded slot.
	 * @param parameterBase
	 * 		Slot of the first parameter. A local may take only a slot below it.
	 * @param registerCount
	 * 		Number of registers the method declares. Every operand must name a slot below it.
	 *
	 * @return The names that replayed to their recorded slots, or an empty map when any name did not.
	 */
	private static @NotNull Map<Integer, String> allocate(@NotNull List<Use> uses, @NotNull Map<Integer, String> names,
	                                                      int parameterBase, int registerCount) {
		Map<Integer, Boolean> wideNames = new HashMap<>();
		Set<Integer> used = new HashSet<>();
		for (Use use : uses) {
			int register = use.register();
			if (register < 0 || register >= registerCount)
				return Map.of();
			if (names.containsKey(register)) {
				Boolean previous = wideNames.putIfAbsent(register, use.wide());
				if (previous != null && previous.booleanValue() != use.wide())
					return Map.of();
				continue;
			}
			used.add(register);
			if (use.wide()) {
				if (register + 1 >= registerCount)
					return Map.of();
				used.add(register + 1);
			}
		}

		Map<Integer, String> allocated = new HashMap<>();
		int next = 0;
		for (Use use : uses) {
			int slot = use.register();
			if (!names.containsKey(slot) || allocated.containsKey(slot))
				continue;

			int index = next;
			if (wideNames.get(slot)) {
				while (index + 1 < parameterBase && (used.contains(index) || used.contains(index + 1)))
					index++;
				if (index + 1 >= parameterBase)
					return Map.of();
				used.add(index);
				used.add(index + 1);
			} else {
				while (index < parameterBase && used.contains(index))
					index++;
				if (index >= parameterBase)
					return Map.of();
				used.add(index);
			}
			if (index != slot)
				return Map.of();

			next = index + 1;
			allocated.put(slot, names.get(slot));
		}
		return allocated;
	}

	/**
	 * Appends the register operands of an instruction in the order the compiler resolves them. An operand the printer
	 * spells by name is replayed in that same position, so the allocation sees the same sequence the recompile will.
	 *
	 * @param instruction
	 * 		Instruction whose register operands are appended.
	 * @param uses
	 * 		Operand list that receives the operands, in the order the compiler resolves them.
	 *
	 * @return {@code false} when the instruction's operands are not known, which disables local names for the method.
	 */
	private static boolean collectUses(@NotNull Instruction instruction, @NotNull List<Use> uses) {
		switch (instruction) {
			case Label label -> {}
			case GotoInstruction jump -> {}
			case NopInstruction nop -> {}
			case ArrayInstruction array -> {
				// The compiler resolves operands in the order the source writes them, which is value, array, then index.
				boolean wide = array.opcode() == Opcodes.AGET_WIDE || array.opcode() == Opcodes.APUT_WIDE;
				uses.add(new Use(array.value(), wide));
				uses.add(new Use(array.array(), false));
				uses.add(new Use(array.index(), false));
			}
			case ArrayLengthInstruction arrayLength -> {
				uses.add(new Use(arrayLength.dest(), false));
				uses.add(new Use(arrayLength.array(), false));
			}
			case Binary2AddrInstruction binary -> {
				boolean[] wide = twoAddressWidths(binary.opcode());
				// The printed form repeats the destination as the first source, which the compiler resolves in turn.
				uses.add(new Use(binary.a(), wide[0]));
				uses.add(new Use(binary.a(), wide[0]));
				uses.add(new Use(binary.b(), wide[1]));
			}
			case BinaryInstruction binary -> {
				boolean[] wide = threeAddressWidths(binary.opcode());
				uses.add(new Use(binary.dest(), wide[0]));
				uses.add(new Use(binary.a(), wide[1]));
				uses.add(new Use(binary.b(), wide[2]));
			}
			case BinaryLiteralInstruction binary -> {
				uses.add(new Use(binary.dest(), false));
				uses.add(new Use(binary.src(), false));
			}
			case BranchInstruction branch -> {
				uses.add(new Use(branch.a(), false));
				uses.add(new Use(branch.b(), false));
			}
			case BranchZeroInstruction branch -> uses.add(new Use(branch.a(), false));
			case CheckCastInstruction checkCast -> uses.add(new Use(checkCast.register(), false));
			case CompareInstruction compare -> {
				boolean wide = compare.opcode() == Opcodes.CMP_LONG
						|| compare.opcode() == Opcodes.CMPL_DOUBLE
						|| compare.opcode() == Opcodes.CMPG_DOUBLE;
				uses.add(new Use(compare.dest(), false));
				uses.add(new Use(compare.a(), wide));
				uses.add(new Use(compare.b(), wide));
			}
			case ConstInstruction constant -> uses.add(new Use(constant.register(), false));
			case ConstTypeInstruction constant -> uses.add(new Use(constant.register(), false));
			case ConstWideInstruction constant -> uses.add(new Use(constant.register(), true));
			case ConstStringInstruction constant -> uses.add(new Use(constant.register(), false));
			case ConstMethodHandleInstruction constant -> uses.add(new Use(constant.destination(), false));
			case ConstMethodTypeInstruction constant -> uses.add(new Use(constant.destination(), false));
			case FillArrayDataInstruction fillArrayData -> uses.add(new Use(fillArrayData.array(), false));
			case FilledNewArrayInstruction filledNewArray -> {
				if (isWideArrayComponent(filledNewArray.componentType().descriptor()))
					return false;
				for (int register : DalvikCodePrinter.filledArrayWords(filledNewArray))
					uses.add(new Use(register, false));
			}
			case InstanceFieldInstruction field -> {
				uses.add(new Use(field.value(), isWideType(field.type().descriptor())));
				uses.add(new Use(field.instance(), false));
			}
			case InstanceOfInstruction instanceOf -> {
				uses.add(new Use(instanceOf.destination(), false));
				uses.add(new Use(instanceOf.register(), false));
			}
			case InvokeCustomInstruction invoke -> {
				int[] words = invoke.isRange()
						? DalvikCodePrinter.rangeWords(invoke.first(), invoke.last())
						: invoke.argumentRegisters();
				if (words == null)
					return false;
				// invoke-custom takes no receiver, so its words are the call-site arguments alone.
				addInvokeUses(uses, invoke.type().parameterTypes(), false, words);
			}
			case InvokeInstruction invoke -> {
				int[] words = invoke.isRange()
						? DalvikCodePrinter.rangeWords(invoke.first(), invoke.last())
						: invoke.arguments();
				if (words == null)
					return false;
				addInvokeUses(uses, invoke.type().parameterTypes(), invoke.opcode() != Opcodes.INVOKE_STATIC, words);
			}
			case MonitorInstruction monitor -> uses.add(new Use(monitor.register(), false));
			case MoveExceptionInstruction moveException -> uses.add(new Use(moveException.register(), false));
			case MoveInstruction move -> {
				uses.add(new Use(move.to(), false));
				uses.add(new Use(move.from(), false));
			}
			case MoveObjectInstruction move -> {
				uses.add(new Use(move.to(), false));
				uses.add(new Use(move.from(), false));
			}
			case MoveResultInstruction moveResult ->
					uses.add(new Use(moveResult.to(), moveResult.opcode() == Opcodes.MOVE_RESULT_WIDE));
			case MoveWideInstruction move -> {
				uses.add(new Use(move.to(), true));
				uses.add(new Use(move.from(), true));
			}
			case NewArrayInstruction newArray -> {
				uses.add(new Use(newArray.dest(), false));
				uses.add(new Use(newArray.sizeRegister(), false));
			}
			case NewInstanceInstruction newInstance -> uses.add(new Use(newInstance.dest(), false));
			case PackedSwitchInstruction packedSwitch -> uses.add(new Use(packedSwitch.register(), false));
			case ReturnInstruction returnInstruction -> {
				if (returnInstruction.opcode() != Opcodes.RETURN_VOID)
					uses.add(new Use(returnInstruction.register(),
							returnInstruction.opcode() == Opcodes.RETURN_WIDE));
			}
			case SparseSwitchInstruction sparseSwitch -> uses.add(new Use(sparseSwitch.register(), false));
			case StaticFieldInstruction field -> uses.add(new Use(field.value(), isWideType(field.type().descriptor())));
			case ThrowInstruction throwInstruction -> uses.add(new Use(throwInstruction.value(), false));
			case UnaryInstruction unary -> {
				boolean[] wide = unaryWidths(unary.opcode());
				uses.add(new Use(unary.dest(), wide[0]));
				uses.add(new Use(unary.source(), wide[1]));
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/**
	 * Appends the operands of an invoke in the order the printer spells them. A call that groups faithfully names one
	 * register per logical argument, and a wide argument's tail is implied by its base. Any other call is spelled as raw
	 * words, and each word names one register.
	 *
	 * @param uses
	 * 		Operand list that receives the operands, in the order the printer spells them.
	 * @param parameters
	 * 		Parameter types of the prototype the call is grouped against.
	 * @param receiver
	 * 		{@code true} when the first word is the receiver of an instance call.
	 * @param words
	 * 		Register words of the call, in the order they are encoded.
	 */
	private static void addInvokeUses(@NotNull List<Use> uses, @NotNull List<ClassType> parameters, boolean receiver,
	                                  int @NotNull [] words) {
		if (!DalvikInvokeLayout.groupsFaithfully(parameters, receiver, words)) {
			for (int word : words)
				uses.add(new Use(word, false));
			return;
		}

		int[] logical = DalvikInvokeLayout.logicalRegisters(parameters, receiver, words);
		int entry = 0;
		if (receiver)
			uses.add(new Use(logical[entry++], false));
		for (ClassType parameter : parameters)
			uses.add(new Use(logical[entry++], isWideType(parameter.descriptor())));
	}

	/**
	 * @param opcode
	 * 		Three-address binary opcode whose operand widths are wanted.
	 *
	 * @return Widths of the destination and two sources of a three-address binary opcode, in that order.
	 */
	private static boolean @NotNull [] threeAddressWidths(int opcode) {
		return switch (opcode) {
			case Opcodes.ADD_LONG, Opcodes.SUB_LONG, Opcodes.MUL_LONG, Opcodes.DIV_LONG, Opcodes.REM_LONG,
			     Opcodes.AND_LONG, Opcodes.OR_LONG, Opcodes.XOR_LONG,
			     Opcodes.ADD_DOUBLE, Opcodes.SUB_DOUBLE, Opcodes.MUL_DOUBLE, Opcodes.DIV_DOUBLE, Opcodes.REM_DOUBLE ->
					new boolean[]{true, true, true};
			case Opcodes.SHL_LONG, Opcodes.SHR_LONG, Opcodes.USHR_LONG -> new boolean[]{true, true, false};
			default -> new boolean[]{false, false, false};
		};
	}

	/**
	 * @param opcode
	 * 		Two-address binary opcode whose operand widths are wanted.
	 *
	 * @return Widths of the destination-and-first-source and the second source of a two-address binary opcode, in that order.
	 */
	private static boolean @NotNull [] twoAddressWidths(int opcode) {
		return switch (opcode) {
			case Opcodes.ADD_LONG_2ADDR, Opcodes.SUB_LONG_2ADDR, Opcodes.MUL_LONG_2ADDR, Opcodes.DIV_LONG_2ADDR,
			     Opcodes.REM_LONG_2ADDR, Opcodes.AND_LONG_2ADDR, Opcodes.OR_LONG_2ADDR, Opcodes.XOR_LONG_2ADDR,
			     Opcodes.ADD_DOUBLE_2ADDR, Opcodes.SUB_DOUBLE_2ADDR, Opcodes.MUL_DOUBLE_2ADDR,
			     Opcodes.DIV_DOUBLE_2ADDR, Opcodes.REM_DOUBLE_2ADDR -> new boolean[]{true, true};
			case Opcodes.SHL_LONG_2ADDR, Opcodes.SHR_LONG_2ADDR, Opcodes.USHR_LONG_2ADDR -> new boolean[]{true, false};
			default -> new boolean[]{false, false};
		};
	}

	/**
	 * @param opcode
	 * 		Unary opcode whose operand widths are wanted.
	 *
	 * @return Widths of the destination and the source of a unary opcode, in that order.
	 */
	private static boolean @NotNull [] unaryWidths(int opcode) {
		return switch (opcode) {
			case Opcodes.NEG_LONG, Opcodes.NOT_LONG, Opcodes.NEG_DOUBLE, Opcodes.LONG_TO_DOUBLE,
			     Opcodes.DOUBLE_TO_LONG -> new boolean[]{true, true};
			case Opcodes.INT_TO_LONG, Opcodes.INT_TO_DOUBLE, Opcodes.FLOAT_TO_LONG,
			     Opcodes.FLOAT_TO_DOUBLE -> new boolean[]{true, false};
			case Opcodes.LONG_TO_INT, Opcodes.LONG_TO_FLOAT, Opcodes.DOUBLE_TO_INT,
			     Opcodes.DOUBLE_TO_FLOAT -> new boolean[]{false, true};
			default -> new boolean[]{false, false};
		};
	}

	/**
	 * @param descriptor
	 * 		Descriptor of the component type to check.
	 *
	 * @return {@code true} if the descriptor is {@code long} or {@code double}, or an array of either.
	 */
	private static boolean isWideArrayComponent(@NotNull String descriptor) {
		return isWideType(descriptor)
				|| (descriptor.length() == 2 && descriptor.charAt(0) == '['
				&& isWideType(descriptor.substring(1)));
	}



	/**
	 * One register operand, in the order the compiler resolves it.
	 *
	 * @param register
	 * 		Slot the operand names.
	 * @param wide
	 * 		{@code true} when the operand is a wide value, which also occupies the slot after it.
	 */
	private record Use(int register, boolean wide) {}
}

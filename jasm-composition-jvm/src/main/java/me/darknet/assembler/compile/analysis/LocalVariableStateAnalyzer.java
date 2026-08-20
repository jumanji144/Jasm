package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Infers local-variable types from scoped debug metadata and verifier frames.
 */
public final class LocalVariableStateAnalyzer {
	private LocalVariableStateAnalyzer() {}

	/**
	 * @param method
	 * 		Method to analyze for local-variable states.
	 * @param results
	 * 		Verifier analysis results for the method.
	 * @param checker
	 * 		Inheritance checker used to for type merging.
	 *
	 * @return Scoped local-variable states.
	 */
	public static @NotNull List<LocalVariableState> infer(@NotNull MethodNode method,
	                                                      @NotNull AnalysisResults results,
	                                                      @NotNull InheritanceChecker checker) {
		List<LocalVariableState> states = new ArrayList<>();
		boolean foundValidRange = false;
		if (method.localVariables != null) {
			for (LocalVariableNode local : method.localVariables) {
				LocalVariableState state = inferTableState(method, results, checker, local);
				if (state == null)
					continue;
				foundValidRange = true;
				states.add(state);
			}
		}

		// Methods without usable debug ranges still expose contiguous states from analysis frames.
		if (!foundValidRange)
			states.addAll(inferFrameStates(method, results, checker));

		return List.copyOf(states);
	}

	/**
	 * @param method
	 * 		Method to analyze.
	 * @param results
	 * 		Verifier analysis results for the method.
	 * @param checker
	 * 		Inheritance checker used to for type merging.
	 * @param local
	 * 		Local variable in the local-variable table to infer a state for.
	 *
	 * @return Scoped local-variable state, or {@code null} if the local variable's range is invalid or outside the method's instruction list.
	 */
	private static @Nullable LocalVariableState inferTableState(@NotNull MethodNode method,
	                                                            @NotNull AnalysisResults results,
	                                                            @NotNull InheritanceChecker checker,
	                                                            @NotNull LocalVariableNode local) {
		// Skip any if the local variable's range is invalid or outside the method's instruction list.
		int start = method.instructions.indexOf(local.start);
		int end = method.instructions.indexOf(local.end);
		if (start < 0 || end <= start)
			return null;

		// Determine the most specific type observed for the local variable across its range.
		Type observedType = null;
		for (int index = start; index < end; index++) {
			Frame frame = results.frames().get(index);
			if (frame == null)
				continue;
			Type type = frame.getLocalType(local.index);
			if (!isDefinedType(type))
				type = storedType(method.instructions.get(index), frame, local.index);
			if (!isDefinedType(type))
				continue;
			observedType = mergeObservedType(checker, observedType, type);
		}

		// If no type was observed, fall back to the declared type in the debug metadata.
		Type declaredType = Type.getType(local.desc);
		Type outputType = isDefinedType(observedType) ? observedType : declaredType;

		// For any special cases (shouldn't happen in practice), fall back to Object.
		if (!isEmittableType(outputType))
			outputType = JvmTypeUtils.OBJECT;

		// Narrow the debug range to only cover the instructions that reference the variable,
		// so reported states reflect where the variable is actually used. Parameters stay in
		// scope for the entire method, matching how the compiler emits their debug ranges.
		if (isParameterSlot(method, local.index))
			return new LocalVariableState(local.index, local.name, outputType, start, end);
		else {
			UsageRange usage = usageRange(method, local.index);
			if (usage != null && usage.start() >= start && usage.end() <= end)
				return new LocalVariableState(local.index, local.name, outputType, usage.start(), usage.end());
		}

		// Not a parameter, nor used anywhere in the method, so fall back to the original debug range.
		return new LocalVariableState(local.index, local.name, outputType, start, end);
	}

	/**
	 * Finds the instruction range that references the given local variable slot.
	 * Reads, writes, and increments all count as references.
	 *
	 * @param method
	 * 		Method to scan for variable references.
	 * @param localIndex
	 * 		Local variable slot index to search for.
	 *
	 * @return The referenced instruction range, or {@code null} when the slot is never referenced.
	 */
	private static @Nullable UsageRange usageRange(@NotNull MethodNode method, int localIndex) {
		int start = -1;
		int end = -1;
		for (int index = 0; index < method.instructions.size(); index++) {
			AbstractInsnNode instruction = method.instructions.get(index);
			if (!referencesLocal(instruction, localIndex))
				continue;
			if (start < 0)
				start = index;
			end = index + 1;
		}
		return start < 0 ? null : new UsageRange(start, end);
	}

	/**
	 * @param instruction
	 * 		Instruction to check for a variable reference.
	 * @param localIndex
	 * 		Local variable slot index to check for.
	 *
	 * @return {@code true} when the instruction reads or writes the given local variable slot.
	 */
	private static boolean referencesLocal(@NotNull AbstractInsnNode instruction, int localIndex) {
		if (instruction instanceof VarInsnNode variable)
			return variable.var == localIndex;
		return instruction instanceof IincInsnNode increment && increment.var == localIndex;
	}

	/**
	 * @param method
	 * 		Method to check for parameter slots.
	 * @param localIndex
	 * 		Local variable slot index to check.
	 *
	 * @return {@code true} when the slot is occupied by a method parameter, including {@code this}.
	 */
	private static boolean isParameterSlot(@NotNull MethodNode method, int localIndex) {
		// The receiver occupies slot 0 in non-static methods.
		if ((method.access & Opcodes.ACC_STATIC) == 0 && localIndex == 0)
			return true;

		// Argument slots follow the receiver, with wide types occupying two slots.
		int slot = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
		for (Type type : Type.getArgumentTypes(method.desc)) {
			if (slot == localIndex)
				return true;
			slot += type.getSize();
		}

		return false;
	}

	/**
	 * Inclusive/exclusive instruction range.
	 *
	 * @param start
	 * 		Inclusive instruction-list index where the range starts.
	 * @param end
	 * 		Exclusive instruction-list index where the range ends.
	 */
	private record UsageRange(int start, int end) {}

	/**
	 * @param instruction
	 * 		Instruction to check for a store operation.
	 * @param frame
	 * 		Frame at the instruction.
	 * @param localIndex
	 * 		Local variable index to check for a store operation.
	 *
	 * @return Type of the value stored in the local variable, or {@code null} if it cannot be determined.
	 */
	private static @Nullable Type storedType(@NotNull AbstractInsnNode instruction,
	                                         @NotNull Frame frame,
	                                         int localIndex) {
		// Skip any non _STORE instructions or any that don't match the local index.
		if (!(instruction instanceof VarInsnNode variable)
				|| variable.var != localIndex
				|| !isStoreOpcode(variable.getOpcode()))
			return null;

		// Extract the type from the frame.
		// The value stored is on the top of the stack.
		if (frame instanceof TypedFrame typedFrame)
			return peekTypedStoreType(typedFrame, variable.getOpcode());
		if (frame instanceof ValuedFrame valuedFrame)
			return peekValuedStoreType(valuedFrame, variable.getOpcode());

		// Some unsupported frame type was encountered, so we can't infer the type.
		return null;
	}

	/**
	 * @param frame
	 * 		Frame to peek the stored value from.
	 * @param opcode
	 * 		Opcode of the store instruction.
	 *
	 * @return Type of the value stored in the local variable, or {@code null} if it cannot be determined.
	 */
	private static @Nullable Type peekTypedStoreType(@NotNull TypedFrame frame, int opcode) {
		var iterator = frame.getStack().iterator();
		if (!iterator.hasNext())
			return null;

		// For wide store opcodes (LSTORE, DSTORE), the value is represented as two slots on the stack.
		Type type = iterator.next();
		if (isWideStoreOpcode(opcode) && JvmTypeUtils.VOID.equals(type))
			return iterator.hasNext() ? iterator.next() : null;

		return type;
	}

	/**
	 * @param frame
	 * 		Frame to peek the stored value from.
	 * @param opcode
	 * 		Opcode of the store instruction.
	 *
	 * @return Type of the value stored in the local variable, or {@code null} if it cannot be determined.
	 */
	private static @Nullable Type peekValuedStoreType(@NotNull ValuedFrame frame, int opcode) {
		var iterator = frame.getStack().iterator();
		if (!iterator.hasNext())
			return null;

		// Same as above.
		Value value = iterator.next();
		if (isWideStoreOpcode(opcode) && value == Values.VOID_VALUE)
			return iterator.hasNext() ? iterator.next().type() : null;

		return value.type();
	}

	/**
	 * @param opcode
	 * 		Opcode to check.
	 *
	 * @return {@code true} for any store opcodes that store a value on the stack.
	 */
	private static boolean isStoreOpcode(int opcode) {
		return opcode == Opcodes.ISTORE
				|| opcode == Opcodes.LSTORE
				|| opcode == Opcodes.FSTORE
				|| opcode == Opcodes.DSTORE
				|| opcode == Opcodes.ASTORE;
	}

	/**
	 * @param opcode
	 * 		Opcode to check.
	 *
	 * @return {@code true} for any store opcodes that store a wide value ({@code long} or {@code double}) on the stack.
	 */
	private static boolean isWideStoreOpcode(int opcode) {
		return opcode == Opcodes.LSTORE || opcode == Opcodes.DSTORE;
	}

	/**
	 * Infers local-variable states from the analysis frames, without relying on debug metadata.
	 *
	 * @param method
	 * 		Method to analyze.
	 * @param results
	 * 		Verifier analysis results for the method.
	 * @param checker
	 * 		Inheritance checker used to for type merging.
	 *
	 * @return Scoped local-variable states inferred from the analysis frames.
	 */
	private static @NotNull List<LocalVariableState> inferFrameStates(@NotNull MethodNode method,
	                                                                  @NotNull AnalysisResults results,
	                                                                  @NotNull InheritanceChecker checker) {
		List<LocalVariableState> states = new ArrayList<>();
		Map<RunKey, MutableRun> active = new TreeMap<>();
		Map<Integer, String> sourceNames = sourceVariableNames(results);

		// Walk through the instructions, tracking the currently defined locals and their types.
		int instructionCount = method.instructions.size();
		for (int index = 0; index < instructionCount; index++) {
			Frame frame = results.frames().get(index);
			Map<RunKey, Type> current = frame == null ? Map.of() : definedLocals(frame, sourceNames);

			// Check for any locals that are no longer defined in the current frame,
			// finishing their runs and removing them from the active map.
			var iterator = active.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<RunKey, MutableRun> entry = iterator.next();
				if (current.containsKey(entry.getKey()))
					continue;
				states.add(entry.getValue().finish(index));
				iterator.remove();
			}

			// Record any new locals that are defined in the current frame,
			// merging types if they were already active.
			for (Map.Entry<RunKey, Type> entry : current.entrySet()) {
				MutableRun run = active.get(entry.getKey());
				if (run == null) {
					active.put(entry.getKey(), new MutableRun(entry.getKey(), index, entry.getValue()));
				} else {
					run.type = mergeObservedType(checker, run.type, entry.getValue());
				}
			}
		}

		// Finalize any remaining active runs at the end of the instruction list.
		for (MutableRun run : active.values())
			states.add(run.finish(instructionCount));

		return states;
	}

	/**
	 * @param results
	 * 		The results to extract source variable names from.
	 *
	 * @return Map of defined locals in the frame, keyed by their index and name.
	 */
	private static @NotNull Map<Integer, String> sourceVariableNames(@NotNull AnalysisResults results) {
		Map<Integer, String> names = new TreeMap<>();
		for (Map.Entry<AbstractInsnNode, ASTInstruction> entry : results.getInstructionToAstMap().entrySet()) {
			int index;
			AbstractInsnNode instruction = entry.getKey();
			if (instruction instanceof VarInsnNode variable) {
				index = variable.var;
			} else if (instruction instanceof IincInsnNode increment) {
				index = increment.var;
			} else {
				continue;
			}

			List<@Nullable ASTElement> arguments = entry.getValue().arguments();
			if (arguments.isEmpty() || !(arguments.getFirst() instanceof ASTIdentifier identifier))
				continue;
			names.putIfAbsent(index, identifier.literal());
		}
		return names;
	}

	/**
	 * @param frame
	 * 		Frame to extract defined locals from.
	 * @param sourceNames
	 * 		Map of source variable names, keyed by their index.
	 *
	 * @return Map of defined locals in the frame, keyed by their index and name.
	 */
	private static @NotNull Map<RunKey, Type> definedLocals(@NotNull Frame frame,
	                                                        @NotNull Map<Integer, String> sourceNames) {
		Map<RunKey, Type> locals = new TreeMap<>();
		frame.locals().forEach(local -> {
			Type type = local.type();
			if (isDefinedType(type)) {
				String name = sourceNames.getOrDefault(local.index(), local.name());
				locals.put(new RunKey(local.index(), name), type);
			}
		});
		return locals;
	}

	/**
	 * Merges two observed types for a local variable, returning the most specific type that is compatible with both.
	 *
	 * @param checker
	 * 		Inheritance checker used to determine type compatibility.
	 * @param current
	 * 		Current observed type for the local variable.
	 * @param observed
	 * 		Newly observed type for the local variable.
	 *
	 * @return Merged type, or {@code null} if neither type is defined.
	 */
	private static @Nullable Type mergeObservedType(@NotNull InheritanceChecker checker,
	                                                @Nullable Type current,
	                                                @NotNull Type observed) {
		// Return the other type for any edge-case types.
		if (!isDefinedType(observed))
			return current;
		if (!isDefinedType(current))
			return observed;

		// Merge the two types using the inheritance checker, falling back to Object if they are incompatible.
		Type merged = JvmTypeUtils.commonType(checker, current, observed);
		return isEmittableType(merged) ? merged : JvmTypeUtils.OBJECT;
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} for any defined types that are not special-case types such as {@code void}, {@code TOP}, or {@code null}.
	 */
	private static boolean isDefinedType(@Nullable Type type) {
		return type != null
				&& !JvmTypeUtils.isTop(type)
				&& !JvmTypeUtils.isNullMarker(type)
				&& !JvmTypeUtils.isUninitialized(type);
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} for any special-case types that should not be emitted in a local-variable table, such as {@code void} or {@code TOP}.
	 */
	private static boolean isEmittableType(@Nullable Type type) {
		return isDefinedType(type) && !JvmTypeUtils.VOID.equals(type);
	}

	/**
	 * Key for a local variable run, used to uniquely identify it in a map.
	 *
	 * @param index
	 * 		Variable slot index.
	 * @param name
	 * 		Variable name.
	 */
	private record RunKey(int index, @NotNull String name) implements Comparable<RunKey> {
		@Override
		public int compareTo(@NotNull RunKey other) {
			int indexCompare = Integer.compare(index, other.index);
			return indexCompare != 0 ? indexCompare : name.compareTo(other.name);
		}
	}

	/**
	 * Mutable run state for a local variable across a range of instructions.
	 */
	private static final class MutableRun {
		private final RunKey key;
		private final int start;
		private Type type;

		private MutableRun(@NotNull RunKey key, int start, @NotNull Type type) {
			this.key = key;
			this.start = start;
			this.type = type;
		}

		private @NotNull LocalVariableState finish(int end) {
			return new LocalVariableState(key.index(), key.name(),
					isEmittableType(type) ? type : JvmTypeUtils.OBJECT, start, end);
		}
	}
}

package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.assembler.descriptor.PrimitiveType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Joins Dalvik register states at control-flow merges.
 */
public final class DalvikRegisterStateMerger {
	private DalvikRegisterStateMerger() {}

	/**
	 * Merges two register frames into a single frame.
	 * <p>
	 * Undefined slots remain undefined when either incoming path has not written them.
	 *
	 * @param inheritanceChecker
	 * 		Checker for determining common superclasses.
	 * @param target
	 * 		Target frame to merge into.
	 * @param incoming
	 * 		Incoming frame to merge from.
	 *
	 * @return Merge result containing whether the target frame was changed and any incompatible registers.
	 *
	 * @throws IllegalArgumentException
	 * 		When the two frames have different register counts.
	 */
	static @NotNull MergeResult merge(@NotNull InheritanceChecker inheritanceChecker,
	                                  @NotNull DalvikAnalysisFrame target,
	                                  @NotNull DalvikAnalysisFrame incoming) {
		if (target.registerCount() != incoming.registerCount())
			throw new IllegalArgumentException("Register frames have different sizes");

		boolean changed = false;
		boolean[] handled = new boolean[target.registerCount()];
		Set<Integer> incompatible = new LinkedHashSet<>();
		for (int register = 0; register < target.registerCount(); register++) {
			if (handled[register])
				continue;

			int targetPair = widePairStart(target, register);
			int incomingPair = widePairStart(incoming, register);

			// Treat a wide value atomically so a merge cannot leave its head and tail out of sync.
			if (targetPair == register || incomingPair == register) {
				if (targetPair == register && incomingPair == register) {
					Value left = target.readWide(register);
					Value right = incoming.readWide(register);
					Value joined = joinValues(inheritanceChecker, left, right);
					if (joined == null) {
						changed |= clearRange(target, register, register + 1);
						incompatible.add(register);
						incompatible.add(register + 1);
					} else if (!left.equals(joined)) {
						target.writeWide(register, joined);
						changed = true;
					}
					mark(handled, register, register + 1);
					continue;
				}

				// One frame has a wide pair while the other has a different layout in the overlapping words.
				int pairStart = targetPair == register ? register : incomingPair;
				int unionStart = pairStart;
				int unionEnd = pairStart + 1;
				DalvikAnalysisFrame other = targetPair == register ? incoming : target;

				// If the other frame has no value in either word of the pair, we can clear the pair and continue.
				// Otherwise, we must clear the entire union of the pair and any overlapping words in the other frame.
				if (other.slot(pairStart) == DalvikRegisterState.Undefined.INSTANCE
						&& other.slot(pairStart + 1) == DalvikRegisterState.Undefined.INSTANCE) {
					changed |= clearRange(target, pairStart, pairStart + 1);
				} else {
					for (int word = pairStart; word <= pairStart + 1; word++) {
						int otherPair = widePairStart(other, word);
						if (otherPair >= 0) {
							unionStart = Math.min(unionStart, otherPair);
							unionEnd = Math.max(unionEnd, otherPair + 1);
						}
					}
					changed |= clearRange(target, unionStart, unionEnd);
					for (int word = unionStart; word <= unionEnd; word++)
						incompatible.add(word);
				}

				mark(handled, unionStart, unionEnd);
				continue;
			}

			DalvikRegisterState.Slot leftSlot = target.slot(register);
			DalvikRegisterState.Slot rightSlot = incoming.slot(register);

			// A tail not handled with a valid pair is an incompatible standalone register word.
			if (leftSlot instanceof DalvikRegisterState.WideTail || rightSlot instanceof DalvikRegisterState.WideTail) {
				changed |= clearRange(target, register, register);
				incompatible.add(register);
				continue;
			}

			// If either slot is undefined, the merged slot is undefined.
			if (leftSlot == DalvikRegisterState.Undefined.INSTANCE
					|| rightSlot == DalvikRegisterState.Undefined.INSTANCE) {
				changed |= target.slot(register) != DalvikRegisterState.Undefined.INSTANCE;
				changed |= clearRange(target, register, register);
				continue;
			}

			// If either slot is not a narrow value, the merged slot is undefined.
			if (!(leftSlot instanceof DalvikRegisterState.ValueHead(Value leftValue))
					|| !(rightSlot instanceof DalvikRegisterState.ValueHead(Value rightValue))) {
				changed |= clearRange(target, register, register);
				incompatible.add(register);
				continue;
			}

			// Both slots are narrow values, so we can attempt to join them.
			Value joined = joinValues(inheritanceChecker, leftValue, rightValue);
			if (joined == null) {
				changed |= clearRange(target, register, register);
				incompatible.add(register);
			} else if (!leftValue.equals(joined)) {
				target.write(register, joined);
				changed = true;
			}
		}

		// Implicit invoke/exception values travel alongside registers and are joined independently.
		Value pendingResult = joinTransient(inheritanceChecker, target.pendingResult(), incoming.pendingResult());
		if (!sameNullableValue(target.pendingResult(), pendingResult)) {
			target.setPendingResult(pendingResult);
			changed = true;
		}

		Value pendingException = joinTransient(inheritanceChecker, target.pendingException(), incoming.pendingException());
		if (!sameNullableValue(target.pendingException(), pendingException)) {
			target.setPendingException(pendingException);
			changed = true;
		}

		return new MergeResult(changed, Set.copyOf(incompatible));
	}

	/**
	 * Joins two transient values, which may be {@code null}.
	 *
	 * @param inheritanceChecker
	 * 		Checker for determining common superclasses.
	 * @param left
	 * 		Left value.
	 * @param right
	 * 		Right value.
	 *
	 * @return Joined value, or {@code null} if either value is {@code null} or the values are incompatible.
	 */
	private static @Nullable Value joinTransient(@NotNull InheritanceChecker inheritanceChecker,
	                                             @Nullable Value left, @Nullable Value right) {
		if (left == null || right == null)
			return null;
		return joinValues(inheritanceChecker, left, right);
	}

	/**
	 * @param left
	 * 		Left value.
	 * @param right
	 * 		Right value.
	 *
	 * @return {@code true} if the values are the same, {@code false} otherwise.
	 */
	private static boolean sameNullableValue(@Nullable Value left, @Nullable Value right) {
		return left == null ? right == null : right != null && sameValue(left, right);
	}

	/**
	 * @param frame
	 * 		Register frame to check.
	 * @param register
	 * 		Register number to check.
	 *
	 * @return Start of the wide pair if the register is part of a wide value, or {@code -1} if it is not.
	 */
	private static int widePairStart(@NotNull DalvikAnalysisFrame frame, int register) {
		DalvikRegisterState.Slot slot = frame.slot(register);

		// If the slot is a wide head, check if the next register is a matching wide tail.
		// If so, return the current register as the start of the wide pair.
		if (slot instanceof DalvikRegisterState.ValueHead(Value value) && DalvikAnalysisFrame.isWide(value)
				&& register + 1 < frame.registerCount()
				&& frame.slot(register + 1) instanceof DalvikRegisterState.WideTail(int headRegister)
				&& headRegister == register)
			return register;

		// Reverse, check for tail, then prior register is head.
		if (slot instanceof DalvikRegisterState.WideTail(int headRegister)) {
			if (headRegister >= 0 && headRegister + 1 == register
					&& frame.slot(headRegister) instanceof DalvikRegisterState.ValueHead(Value value)
					&& DalvikAnalysisFrame.isWide(value))
				return headRegister;
		}

		return -1;
	}

	/**
	 * Clears a range of registers in the given frame.
	 *
	 * @param frame
	 * 		Register frame to clear.
	 * @param start
	 * 		Start register (inclusive).
	 * @param end
	 * 		End register (inclusive).
	 *
	 * @return {@code true} if any registers were cleared, {@code false} otherwise.
	 */
	private static boolean clearRange(@NotNull DalvikAnalysisFrame frame, int start, int end) {
		boolean changed = false;
		for (int register = start; register <= end; register++) {
			if (frame.slot(register) != DalvikRegisterState.Undefined.INSTANCE)
				changed = true;
			frame.clear(register);
		}
		return changed;
	}

	/**
	 * Marks a range of registers as handled.
	 *
	 * @param handled
	 * 		Array of booleans indicating which registers have been handled.
	 * @param start
	 * 		Start register (inclusive).
	 * @param end
	 * 		End register (inclusive).
	 */
	private static void mark(boolean @NotNull [] handled, int start, int end) {
		for (int register = start; register <= end && register < handled.length; register++)
			if (register >= 0)
				handled[register] = true;
	}

	/**
	 * Computes a safe join for two register values.
	 *
	 * @param inheritanceChecker
	 * 		Checker for determining common superclasses.
	 * @param left
	 * 		Value from the target frame.
	 * @param right
	 * 		Value from the incoming frame.
	 *
	 * @return Joined value, or {@code null} when the values cannot be represented by a safe common value.
	 */
	private static @Nullable Value joinValues(@NotNull InheritanceChecker inheritanceChecker,
	                                          @NotNull Value left, @NotNull Value right) {
		// Uninitialized reference values are only equal to themselves, so we can return one of them if they are equal.
		if (left instanceof Value.UninitializedReferenceValue || right instanceof Value.UninitializedReferenceValue)
			return left.equals(right) ? left : null;

		// Nonzero raw values are category-agnostic primitive values, but never references.
		// Preserve that distinction when joining raw payloads or the untyped zero sentinel,
		// and refine to a concrete primitive category when available.
		if (left instanceof DalvikRawValue leftRaw)
			return joinRaw(leftRaw, right);
		if (right instanceof DalvikRawValue rightRaw)
			return joinRaw(rightRaw, left);
		if (left instanceof DalvikWideRawValue leftRaw)
			return joinWideRaw(leftRaw, right);
		if (right instanceof DalvikWideRawValue rightRaw)
			return joinWideRaw(rightRaw, left);

		// Dalvik "zero" values are compatible with any integer or null reference value, so we can join them specially.
		if (left == DalvikZeroValue.INSTANCE || right == DalvikZeroValue.INSTANCE)
			return joinZero(left, right);

		// The wide zero is compatible with either wide type, so it joins with the other path's wide type.
		if (left == DalvikWideZeroValue.INSTANCE || right == DalvikWideZeroValue.INSTANCE)
			return joinWideZero(left, right);

		// Other backend markers cannot be represented as a safe common value.
		if (left instanceof Value.TopValue
				|| right instanceof Value.TopValue
				|| left instanceof Value.BackendMarker
				|| right instanceof Value.BackendMarker)
			return null;

		// Any values that are 'equal' are compatible, so we can return one of them.
		if (sameValue(left, right))
			return left;

		// Primitive values of different types are incompatible, so we can return null to indicate that the result is unknown.
		if (left instanceof Value.PrimitiveValue leftPrimitive && right instanceof Value.PrimitiveValue rightPrimitive) {
			if (leftPrimitive.type() != rightPrimitive.type())
				return null;
			return Values.valueOfPrimitive(leftPrimitive.type());
		}

		// For mixed null/reference values, we can return an unknown reference value of the non-null type.
		if (left instanceof Value.NullValue && right instanceof Value.ObjectValue rightObject)
			return unknownReference(rightObject.type());
		if (right instanceof Value.NullValue && left instanceof Value.ObjectValue leftObject)
			return unknownReference(leftObject.type());

		// If we have any non-objects at this point, like left is a primitive but right is a reference
		// then we can return null to indicate that the result is unknown.
		if (!(left instanceof Value.ObjectValue leftObject) || !(right instanceof Value.ObjectValue rightObject))
			return null;

		DescriptorType leftType = leftObject.type();
		DescriptorType rightType = rightObject.type();

		// If both values are arrays, we can return an unknown array value of the common type.
		if (leftType instanceof ArrayDescriptor leftArray && rightType instanceof ArrayDescriptor rightArray) {
			if (!leftArray.equals(rightArray))
				return Values.OBJECT_VALUE;
			if (left instanceof Value.KnownLengthArrayValue leftKnown
					&& right instanceof Value.KnownLengthArrayValue rightKnown
					&& leftKnown.length() == rightKnown.length())
				return leftKnown;
			return Values.valueOfArray(leftArray);
		}

		// If both values are class types, we can return an unknown reference value of the common superclass.
		if (leftType instanceof ClassDescriptor(String leftName) && rightType instanceof ClassDescriptor(
				String rightName
		)) {
			String common = inheritanceChecker.getCommonSuperclass(leftName, rightName);
			return Values.valueOfInstance(new ClassDescriptor(common == null ? "java/lang/Object" : common));
		}

		// If one value is an array and the other is a class type, we can return an unknown reference value of type java/lang/Object.
		if (leftType instanceof ArrayDescriptor || rightType instanceof ArrayDescriptor)
			return Values.OBJECT_VALUE;

		// IDK what this is.
		return null;
	}

	/**
	 * @param left
	 * 		Left value.
	 * @param right
	 * 		Right value.
	 *
	 * @return {@code true} if the values are the same, {@code false} otherwise.
	 */
	private static boolean sameValue(@NotNull Value left, @NotNull Value right) {
		if (left instanceof Value.KnownFloatValue(float leftF) && right instanceof Value.KnownFloatValue(float rightF))
			return Float.floatToRawIntBits(leftF) == Float.floatToRawIntBits(rightF);
		if (left instanceof Value.KnownDoubleValue(double leftD) && right instanceof Value.KnownDoubleValue(
				double rightD
		))
			return Double.doubleToRawLongBits(leftD) == Double.doubleToRawLongBits(rightD);
		return left.equals(right);
	}

	/**
	 * Joins a narrow raw value with another value.
	 *
	 * @param raw
	 * 		Raw narrow value.
	 * @param other
	 * 		Other value.
	 *
	 * @return Joined value, or {@code null} when the other value is not a narrow primitive.
	 */
	private static @Nullable Value joinRaw(@NotNull DalvikRawValue raw, @NotNull Value other) {
		if (other instanceof DalvikRawValue otherRaw)
			return DalvikRawValue.join(raw, otherRaw);
		if (other == DalvikZeroValue.INSTANCE)
			return DalvikRawValue.UNKNOWN;
		if (other instanceof Value.IntValue)
			return Values.INT_VALUE;
		if (other instanceof Value.FloatValue)
			return Values.FLOAT_VALUE;
		return null;
	}

	/**
	 * Joins a wide raw value with another value.
	 *
	 * @param raw
	 * 		Raw wide value.
	 * @param other
	 * 		Other value.
	 *
	 * @return Joined value, or {@code null} when the other value is not a wide primitive.
	 */
	private static @Nullable Value joinWideRaw(@NotNull DalvikWideRawValue raw, @NotNull Value other) {
		if (other instanceof DalvikWideRawValue otherRaw)
			return DalvikWideRawValue.join(raw, otherRaw);
		if (other == DalvikWideZeroValue.INSTANCE)
			return DalvikWideRawValue.UNKNOWN;
		if (other instanceof Value.LongValue)
			return Values.LONG_VALUE;
		if (other instanceof Value.DoubleValue)
			return Values.DOUBLE_VALUE;
		return null;
	}

	/**
	 * Joins two values where at least one of them is a Dalvik <i>{@link DalvikZeroValue "zero"}</i> value.
	 *
	 * @param left
	 * 		Left value.
	 * @param right
	 * 		Right value.
	 *
	 * @return Joined value, or {@code null} if the values are incompatible.
	 */
	private static @Nullable Value joinZero(@NotNull Value left, @NotNull Value right) {
		Value other = left == DalvikZeroValue.INSTANCE ? right : left;
		if (other == DalvikZeroValue.INSTANCE || other instanceof Value.KnownIntValue(int value) && value == 0)
			return DalvikZeroValue.INSTANCE;
		else if (other instanceof Value.IntValue)
			return Values.INT_VALUE;
		else if (other instanceof Value.FloatValue)
			return Values.valueOfPrimitive(PrimitiveType.FLOAT);
		else if (other instanceof Value.NullValue)
			return DalvikZeroValue.INSTANCE;
		else if (other instanceof Value.ObjectValue objectValue)
			return unknownReference(objectValue.type());
		return null;
	}

	/**
	 * Joins a wide {@link DalvikWideZeroValue "zero"} value with another value.
	 *
	 * @param left
	 * 		Left value.
	 * @param right
	 * 		Right value.
	 *
	 * @return Joined value, or {@code null} if the other value is not a wide primitive.
	 */
	private static @Nullable Value joinWideZero(@NotNull Value left, @NotNull Value right) {
		Value other = left == DalvikWideZeroValue.INSTANCE ? right : left;
		if (other == DalvikWideZeroValue.INSTANCE)
			return DalvikWideZeroValue.INSTANCE;
		if (other instanceof Value.PrimitiveValue primitive && primitive.isWide())
			return Values.valueOfPrimitive(primitive.type());
		return null;
	}

	/**
	 * @param type
	 * 		Type of the reference, or {@code null} for an untyped reference.
	 *
	 * @return Unknown reference value of the given type, or {@link Values#OBJECT_VALUE} as a fallback.
	 */
	private static @NotNull Value unknownReference(@Nullable DescriptorType type) {
		if (type instanceof ArrayDescriptor arrayType)
			return Values.valueOfArray(arrayType);
		if (type instanceof ClassDescriptor classType)
			return Values.valueOfInstance(classType);
		return Values.OBJECT_VALUE;
	}

	/**
	 * Result of a register state merge operation.
	 *
	 * @param changed
	 * 		Whether the target frame was changed during the merge.
	 * @param incompatibleRegisters
	 * 		Set of registers that were found to be incompatible during the merge.
	 */
	record MergeResult(boolean changed, @NotNull Set<Integer> incompatibleRegisters) {}
}

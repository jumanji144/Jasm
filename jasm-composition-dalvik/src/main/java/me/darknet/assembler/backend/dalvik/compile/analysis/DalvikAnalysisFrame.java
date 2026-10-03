package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Mutable register frame used during Dalvik bytecode analysis.
 */
final class DalvikAnalysisFrame {
	private final DalvikRegisterState.Slot[] slots;
	private @Nullable Value pendingResult;
	private @Nullable Value pendingException;

	DalvikAnalysisFrame(@NotNull DalvikRegisterState seed) {
		slots = seed.slots().toArray(DalvikRegisterState.Slot[]::new);
		pendingResult = seed.pendingResult();
	}

	private DalvikAnalysisFrame(DalvikRegisterState.Slot[] slots, @Nullable Value pendingResult, @Nullable Value pendingException) {
		this.slots = slots;
		this.pendingResult = pendingResult;
		this.pendingException = pendingException;
	}

	int registerCount() {
		return slots.length;
	}

	@Nullable Value pendingResult() {
		return pendingResult;
	}

	void setPendingResult(@Nullable Value value) {
		pendingResult = value;
	}

	@Nullable Value pendingException() {
		return pendingException;
	}

	void setPendingException(@Nullable Value value) {
		pendingException = value;
	}

	void clearPendingResult() {
		pendingResult = null;
	}

	void clearPendingException() {
		pendingException = null;
	}

	DalvikRegisterState.Slot slot(int register) {
		checkRegister(register);
		return slots[register];
	}

	Value readSingle(int register) {
		DalvikRegisterState.Slot slot = slot(register);
		if (!(slot instanceof DalvikRegisterState.ValueHead(Value value)) || isWide(value))
			throw new IllegalStateException("Register v" + register + " does not contain a narrow value");
		return value;
	}

	Value readHead(int register) {
		DalvikRegisterState.Slot slot = slot(register);
		if (!(slot instanceof DalvikRegisterState.ValueHead(Value value)))
			throw new IllegalStateException("Register v" + register + " does not contain a value head");
		return value;
	}

	Value readWide(int register) {
		checkWideDestination(register);
		if (!(slots[register] instanceof DalvikRegisterState.ValueHead(Value value))
				|| !isWide(value)
				|| !(slots[register + 1] instanceof DalvikRegisterState.WideTail(int headRegister))
				|| headRegister != register)
			throw new IllegalStateException("Register pair v" + register + "/v" + (register + 1) + " is not a valid wide value");
		return value;
	}

	void write(int register, @NotNull Value value) {
		if (isWide(value))
			throw new IllegalArgumentException("Wide values require a two-word write");
		checkRegister(register);
		clearPairAt(register);
		slots[register] = new DalvikRegisterState.ValueHead(value);
	}

	void writeWide(int register, @NotNull Value value) {
		if (!isWide(value))
			throw new IllegalArgumentException("A wide register pair requires a LONG or DOUBLE value");
		checkWideDestination(register);
		clearPairAt(register);
		clearPairAt(register + 1);
		slots[register] = new DalvikRegisterState.ValueHead(value);
		slots[register + 1] = new DalvikRegisterState.WideTail(register);
	}

	void copyWide(int destination, int source) {
		Value value = readWide(source);
		writeWide(destination, value);
	}

	void clear(int register) {
		checkRegister(register);
		clearPairAt(register);
		slots[register] = DalvikRegisterState.Undefined.INSTANCE;
	}

	void clearWide(int register) {
		checkWideDestination(register);
		clearPairAt(register);
		clearPairAt(register + 1);
		slots[register] = DalvikRegisterState.Undefined.INSTANCE;
		slots[register + 1] = DalvikRegisterState.Undefined.INSTANCE;
	}

	void invalidateAll() {
		Arrays.fill(slots, DalvikRegisterState.Undefined.INSTANCE);
	}

	DalvikAnalysisFrame copy() {
		return new DalvikAnalysisFrame(slots.clone(), pendingResult, pendingException);
	}

	@NotNull DalvikRegisterState snapshot() {
		return new DalvikRegisterState(List.of(slots), pendingResult);
	}

	private void clearPairAt(int register) {
		DalvikRegisterState.Slot slot = slots[register];
		if (slot instanceof DalvikRegisterState.WideTail(int headRegister)) {
			if (headRegister >= 0 && headRegister < slots.length
					&& slots[headRegister] instanceof DalvikRegisterState.ValueHead(Value value)
					&& isWide(value)
					&& headRegister + 1 == register)
				slots[headRegister] = DalvikRegisterState.Undefined.INSTANCE;
		} else if (slot instanceof DalvikRegisterState.ValueHead(Value value) && isWide(value)) {
			int tailRegister = register + 1;
			if (tailRegister < slots.length
					&& slots[tailRegister] instanceof DalvikRegisterState.WideTail(int headRegister)
					&& headRegister == register)
				slots[tailRegister] = DalvikRegisterState.Undefined.INSTANCE;
		}
		slots[register] = DalvikRegisterState.Undefined.INSTANCE;
	}

	private void checkWideDestination(int register) {
		if (register < 0 || register + 1 >= slots.length)
			throw new IllegalArgumentException("Wide register pair v" + register + " is outside the register frame");
	}

	private void checkRegister(int register) {
		if (register < 0 || register >= slots.length)
			throw new IllegalArgumentException("Register v" + register + " is outside the register frame");
	}

	static boolean isWide(Value value) {
		return value instanceof Value.PrimitiveValue primitive && primitive.isWide();
	}
}

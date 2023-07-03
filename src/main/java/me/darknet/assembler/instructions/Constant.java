package me.darknet.assembler.instructions;

public class Constant {

	public enum Type {
		String,
		Number,
		ClassType,
		MethodType,
		MethodHandle
	}

	private final Type type;
	private final Object value;

	public Constant(Type type, Object value) {
		this.type = type;
		this.value = value;
	}

	public Type getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

}

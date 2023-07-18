package me.darknet.assembler.helper;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;

public class Constant {

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

	public enum Type {
		String,
		Number,
		ClassType,
		MethodType,
		MethodHandle
	}

	/**
	 * Create a new constant from element
	 * @param element the element, must be verified before
	 * @return the constant
	 */
	public static Constant from(ASTElement element) {
		switch (element.getType()) {
			case NUMBER:
				ASTNumber number = (ASTNumber) element;
				return new Constant(Constant.Type.Number, number.getNumber());
			case STRING:
				return new Constant(Constant.Type.String, element.getValue());
			case IDENTIFIER:
				ASTIdentifier identifier = (ASTIdentifier) element;
				if(identifier.getContent().startsWith("L")) {
					return new Constant(Constant.Type.ClassType, identifier.getContent());
				} else {
					return new Constant(Constant.Type.MethodType, identifier.getContent());
				}
			case ARRAY: {
				ASTArray array = (ASTArray) element;
				return new Constant(Constant.Type.MethodHandle, Handle.from(array));
			}
		}
		return null;
	}

}

package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.parser.Token;

public class ASTNumber extends ASTValue {

	public ASTNumber(Token number) {
		super(ElementType.NUMBER, number);
	}

	public Number getNumber() {
		String value = getContent();
		int radix = 10;
		if(value.startsWith("0x") || value.startsWith("0X")){
			radix = 16;
			value = value.substring(2);
		}
		if (value.contains(".")) {
			if(value.endsWith("f") || value.endsWith("F")) {
				return Float.parseFloat(value.substring(0, value.length() - 1));
			}
			return Double.parseDouble(value);
		}else {
			if(value.endsWith("L")) {
				return Long.parseLong(value.substring(0, value.length() - 1), radix);
			}
			return Integer.parseInt(value, radix);
		}
	}

	public boolean isWide() {
		String value = getContent();
		if (value.contains(".")) {
			return !value.endsWith("f") && !value.endsWith("F");
		} else {
			return value.endsWith("L");
		}
	}

	public int asInt() {
		return getNumber().intValue();
	}

	public long asLong() {
		return getNumber().longValue();
	}

	public float asFloat() {
		return getNumber().floatValue();
	}

	public double asDouble() {
		return getNumber().doubleValue();
	}

	public boolean isFloatingPoint() {
		String value = getContent();
		return value.contains(".");
	}
}

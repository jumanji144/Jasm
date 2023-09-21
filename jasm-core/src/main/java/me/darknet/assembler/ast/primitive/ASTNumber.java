package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.parser.Token;

public class ASTNumber extends ASTValue {

    public ASTNumber(Token number) {
        super(ElementType.NUMBER, number);
    }

    public Number number() {
        String value = content();
        int radix = 10;
        if (value.startsWith("0x") || value.startsWith("0X")) {
            radix = 16;
            value = value.substring(2);
        }
        if (value.contains(".")) {
            if (value.endsWith("f") || value.endsWith("F")) {
                return Float.parseFloat(value.substring(0, value.length() - 1));
            }
            return Double.parseDouble(value);
        } else {
            if (value.endsWith("L") || value.endsWith("l")) {
                return Long.parseLong(value.substring(0, value.length() - 1), radix);
            }
            return Integer.parseInt(value, radix);
        }
    }

    public boolean isWide() {
        String value = content();
        if (value.contains(".")) {
            return !value.endsWith("f") && !value.endsWith("F");
        } else {
            return value.endsWith("L");
        }
    }

    public int asInt() {
        return number().intValue();
    }

    public long asLong() {
        return number().longValue();
    }

    public float asFloat() {
        return number().floatValue();
    }

    public double asDouble() {
        return number().doubleValue();
    }

    public boolean isFloatingPoint() {
        String value = content();
        return value.contains(".") || value.endsWith("f") || value.endsWith("F");
    }
}

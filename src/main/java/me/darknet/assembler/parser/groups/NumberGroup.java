package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class NumberGroup extends Group {

    public NumberGroup(Token value) {
        super(GroupType.NUMBER, value);
    }

    public Number getNumber() {
        String value = content();
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
        String value = content();
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

    public boolean isFloat() {
        String value = content();
        return value.contains(".");
    }

}

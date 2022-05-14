package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class NumberGroup extends Group {

    public NumberGroup(Token value) {
        super(GroupType.NUMBER, value);
    }

    public Number getNumber() {
        String value = content();
        if (value.contains(".")) {
            if(value.endsWith("f")) {
                return Float.parseFloat(value.substring(0, value.length() - 1));
            }
            return Double.parseDouble(value);
        }else {
            if(value.endsWith("L")) {
                return Long.parseLong(value.substring(0, value.length() - 1));
            }
            return Integer.parseInt(value);
        }
    }

    public boolean isWide() {
        String value = content();
        if (value.contains(".")) {
            return !value.endsWith("f");
        } else {
            return value.endsWith("L");
        }
    }

    public int asInt() {
        return (int) getNumber();
    }

    public long asLong() {
        return (long) getNumber();
    }

    public float asFloat() {
        return (float) getNumber();
    }

    public double asDouble() {
        return (double) getNumber();
    }

    public boolean isFloat() {
        String value = content();
        return value.contains(".");
    }

}

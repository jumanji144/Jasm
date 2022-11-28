package me.darknet.assembler.exceptions.arguments;

import lombok.Getter;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Location;

@Getter
public class TooManyArgumentException extends AssemblerException {

    private final int expected;
    private final int actual;

    public TooManyArgumentException(Location where, int expected, int actual) {
        super("Too many arguments for instruction", where);
        this.expected = expected;
        this.actual = actual;
    }

}

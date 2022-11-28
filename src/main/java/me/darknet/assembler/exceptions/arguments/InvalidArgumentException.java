package me.darknet.assembler.exceptions.arguments;

import lombok.Getter;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.instructions.Argument;
import me.darknet.assembler.parser.Location;

@Getter
public class InvalidArgumentException extends AssemblerException {

    private final Argument expected;
    private final String actual;

    public InvalidArgumentException(Location where, Argument expected, String actual) {
        super("Expected argument on instruction", where);
        this.expected = expected;
        this.actual = actual;
    }

    public InvalidArgumentException(Location where, Argument expected, String actual, Throwable cause) {
        super("Expected argument on instruction", where, cause);
        this.expected = expected;
        this.actual = actual;
    }
}

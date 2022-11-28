package me.darknet.assembler.exceptions.parser;

import lombok.Getter;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Location;

@Getter
public class UnexpectedIdentifierException extends AssemblerException {

    private final String actual;
    private final String[] expected;

    public UnexpectedIdentifierException(Location where, String actual, String... expected) {
        super("Unexpected identifier, expected one of: " + String.join(", ", expected) + " got: " + actual, where);
        this.actual = actual;
        this.expected = expected;
    }

}

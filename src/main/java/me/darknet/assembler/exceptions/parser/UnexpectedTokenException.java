package me.darknet.assembler.exceptions.parser;

import lombok.Getter;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Location;
import me.darknet.assembler.parser.Token;

import java.util.Arrays;

@Getter
public class UnexpectedTokenException extends AssemblerException {

    private final String actual;
    private final Token.TokenType[] expected;

    public UnexpectedTokenException(Location where, String actual, Token.TokenType... expected) {
        super("Unexpected token, expected one of: " + Arrays.toString(expected) + " got: " + actual, where);
        this.actual = actual;
        this.expected = expected;
    }

}

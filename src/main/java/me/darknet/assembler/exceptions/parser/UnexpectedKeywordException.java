package me.darknet.assembler.exceptions.parser;

import lombok.Getter;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Keyword;
import me.darknet.assembler.parser.Location;

import java.util.Arrays;

@Getter
public class UnexpectedKeywordException extends AssemblerException {

    private final String actual;
    private final Keyword[] expected;

    public UnexpectedKeywordException(Location where, String actual, Keyword... expected) {
        super("Unexpected keyword, expected one of: " + Arrays.toString(expected) + " got: " + actual, where);
        this.actual = actual;
        this.expected = expected;
    }

}

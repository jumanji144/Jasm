package me.darknet.assembler.exceptions.parser;

import lombok.Getter;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Location;

@Getter
public class UnexpectedGroupException extends AssemblerException {

    private final Group.GroupType actual;
    private final Group.GroupType expected;

    public UnexpectedGroupException(Location where, Group.GroupType actual, Group.GroupType expected) {
        super("Unexpected group expected: " + expected.name() + " got: " + actual.name(), where);
        this.actual = actual;
        this.expected = expected;
    }

}

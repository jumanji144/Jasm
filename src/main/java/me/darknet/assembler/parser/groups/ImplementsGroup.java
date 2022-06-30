package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class ImplementsGroup extends Group {
    public IdentifierGroup className;

    public ImplementsGroup(Token value, IdentifierGroup className) {
        super(GroupType.IMPLEMENTS_DIRECTIVE, value, className);
        this.className = className;
    }

    public IdentifierGroup getClassName() {
        return className;
    }
}

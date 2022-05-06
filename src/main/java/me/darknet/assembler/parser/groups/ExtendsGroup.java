package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class ExtendsGroup extends Group {

    public IdentifierGroup className;

    public ExtendsGroup(Token value, IdentifierGroup className) {
        super(GroupType.EXTENDS_DIRECTIVE, value, className);
        this.className = className;
    }

    public IdentifierGroup getClassName() {
        return className;
    }

}

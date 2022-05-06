package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

/**
 *
 */
public class IdentifierGroup extends Group {

    public IdentifierGroup(GroupType type, Token value) {
        super(type, value);
    }

    public IdentifierGroup(Token value) {
        super(GroupType.IDENTIFIER, value);
    }

}

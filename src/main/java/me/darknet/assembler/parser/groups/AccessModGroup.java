package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class AccessModGroup extends Group {

    public AccessModGroup(Token token) {
        super(GroupType.ACCESS_MOD, token);
    }

}

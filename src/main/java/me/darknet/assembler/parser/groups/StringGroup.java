package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class StringGroup extends Group {

    public StringGroup(Token value) {
        super(GroupType.STRING, value);
    }

}

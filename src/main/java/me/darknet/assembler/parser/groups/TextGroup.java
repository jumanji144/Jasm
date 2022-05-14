package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class TextGroup extends Group {

    public TextGroup(Token token) {
        super(GroupType.TEXT, token);
    }

}

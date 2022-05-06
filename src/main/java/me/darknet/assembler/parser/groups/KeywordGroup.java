package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class KeywordGroup extends Group {
    public KeywordGroup(Token value) {
        super(GroupType.KEYWORD, value);
    }
}

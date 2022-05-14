package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class ExprGroup extends Group {

    public TextGroup textGroup;

    public ExprGroup(Token token, TextGroup textGroup) {
        super(GroupType.EXPR, token, textGroup);
        this.textGroup = textGroup;
    }

}

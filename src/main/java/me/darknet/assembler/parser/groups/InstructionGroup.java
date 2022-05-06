package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class InstructionGroup extends Group {

    public InstructionGroup(Token value, Group... children) {
        super(GroupType.INSTRUCTION, value, children);
    }

}

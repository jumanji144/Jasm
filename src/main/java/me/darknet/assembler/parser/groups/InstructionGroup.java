package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.List;

public class InstructionGroup extends Group {

    public InstructionGroup(Token value, List<Group> children) {
        super(GroupType.INSTRUCTION, value, children);
    }

}

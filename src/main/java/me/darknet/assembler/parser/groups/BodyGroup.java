package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class BodyGroup extends Group {
    public BodyGroup(Group... groups) {
        super(GroupType.BODY, groups);
    }
}

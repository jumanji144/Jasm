package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.List;

@Getter
public class WithGroup extends Group {

    private final List<IdentifierGroup> with;

    public WithGroup(Token value, List<IdentifierGroup> with) {
        super(GroupType.MODULE_WITH, value, with);
        this.with = with;
    }

}

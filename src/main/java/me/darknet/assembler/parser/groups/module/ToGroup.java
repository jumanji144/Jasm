package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.List;

@Getter
public class ToGroup extends Group {

    private final List<IdentifierGroup> to;

    public ToGroup(Token value, List<IdentifierGroup> to) {
        super(GroupType.MODULE_TO, value, to);
        this.to = to;
    }

}

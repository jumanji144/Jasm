package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.util.GroupLists;

import java.util.List;

@Getter
public class ProvideGroup extends Group {

    private final IdentifierGroup service;
    private final WithGroup with;

    public ProvideGroup(Token value, IdentifierGroup service, WithGroup with) {
        super(GroupType.MODULE_PROVIDE, value, with, service);
        this.service = service;
        this.with = with;
    }
}

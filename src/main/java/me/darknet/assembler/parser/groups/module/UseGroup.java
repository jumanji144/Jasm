package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

@Getter
public class UseGroup extends Group {

    private final IdentifierGroup service;

    public UseGroup(Token value, IdentifierGroup service) {
        super(GroupType.MODULE_USE, value, service);
        this.service = service;
    }

}

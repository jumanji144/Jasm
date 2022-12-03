package me.darknet.assembler.parser.groups.module;

import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.attributes.AccessModsGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;

public class OpenGroup extends ModuleParameter {
    public OpenGroup(Token token, AccessModsGroup accessModsGroup, IdentifierGroup module, ToGroup to) {
        super(GroupType.MODULE_OPEN, token, accessModsGroup, module, to);
    }
}

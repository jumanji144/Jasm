package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.AccessModsGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;

public class ExportGroup extends ModuleParameter {
    public ExportGroup(Token value, AccessModsGroup accessModsGroup, IdentifierGroup module, ToGroup to) {
        super(GroupType.MODULE_EXPORT, value, accessModsGroup, module, to);
    }
}

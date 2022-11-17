package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.AccessModsGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.List;

@Getter
public class ModuleParameter extends Group {

    private final AccessModsGroup accessMods;
    private final IdentifierGroup module;
    private final ToGroup to;

    public ModuleParameter(GroupType type, Token value, AccessModsGroup accessMods, IdentifierGroup module, ToGroup to) {
        super(type, value, accessMods, module, to);
        this.module = module;
        this.accessMods = accessMods;
        this.to = to;
    }
}

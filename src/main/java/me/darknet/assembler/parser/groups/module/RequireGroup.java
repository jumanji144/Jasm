package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.attributes.AccessModsGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.parser.groups.attributes.VersionGroup;

import java.util.ArrayList;

@Getter
public class RequireGroup extends ModuleParameter {

    private final VersionGroup version;

    public RequireGroup(Token token, AccessModsGroup accessModsGroup, IdentifierGroup module, VersionGroup version) {
        super(GroupType.MODULE_REQUIRE, token, accessModsGroup, module, new ToGroup(token, new ArrayList<>()));
        this.version = version;
    }
}

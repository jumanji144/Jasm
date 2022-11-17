package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.AccessModsGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.ArrayList;

@Getter
public class RequireGroup extends ModuleParameter {

    private final IdentifierGroup version;

    public RequireGroup(Token token, AccessModsGroup accessModsGroup, IdentifierGroup module, IdentifierGroup version) {
        super(GroupType.MODULE_REQUIRE, token, accessModsGroup, module, new ToGroup(token, new ArrayList<>()));
        this.version = version;
    }
}

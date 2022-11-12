package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Arrays;

@Getter
public class ClassDeclarationGroup extends Group {
    private final AccessModsGroup accessMods;
    private final IdentifierGroup name;

    public ClassDeclarationGroup(Token value, AccessModsGroup accessMods, IdentifierGroup name) {
        super(GroupType.CLASS_DECLARATION, value, Arrays.asList(accessMods, name));
        this.accessMods = accessMods;
        this.name = name;
    }
}

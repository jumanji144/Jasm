package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class ClassDeclarationGroup extends Group {


    public AccessModsGroup accessMods;
    public IdentifierGroup name;

    public ClassDeclarationGroup(Token value, AccessModsGroup accessMods, IdentifierGroup name) {
        super(GroupType.CLASS_DECLARATION, value, accessMods, name);
        this.accessMods = accessMods;
        this.name = name;
    }

    public AccessModsGroup getAccessMods() {
        return accessMods;
    }

    public IdentifierGroup getName() {
        return name;
    }

}

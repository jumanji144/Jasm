package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class FieldDeclarationGroup extends Group {

    public AccessModsGroup accessMods;
    public IdentifierGroup name;
    public IdentifierGroup descriptor;

    public FieldDeclarationGroup(Token t, AccessModsGroup accessMods, IdentifierGroup name, IdentifierGroup descriptor) {
        super(GroupType.FIELD_DECLARATION, t, accessMods, name, descriptor);
        this.accessMods = accessMods;
        this.name = name;
        this.descriptor = descriptor;
    }

    public AccessModsGroup getAccessMods() {
        return accessMods;
    }

    public IdentifierGroup getName() {
        return name;
    }

    public IdentifierGroup getDescriptor() {
        return descriptor;
    }



}

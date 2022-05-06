package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class MethodDeclarationGroup extends Group {

    public AccessModsGroup accessMods;
    public IdentifierGroup descriptor;
    public BodyGroup body;

    public MethodDeclarationGroup(Token value, AccessModsGroup accessMods, IdentifierGroup descriptor, BodyGroup body) {
        super(GroupType.METHOD_DECLARATION, value, accessMods, descriptor, body);
    }

    public AccessModsGroup getAccessMods() {
        return accessMods;
    }

    public IdentifierGroup getDescriptor() {
        return descriptor;
    }

    public BodyGroup getBody() {
        return body;
    }


}

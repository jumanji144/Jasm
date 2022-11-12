package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Arrays;

@Getter
public class FieldDeclarationGroup extends Group {
    private final AccessModsGroup accessMods;
    private final IdentifierGroup name;
    private final IdentifierGroup descriptor;
    private final Group constantValue;

    public FieldDeclarationGroup(Token t, AccessModsGroup accessMods, IdentifierGroup name, IdentifierGroup descriptor) {
        super(GroupType.FIELD_DECLARATION, t, Arrays.asList(accessMods, name, descriptor));
        this.accessMods = accessMods;
        this.name = name;
        this.descriptor = descriptor;
        this.constantValue = null;
    }

    public FieldDeclarationGroup(Token t, AccessModsGroup accessMods, IdentifierGroup name, IdentifierGroup descriptor, Group constValue) {
        super(GroupType.FIELD_DECLARATION, t, Arrays.asList(accessMods, name, descriptor, constValue));
        this.accessMods = accessMods;
        this.name = name;
        this.descriptor = descriptor;
        this.constantValue = constValue;
    }
}

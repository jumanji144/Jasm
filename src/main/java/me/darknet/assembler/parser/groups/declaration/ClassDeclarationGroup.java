package me.darknet.assembler.parser.groups.declaration;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.ExtendsGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.parser.groups.ImplementsGroup;
import me.darknet.assembler.parser.groups.attributes.AccessModsGroup;
import me.darknet.assembler.util.GroupLists;

import java.util.List;

@Getter
public class ClassDeclarationGroup extends Group {
    private final AccessModsGroup accessMods;
    private final IdentifierGroup name;
    private final ExtendsGroup extendsGroup;
    private final List<ImplementsGroup> implementsGroups;

    public ClassDeclarationGroup(Token value, AccessModsGroup accessMods, IdentifierGroup name, ExtendsGroup extendsGroup, List<ImplementsGroup> implementsGroups) {
        super(GroupType.CLASS_DECLARATION, value, GroupLists.fromArray(accessMods, name, extendsGroup, implementsGroups));
        this.accessMods = accessMods;
        this.name = name;
        this.extendsGroup = extendsGroup;
        this.implementsGroups = implementsGroups;
    }
}

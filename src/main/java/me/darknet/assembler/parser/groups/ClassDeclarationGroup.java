package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
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

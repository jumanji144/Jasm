package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;

import java.util.List;

public class AccessModsGroup extends Group {

    public AccessModGroup[] accessMods;

    public AccessModsGroup(AccessModGroup... accessModGroups) {
        super(GroupType.ACCESS_MODS, accessModGroups);
        this.accessMods = accessModGroups;
    }

}

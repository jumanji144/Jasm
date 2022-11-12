package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;

import java.util.List;

@Getter
public class AccessModsGroup extends Group {
	private final List<AccessModGroup> accessMods;

	public AccessModsGroup(List<AccessModGroup> accessModGroups) {
		super(GroupType.ACCESS_MODS, accessModGroups);
		this.accessMods = accessModGroups;
	}
}

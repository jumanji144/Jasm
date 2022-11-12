package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;

import java.util.List;

public class BodyGroup extends Group {
	public BodyGroup(List<Group> groups) {
		super(GroupType.BODY, groups);
	}
}

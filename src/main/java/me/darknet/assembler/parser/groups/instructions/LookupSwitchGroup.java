package me.darknet.assembler.parser.groups.instructions;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.GroupLists;

import java.util.List;

@Getter
public class LookupSwitchGroup extends Group {
	private final List<CaseLabelGroup> caseLabels;
	private final DefaultLabelGroup defaultLabel;

	public LookupSwitchGroup(Token value, DefaultLabelGroup defaultLable, List<CaseLabelGroup> caseGroups) {
		super(GroupType.LOOKUP_SWITCH, value, GroupLists.add(caseGroups, defaultLable));
		this.caseLabels = caseGroups;
		this.defaultLabel = defaultLable;
	}
}

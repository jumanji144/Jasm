package me.darknet.assembler.parser.groups.instructions;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.NumberGroup;
import me.darknet.assembler.util.GroupLists;

import java.util.List;

@Getter
public class TableSwitchGroup extends Group {
	private final List<LabelGroup> labels;
	private final DefaultLabelGroup defaultLabel;
	private final NumberGroup min;
	private final NumberGroup max;

	public TableSwitchGroup(Token token, NumberGroup min, NumberGroup max, DefaultLabelGroup defaultLabel, List<LabelGroup> labels) {
		super(GroupType.TABLE_SWITCH, token, GroupLists.add(labels, defaultLabel));
		this.defaultLabel = defaultLabel;
		this.min = min;
		this.max = max;
		this.labels = labels;
	}
}

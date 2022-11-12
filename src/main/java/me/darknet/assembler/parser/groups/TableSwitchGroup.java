package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.ArrayUtil;

import java.util.List;

@Getter
public class TableSwitchGroup extends Group {
	private final List<LabelGroup> labels;
	private final DefaultLabelGroup defaultLabel;
	private final NumberGroup min;
	private final NumberGroup max;

	public TableSwitchGroup(Token token, NumberGroup min, NumberGroup max, DefaultLabelGroup defaultLabel, List<LabelGroup> labels) {
		super(GroupType.TABLE_SWITCH, token, ArrayUtil.add(labels, defaultLabel));
		this.defaultLabel = defaultLabel;
		this.min = min;
		this.max = max;
		this.labels = labels;
	}
}

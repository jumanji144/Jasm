package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.ArrayUtil;

public class TableSwitchGroup extends Group {

    LabelGroup[] labels;
    DefaultLabelGroup defaultLabel;
    @Getter
    NumberGroup min;
    @Getter
    NumberGroup max;

    public TableSwitchGroup(Token token, NumberGroup min, NumberGroup max, DefaultLabelGroup defaultLabel, LabelGroup... labels) {
        super(GroupType.TABLE_SWITCH, token, ArrayUtil.add(labels, defaultLabel));
        this.defaultLabel = defaultLabel;
        this.min = min;
        this.max = max;
        this.labels = labels;
    }

    /**
     * Returns the case labels without the default label.
     * @return the case labels without the default label.
     */
    public LabelGroup[] getLabelGroups() {
        return labels;
    }

    /**
     * Returns the default label.
     * @return the default label.
     */
    public DefaultLabelGroup getDefaultLabel() {
        return defaultLabel;
    }


}

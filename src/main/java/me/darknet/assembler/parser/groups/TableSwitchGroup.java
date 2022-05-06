package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.ArrayUtil;

public class TableSwitchGroup extends Group {


    public TableSwitchGroup(Token token, DefaultLabelGroup defaultLabel, LabelGroup... labels) {
        super(GroupType.TABLE_SWITCH, token, ArrayUtil.add(labels, defaultLabel));
    }

    /**
     * Returns the case labels without the default label.
     * @return the case labels without the default label.
     */
    public LabelGroup[] getLabelGroups() {
        LabelGroup[] caseGroups = new LabelGroup[this.size() - 1];
        for(int i = 0; i < caseGroups.length; i++) {
            caseGroups[i] = (LabelGroup) this.get(i);
        }
        return caseGroups;
    }

    /**
     * Returns the default label.
     * @return the default label.
     */
    public DefaultLabelGroup getDefaultLabel() {
        return (DefaultLabelGroup) getChild(GroupType.DEFAULT_LABEL);
    }


}

package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.ArrayUtil;

public class LookupSwitchGroup extends Group {

    public CaseLabelGroup[] caseLabels;
    public DefaultLabelGroup defaultLabel;

    public LookupSwitchGroup(Token value, DefaultLabelGroup defaultLable, CaseLabelGroup... caseGroups) {
        super(GroupType.LOOKUP_SWITCH, value, ArrayUtil.add(caseGroups, defaultLable));
        this.caseLabels = caseGroups;
        this.defaultLabel = defaultLable;
    }

    /**
     * Returns the case labels without the default label.
     * @return the case labels without the default label.
     */
    public CaseLabelGroup[] getCaseGroups() {
        return caseLabels;
    }

    /**
     * Returns the default label.
     * @return the default label.
     */
    public DefaultLabelGroup getDefaultLabel() {
        return defaultLabel;
    }

}

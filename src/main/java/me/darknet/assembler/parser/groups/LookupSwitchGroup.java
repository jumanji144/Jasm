package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.ArrayUtil;

public class LookupSwitchGroup extends Group {

    public LookupSwitchGroup(Token value, DefaultLabelGroup defaultLable, CaseLabelGroup... caseGroups) {
        super(GroupType.LOOKUP_SWITCH, value, ArrayUtil.add(caseGroups, defaultLable));
    }

    /**
     * Returns the case labels without the default label.
     * @return the case labels without the default label.
     */
    public CaseLabelGroup[] getCaseGroups() {
        CaseLabelGroup[] caseGroups = new CaseLabelGroup[this.size() - 1];
        for(int i = 0; i < caseGroups.length; i++) {
            caseGroups[i] = (CaseLabelGroup) this.get(i);
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

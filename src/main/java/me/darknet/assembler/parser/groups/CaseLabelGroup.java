package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class CaseLabelGroup extends Group {

    public NumberGroup key;
    public LabelGroup value;

    public CaseLabelGroup(Token val, NumberGroup key, LabelGroup value) {
        super(GroupType.CASE_LABEL, val, key, value);
        this.key = key;
        this.value = value;
    }

    public NumberGroup getKey() {
        return key;
    }

    public LabelGroup getVal() {
        return value;
    }

}

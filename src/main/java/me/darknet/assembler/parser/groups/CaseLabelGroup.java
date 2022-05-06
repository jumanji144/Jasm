package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class CaseLabelGroup extends Group {

    public NumberGroup key;
    public IdentifierGroup value;

    public CaseLabelGroup(Token val, NumberGroup key, IdentifierGroup value) {
        super(GroupType.CASE_LABEL, val);
    }

    public NumberGroup getKey() {
        return key;
    }

    public IdentifierGroup getVal() {
        return value;
    }

}

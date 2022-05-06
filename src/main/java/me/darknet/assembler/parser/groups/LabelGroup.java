package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class LabelGroup extends Group {

    public LabelGroup(Group identifierGroup) {
        super(GroupType.LABEL, identifierGroup.value);
    }
    public LabelGroup(Token value) {
        super(GroupType.LABEL, value);
    }

    public String getLabel() {
        return content().replace(":", "");
    }

}

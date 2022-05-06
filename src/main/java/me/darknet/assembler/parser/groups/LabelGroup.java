package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class LabelGroup extends IdentifierGroup {

    public LabelGroup(Token value) {
        super(GroupType.LABEL, value);
    }

    public String getLabel() {
        return content().replace(":", "");
    }

}

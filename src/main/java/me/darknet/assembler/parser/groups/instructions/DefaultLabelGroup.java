package me.darknet.assembler.parser.groups.instructions;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Collections;

public class DefaultLabelGroup extends Group {
    public DefaultLabelGroup(Token value, LabelGroup identifier) {
        super(GroupType.DEFAULT_LABEL, value, Collections.singletonList(identifier));
    }

    public String getLabel() {
        LabelGroup identifier = (LabelGroup) get(0);
        return identifier.getLabel();
    }
}

package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.awt.*;

public class DefaultLabelGroup extends Group {

    public DefaultLabelGroup(Token value, LabelGroup identifier) {
        super(GroupType.DEFAULT_LABEL, value, identifier);
    }

    public String getLabel() {
        LabelGroup identifier = (LabelGroup) get(0);
        return identifier.getLabel();
    }

}

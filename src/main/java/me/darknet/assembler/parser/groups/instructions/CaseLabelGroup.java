package me.darknet.assembler.parser.groups.instructions;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.NumberGroup;

import java.util.Arrays;

@Getter
public class CaseLabelGroup extends Group {
    private final NumberGroup key;
    private final LabelGroup labelValue;

    public CaseLabelGroup(Token val, NumberGroup key, LabelGroup value) {
        super(GroupType.CASE_LABEL, val, Arrays.asList(key, value));
        this.key = key;
        this.labelValue = value;
    }
}

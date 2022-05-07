package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class AnnotationParamGroup extends Group {

    public IdentifierGroup name;
    public Group value;

    public AnnotationParamGroup(Token token, IdentifierGroup name, Group value) {
        super(GroupType.ANNOTATION_PARAMETER, token, name, value);
        this.name = name;
        this.value = value;
    }

}

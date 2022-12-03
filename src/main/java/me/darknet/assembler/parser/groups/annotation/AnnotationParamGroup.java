package me.darknet.assembler.parser.groups.annotation;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.Arrays;

@Getter
public class AnnotationParamGroup extends Group {
    private final IdentifierGroup name;
    private final Group paramValue;

    public AnnotationParamGroup(Token token, IdentifierGroup name, Group value) {
        super(GroupType.ANNOTATION_PARAMETER, token, Arrays.asList(name, value));
        this.name = name;
        this.paramValue = value;
    }
}

package me.darknet.assembler.parser.groups.attributes;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.Collections;

@Getter
public class PermittedSubclassGroup extends Group {

    private final IdentifierGroup subclass;

    public PermittedSubclassGroup(Token token, IdentifierGroup subclass) {
        super(GroupType.PERMITTED_SUBCLASS_DIRECTIVE, token, Collections.singletonList(subclass));
        this.subclass = subclass;
    }
}

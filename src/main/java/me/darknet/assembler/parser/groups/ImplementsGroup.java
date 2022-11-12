package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Collections;

@Getter
public class ImplementsGroup extends Group {
    private final IdentifierGroup className;

    public ImplementsGroup(Token value, IdentifierGroup className) {
        super(GroupType.IMPLEMENTS_DIRECTIVE, value, Collections.singletonList(className));
        this.className = className;
    }
}

package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class ThrowsGroup extends Group {

    @Getter
    IdentifierGroup className;

    public ThrowsGroup(Token token, IdentifierGroup className) {
        super(GroupType.THROWS, token, className);
        this.className = className;
    }
}

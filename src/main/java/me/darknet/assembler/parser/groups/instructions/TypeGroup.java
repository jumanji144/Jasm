package me.darknet.assembler.parser.groups.instructions;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.Collections;

@Getter
public class TypeGroup extends Group {
    private final IdentifierGroup descriptor;

    public TypeGroup(Token token, IdentifierGroup descriptor) {
        super(GroupType.TYPE, token, Collections.singletonList(descriptor));
        this.descriptor = descriptor;
    }
}

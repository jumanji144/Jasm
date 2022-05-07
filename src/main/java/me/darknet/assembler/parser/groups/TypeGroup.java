package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class TypeGroup extends Group {

    public IdentifierGroup descriptor;

    public TypeGroup(Token token, IdentifierGroup descriptor) {
        super(GroupType.TYPE, token, descriptor);
        this.descriptor = descriptor;
    }


}

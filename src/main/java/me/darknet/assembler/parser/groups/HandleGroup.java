package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

@Getter
public class HandleGroup extends Group {

    IdentifierGroup handleType;
    IdentifierGroup name;

    IdentifierGroup descriptor;

    public HandleGroup(Token value, IdentifierGroup type, IdentifierGroup name, IdentifierGroup descriptor) {
        super(GroupType.HANDLE, value, type, descriptor);
        this.handleType = type;
        this.descriptor = descriptor;
        this.name = name;
    }
}

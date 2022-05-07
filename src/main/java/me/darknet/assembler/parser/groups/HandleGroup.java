package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

@Getter
public class HandleGroup extends Group {

    IdentifierGroup handleType;
    IdentifierGroup descriptor;

    public HandleGroup(Token value, IdentifierGroup type, IdentifierGroup descriptor) {
        super(GroupType.HANDLE, value, type, descriptor);
        this.handleType = type;
        this.descriptor = descriptor;
    }
}

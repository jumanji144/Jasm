package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

@Getter
public class HandleGroup extends Group {

    IdentifierGroup handleType;
    IdentifierGroup descriptor;

    IdentifierGroup fieldDescriptor;

    public HandleGroup(Token value, IdentifierGroup type, IdentifierGroup descriptor) {
        super(GroupType.HANDLE, value, type, descriptor);
        this.handleType = type;
        this.descriptor = descriptor;
    }

    public HandleGroup(Token value, IdentifierGroup type, IdentifierGroup descriptor, IdentifierGroup fieldDescriptor) {
        super(GroupType.HANDLE, value, type, descriptor);
        this.handleType = type;
        this.descriptor = descriptor;
        this.fieldDescriptor = fieldDescriptor;
    }

    public boolean isField() {
        return fieldDescriptor != null;
    }
}

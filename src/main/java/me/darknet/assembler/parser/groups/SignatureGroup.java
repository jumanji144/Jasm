package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class SignatureGroup extends Group {

    public IdentifierGroup descriptor;

    public SignatureGroup (Token value, IdentifierGroup descriptor) {
        super(GroupType.SIGNATURE_DIRECTIVE, value, descriptor);
        this.descriptor = descriptor;
    }

    public IdentifierGroup getDescriptor() {
        return descriptor;
    }

}

package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class EnumGroup extends Group {

    @Getter
    IdentifierGroup descriptor;
    @Getter
    IdentifierGroup enumValue;

    public EnumGroup(Token token, IdentifierGroup descriptor, IdentifierGroup value) {
        super(GroupType.ENUM, token, descriptor, value);
        this.descriptor = descriptor;
        this.enumValue = value;
    }

}

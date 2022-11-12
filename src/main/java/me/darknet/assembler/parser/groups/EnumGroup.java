package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Arrays;

@Getter
public class EnumGroup extends Group {
   private final IdentifierGroup descriptor;
   private final IdentifierGroup enumValue;

    public EnumGroup(Token token, IdentifierGroup descriptor, IdentifierGroup value) {
        super(GroupType.ENUM, token, Arrays.asList(descriptor, value));
        this.descriptor = descriptor;
        this.enumValue = value;
    }
}

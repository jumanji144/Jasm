package me.darknet.assembler.parser.groups.attributes;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class DeprecatedGroup extends Group implements ClassAttributeGroup, FieldAttributeGroup, MethodAttributeGroup {
    public DeprecatedGroup(Token token) {
        super(GroupType.DEPRECATED, token);
    }
}

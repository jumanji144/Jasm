package me.darknet.assembler.parser.groups.attributes;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.Collections;

@Getter
public class NestMemberGroup extends Group implements ClassAttributeGroup {

    private final IdentifierGroup memberName;

    public NestMemberGroup(Token token, IdentifierGroup memberName) {
        super(GroupType.NEST_HOST_DIRECTIVE, token, Collections.singletonList(memberName));
        this.memberName = memberName;
    }
}
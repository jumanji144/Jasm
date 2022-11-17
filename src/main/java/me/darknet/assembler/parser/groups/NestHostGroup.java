package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Collections;

@Getter
public class NestHostGroup extends Group implements ClassAttributeGroup {

    private final IdentifierGroup hostName;

    public NestHostGroup(Token token, IdentifierGroup hostName) {
        super(GroupType.NEST_HOST_DIRECTIVE, token, Collections.singletonList(hostName));
        this.hostName = hostName;
    }
}

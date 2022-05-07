package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class ArgsGroup extends Group {

    @Getter
    BodyGroup body;

    public ArgsGroup(Token token, BodyGroup body) {
        super(GroupType.ARGS, token, body);
        this.body = body;
    }

}

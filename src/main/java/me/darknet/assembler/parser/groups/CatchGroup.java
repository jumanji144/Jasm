package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

@Getter
public class CatchGroup extends Group {

    IdentifierGroup exception;
    LabelGroup begin;
    LabelGroup end;
    LabelGroup handler;

    public CatchGroup(Token value, IdentifierGroup exception, LabelGroup begin, LabelGroup end, LabelGroup handler) {
        super(GroupType.CATCH, value, begin, end, handler);
        this.exception = exception;
        this.begin = begin;
        this.end = end;
        this.handler = handler;
    }

}

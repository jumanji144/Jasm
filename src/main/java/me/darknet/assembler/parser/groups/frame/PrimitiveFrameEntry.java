package me.darknet.assembler.parser.groups.frame;

import lombok.Getter;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

@Getter
public class PrimitiveFrameEntry extends FrameEntryGroup{

    private final IdentifierGroup objectType;

    public PrimitiveFrameEntry(Token token, IdentifierGroup objectType) {
        super(token, objectType);
        this.objectType = objectType;
    }
}

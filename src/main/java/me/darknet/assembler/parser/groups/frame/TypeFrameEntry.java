package me.darknet.assembler.parser.groups.frame;

import lombok.Getter;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.instructions.TypeGroup;

@Getter
public class TypeFrameEntry extends FrameEntryGroup{

    private final TypeGroup objectType;

    public TypeFrameEntry(Token token, TypeGroup objectType) {
        super(token, objectType);
        this.objectType = objectType;
    }

}
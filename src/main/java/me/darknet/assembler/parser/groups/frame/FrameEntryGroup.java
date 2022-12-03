package me.darknet.assembler.parser.groups.frame;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

@Getter
public class FrameEntryGroup extends Group {

    private final Group frameObject;

    public FrameEntryGroup(Token token, Group frameObject) {
        super(GroupType.FRAME_ENTRY, token, frameObject.getChildren());
        this.frameObject = frameObject;
    }

}

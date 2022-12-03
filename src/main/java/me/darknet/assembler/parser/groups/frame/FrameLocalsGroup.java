package me.darknet.assembler.parser.groups.frame;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.List;

@Getter
public class FrameLocalsGroup extends Group {

    private final List<FrameEntryGroup> frameEntries;

    public FrameLocalsGroup(Token token, List<FrameEntryGroup> children) {
        super(GroupType.FRAME_LOCALS, token, children);
        frameEntries = children;
    }

}

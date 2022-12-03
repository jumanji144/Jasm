package me.darknet.assembler.parser.groups.frame;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.List;

@Getter
public class FrameStackGroup extends Group {

    private final List<FrameEntryGroup> frameEntries;

    public FrameStackGroup(Token token, List<FrameEntryGroup> children) {
        super(GroupType.FRAME_STACK, token, children);
        frameEntries = children;
    }

}

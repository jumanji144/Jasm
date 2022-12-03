package me.darknet.assembler.parser.groups.frame;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.util.GroupLists;

import java.util.List;

@Getter
public class FrameGroup extends Group {

    private final IdentifierGroup frameType;
    /**
     * List of the types of the locals. special type for TOP and UNINITIALIZED_THIS
     */
    private final List<FrameEntryGroup> locals;
    /**
     * List of the types of the stack.
     */
    private final List<FrameEntryGroup> stack;

    public FrameGroup(Token token, IdentifierGroup frameType, List<FrameEntryGroup> locals, List<FrameEntryGroup> stack) {
        super(GroupType.FRAME, token, GroupLists.fromArray(frameType, locals, stack));
        this.frameType = frameType;
        this.locals = locals;
        this.stack = stack;
    }

}

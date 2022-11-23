package me.darknet.assembler.parser.groups;

import me.darknet.assembler.instructions.Argument;
import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.List;

public class InstructionGroup extends Group {

    public InstructionGroup(Token value, List<Group> children) {
        super(GroupType.INSTRUCTION, value, children);
    }

    public Argument[] getArguments() {
        ParseInfo info = ParseInfo.get(getValue().getContent());
        if(info == null) {
            throw new IllegalStateException("Illegal instruction: " + getValue().getContent());
        }
        return info.getArgs();
    }

    public Argument[] getMissingArguments() throws AssemblerException {
        List<Group> onLine = getChildrenOnLine();
        Argument[] args = getArguments();
        if(onLine.size() > args.length) {
            throw new AssemblerException("Too many arguments for instruction " + getValue().getContent(), getStartLocation());
        }
        Argument[] missing = new Argument[args.length - onLine.size()];
        System.arraycopy(args, onLine.size(), missing, 0, missing.length);
        return missing;
    }

}

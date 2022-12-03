package me.darknet.assembler.parser.groups.instructions;

import me.darknet.assembler.exceptions.arguments.TooManyArgumentException;
import me.darknet.assembler.instructions.Argument;
import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Arrays;
import java.util.List;

public class InstructionGroup extends Group {

    public InstructionGroup(Token value, List<Group> children) {
        super(GroupType.INSTRUCTION, value, children);
    }

    /**
     * Get the parsing arguments of this instruction
     * @return argument array
     * @throws IllegalStateException if the instruction does not exist
     */
    public Argument[] getArguments() {
        ParseInfo info = ParseInfo.get(getValue().getContent());
        if(info == null) {
            throw new IllegalStateException("Illegal instruction: " + getValue().getContent());
        }
        return info.getArgs();
    }

    /**
     * Support method to give you missing arguments of this instruction for argument validation.
     * @return Missing arguments array (length 0 if no missing arguments)
     * @throws TooManyArgumentException If there are too many arguments
     */
    public Argument[] getMissingArguments() throws TooManyArgumentException {
        List<Group> onLine = getChildrenOnLine();
        Argument[] args = getArguments();
        if(onLine.size() > args.length) {
            throw new TooManyArgumentException(getValue().getLocation(), args.length, onLine.size());
        }
        return Arrays.copyOfRange(args, onLine.size(), args.length);
    }

}

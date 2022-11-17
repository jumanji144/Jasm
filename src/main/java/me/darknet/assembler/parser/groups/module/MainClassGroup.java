package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

@Getter
public class MainClassGroup extends Group {

    IdentifierGroup mainClass;

    public MainClassGroup(Token token, IdentifierGroup mainClass) {
        super(GroupType.MODULE_MAIN_CLASS, token, mainClass);
        this.mainClass = mainClass;
    }

}

package me.darknet.assembler.parser.groups.module;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

@Getter
public class PackageGroup extends Group {

    private final IdentifierGroup packageClass;

    public PackageGroup(Token token, IdentifierGroup packageClass) {
        super(GroupType.MODULE_PACKAGE, token, packageClass);
        this.packageClass = packageClass;
    }

}

package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class MethodParametersGroup extends Group {

    public MethodParameterGroup[] methodParameters;

    public MethodParametersGroup(MethodParameterGroup... children) {
        super(GroupType.METHOD_PARAMETERS, children);
        methodParameters = children;
    }
}

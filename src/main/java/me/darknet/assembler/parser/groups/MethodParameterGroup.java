package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class MethodParameterGroup extends Group {

    IdentifierGroup descriptor;
    IdentifierGroup name;

    public MethodParameterGroup(Token value, IdentifierGroup descriptor, IdentifierGroup name) {
        super(GroupType.METHOD_PARAMETER, value, descriptor, name);
        this.descriptor = descriptor;
        this.name = name;
    }

    public IdentifierGroup getDescriptor() {
        return descriptor;
    }

    public IdentifierGroup getName() {
        return name;
    }

    public String getDescriptorValue() {
        String content = descriptor.content();
        if(content.charAt(0) == '(') {
            // remove first and last character
            return content.substring(1);
        }
        return content;
    }

    public String getNameValue() {
        String content = name.content();
        if(content.indexOf(')') != -1) {
            // remove everything after the last )
            return content.substring(0, content.indexOf(')'));
        }
        // remove last character
        return content.substring(0, content.length() - 1);
    }
}

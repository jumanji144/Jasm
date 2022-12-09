package me.darknet.assembler.parser.groups.record;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.parser.groups.attributes.AttributeGroup;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RecordComponentGroup extends Group {

    private final IdentifierGroup identifier;
    private final IdentifierGroup descriptor;
    private final List<AttributeGroup> attributes = new ArrayList<>();

    public RecordComponentGroup(Token value, IdentifierGroup identifier, IdentifierGroup descriptor) {
        super(GroupType.RECORD_COMPONENT, value, identifier, descriptor);
        this.identifier = identifier;
        this.descriptor = descriptor;
    }

    public void addAttribute(AttributeGroup attribute) {
        attributes.add(attribute);
    }
}

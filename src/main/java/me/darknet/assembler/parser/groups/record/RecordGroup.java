package me.darknet.assembler.parser.groups.record;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.attributes.ClassAttributeGroup;

import java.util.List;

@Getter
public class RecordGroup extends Group implements ClassAttributeGroup {

    private final List<RecordComponentGroup> components;

    public RecordGroup(Token value, List<RecordComponentGroup> components) {
        super(GroupType.RECORD, value, components);
        this.components = components;
    }

}

package me.darknet.assembler.parser.groups.method;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.parser.groups.attributes.MethodAttributeGroup;

import java.util.Collections;

@Getter

public class ThrowsGroup extends Group implements MethodAttributeGroup {
	private final IdentifierGroup className;

	public ThrowsGroup(Token token, IdentifierGroup className) {
		super(GroupType.THROWS, token, Collections.singletonList(className));
		this.className = className;
	}
}

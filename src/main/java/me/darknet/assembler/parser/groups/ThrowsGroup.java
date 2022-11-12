package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Collections;

@Getter

public class ThrowsGroup extends Group {
	private final IdentifierGroup className;

	public ThrowsGroup(Token token, IdentifierGroup className) {
		super(GroupType.THROWS, token, Collections.singletonList(className));
		this.className = className;
	}
}

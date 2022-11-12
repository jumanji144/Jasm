package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Collections;

@Getter
public class ExtendsGroup extends Group {
	private final IdentifierGroup className;

	public ExtendsGroup(Token value, IdentifierGroup className) {
		super(GroupType.EXTENDS_DIRECTIVE, value, Collections.singletonList(className));
		this.className = className;
	}
}

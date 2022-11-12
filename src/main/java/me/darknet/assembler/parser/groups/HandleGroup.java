package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Arrays;

@Getter
public class HandleGroup extends Group {
	private final IdentifierGroup handleType;
	private final IdentifierGroup name;
	private final IdentifierGroup descriptor;

	public HandleGroup(Token value, IdentifierGroup type, IdentifierGroup name, IdentifierGroup descriptor) {
		super(GroupType.HANDLE, value, Arrays.asList(type, name, descriptor));
		this.handleType = type;
		this.descriptor = descriptor;
		this.name = name;
	}
}

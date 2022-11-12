package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Collections;

@Getter
public class ArgsGroup extends Group {
	private final BodyGroup body;

	public ArgsGroup(Token token, BodyGroup body) {
		super(GroupType.ARGS, token, Collections.singletonList(body));
		this.body = body;
	}
}

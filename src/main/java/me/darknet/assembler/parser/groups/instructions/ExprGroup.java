package me.darknet.assembler.parser.groups.instructions;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.TextGroup;

import java.util.Collections;

@Getter
public class ExprGroup extends Group {
	private final TextGroup textGroup;

	public ExprGroup(Token token, TextGroup textGroup) {
		super(GroupType.EXPR, token, Collections.singletonList(textGroup));
		this.textGroup = textGroup;
	}
}

package me.darknet.assembler.parser.groups.instructions;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.Arrays;

@Getter
public class CatchGroup extends Group {
	private final IdentifierGroup exception;
	private final LabelGroup begin;
	private final LabelGroup end;
	private final LabelGroup handler;

	public CatchGroup(Token value, IdentifierGroup exception, LabelGroup begin, LabelGroup end, LabelGroup handler) {
		super(GroupType.CATCH, value, Arrays.asList(begin, end, handler));
		this.exception = exception;
		this.begin = begin;
		this.end = end;
		this.handler = handler;
	}
}

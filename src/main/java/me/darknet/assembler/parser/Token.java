package me.darknet.assembler.parser;

import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;

public class Token {

	private final Range range;
	private final Location location;
	private final TokenType type;
	private final String content;

	public Token(Range range, Location location, TokenType type, String content) {
		this.range = range;
		this.location = location;
		this.type = type;
		this.content = content;
	}

	public Range getRange() {
		return range;
	}

	public TokenType getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public Location getLocation() {
		return location;
	}

}

package me.darknet.assembler.parser;

import lombok.Data;

import java.util.Objects;

@Data
public class Token {
	private final String content;
	private final Location location;
	private final TokenType type;

	/**
	 * @return Location start position.
	 */
	public int getStart() {
		return location.getPosition();
	}

	/**
	 * @return Location end position.
	 */
	public int getEnd() {
		return location.getPosition() + content.length();
	}

	/**
	 * @return Size of location end-start.
	 */
	public int getWidth() {
		return getEnd() - getStart();
	}

	/**
	 * @param type
	 * 		Type to check against.
	 *
	 * @return {@code true} when this {@link #getType()} matches the given type.
	 */
	public boolean isType(TokenType type) {
		return Objects.equals(this.type, type);
	}

	public enum TokenType {
		EOF,
		STRING,
		NUMBER,
		IDENTIFIER,
		TEXT,
		KEYWORD
	}
}

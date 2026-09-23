package me.darknet.assembler.parser;

import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;

/**
 * Lexical token carrying its source range, location, type, and content.
 *
 * @param range
 * 		Range occupied by the token in the source input.
 * @param location
 * 		Source location where the token begins.
 * @param type
 * 		Lexical category of the token.
 * @param content
 * 		Text contained by the token.
 */
public record Token(Range range, Location location, TokenType type, String content) {
}

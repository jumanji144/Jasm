package me.darknet.assembler.parser;

import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {

	public static boolean isOperator(char c) {
		return c == '{' || c == '}' || c == ':' || c == ',';
	}

	public List<Token> tokenize(String source, String input) {
		TokenizerContext ctx = new TokenizerContext();
		ctx.input = input;
		ctx.buffer = new StringBuffer();
		ctx.source = source;
		int length = input.length();
		while (ctx.index < length) {
			char c = input.charAt(ctx.index);
			if (ctx.inComment) {
				if (c == '\n') {
					ctx.collectToken();
					ctx.inComment = false;
					ctx.nextLine();
					ctx.index++;
					continue;
				}
			}
			if (ctx.isString()) {
				switch (c) {
					case '"':
						ctx.collectToken();
						ctx.inString = false;
						ctx.index++;
						break;
					case '\\': {
						ctx.index++;
						ctx.processEscape();
						break;
					}
					default:
						ctx.forward();
						break;
				}
			} else if (Character.isWhitespace(c)) {
				ctx.collectToken();
				if (c == '\n') {
					ctx.nextLine();
				} else {
					ctx.column++;
				}
				ctx.index++;
			} else {
				if (c == ';' && input.charAt(ctx.index + 1) == ';') {
					ctx.index++;
					ctx.index++;
					ctx.inComment = true;
				} else if (c == '"') {
					ctx.index++;
					ctx.inString = true;
				} else if (isOperator(c)) {
					ctx.collectToken();
					ctx.forward();
					ctx.collectToken();
				} else {
					ctx.forward();
				}
			}
		}

		ctx.collectToken();

		return ctx.tokens;
	}

	private static class TokenizerContext {

		private int line = 1;
		private int column = 1;
		private int index;
		private boolean inString;
		private boolean inComment;
		private StringBuffer buffer;
		private List<Token> tokens = new ArrayList<>();

		private String input, source;

		public void forward() {
			buffer.append(input.charAt(index++));
			column++;
		}

		public void nextLine() {
			line++;
			column = 1;
		}

		public boolean isString() {
			return inString;
		}

		public TokenType getType(String content) {
			if (content.length() == 1) {
				char c = content.charAt(0);
				if (isOperator(c)) return TokenType.OPERATOR;
			}
			if (inString) return TokenType.STRING;
			TokenType type = TokenType.IDENTIFIER;
			// check if all the characters in the token are digits (and the '-' sign)
			boolean isNumber = true;
			boolean numberAppeared = false;
			boolean isHex = false;
			for (int j = 0; j < content.length(); j++) {
				char c2 = content.charAt(j);
				if (c2 == '-') {
					if (j == 0)
						continue;
					else if (content.charAt(j - 1) == 'E')
						continue;
				}
				if (c2 < '0' || c2 > '9') { // is not number
					if (numberAppeared) { // check if there was a number before
						if (c2 == 'x') { // hex number
							isHex = true; // toggle state
							continue;
						}
						if (c2 != '.'
								&& c2 != 'f'
								&& c2 != 'F'
								&& c2 != 'L'
								&& c2 != 'D'
								&& c2 != 'E') { // is not one of the suffixes
							if (!isHex) { // if not hex, then it is not a number
								isNumber = false;
								break;
							} else { // if hex check if it is a valid hex number
								if (c2 < 'a' || c2 > 'f') { // lowercase
									if (c2 < 'A' || c2 > 'F') { // uppercase
										isNumber = false;
										break;
									}
								}
							}
						}
					} else {
						isNumber = false;
						break;
					}
				}
				numberAppeared = true;
			}
			if (isNumber)
				type = TokenType.NUMBER;
			return type;
		}

		public void collectToken() {
			if (buffer.length() == 0) return;
			String content = buffer.toString();
			Range range = new Range(index - content.length(), index);
			Location location = new Location(line, column, source);

			TokenType type = getType(content);

			tokens.add(new Token(range, location, type, content));

			buffer = new StringBuffer();
		}

		public void processEscape() {
			switch (input.charAt(index++)) {
				case 'n':
					buffer.append('\n');
					break;
				case 'r':
					buffer.append('\r');
					break;
				case 't':
					buffer.append('\t');
					break;
				case 'b':
					buffer.append('\b');
					break;
				case 'f':
					buffer.append('\f');
					break;
				case '"':
					buffer.append('"');
					break;
				case '\'':
					buffer.append('\'');
					break;
				case 'u':
					buffer.append((char) Integer.parseInt(input.substring(index, index + 4), 16));
					index += 4;
					break;
				default:
					buffer.append('\\');
			}
		}

	}

}

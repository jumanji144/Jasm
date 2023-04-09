package me.darknet.assembler.parser;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTCode;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.ast.specific.ASTDeclaration;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Parser for parsing generic structures from tokens to ast elements.
 * @see me.darknet.assembler.ast.primitive
 */
public class DeclarationParser {

	private ParserContext ctx;

	/**
	 * Parse all declarations from the given tokens, this will only try to parse declarations.
	 * Result for this method will always be a list of {@link ASTDeclaration} or null if parsing that element failed.
	 * @param tokens the tokens to parse
	 * @return {@link Result} of the parsing
	 */
	public Result<List<@Nullable ASTDeclaration>> parseDeclarations(Collection<Token> tokens) {
		this.ctx = new ParserContext(this, new ArrayDeque<>(tokens));
		List<ASTDeclaration> declarations = new ArrayList<>();
		while (!this.ctx.tokens.isEmpty()) {
			declarations.add(parseDeclaration(false));
		}
		return new Result<>(declarations, ctx.errorCollector.getErrors());
	}

	/**
	 * Parse any element from the given tokens, this will try to parse any element.
	 * Results for this method can only include objects from {@link me.darknet.assembler.ast.primitive} and
	 * {@link me.darknet.assembler.ast.specific.ASTDeclaration}
	 * @param tokens the tokens to parse
	 * @return {@link Result} of the parsing
	 */
	public Result<List<@Nullable ASTElement>> parseAny(Collection<Token> tokens) {
		this.ctx = new ParserContext(this, new ArrayDeque<>(tokens));
		List<ASTElement> result = new ArrayList<>();
		while (!ctx.tokens.isEmpty()) {
			ASTElement element = parse();
			result.add(element);
		}
		return new Result<>(result, ctx.errorCollector.getErrors());
	}

	private @Nullable ASTElement parse() {
		Token token = ctx.peek();

		switch (token.getType()) {
			case IDENTIFIER: {
				String content = token.getContent();
				if(content.charAt(0) == '.') {
					// begin of declaration
					return parseDeclaration(false);
				} else return new ASTIdentifier(ctx.takeAny());
			}
			case NUMBER: {
				return new ASTNumber(ctx.takeAny());
			}
			case STRING: {
				return new ASTString(ctx.takeAny());
			}
			case OPERATOR: {
				char operator = token.getContent().charAt(0);
				switch (operator) {
					case '[': {
						return parseArray();
					}
					case '{': {
						if(token.getContent().equals(".code")) {
							return parseCode();
						}
						Token next = ctx.peek(1);
						if(next == null) {
							ctx.throwEofError("identifier");
							return null;
						}
						if(next.getType().equals(TokenType.OPERATOR)) {
							if(next.getContent().equals("}")) { // empty object
								return parseObject();
							}
						}
						if(next.getType() != TokenType.IDENTIFIER) {
							ctx.takeAny();
							ctx.takeAny();
							ctx.throwExpectedError("identifier", next.getContent());
							return null;
						}
						if(next.getContent().startsWith(".")) {
							return parseNestedDeclaration();
						}
						return parseObject();
					}
					default: {
						ctx.takeAny();
						ctx.throwExpectedError("[, {", token.getContent());
					}
				}
			}
			default: {
				ctx.errorCollector.addError(new Error(
						"Unexpected token " + token.getContent(),
						token.getLocation()));
			}
		}
		return null;
	}

	private ASTArray parseArray() {
		if(ctx.take("[") == null) return null;
		List<ASTElement> elements = new ArrayList<>();
		Token peek = ctx.peek();
		while(!peek.getContent().equals("]")) {
			elements.add(parse());
			peek = ctx.peek();
			if(peek == null) {
				ctx.throwEofError(", or ]");
				return null;
			}
			if(!peek.getContent().equals("]")) {
				if(ctx.take(",") == null) return null;
			}
		}
		ctx.take("]");
		return new ASTArray(elements);
	}

	private ASTObject parseObject() {
		if(ctx.take("{") == null) return null;
		ElementMap<ASTIdentifier, ASTElement> elements = new ElementMap<>();
		Token peek = ctx.peek();
		while(!peek.getContent().equals("}")) {
			ASTIdentifier identifier = ctx.parseElement(ElementType.IDENTIFIER);
			if(identifier == null) return null;
			if(ctx.take(":") == null) return null;
			ASTElement element = parse();
			if(element == null) {
				ctx.throwExpectedError("element", peek.getContent());
				return null;
			}
			elements.put(identifier, element);
			peek = ctx.peek();
			if(peek == null) {
				ctx.throwEofError(", or }");
				return null;
			}
			if(!peek.getContent().equals("}")) {
				if(ctx.take(",") == null) return null;
			}
		}
		ctx.take("}");
		return new ASTObject(elements);
	}

	private ASTDeclaration parseDeclaration(boolean inNestedDeclaration) {
		ASTIdentifier identifier = new ASTIdentifier(ctx.takeAny());
		if(identifier.getContent().charAt(0) != '.') {
			ctx.throwExpectedError("identifier starting with '.'", identifier.getContent());
			return null;
		}
		Token peek = ctx.peek();
		List<ASTElement> elements = new ArrayList<>();
		while (!peek.getContent().startsWith(".") && !(inNestedDeclaration && peek.getContent().equals("}"))) {
			elements.add(parse());
			peek = ctx.peek();
			if(peek == null) break; // declarations are the top level elements, so we can just stop here
		}
		return new ASTDeclaration(identifier, elements);
	}

	private ASTDeclaration parseNestedDeclaration() {
		if(ctx.take("{") == null) return null;
		Token peek = ctx.peek();
		List<ASTElement> elements = new ArrayList<>();
		while(!peek.getContent().equals("}")) {
			elements.add(parseDeclaration(true));
			peek = ctx.peek();
			if(peek == null) {
				ctx.throwEofError("} or declaration");
				return null;
			}
		}
		ctx.take("}");
		return new ASTDeclaration(null, elements);
	}

	private ASTCode parseCode() {
		if(ctx.take(".code") == null) return null;
		if(ctx.take("{") == null) return null;
		return null;
	}

	private static class ParserContext {

		private final DeclarationParser parser;
		private final Queue<Token> tokens;
		private Token latest;
		private final ErrorCollector errorCollector = new ErrorCollector();

		private ParserContext(DeclarationParser parser, Queue<Token> tokens) {
			this.parser = parser;
			this.tokens = tokens;
			this.latest = tokens.peek();
		}

		private Token next() {
			latest = tokens.poll();
			return latest;
		}

		private Token peek() {
			return peek(0);
		}

		private Token peek(int offset) {
			if(offset < 0) {
				throw new IllegalArgumentException("Offset must be positive");
			}
			if(offset == 0) {
				return tokens.peek();
			}
			Iterator<Token> iterator = tokens.iterator();
			for(int i = 0; i < offset; i++) {
				if(!iterator.hasNext()) {
					return null;
				}
				iterator.next();
			}
			return iterator.next();
		}

		private Token take(String exact) {
			if(tokens.isEmpty()) {
				throwEofError(exact);
				return null;
			}
			Token token = next();
			if(token.getContent().equals(exact)) {
				return token;
			} else {
				throwExpectedError(exact, token.getContent());
				return null;
			}
		}

		private Token takeAny() {
			if(tokens.isEmpty()) {
				throwEofError("any token");
				return null;
			}
			Token token = next();
			return token;
		}

		@SuppressWarnings("unchecked")
		private <T extends ASTElement> T parseElement(ElementType... validTypes) {
			T element = (T) parser.parse();
			if(element == null) {
				return null;
			}
			if(validTypes.length == 0) {
				return element;
			}
			boolean valid = false;
			for (ElementType validType : validTypes) {
				if(element.getType() == validType) {
					valid = true;
					break;
				}
			}
			if(!valid) {
				errorCollector.addError(new Error(
						"Expected one of " + Arrays.toString(validTypes) + " but got " + element.getType(),
						element.getValue().getLocation()));
				return null;
			}
			return element;
		}

		public void throwEofError(String expected) {
			if(latest == null) {
				errorCollector.addError(new Error(
						"Expected '" + expected + "' but got EOF",
						new Location(-1, -1, "")));
				return;
			}
			errorCollector.addError(new Error(
					"Expected '" + expected + "' but got EOF",
					latest.getLocation()));
		}

		public void throwExpectedError(String expected, String got) {
			if(latest == null) {
				throwEofError(expected);
				return;
			}
			errorCollector.addError(new Error(
					"Expected '" + expected + "' but got '" + got + "'",
					latest.getLocation()));
		}

	}

}

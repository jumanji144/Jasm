package me.darknet.assembler.parser;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTComment;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Pair;
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
	 * @return {@link ParsingResult} of the parsing
	 */
	public ParsingResult<List<@Nullable ASTElement>> parseDeclarations(Collection<Token> tokens) {
		Pair<List<ASTComment>, Collection<Token>> filtered = filterComments(tokens);
		this.ctx = new ParserContext(this, new ArrayDeque<>(filtered.getSecond()));
		List<ASTElement> declarations = new ArrayList<>();
		while (!this.ctx.tokens.isEmpty()) {
			declarations.add(parseDeclaration());
		}
		return new ParsingResult<>(declarations, ctx.errorCollector.getErrors(), filtered.getFirst());
	}

	/**
	 * Parse any element from the given tokens, this will try to parse any element.
	 * Results for this method can only include objects from {@link me.darknet.assembler.ast.primitive} and
	 * {@link ASTDeclaration}
	 * @param tokens the tokens to parse
	 * @return {@link ParsingResult} of the parsing
	 */
	public ParsingResult<List<@Nullable ASTElement>> parseAny(Collection<Token> tokens) {
		Pair<List<ASTComment>, Collection<Token>> filtered = filterComments(tokens);
		this.ctx = new ParserContext(this, new ArrayDeque<>(filtered.getSecond()));
		List<ASTElement> result = new ArrayList<>();
		while (!ctx.tokens.isEmpty()) {
			ASTElement element = parse();
			result.add(element);
		}
		return new ParsingResult<>(result, ctx.errorCollector.getErrors(), filtered.getFirst());
	}

	private Pair<List<ASTComment>, Collection<Token>> filterComments(Collection<Token> tokens) {
		List<Token> filtered = new ArrayList<>();
		List<ASTComment> comments = new ArrayList<>();
		for (Token token : tokens) {
			if(token.getType().equals(TokenType.COMMENT)) {
				comments.add(new ASTComment(token));
			} else {
				filtered.add(token);
			}
		}
		return new Pair<>(comments, filtered);
	}

	private @Nullable ASTElement parse() {
		Token token = ctx.peek();

		switch (token.getType()) {
			case IDENTIFIER: {
				String content = token.getContent();
				if(content.charAt(0) == '.') {
					// begin of declaration
					return parseDeclaration();
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
		ctx.enterState(State.IN_ARRAY);
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
		if(ctx.take("]") == null) return null;
		ctx.leaveState(State.IN_ARRAY);
		return new ASTArray(elements);
	}

	private ASTObject parseObject() {
		ctx.enterState(State.IN_OBJECT);
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
		if(ctx.take("}") == null) return null;
		ctx.leaveState(State.IN_OBJECT);
		return new ASTObject(elements);
	}

	private ASTDeclaration parseDeclaration() {
		ASTIdentifier identifier = new ASTIdentifier(ctx.takeAny());
		if(identifier.getContent().charAt(0) != '.') {
			ctx.throwExpectedError("identifier starting with '.'", identifier.getContent());
			return null;
		}
		State state = ctx.getState();
		Token peek = ctx.peek();
		List<ASTElement> elements = new ArrayList<>();
		while (!peek.getContent().startsWith(".")) {
			elements.add(parse());
			peek = ctx.peek();
			if(peek == null) break; // declarations are the top level elements, so we can just stop here
			if(state == State.IN_NESTED_DECLARATION && peek.getContent().equals("}")) {
				break;
			}
			if(state == State.IN_OBJECT) {
				// detection is a bit hacky, but it works
				// check if over next token is a : or next token is a }
				if(peek.getContent().equals("}")) {
					break;
				}
				Token next = ctx.peek(1);
				if(next == null) {
					ctx.throwEofError("end of declaration");
					return null;
				}
				if(next.getContent().equals(":")) {
					break;
				}
			}
		}
		return new ASTDeclaration(identifier, elements);
	}

	private ASTDeclaration parseNestedDeclaration() {
		ctx.enterState(State.IN_NESTED_DECLARATION);
		if(ctx.take("{") == null) return null;
		Token peek = ctx.peek();
		List<ASTElement> elements = new ArrayList<>();
		while(!peek.getContent().equals("}")) {
			elements.add(parseDeclaration());
			peek = ctx.peek();
			if(peek == null) {
				ctx.throwEofError("} or declaration");
				return null;
			}
		}
		if(ctx.take("}") == null) return null;
		ctx.leaveState();
		return new ASTDeclaration(null, elements);
	}

	private ASTCode parseCode() {
		ctx.enterState(State.IN_CODE);
		if(ctx.take(".code") == null) return null;
		if(ctx.take("{") == null) return null;
		Token peek = ctx.peek();
		List<ASTInstruction> instructions = new ArrayList<>();
		while(!peek.getContent().equals("}")) {
			ASTInstruction instruction = parseInstruction();
			if(instruction == null) return null;
			instructions.add(instruction);
			peek = ctx.peek();
			if(peek == null) {
				ctx.throwEofError("} or instruction");
				return null;
			}
		}
		if(ctx.take("}") == null) return null;
		ctx.leaveState();
		return new ASTCode(instructions);
	}

	private ASTInstruction parseInstruction() {
		ctx.enterState(State.IN_INSTRUCTION);
		Token instruction = ctx.takeAny();
		if(instruction == null) return null;
		if(instruction.getType() != TokenType.IDENTIFIER) {
			ctx.throwExpectedError("instruction", instruction.getContent());
			return null;
		}
		ASTIdentifier identifier = new ASTIdentifier(instruction);
		List<ASTElement> arguments = new ArrayList<>();
		// parse until peek is eof or on a different line
		Token peek = ctx.peek();
		while(peek.getLocation().getLine() == instruction.getLocation().getLine()) {
			arguments.add(parse());
			peek = ctx.peek();
			if(peek == null) {
				ctx.throwEofError("instruction argument");
				return null;
			}
		}
		ctx.leaveState();
		return new ASTInstruction(identifier, arguments);
	}

	private enum State {
		DEFAULT,
		IN_DECLARATION,
		IN_NESTED_DECLARATION,
		IN_OBJECT,
		IN_ARRAY,
		IN_CODE,
		IN_INSTRUCTION
	}

	private static class ParserContext {

		private final DeclarationParser parser;
		private final Queue<Token> tokens;
		private Token latest;
		private final ErrorCollector errorCollector = new ErrorCollector();
		private State state = State.DEFAULT;

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
			return next();
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

		public void enterState(State state) {
			this.state = state;
		}

		public void leaveState() {
			this.state = State.DEFAULT;
		}

		public <T> T leaveState(T value) {
			this.state = State.DEFAULT;
			return value;
		}

		public State getState() {
			return state;
		}

		public boolean isInState(State state) {
			return this.state == state;
		}

		public void throwError(Error error) {
			errorCollector.addError(error);
		}

		public void throwEofError(String expected) {
			if(latest == null) {
				throwError(new Error(
						"Expected '" + expected + "' but got EOF",
						new Location(-1, -1, "")));
				return;
			}
			throwError(new Error(
					"Expected '" + expected + "' but got EOF",
					latest.getLocation()));
		}

		public void throwExpectedError(String expected, String got) {
			if(latest == null) {
				throwEofError(expected);
				return;
			}
			throwError(new Error(
					"Expected '" + expected + "' but got '" + got + "'",
					latest.getLocation()));
		}

	}

}

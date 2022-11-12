package me.darknet.assembler.parser;

import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.*;

public class ParserContext {
	private final Map<String, List<Group>> macros = new HashMap<>();
	private final Queue<Token> tokens;
	private List<Group> groups;
	private Token currentToken;
	public Parser parser;

	public ParserContext(Queue<Token> tokens, Parser parser) {
		this.tokens = tokens;
		this.parser = parser;
	}

	/**
	 * @return Parsed tokens, also accessible via {@link #getGroups()}.
	 *
	 * @throws AssemblerException
	 * 		When an illegal ordering of tokens occurs.
	 */
	public List<Group> parse() throws AssemblerException {
		if (groups == null) {
			groups = new ArrayList<>();
			while (hasNextToken()) {
				Group group = parseNext();
				if (group != null) {
					groups.add(group);
				}
			}
			pass();
		}
		return groups;
	}

	/**
	 * @return Next group.
	 *
	 * @throws AssemblerException
	 * 		When an illegal ordering of tokens occurs.
	 */
	public Group parseNext() throws AssemblerException {
		return parser.group(this);
	}

	/**
	 * Does the final pass over the groups to finalize them and move extends and implements to the correct place
	 */
	public void pass() {
		// no-op by default
	}

	/**
	 * Parses next group and checks if it is {@param type} if not throws exception
	 *
	 * @param type
	 * 		type of group
	 *
	 * @return next group
	 */
	public Group nextGroup(Group.GroupType type) throws AssemblerException {
		Group group = parseNext();
		if (group == null) {
			throw new AssemblerException("Unexpected end of file", currentToken.getLocation());
		}
		if (!group.isType(type)) {
			throw new AssemblerException("Expected " + type.name() + " but got " + group.getType().name(), group.getStartLocation());
		}
		return group;
	}

	/**
	 * @return Next token from queue.
	 */
	public Token nextToken() {
		currentToken = tokens.poll();
		return currentToken;
	}

	/**
	 * @return {@code true} when there are tokens remaining, {@code false} for EOF.
	 */
	public boolean hasNextToken() {
		return !tokens.isEmpty();
	}

	/**
	 * Adds the given group to {@link #getGroups() the output}.
	 *
	 * @param group
	 * 		Group to add.
	 */
	public void pushGroup(Group group) {
		groups.add(group);
	}

	/**
	 * Reads the next token and directly interprets it as an identifier group.
	 *
	 * @return Identifier group
	 */
	public IdentifierGroup explicitIdentifier() throws AssemblerException {
		if (!hasNextToken()) {
			throw new AssemblerException("Expected identifier", currentToken.getLocation());
		}
		// do still macro expansion
		Token token = nextToken();
		if (macros.containsKey(token.getContent())) {
			// dirty hack, interpret first token of macro
			List<Group> macro = getMacro(token.getContent());
			return new IdentifierGroup(macro.get(0).getValue());
		}
		return new IdentifierGroup(token);
	}

	/**
	 * @return Prior parsed group.
	 *
	 * @throws AssemblerException
	 * 		When there were no previous groups
	 */
	public Group previousGroup() throws AssemblerException {
		if (groups.isEmpty())
			throw new AssemblerException("No previous group", currentToken.getLocation());
		return groups.get(groups.size() - 1);
	}

	/**
	 * @return Next token.
	 *
	 * @throws AssemblerException
	 * 		When the end of file is reached.
	 * @see #peekTokenSilent() Without exception, instead yielding EOF token.
	 */
	public Token peekToken() throws AssemblerException {
		if (!hasNextToken()) {
			throw new AssemblerException("Unexpected end of file", currentToken.getLocation());
		}
		return tokens.peek();
	}

	/**
	 * Silently peek a token and instead of throwing an error just return EOF
	 *
	 * @return Next token, or EOF
	 */
	public Token peekTokenSilent() {
		if (!hasNextToken()) {
			return new Token("EOF", currentToken.getLocation(), Token.TokenType.EOF);
		}
		return tokens.peek();
	}

	/**
	 * @return Parsed groups, or {@code null} if {@link #parse()} has not yet been called.
	 */
	public List<Group> getGroups() {
		return groups;
	}

	/**
	 * @param id
	 * 		Macro key.
	 *
	 * @return {@code true} when macro exists, {@code false} otherwise.
	 */
	public boolean hasMacro(String id) {
		return macros.containsKey(id);
	}

	/**
	 * @param id
	 * 		Macro key.
	 *
	 * @return Contents of macro.
	 */
	public List<Group> getMacro(String id) {
		return macros.get(id);
	}

	/**
	 * @param id
	 * 		Macro key.
	 * @param contents
	 * 		Contents of macro.
	 */
	public void putMacro(String id, List<Group> contents) {
		macros.put(id, contents);
	}

	/**
	 * @return Current token.
	 */
	public Token getCurrentToken() {
		return currentToken;
	}

	/**
	 * @return Current location of current token.
	 */
	public Location getCurrentLocation() {
		return getCurrentToken().getLocation();
	}
}

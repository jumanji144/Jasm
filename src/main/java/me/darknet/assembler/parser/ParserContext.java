package me.darknet.assembler.parser;

import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.*;

public class ParserContext {

    public List<Group> groups;
    public Queue<Token> tokens;
    public Token currentToken;
    public Parser parser;
    public Map<String, Group[]> macros = new HashMap<>();

    public ParserContext(Queue<Token> tokens, Parser parser) {
        this.groups = new ArrayList<>();
        this.tokens = tokens;
        this.parser = parser;
    }

    /**
     * Parses next group and checks if it is {@param type} if not throws exception
     * @param type type of group
     * @return next group
     */
    public Group nextGroup(Group.GroupType type) throws AssemblerException {
        Group group = parseNext();
        if (group == null) {
            throw new AssemblerException("Unexpected end of file", currentToken.getLocation());
        }
        if (group.type != type) {
            throw new AssemblerException("Expected " + type.name() + " but got " + group.type.name(), group.start().getLocation());
        }
        return group;
    }

    public Group parseNext() throws AssemblerException {
        return parser.group(this);
    }

    public Token nextToken() {
        currentToken = tokens.poll();
        return currentToken;
    }

    public boolean hasNextToken() {
        return !tokens.isEmpty();
    }

    public void pushGroup(Group group) {
        groups.add(group);
    }

    public IdentifierGroup explicitIdentifier() {
        return new IdentifierGroup(nextToken());
    }

    public void pushGroup(Group.GroupType type, Token val, Group... children) {
        groups.add(new Group(type, val, children));
    }

    public void pushGroup(Group.GroupType type, Group... children) {
        pushGroup(type, currentToken, children);
    }

    public Group previousGroup() {
        return groups.get(groups.size() - 1);
    }

    public Token peekToken() {
        return tokens.peek();
    }

    /**
     * Does the final pass over the groups to finalize them and move extends and implements to the correct place
     */
    public void pass() {

    }

    public Collection<Group> parse() throws AssemblerException {
        while (hasNextToken()) {
            Group group = parseNext();
            if (group != null) {
                groups.add(group);
            }
        }
        pass();
        return groups;
    }
}

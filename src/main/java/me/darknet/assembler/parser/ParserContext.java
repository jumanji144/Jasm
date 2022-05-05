package me.darknet.assembler.parser;

import java.util.*;

public class ParserContext {

    public Queue<Group> groups;
    public Queue<Token> tokens;
    public Token currentToken;
    public Parser parser;

    public ParserContext(Queue<Token> tokens, Parser parser) {
        this.groups = new LinkedList<>();
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

    public void pushGroup(Group.GroupType type, Token val, Group... children) {
        groups.add(new Group(type, val, children));
    }

    public void pushGroup(Group.GroupType type, Group... children) {
        pushGroup(type, currentToken, children);
    }

    public Group previousGroup() {
        return groups.peek();
    }

    public Token peekToken() {
        return tokens.peek();
    }

    /**
     * Does the final pass over the groups to finalize them and move extends and implements to the correct place
     */
    public void pass() {
        // go over all groups and add the extends and implements to the class

        // HACK: this is a hack to get the extends and implements to the class,
        //       but it allows the parser to be a bit more flexible

        Group classGroup = (Group) groups.toArray()[0];
        Group extendsGroup = null;
        List<Group> implementsGroups = new ArrayList<>();
        for (Group group : groups) {
            if (group.type == Group.GroupType.EXTENDS_DIRECTIVE) {
                extendsGroup = group;
            }
            if (group.type == Group.GroupType.IMPLEMENTS_DIRECTIVE) {
                implementsGroups.add(group);
            }
        }
        List<Group> newChildren = new ArrayList<>();
        newChildren.add(classGroup.children[0]);
        newChildren.add(classGroup.children[1]);
        if (extendsGroup != null) {
            newChildren.add(extendsGroup);
        }
        newChildren.addAll(implementsGroups);

        classGroup.children = newChildren.toArray(new Group[0]);

        // remove the extends and implements from the groups
        groups.remove(extendsGroup);
        groups.removeAll(implementsGroups);

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

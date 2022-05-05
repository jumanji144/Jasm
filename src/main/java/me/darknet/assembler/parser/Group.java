package me.darknet.assembler.parser;

import lombok.Getter;

/**
 * A group of tokens representing a logical expression.
 */
@Getter
public class Group {


    public final GroupType type;
    public final Token value;
    public Group[] children;

    public Group(GroupType type, Token value, Group... children) {
        this.type = type;
        this.value = value;
        this.children = children;
    }

    public Group(GroupType type, Group... children) {
        this(type, null, children);
    }

    public Token start() {
        if(children.length == 0) {
            return value;
        }
        return children[0].start();
    }
    public Token end() {
        return children[children.length - 1].end();
    }

    public String content() {
        return value.content;
    }

    public String toString() {
        return "Group(" + type + "," + value + "," + children.length + ")";
    }

    public Group get(int index) {
        return children[index];
    }

    public int size() {
        return children.length;
    }

    public Group getChild(GroupType type) {
        for(Group child : children) {
            if(child.type == type) {
                return child;
            }
        }
        return null;
    }

    public enum GroupType {

        // Pass through of token types to allow for easy extension.
        NUMBER,
        STRING,
        IDENTIFIER,
        KEYWORD,
        ACCESS_MOD,
        ACCESS_MODS,
        LABEL,

        LOOKUP_SWITCH,
        CASE_LABEL,
        DEFAULT_LABEL,

        TABLE_SWITCH,
        MACRO_DIRECTIVE,

        // higher order abstraction
        CLASS_DECLARATION,
        METHOD_DECLARATION,
        FIELD_DECLARATION,

        EXTENDS_DIRECTIVE,
        IMPLEMENTS_DIRECTIVE,
        BODY,
        END_BODY,
        INSTRUCTION,
        STACK_LIMIT,
        LOCAL_LIMIT,
        RETURN,

    }
}


package me.darknet.assembler.parser;

import lombok.Data;

@Data
public class Token {

    public final String content;
    public final Location location;
    public final TokenType type;

    public enum TokenType {
        EOF,
        STRING,
        NUMBER,
        IDENTIFIER,
        KEYWORD;

        public Group.GroupType toGroupType() {
            switch (this) {
                case STRING:
                    return Group.GroupType.STRING;
                case NUMBER:
                    return Group.GroupType.NUMBER;
                case IDENTIFIER:
                    return Group.GroupType.IDENTIFIER;
                case KEYWORD:
                    return Group.GroupType.KEYWORD;
                default:
                    return null;
            }
        }
    }

}

package me.darknet.assembler.parser;

import me.darknet.assembler.instructions.ParseInfo;

import java.util.*;

import static me.darknet.assembler.parser.Token.TokenType.*;
import static me.darknet.assembler.parser.Token.TokenType;
import static me.darknet.assembler.parser.Group.GroupType;

public class Parser {

    public static final String KEYWORD_CLASS = "class";
    public static final String KEYWORD_METHOD = "method";
    public static final String KEYWORD_END = "end";
    public static final String KEYWORD_FIELD = "field";
    public static final String KEYWORD_STATIC = "static";
    public static final String KEYWORD_PUBLIC = "public";
    public static final String KEYWORD_PRIVATE = "private";
    public static final String KEYWORD_EXTENDS = "extends";
    public static final String KEYWORD_IMPLEMENTS = "implements";
    public static final String KEYWORD_FINAL = "final";
    public static final String KEYWORD_STACK = "stack";
    public static final String KEYWORD_LOCALS = "locals";
    public static final String KEYWORD_SWITCH = "lookupswitch";
    public static final String KEYWORD_TABLESWITCH = "tableswitch";
    public static final String KEYWORD_DEFAULT = "default";
    public static final String KEYWORD_CASE = "case";
    private static final String[] keywords = {
            KEYWORD_CLASS,
            KEYWORD_METHOD,
            KEYWORD_END,
            KEYWORD_FIELD,
            KEYWORD_STATIC,
            KEYWORD_PUBLIC,
            KEYWORD_PRIVATE,
            KEYWORD_EXTENDS,
            KEYWORD_IMPLEMENTS,
            KEYWORD_FINAL,
            KEYWORD_STACK,
            KEYWORD_LOCALS,
            KEYWORD_SWITCH,
            KEYWORD_TABLESWITCH,
            KEYWORD_DEFAULT,
            KEYWORD_CASE
    };

    public List<Token> tokenize(String source, String code) {

        List<Token> tokens = new ArrayList<>();
        Location location = new Location(1, 1, source);
        char[] chars = code.toCharArray();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < chars.length) {
            char c = chars[i];
            if(c == '\r') {
                i++;
                continue;
            }
            if(c == '#') {
                while(i < chars.length) {
                    if(chars[i] == '\n') {
                        i++;
                        location.line++;
                        break;
                    }
                    i++;
                    location.column++;
                }
                continue;
            }
            if(c == '"'){
                c = chars[++i];
                location.column++;
                sb.append(c);
                while(c != '"'){
                    c = chars[++i];
                    location.column++;
                    if(c == '\n'){
                        location.line++;
                    }
                    if(c == '"'){
                        i++;
                        location.column++;
                        break;
                    }
                    sb.append(c);
                }
                tokens.add(new Token(sb.toString(), location.sub(sb.length()), STRING));
                sb = new StringBuilder();
                continue;
            }
            if (c == ' ' || c == '\n' || c == '\t') {
                // flush the string builder
                if (sb.length() > 0) {
                    // determine the type of token
                    TokenType type = IDENTIFIER;
                    String token = sb.toString();
                    // check if all the characters in the token are digits (and the '-' sign)
                    boolean isNumber = true;
                    for (int j = 0; j < token.length(); j++) {
                        char c2 = token.charAt(j);
                        if (c2 == '-' && j == 0) {
                            continue;
                        }
                        if (c2 < '0' || c2 > '9') {
                            isNumber = false;
                            break;
                        }
                    }
                    if (isNumber) {
                        type = NUMBER;
                    } else {
                        // check if the token is a keyword
                        for (String keyword : keywords) {
                            if (keyword.equals(token)) {
                                type = KEYWORD;
                                break;
                            }
                        }
                    }

                    tokens.add(new Token(sb.toString(), location.sub(sb.length()), type));
                    sb = new StringBuilder();
                }
                i++;
                location.column++;
                if(c == '\n'){
                    location.line++;
                    location.column = 0;
                }
                continue;
            }
            sb.append(c);
            i++;
            location.column++;
        }

        if (sb.length() > 0) {
            // determine the type of token
            TokenType type = IDENTIFIER;
            String token = sb.toString();
            // check if all the characters in the token are digits (and the '-' sign)
            boolean isNumber = true;
            for (int j = 0; j < token.length(); j++) {
                char c2 = token.charAt(j);
                if (c2 == '-' && j == 0) {
                    continue;
                }
                if (c2 < '0' || c2 > '9') {
                    isNumber = false;
                }
            }
            if (isNumber) {
                type = NUMBER;
            } else {
                // check if the token is a keyword
                for (String keyword : keywords) {
                    if (keyword.equals(token)) {
                        type = KEYWORD;
                        break;
                    }
                }
            }

            tokens.add(new Token(sb.toString(), location.sub(sb.length()), type));
        }

        return tokens;

    }

    public Group group(ParserContext ctx) throws AssemblerException {

        Token token = ctx.nextToken();

        if(token.type != KEYWORD) {
            String content = token.content;
            if(ParseInfo.actions.containsKey(content)){
                ParseInfo info = ParseInfo.actions.get(content);
                int argc = info.args.length;
                List<Group> children = new ArrayList<>();
                for(int i = 0; i < argc; i++){
                    Group group = group(ctx);
                    GroupType type = group.type;
                    if(type != GroupType.IDENTIFIER && type != GroupType.STRING && type != GroupType.NUMBER){
                        throw new AssemblerException("Unexpected expression: " + group.start().content, group.start().getLocation());
                    }
                    children.add(group);
                }
                return new Group(GroupType.INSTRUCTION, token, children.toArray(new Group[0]));
            } else {
                if(content.endsWith(":")){
                    return new Group(GroupType.LABEL, token);
                }
            }
            return new Group(token.type.toGroupType(), token);
        }

        switch(token.content){
            case KEYWORD_CLASS: {
                Group access = readAccess(ctx);
                Group name = ctx.nextGroup(GroupType.IDENTIFIER);
                return new Group(GroupType.CLASS_DECLARATION, token, access, name);
            }
            case KEYWORD_EXTENDS: {
                // also ensure that the previous group is a class declaration or an implements directive
                Group previous = ctx.previousGroup();
                if(previous.type != GroupType.CLASS_DECLARATION
                        && previous.type != GroupType.IMPLEMENTS_DIRECTIVE) {
                    throw new AssemblerException("Extends can only follow a class declaration", token.location);
                }
                Group cls = ctx.nextGroup(GroupType.IDENTIFIER);
                return new Group(GroupType.EXTENDS_DIRECTIVE, token, cls);
            }
            case KEYWORD_IMPLEMENTS: {
                // same with implements
                Group previous = ctx.previousGroup();
                if(previous.type != GroupType.CLASS_DECLARATION
                        && previous.type != GroupType.IMPLEMENTS_DIRECTIVE
                        && previous.type != GroupType.EXTENDS_DIRECTIVE) {
                    throw new AssemblerException("Implements can only follow a class declaration", token.location);
                }
                Group cls = ctx.nextGroup(GroupType.IDENTIFIER);
                return new Group(GroupType.IMPLEMENTS_DIRECTIVE, token, cls);
            }
            case KEYWORD_FIELD: {
                // maybe read access modifiers
                Group access = readAccess(ctx);
                Group name = ctx.nextGroup(GroupType.IDENTIFIER);
                Group descriptor = ctx.nextGroup(GroupType.IDENTIFIER);

                return new Group(GroupType.FIELD_DECLARATION, token, access, name, descriptor);
            }
            case KEYWORD_METHOD: {
                // maybe read access modifiers
                Group access = readAccess(ctx);
                Group methodDescriptor = ctx.nextGroup(GroupType.IDENTIFIER);
                return new Group(GroupType.METHOD_DECLARATION, token, access, methodDescriptor, readBody(ctx));
            }
            case KEYWORD_END: {
                return new Group(GroupType.END_BODY, token);
            }
            case KEYWORD_SWITCH: {
                List<Group> labels = readLookupSwitch(ctx);
                return new Group(GroupType.LOOKUP_SWITCH, token, labels.toArray(new Group[0]));
            }
            case KEYWORD_TABLESWITCH: {
                List<Group> labels = readTableSwitch(ctx);
                return new Group(GroupType.TABLE_SWITCH, token, labels.toArray(new Group[0]));
            }
            case KEYWORD_CASE: {
                Group value = ctx.nextGroup(GroupType.NUMBER);
                Group label = ctx.nextGroup(GroupType.IDENTIFIER);
                return new Group(GroupType.CASE_LABEL, token, value, label);
            }
            case KEYWORD_DEFAULT: {
                Group label = ctx.nextGroup(GroupType.IDENTIFIER);
                return new Group(GroupType.DEFAULT_LABEL, token, label);
            }

            case KEYWORD_PUBLIC:
            case KEYWORD_PRIVATE:
            case KEYWORD_STATIC:
                return new Group(GroupType.ACCESS_MOD, token);

        }

        return new Group(GroupType.IDENTIFIER, token);
    }

    public Group readAccess(ParserContext ctx) throws AssemblerException {
        List<Group> access = new ArrayList<>();
        while(ctx.peekToken().type == KEYWORD){
            access.add(ctx.nextGroup(GroupType.ACCESS_MOD));
        }
        return new Group(GroupType.ACCESS_MODS, access.toArray(new Group[0]));
    }

    public Group readBody(ParserContext ctx) throws AssemblerException {
        List<Group> body = new ArrayList<>();
        while(ctx.hasNextToken()){
            Group grp = ctx.parseNext();
            if(grp.type == GroupType.END_BODY){
                return new Group(GroupType.BODY, body.toArray(new Group[0]));
            }
            body.add(grp);
        }
        throw new AssemblerException("Unexpected end of file", ctx.currentToken.getLocation());
    }

    public List<Group> readLookupSwitch(ParserContext ctx) throws AssemblerException {
        List<Group> caseLabels = new ArrayList<>();
        while(ctx.hasNextToken()){
            Group grp = ctx.parseNext();
            if(grp.type == GroupType.DEFAULT_LABEL){
                caseLabels.add(grp);
                return caseLabels;
            }
            if(grp.type != GroupType.CASE_LABEL){
                throw new AssemblerException("Expected case label", grp.start().location);
            }
            caseLabels.add(grp);
        }
        throw new AssemblerException("Unexpected end of file", ctx.currentToken.getLocation());
    }

    private List<Group> readTableSwitch(ParserContext ctx) throws AssemblerException {
        List<Group> caseLabels = new ArrayList<>();
        while(ctx.hasNextToken()){
            Group grp = ctx.parseNext();
            if(grp.type == GroupType.DEFAULT_LABEL){
                caseLabels.add(grp);
                return caseLabels;
            }
            if(grp.type != GroupType.IDENTIFIER){
                throw new AssemblerException("Expected case label", grp.start().location);
            }
            caseLabels.add(grp);
        }
        throw new AssemblerException("Unexpected end of file", ctx.currentToken.getLocation());
    }

}

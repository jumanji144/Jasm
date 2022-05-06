package me.darknet.assembler.parser;

import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.groups.*;

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
    public static final String KEYWORD_MACRO = "macro";
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
            KEYWORD_CASE,
            KEYWORD_MACRO
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
                return new InstructionGroup(token, children.toArray(new Group[0]));
            } else {
                if(content.endsWith(":")){
                    return new LabelGroup(token);
                }
            }
            if(ctx.macros.containsKey(content)){
                Group[] groups = ctx.macros.get(content);
                for (int i = 0; i < groups.length - 1; i++) {
                    ctx.pushGroup(groups[i]);
                }
                return groups[groups.length - 1];
            }
            return new IdentifierGroup(token);
        }

        switch(token.content){
            case KEYWORD_CLASS: {
                AccessModsGroup access = readAccess(ctx);
                IdentifierGroup name = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
                return new ClassDeclarationGroup(token, access, name);
            }
            case KEYWORD_EXTENDS: {
                // also ensure that the previous group is a class declaration or an implements directive
                Group previous = ctx.previousGroup();
                if(previous.type != GroupType.CLASS_DECLARATION
                        && previous.type != GroupType.IMPLEMENTS_DIRECTIVE) {
                    throw new AssemblerException("Extends can only follow a class declaration", token.location);
                }
                IdentifierGroup group = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
                return new ExtendsGroup(token, group);
            }
            case KEYWORD_IMPLEMENTS: {
                // same with implements
                Group previous = ctx.previousGroup();
                if(previous.type != GroupType.CLASS_DECLARATION
                        && previous.type != GroupType.IMPLEMENTS_DIRECTIVE
                        && previous.type != GroupType.EXTENDS_DIRECTIVE) {
                    throw new AssemblerException("Implements can only follow a class declaration", token.location);
                }
                IdentifierGroup group = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
                return new ImplementsGroup(token, group);
            }
            case KEYWORD_FIELD: {
                // maybe read access modifiers
                AccessModsGroup access = readAccess(ctx);
                IdentifierGroup name = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
                IdentifierGroup descriptor = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);

                return new FieldDeclarationGroup(token, access, name, descriptor);
            }
            case KEYWORD_METHOD: {
                // maybe read access modifiers
                AccessModsGroup access = readAccess(ctx);
                IdentifierGroup methodDescriptor = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
                return new MethodDeclarationGroup(token, access, methodDescriptor, readBody(ctx));
            }
            case KEYWORD_END: {
                return new Group(GroupType.END_BODY, token);
            }
            case KEYWORD_SWITCH: return readLookupSwitch(token, ctx);
            case KEYWORD_TABLESWITCH:  return readTableSwitch(token, ctx);
            case KEYWORD_CASE: {
                NumberGroup value = (NumberGroup) ctx.nextGroup(GroupType.NUMBER);
                IdentifierGroup label = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
                return new CaseLabelGroup(token, value, label);
            }
            case KEYWORD_DEFAULT: {
                IdentifierGroup label = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
                return new DefaultLabelGroup(token, new LabelGroup(label.value));
            }
            case KEYWORD_MACRO: {
                Group macroName = ctx.nextGroup(GroupType.IDENTIFIER);
                Group[] children = readBody(ctx).children;
                ctx.macros.put(macroName.content(), children);
                return new Group(GroupType.MACRO_DIRECTIVE, token, children);
            }

            case KEYWORD_PUBLIC:
            case KEYWORD_PRIVATE:
            case KEYWORD_STATIC:
                return new AccessModGroup(token);

        }

        return new Group(GroupType.IDENTIFIER, token);
    }

    public AccessModsGroup readAccess(ParserContext ctx) throws AssemblerException {
        List<AccessModGroup> access = new ArrayList<>();
        while(ctx.peekToken().type == KEYWORD){
            access.add((AccessModGroup) ctx.nextGroup(GroupType.ACCESS_MOD));
        }
        return new AccessModsGroup(access.toArray(new AccessModGroup[0]));
    }

    public BodyGroup readBody(ParserContext ctx) throws AssemblerException {
        List<Group> body = new ArrayList<>();
        while(ctx.hasNextToken()){
            Group grp = ctx.parseNext();
            if(grp.type == GroupType.END_BODY){
                return new BodyGroup(body.toArray(new Group[0]));
            }
            body.add(grp);
        }
        throw new AssemblerException("Unexpected end of file", ctx.currentToken.getLocation());
    }

    public LookupSwitchGroup readLookupSwitch(Token begin, ParserContext ctx) throws AssemblerException {
        List<CaseLabelGroup> caseLabels = new ArrayList<>();
        while(ctx.hasNextToken()){
            Group grp = ctx.parseNext();
            if(grp.type == GroupType.DEFAULT_LABEL){
                return new LookupSwitchGroup(begin, (DefaultLabelGroup) grp, caseLabels.toArray(new CaseLabelGroup[0]));
            }
            if(grp.type != GroupType.CASE_LABEL){
                throw new AssemblerException("Expected case label", grp.start().location);
            }
            caseLabels.add((CaseLabelGroup) grp);
        }
        throw new AssemblerException("Unexpected end of file", ctx.currentToken.getLocation());
    }

    private TableSwitchGroup readTableSwitch(Token begin, ParserContext ctx) throws AssemblerException {
        List<LabelGroup> caseLabels = new ArrayList<>();
        while(ctx.hasNextToken()){
            Group grp = ctx.parseNext();
            if(grp.type == GroupType.DEFAULT_LABEL){
                return new TableSwitchGroup(begin, (DefaultLabelGroup) grp, caseLabels.toArray(new LabelGroup[0]));
            }
            if(grp.type != GroupType.IDENTIFIER){
                throw new AssemblerException("Expected case label", grp.start().location);
            }
            caseLabels.add(new LabelGroup(grp.value));
        }
        throw new AssemblerException("Unexpected end of file", ctx.currentToken.getLocation());
    }

}

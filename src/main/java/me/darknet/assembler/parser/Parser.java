package me.darknet.assembler.parser;

import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.groups.*;

import java.lang.annotation.Target;
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
    public static final String KEYWORD_PROTECTED = "protected";
    public static final String KEYWORD_SYNCHRONIZED = "synchronized";
    public static final String KEYWORD_VOLATILE = "volatile";
    public static final String KEYWORD_TRANSIENT = "transient";
    public static final String KEYWORD_NATIVE = "native";
    public static final String KEYWORD_ABSTRACT = "abstract";
    public static final String KEYWORD_STRICT = "strictfp";
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
    public static final String KEYWORD_CATCH = "catch";
    public static final String KEYWORD_HANDLE = ".handle";
    public static final String KEYWORD_ARGS = "args";
    public static final String KEYWORD_TYPE = ".type";
    public static final String KEYWORD_ANNOTATION = "annotation";
    public static final String KEYWORD_INVISIBLE_ANNOTATION = "invisible-annotation";
    public static final String KEYWORD_PARAMETER_ANNOTATION = "parameter-annotation";
    public static final String KEYWORD_INVISIBLE_PARAMETER_ANNOTATION = "invisible-parameter-annotation";
    public static final String KEYWORD_TYPE_ANNOTATION = "type-annotation";
    public static final String KEYWORD_INVISIBLE_TYPE_ANNOTATION = "invisible-type-annotation";
    public static final String KEYWORD_ENUM = "enum";
    public static final String KEYWORD_SIGNATURE = "signature";
    private static final String[] keywords = {
            KEYWORD_CLASS,
            KEYWORD_METHOD,
            KEYWORD_END,
            KEYWORD_FIELD,
            KEYWORD_STATIC,
            KEYWORD_PUBLIC,
            KEYWORD_PRIVATE,
            KEYWORD_PROTECTED,
            KEYWORD_EXTENDS,
            KEYWORD_IMPLEMENTS,
            KEYWORD_FINAL,
            KEYWORD_STACK,
            KEYWORD_LOCALS,
            KEYWORD_SWITCH,
            KEYWORD_TABLESWITCH,
            KEYWORD_DEFAULT,
            KEYWORD_CASE,
            KEYWORD_MACRO,
            KEYWORD_CATCH,
            KEYWORD_HANDLE,
            KEYWORD_ARGS,
            KEYWORD_TYPE,
            KEYWORD_ANNOTATION,
            KEYWORD_INVISIBLE_ANNOTATION,
            KEYWORD_TRANSIENT,
            KEYWORD_VOLATILE,
            KEYWORD_STRICT,
            KEYWORD_NATIVE,
            KEYWORD_ABSTRACT,
            KEYWORD_SYNCHRONIZED,
            KEYWORD_ENUM,
            KEYWORD_PARAMETER_ANNOTATION,
            KEYWORD_INVISIBLE_PARAMETER_ANNOTATION,
            KEYWORD_TYPE_ANNOTATION,
            KEYWORD_INVISIBLE_TYPE_ANNOTATION,
            KEYWORD_SIGNATURE
    };

    public static final List<String> accessModifiers = Arrays.asList(
            "public",
            "private",
            "protected",
            "static",
            "final",
            "synchronized",
            "native",
            "abstract",
            "strictfp"
    );

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

                // invokedynamic is a special case
                if(content.equals("invokedynamic")){
                    return readInvokeDynamic(token, ctx);
                }

                ParseInfo info = ParseInfo.actions.get(content);
                int argc = info.args.length;
                List<Group> children = new ArrayList<>();
                for(int i = 0; i < argc; i++){
                    Token peek = ctx.peekToken();
                    // this needed for illegal class names to ensure that the correct tokens are parsed here
                    // also the use of ctx.explicitIdentifier is needed to ensure that nothing illegal happens
                    if(peek.type == KEYWORD) {
                        if(peek.content.equals(KEYWORD_HANDLE) || peek.content.equals(KEYWORD_TYPE))
                            children.add(ctx.parseNext());
                        else {
                            children.add(ctx.explicitIdentifier());
                        }
                    }else if(peek.type == NUMBER) {
                        children.add(ctx.nextGroup(GroupType.NUMBER));
                    }else if(peek.type == STRING){
                        children.add(ctx.nextGroup(GroupType.STRING));
                    }else {
                        if(ctx.macros.containsKey(peek.content)){
                            ctx.nextToken();
                            Group[] groups = ctx.macros.get(peek.content);
                            Collections.addAll(children, groups);
                            continue;
                        }
                        // this is the case where the next token is an identifier
                        // explicitly just parse whatever is next as an identifier to avoid illegal values
                        children.add(ctx.explicitIdentifier());
                    }
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
            if(token.type == NUMBER){
                return new NumberGroup(token);
            } else if(token.type == STRING){
                return new StringGroup(token);
            }
            return new IdentifierGroup(token);
        }

        switch(token.content){
            case KEYWORD_CLASS: {
                IdentifierGroup name = ctx.explicitIdentifier();
                AccessModsGroup access = readAccess(ctx);
                return new ClassDeclarationGroup(token, access, name);
            }
            case KEYWORD_EXTENDS: {
                // also ensure that the previous group is a class declaration or an implements directive
                Group previous = ctx.previousGroup();
                if(previous.type != GroupType.CLASS_DECLARATION
                        && previous.type != GroupType.IMPLEMENTS_DIRECTIVE) {
                    throw new AssemblerException("Extends can only follow a class declaration", token.location);
                }
                IdentifierGroup group = ctx.explicitIdentifier();
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
                IdentifierGroup group = ctx.explicitIdentifier();
                return new ImplementsGroup(token, group);
            }
            case KEYWORD_FIELD: {
                // maybe read access modifiers
                IdentifierGroup name = ctx.explicitIdentifier();
                IdentifierGroup descriptor = ctx.explicitIdentifier();
                AccessModsGroup access = readAccess(ctx);

                return new FieldDeclarationGroup(token, access, name, descriptor);
            }
            case KEYWORD_METHOD: {
                // maybe read access modifiers
                IdentifierGroup methodDescriptor = ctx.explicitIdentifier();
                AccessModsGroup access = readAccess(ctx);
                return new MethodDeclarationGroup(token, access, methodDescriptor, readBody(ctx));
            }
            case KEYWORD_END: {
                return new Group(GroupType.END_BODY, token);
            }
            case KEYWORD_SWITCH: return readLookupSwitch(token, ctx);
            case KEYWORD_TABLESWITCH:  return readTableSwitch(token, ctx);
            case KEYWORD_CASE: {
                NumberGroup value = (NumberGroup) ctx.nextGroup(GroupType.NUMBER);
                LabelGroup label = new LabelGroup(ctx.nextGroup(GroupType.IDENTIFIER));
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
            case KEYWORD_CATCH: {
                IdentifierGroup exception = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
                LabelGroup begin = new LabelGroup(ctx.nextGroup(GroupType.IDENTIFIER));
                LabelGroup end = new LabelGroup(ctx.nextGroup(GroupType.IDENTIFIER));
                LabelGroup handler = new LabelGroup(ctx.nextGroup(GroupType.IDENTIFIER));
                return new CatchGroup(token, exception, begin, end, handler);
            }
            case KEYWORD_HANDLE: {
                IdentifierGroup type = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
                IdentifierGroup descriptor = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);

                return new HandleGroup(token, type, descriptor);
            }
            case KEYWORD_TYPE: {
                // explicit identifier is needed to account for illegal type names
                return new TypeGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_ARGS: {
                return new ArgsGroup(token, readBody(ctx));
            }
            case KEYWORD_INVISIBLE_ANNOTATION:
            case KEYWORD_ANNOTATION: {
                return readAnnotation(token, token.content.equals(KEYWORD_INVISIBLE_ANNOTATION), ctx);
            }
            case KEYWORD_TYPE_ANNOTATION:
            case KEYWORD_INVISIBLE_TYPE_ANNOTATION:
            case KEYWORD_INVISIBLE_PARAMETER_ANNOTATION:
            case KEYWORD_PARAMETER_ANNOTATION: {
                // not yet supported by the parser
                throw new UnsupportedOperationException("Type and parameter annotations are not yet supported");
            }
            case KEYWORD_SIGNATURE: {
                IdentifierGroup descriptor = ctx.explicitIdentifier();
                return new SignatureGroup(token, descriptor);
            }

            case KEYWORD_PUBLIC:
            case KEYWORD_PRIVATE:
            case KEYWORD_PROTECTED:
            case KEYWORD_STATIC:
            case KEYWORD_FINAL:
            case KEYWORD_SYNCHRONIZED:
            case KEYWORD_VOLATILE:
            case KEYWORD_TRANSIENT:
            case KEYWORD_NATIVE:
            case KEYWORD_ABSTRACT:
            case KEYWORD_STRICT:
                return new AccessModGroup(token);

        }

        return new Group(GroupType.IDENTIFIER, token);
    }

    public AnnotationGroup readAnnotation(Token token, boolean visible, ParserContext ctx) throws AssemblerException {
        List<AnnotationParamGroup> params = new ArrayList<>();
        IdentifierGroup classGroup = ctx.explicitIdentifier();
        while(ctx.hasNextToken()) {
            IdentifierGroup name = ctx.explicitIdentifier();
            if(name.content().equals(KEYWORD_END)) {
                Token next = ctx.peekToken();
                AnnotationTarget target;
                switch (next.content) {
                    case KEYWORD_FIELD:
                        target = AnnotationTarget.FIELD;
                        break;
                    case KEYWORD_METHOD:
                        target = AnnotationTarget.METHOD;
                        break;
                    case KEYWORD_CLASS:
                        target = AnnotationTarget.CLASS;
                        break;
                    default:
                        target = AnnotationTarget.UNKNOWN;
                        break;
                }
                return new AnnotationGroup(token, target, !visible, classGroup, params.toArray(new AnnotationParamGroup[0]));
            }
            Token next = ctx.peekToken();
            // enum intrinsic annotation
            if(next.content.equals(KEYWORD_ENUM)) {
                Token enumToken = ctx.nextToken();
                IdentifierGroup enumGroup = ctx.explicitIdentifier();
                IdentifierGroup enumValue = ctx.explicitIdentifier();
                params.add(new AnnotationParamGroup(name.value, name, new EnumGroup(enumToken, enumGroup, enumValue)));
                continue;
            }
            Group param = ctx.parseNext();
            params.add(new AnnotationParamGroup(name.value, name, param));
        }
        throw new AssemblerException("Unexpected end of file", ctx.previousGroup().location());
    }

    public InstructionGroup readInvokeDynamic(Token token, ParserContext ctx) throws AssemblerException {
        IdentifierGroup name = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
        IdentifierGroup descriptor = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
        HandleGroup bsmHandle = (HandleGroup) ctx.nextGroup(GroupType.HANDLE);
        ArgsGroup args = (ArgsGroup) ctx.nextGroup(GroupType.ARGS);

        return new InstructionGroup(token , name, descriptor, bsmHandle, args);
    }

    public AccessModsGroup readAccess(ParserContext ctx) throws AssemblerException {
        List<AccessModGroup> access = new ArrayList<>();
        while(accessModifiers.contains(ctx.peekToken().content)){
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

package me.darknet.assembler.parser;

import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.groups.AccessModGroup;
import me.darknet.assembler.parser.groups.AccessModsGroup;
import me.darknet.assembler.parser.groups.AnnotationGroup;
import me.darknet.assembler.parser.groups.AnnotationParamGroup;
import me.darknet.assembler.parser.groups.ArgsGroup;
import me.darknet.assembler.parser.groups.BodyGroup;
import me.darknet.assembler.parser.groups.CaseLabelGroup;
import me.darknet.assembler.parser.groups.CatchGroup;
import me.darknet.assembler.parser.groups.ClassDeclarationGroup;
import me.darknet.assembler.parser.groups.DefaultLabelGroup;
import me.darknet.assembler.parser.groups.EnumGroup;
import me.darknet.assembler.parser.groups.ExprGroup;
import me.darknet.assembler.parser.groups.ExtendsGroup;
import me.darknet.assembler.parser.groups.FieldDeclarationGroup;
import me.darknet.assembler.parser.groups.HandleGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.parser.groups.ImplementsGroup;
import me.darknet.assembler.parser.groups.InstructionGroup;
import me.darknet.assembler.parser.groups.LabelGroup;
import me.darknet.assembler.parser.groups.LookupSwitchGroup;
import me.darknet.assembler.parser.groups.MethodDeclarationGroup;
import me.darknet.assembler.parser.groups.MethodParameterGroup;
import me.darknet.assembler.parser.groups.MethodParametersGroup;
import me.darknet.assembler.parser.groups.NumberGroup;
import me.darknet.assembler.parser.groups.SignatureGroup;
import me.darknet.assembler.parser.groups.StringGroup;
import me.darknet.assembler.parser.groups.TableSwitchGroup;
import me.darknet.assembler.parser.groups.TextGroup;
import me.darknet.assembler.parser.groups.ThrowsGroup;
import me.darknet.assembler.parser.groups.TypeGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.darknet.assembler.parser.Group.GroupType;
import static me.darknet.assembler.parser.Token.TokenType.*;

public class Parser {
    private final Keywords keywords;

    public Parser() {
        this(new Keywords());
    }

    public Parser(Keywords keywords) {
        this.keywords = keywords;
    }

    public List<Token> tokenize(String source, String code) {
        Location location = new Location(1, 0, source, 0);
        StringBuilder sb = new StringBuilder();
        TokenizerContext ctx = new TokenizerContext(keywords, code.toCharArray(), location);
        while (ctx.canRead()) {
            Location previousLocation = ctx.currentLocation.copy();
            char c = ctx.next();
            if (c == '\r') {
                continue;
            }
            if (c == '/') { // comment
                if (ctx.canRead() && ctx.peek() == '/') {
                    while (ctx.canRead()) {
                        if (ctx.next() == '\n') { // single line comment
                            break;
                        }
                    }
                    continue;
                }
            }
            if (c == '"') { // string
                StringBuilder sb2 = new StringBuilder();
                while (ctx.canRead()) { // read until end of string
                    char c2 = ctx.next();
                    if (c2 == '"') {
                        ctx.add(new Token(sb2.toString(), location.sub(sb2.length() + 2), STRING));
                        break;
                    }
                    if (c2 == '\\') { // escape sequence
                        sb2.append(c2); // add backslash
                        sb2.append(ctx.next()); // add escaped char
                        continue; // skip next char
                    }
                    sb2.append(c2);
                }
                continue;
            }
            if (c == ' ' || c == '\n' || c == '\t') {

                // flush the string builder
                if (sb.length() > 0) {

                    ctx.finishToken(sb.toString(), previousLocation);
                    sb = new StringBuilder();
                }
                continue;
            }
            sb.append(c);
        }

        if (sb.length() > 0) { // if buffer is not empty flush it
            ctx.finishToken(sb.toString(), ctx.currentLocation);
        }

        return ctx.tokens;

    }

    public Group group(ParserContext ctx) throws AssemblerException {

        Token token = ctx.nextToken();

        if (token.type != KEYWORD) {
            String content = token.content;
            if (ParseInfo.actions.containsKey(content)) {

                // invokedynamic is a special case
                if (content.equals("invokedynamic")) {
                    return readInvokeDynamic(token, ctx);
                }

                ParseInfo info = ParseInfo.actions.get(content);
                int argc = info.args.length;
                List<Group> children = new ArrayList<>();
                for (int i = 0; i < argc; i++) {
                    Token peek = ctx.peekToken();
                    // this needed for illegal class names to ensure that the correct tokens are parsed here
                    // also the use of ctx.explicitIdentifier is needed to ensure that nothing illegal happens
                    if (peek.type == KEYWORD) {
                        if (keywords.match(Keyword.KEYWORD_HANDLE, peek) || 
                                keywords.match(Keyword.KEYWORD_TYPE, peek))
                            children.add(ctx.parseNext());
                        else {
                            children.add(ctx.explicitIdentifier());
                        }
                    } else if (info.args[i].equals("const")) {
                        // only parse arguments as constant if the argument is a constant
                        if (peek.type == NUMBER) {
                            children.add(new NumberGroup(ctx.nextToken()));
                        } else if (peek.type == STRING) {
                            children.add(new StringGroup(ctx.nextToken()));
                        } else {
                            if (ctx.macros.containsKey(peek.content)) {
                                ctx.nextToken();
                                Group[] groups = ctx.macros.get(peek.content);
                                Collections.addAll(children, groups);
                                continue;
                            }
                            // this is the case where the next token is an identifier
                            // explicitly just parse whatever is next as an identifier to avoid illegal values
                            children.add(ctx.explicitIdentifier());
                        }
                    } else {
                        // else just interpret them as explicit identifiers
                        if (ctx.macros.containsKey(peek.content)) {
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
                if (content.endsWith(":") && token.type == IDENTIFIER) {
                    return new LabelGroup(token);
                }
            }
            if (ctx.macros.containsKey(content)) {
                Group[] groups = ctx.macros.get(content);
                for (int i = 0; i < groups.length - 1; i++) {
                    ctx.pushGroup(groups[i]);
                }
                return groups[groups.length - 1];
            }
            if (token.type == NUMBER) {
                return new NumberGroup(token);
            } else if (token.type == STRING) {
                return new StringGroup(token);
            } else if (token.type == TEXT) {
                return new TextGroup(token);
            }
            return new IdentifierGroup(token);
        }

        Keyword keyword = keywords.fromToken(token);
        if (keyword == null)
            throw new AssemblerException("Cannot determine keyword from token: " + token.content, token.location);
        switch (keyword) {
            case KEYWORD_CLASS: {
                AccessModsGroup access = readAccess(ctx);
                IdentifierGroup name = ctx.explicitIdentifier();
                return new ClassDeclarationGroup(token, access, name);
            }
            case KEYWORD_EXTENDS: {
                // also ensure that the previous group is a class declaration or an implements directive
                Group previous = ctx.previousGroup();
                if (previous.type != GroupType.CLASS_DECLARATION
                        && previous.type != GroupType.IMPLEMENTS_DIRECTIVE) {
                    throw new AssemblerException("Extends can only follow a class declaration", token.location);
                }
                IdentifierGroup group = ctx.explicitIdentifier();
                return new ExtendsGroup(token, group);
            }
            case KEYWORD_IMPLEMENTS: {
                // same with implements
                Group previous = ctx.previousGroup();
                if (previous.type != GroupType.CLASS_DECLARATION
                        && previous.type != GroupType.IMPLEMENTS_DIRECTIVE
                        && previous.type != GroupType.EXTENDS_DIRECTIVE) {
                    throw new AssemblerException("Implements can only follow a class declaration", token.location);
                }
                IdentifierGroup group = ctx.explicitIdentifier();
                return new ImplementsGroup(token, group);
            }
            case KEYWORD_FIELD: {
                // maybe read access modifiers
                AccessModsGroup access = readAccess(ctx);
                IdentifierGroup name = ctx.explicitIdentifier();
                IdentifierGroup descriptor = ctx.explicitIdentifier();

                Token next = ctx.silentPeek();
                if (next.type != KEYWORD && !(next.type == EOF)) { // next value is not a keyword, so it must be a constant value
                    Group constantValue = ctx.parseNext();
                    return new FieldDeclarationGroup(token, access, name, descriptor, constantValue);
                }

                return new FieldDeclarationGroup(token, access, name, descriptor);
            }
            case KEYWORD_METHOD: {
                // HACK: giant hack but not sure how to fix it
                // maybe read access modifiers
                AccessModsGroup access = readAccess(ctx);
                // this parsing method does extend a bit far out of scope, but it is needed for
                // good consistency
                IdentifierGroup name = ctx.explicitIdentifier();
                List<MethodParameterGroup> params = new ArrayList<>();
                while (ctx.hasNextToken()) {
                    Token next = ctx.silentPeek();
                    if (next.type != IDENTIFIER || ParseInfo.has(next.content) || next.content.endsWith(":")) {
                        // next token is not an identifier or and instruction -> end of parameters
                        int index = params.size() - 1;
                        if (index < 0)
                            throw new AssemblerException("Cannot get parameter index: " + index, token.location);
                        MethodParameterGroup param = params.get(index); // get last parameter
                        String nme = param.getName().content();
                        String returnType = nme.substring(nme.indexOf(')') + 1);
                        return new MethodDeclarationGroup(
                                token,
                                access,
                                name,
                                new MethodParametersGroup(params.toArray(new MethodParameterGroup[0])),
                                returnType,
                                readBody(ctx));
                    }
                    IdentifierGroup desc = ctx.explicitIdentifier();
                    next = ctx.silentPeek();
                    if (next.type != IDENTIFIER || ParseInfo.has(next.content) || next.content.endsWith(":")) {
                        // next token is not an identifier or and instruction -> empty parameters
                        // that means the desc is the entire descriptor
                        String returnType = desc.content().substring(desc.content().indexOf(')') + 1);
                        return new MethodDeclarationGroup(
                                token,
                                access,
                                name,
                                new MethodParametersGroup(),
                                returnType,
                                readBody(ctx));

                    }
                    IdentifierGroup paramName = ctx.explicitIdentifier();
                    params.add(new MethodParameterGroup(desc.value, desc, paramName));
                }
                throw new AssemblerException("Method declaration must have a return type", token.location);
            }
            case KEYWORD_END: {
                return new Group(GroupType.END_BODY, token);
            }
            case KEYWORD_SWITCH:
                return readLookupSwitch(token, ctx);
            case KEYWORD_TABLESWITCH:
                return readTableSwitch(token, ctx);
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
                String typeName = type.content();
                switch (typeName) {
                    case "H_GETSTATIC":
                    case "H_PUTSTATIC":
                    case "H_GETFIELD":
                    case "H_PUTFIELD":
                    case "H_INVOKEVIRTUAL":
                    case "H_INVOKESPECIAL":
                    case "H_INVOKESTATIC":
                    case "H_NEWINVOKESPECIAL":
                    case "H_INVOKEINTERFACE":
                        return new HandleGroup(token,
                                type,
                                (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER), // owner.name
                                (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER)); // descriptor
                    default: {
                        throw new AssemblerException("Unknown handle type: " + typeName, type.location());
                    }
                }
            }
            case KEYWORD_TYPE: {
                // explicit identifier is needed to account for illegal type names
                return new TypeGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_METHOD_TYPE:
            case KEYWORD_ARGS: {
                return new ArgsGroup(token, readBody(ctx));
            }
            case KEYWORD_INVISIBLE_ANNOTATION:
            case KEYWORD_ANNOTATION: {
                return readAnnotation(token, keywords.match(Keyword.KEYWORD_INVISIBLE_ANNOTATION, token), ctx);
            }
            case KEYWORD_TYPE_ANNOTATION:
            case KEYWORD_INVISIBLE_TYPE_ANNOTATION:
            case KEYWORD_INVISIBLE_PARAMETER_ANNOTATION:
            case KEYWORD_PARAMETER_ANNOTATION: {
                // not yet supported by the parser
                throw new UnsupportedOperationException("Type and parameter annotations are not yet supported");
            }
            case KEYWORD_SIGNATURE: {
                return new SignatureGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_THROWS: {
                return new ThrowsGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_EXPR: {
                TextGroup text = (TextGroup) ctx.nextGroup(GroupType.TEXT);
                return new ExprGroup(token, text);
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
            case KEYWORD_BRIDGE:
            case KEYWORD_SYNTHETIC:
            case KEYWORD_VARARGS:
            case KEYWORD_SUPER:
            case KEYWORD_ENUM_ACCESS:
                return new AccessModGroup(token);

        }

        return new Group(GroupType.IDENTIFIER, token);
    }

    public AnnotationGroup readAnnotation(Token token, boolean visible, ParserContext ctx) throws AssemblerException {
        List<AnnotationParamGroup> params = new ArrayList<>();
        IdentifierGroup classGroup = ctx.explicitIdentifier();
        while (ctx.hasNextToken()) {
            IdentifierGroup name = ctx.explicitIdentifier();
            if (keywords.match(Keyword.KEYWORD_END, name)) {
                Token next = ctx.peekToken();
                AnnotationTarget target;
                Keyword keyword = keywords.fromToken(next);
                if (keyword == null)
                    target = AnnotationTarget.UNKNOWN;
                else {
                    switch (keyword) {
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
                }
                return new AnnotationGroup(token, target, !visible, classGroup, params.toArray(new AnnotationParamGroup[0]));
            }
            Token next = ctx.peekToken();
            // enum intrinsic annotation
            if (keywords.match(Keyword.KEYWORD_ENUM, next)) {
                Token enumToken = ctx.nextToken();
                IdentifierGroup enumGroup = ctx.explicitIdentifier();
                IdentifierGroup enumValue = ctx.explicitIdentifier();
                params.add(new AnnotationParamGroup(name.value, name, new EnumGroup(enumToken, enumGroup, enumValue)));
                continue;
            }
            Group param = ctx.parseNext();
            params.add(new AnnotationParamGroup(name.value, name, param));
        }
        throw new AssemblerException("Expected 'end' keyword", ctx.previousGroup().location());
    }

    public InstructionGroup readInvokeDynamic(Token token, ParserContext ctx) throws AssemblerException {
        IdentifierGroup name = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
        IdentifierGroup descriptor = (IdentifierGroup) ctx.nextGroup(GroupType.IDENTIFIER);
        HandleGroup bsmHandle = (HandleGroup) ctx.nextGroup(GroupType.HANDLE);
        ArgsGroup args = (ArgsGroup) ctx.nextGroup(GroupType.ARGS);

        return new InstructionGroup(token, name, descriptor, bsmHandle, args);
    }

    public AccessModsGroup readAccess(ParserContext ctx) throws AssemblerException {
        List<AccessModGroup> access = new ArrayList<>();
        while (keywords.isAccessModifier(ctx.silentPeek())) {
            access.add((AccessModGroup) ctx.nextGroup(GroupType.ACCESS_MOD));
        }
        return new AccessModsGroup(access.toArray(new AccessModGroup[0]));
    }

    public BodyGroup readBody(ParserContext ctx) throws AssemblerException {
        List<Group> body = new ArrayList<>();
        while (ctx.hasNextToken()) {
            Group grp = ctx.parseNext();
            if (grp.type == GroupType.END_BODY) {
                return new BodyGroup(body.toArray(new Group[0]));
            }
            body.add(grp);
        }
        throw new AssemblerException("Expected 'end' keyword", ctx.currentToken.getLocation());
    }

    public LookupSwitchGroup readLookupSwitch(Token begin, ParserContext ctx) throws AssemblerException {
        List<CaseLabelGroup> caseLabels = new ArrayList<>();
        while (ctx.hasNextToken()) {
            Group grp = ctx.parseNext();
            if (grp.type == GroupType.DEFAULT_LABEL) {
                return new LookupSwitchGroup(begin, (DefaultLabelGroup) grp, caseLabels.toArray(new CaseLabelGroup[0]));
            }
            if (grp.type != GroupType.CASE_LABEL) {
                throw new AssemblerException("Expected case label", grp.start().location);
            }
            caseLabels.add((CaseLabelGroup) grp);
        }
        throw new AssemblerException("Expected 'default' label", ctx.currentToken.getLocation());
    }

    private TableSwitchGroup readTableSwitch(Token begin, ParserContext ctx) throws AssemblerException {
        List<LabelGroup> caseLabels = new ArrayList<>();
        NumberGroup low = (NumberGroup) ctx.nextGroup(GroupType.NUMBER);
        NumberGroup high = (NumberGroup) ctx.nextGroup(GroupType.NUMBER);
        while (ctx.hasNextToken()) {
            Group grp = ctx.parseNext();
            if (grp.type == GroupType.DEFAULT_LABEL) {
                return new TableSwitchGroup(begin, low, high, (DefaultLabelGroup) grp, caseLabels.toArray(new LabelGroup[0]));
            }
            if (grp.type != GroupType.IDENTIFIER) {
                throw new AssemblerException("Expected 'default' label", grp.start().location);
            }
            caseLabels.add(new LabelGroup(grp.value));
        }
        throw new AssemblerException("Expected 'default' label", ctx.currentToken.getLocation());
    }

}

package me.darknet.assembler.parser;

import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.exceptions.arguments.InvalidArgumentException;
import me.darknet.assembler.exceptions.parser.UnexpectedIdentifierException;
import me.darknet.assembler.exceptions.parser.UnexpectedKeywordException;
import me.darknet.assembler.exceptions.parser.UnexpectedTokenException;
import me.darknet.assembler.instructions.Argument;
import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.parser.groups.module.*;
import me.darknet.assembler.util.ArrayTypes;

import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * @param ctx Context to read tokens from.
     * @return Parsed group.
     * @throws AssemblerException When an illegal ordering of tokens occurs.
     */
    public Group group(ParserContext ctx) throws AssemblerException {

        Token token = ctx.nextToken();

        if (!token.isType(KEYWORD)) {
            String content = token.getContent();
            if (ParseInfo.actions.containsKey(content)) {

                // invokedynamic is a special case
                if (content.equals("invokedynamic")) {
                    return readInvokeDynamic(token, ctx);
                }

                return readInstruction(token, ParseInfo.get(content).getArgs(), ctx);
            } else {
                if (content.endsWith(":") && token.isType(IDENTIFIER)) {
                    return new LabelGroup(token);
                }
            }
            switch (token.getType()) {
                case STRING:
                    return new StringGroup(token);
                case NUMBER:
                    return new NumberGroup(token);
                case TEXT:
                    return new TextGroup(token);
                default:
                    return new IdentifierGroup(token);
            }
        }

        Keyword keyword = keywords.fromToken(token);
        if (keyword == null)
            throw new AssemblerException("Cannot determine keyword from token: " + token.getContent(), token.getLocation());
        switch (keyword) {
            case KEYWORD_CLASS: {
                AccessModsGroup access = readAccess(ctx);
                IdentifierGroup name = ctx.explicitIdentifier();
                ExtendsGroup ext = ctx.maybeGroup(GroupType.EXTENDS_DIRECTIVE).getOrNull();
                List<ImplementsGroup> impls = new ArrayList<>();
                while (ctx.hasNextToken()) {
                    ParserContext.MaybeParsed possible = ctx.maybeGroup(GroupType.IMPLEMENTS_DIRECTIVE);
                    if (possible == null)
                        break;
                    if (possible.isTarget()) {
                        impls.add(possible.getOrNull());
                    } else {
                        if(possible.isType(GroupType.EXTENDS_DIRECTIVE)) {
                            if(ext == null) ext = possible.getOrNull();
                            else throw new AssemblerException("Cannot have multiple extends directives", possible.get().getStartLocation());
                        }
                        break; // not the group, so just continue
                    }
                }
                return new ClassDeclarationGroup(token, access, name, ext, impls);
            }
            case KEYWORD_EXTENDS: {
                return new ExtendsGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_IMPLEMENTS: {
                return new ImplementsGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_FIELD: {
                // maybe read access modifiers
                AccessModsGroup access = readAccess(ctx);
                IdentifierGroup name = ctx.explicitIdentifier();
                IdentifierGroup descriptor = ctx.explicitIdentifier();

                Token next = ctx.peekTokenSilent();
                if (!next.isType(KEYWORD) && !next.isType(EOF)) { // next value is not a keyword, so it must be a constant value
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
                    Token next = ctx.peekTokenSilent();
                    if (!next.isType(IDENTIFIER) || ParseInfo.has(next.getContent()) || next.getContent().endsWith(":")) {
                        // next token is not an identifier or and instruction -> end of parameters
                        int index = params.size() - 1;
                        if (index < 0)
                            throw new AssemblerException("Cannot get parameter index: " + index, token.getLocation());
                        MethodParameterGroup param = params.get(index); // get last parameter
                        String nme = param.getName().content();
                        String returnType = nme.substring(nme.indexOf(')') + 1);
                        return new MethodDeclarationGroup(
                                token,
                                access,
                                name,
                                new MethodParametersGroup(params),
                                returnType,
                                readBody(ctx));
                    }
                    IdentifierGroup desc = ctx.explicitIdentifier();
                    next = ctx.peekTokenSilent();
                    if (!next.isType(IDENTIFIER) || ParseInfo.has(next.getContent()) || next.getContent().endsWith(":")) {
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
                    params.add(new MethodParameterGroup(desc.getValue(), desc, paramName));
                }
                throw new AssemblerException("Method declaration must have a return type", token.getLocation());
            }
            case KEYWORD_END: {
                return new Group(GroupType.END_BODY, token);
            }
            case KEYWORD_SWITCH:
                return readLookupSwitch(token, ctx);
            case KEYWORD_TABLESWITCH:
                return readTableSwitch(token, ctx);
            case KEYWORD_CASE: {
                NumberGroup value = ctx.nextGroup(GroupType.NUMBER);
                LabelGroup label = new LabelGroup(ctx.nextGroup(GroupType.IDENTIFIER));
                return new CaseLabelGroup(token, value, label);
            }
            case KEYWORD_DEFAULT: {
                IdentifierGroup label = ctx.nextGroup(GroupType.IDENTIFIER);
                return new DefaultLabelGroup(token, new LabelGroup(label.getValue()));
            }
            case KEYWORD_MACRO: {
                Group macroName = ctx.nextGroup(GroupType.IDENTIFIER);
                List<Group> children = readBody(ctx).getChildren();
                ctx.putMacro(macroName.content(), children);
                return new Group(GroupType.MACRO_DIRECTIVE, token, children);
            }
            case KEYWORD_CATCH: {
                IdentifierGroup exception = ctx.nextGroup(GroupType.IDENTIFIER);
                LabelGroup begin = new LabelGroup(ctx.nextGroup(GroupType.IDENTIFIER));
                LabelGroup end = new LabelGroup(ctx.nextGroup(GroupType.IDENTIFIER));
                LabelGroup handler = new LabelGroup(ctx.nextGroup(GroupType.IDENTIFIER));
                return new CatchGroup(token, exception, begin, end, handler);
            }
            case KEYWORD_HANDLE: {
                IdentifierGroup type = ctx.nextGroup(GroupType.IDENTIFIER);
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
                                ctx.nextGroup(GroupType.IDENTIFIER), // owner.name
                                ctx.nextGroup(GroupType.IDENTIFIER)); // descriptor
                    default: {
                        throw new AssemblerException("Unknown handle type: " + typeName, type.getStartLocation());
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
            case KEYWORD_VERSION: {
                return new VersionGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_SOURCE_FILE: {
                return new SourceFileGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_NEST_HOST: {
                return new NestHostGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_NEST_MEMBER: {
                return new NestMemberGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_PERMITTED_SUBCLASS: {
                return new PermittedSubclassGroup(token, ctx.explicitIdentifier());
            }
            case KEYWORD_MODULE: {
                return readModule(token, ctx);
            }
            case KEYWORD_INNER_CLASS: {
                return new InnerClassGroup(token, readAccess(ctx), ctx.explicitIdentifier(), ctx.explicitIdentifier(), ctx.explicitIdentifier());
            }
            case KEYWORD_EXPR: {
                TextGroup text = ctx.nextGroup(GroupType.TEXT);
                return new ExprGroup(token, text);
            }
            case KEYWORD_WITH:
            case KEYWORD_TO: {
                List<IdentifierGroup> identifiers = new ArrayList<>();
                while (ctx.hasNextToken()) {
                    Token next = ctx.peekToken();
                    if (next.isType(IDENTIFIER)) {
                        identifiers.add(ctx.explicitIdentifier());
                    } else {
                        if(next.isType(KEYWORD)) {
                            Keyword key = keywords.fromToken(next);
                            if (key != Keyword.KEYWORD_END) {
                                throw new UnexpectedKeywordException(next.getLocation(), next.getContent(), Keyword.KEYWORD_END);
                            }
                            ctx.nextToken();
                            if(keyword == Keyword.KEYWORD_WITH) {
                                return new WithGroup(token, identifiers);
                            } else {
                                return new ToGroup(token, identifiers);
                            }
                        } else {
                            throw new UnexpectedKeywordException(next.getLocation(), next.getContent(), Keyword.KEYWORD_END);
                        }
                    }
                }
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
            case KEYWORD_OPEN:
            case KEYWORD_STRICT:
            case KEYWORD_BRIDGE:
            case KEYWORD_SYNTHETIC:
            case KEYWORD_VARARGS:
            case KEYWORD_SUPER:
            case KEYWORD_INTERFACE:
            case KEYWORD_ANNOTATION_ACCESS:
            case KEYWORD_ENUM_ACCESS:
            case KEYWORD_TRANSITIVE:
            case KEYWORD_STATIC_PHASE:
            case KEYWORD_MANDATED:
                return new AccessModGroup(token);

        }

        return new Group(GroupType.IDENTIFIER, token);
    }

    public InstructionGroup readInstruction(Token token, Argument[] arguments, ParserContext ctx) throws AssemblerException {
        List<Group> children = new ArrayList<>();
        for (Argument arg : arguments) {
            Token peek = ctx.peekToken();
            if(ctx.isOneLine()) {
                if(peek.getLocation().getLine() != token.getLocation().getLine()) {
                    return new InstructionGroup(token, children);
                }
            }
            switch (arg) {
                case NAME:
                case CLASS:
                case DESCRIPTOR:
                case LABEL:
                    children.add(ctx.explicitIdentifier());
                    break;
                case FIELD:
                case METHOD:
                    IdentifierGroup path = ctx.explicitIdentifier();
                    if(ctx.isVerifyInstructions() && !path.content().contains(".")) {
                        throw new InvalidArgumentException(path.getStartLocation(), arg, path.content());
                    }
                    children.add(path);
                    break;
                case BYTE:
                case SHORT:
                case INTEGER:
                    if(!peek.isType(NUMBER)) {
                        throw new InvalidArgumentException(peek.getLocation(), arg, peek.getContent());
                    }
                    NumberGroup number = ctx.nextGroup(GroupType.NUMBER);
                    long value = number.getNumber().longValue();
                    if(ctx.isVerifyInstructions()) {
                        switch (arg) {
                            case BYTE:
                                if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                                    throw new InvalidArgumentException(number.getStartLocation(),
                                            arg,
                                            number.content(),
                                            new NumberFormatException("Value out of range. Value:\"" + value + "\" Radix:10"));
                                }
                                break;
                            case SHORT:
                                if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                                    throw new InvalidArgumentException(number.getStartLocation(),
                                            arg,
                                            number.content(),
                                            new NumberFormatException("Value out of range. Value:\"" + value + "\" Radix:10"));
                                }
                                break;
                            case INTEGER:
                                if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                                    throw new InvalidArgumentException(number.getStartLocation(),
                                            arg,
                                            number.content(),
                                            new NumberFormatException("Value out of range. Value:\"" + value + "\" Radix:10"));
                                }
                                break;
                        }
                    }
                    children.add(number);
                    break;
                case CONSTANT: {
                    switch (peek.getType()) {
                        case IDENTIFIER:
                            children.add(ctx.explicitIdentifier());
                            break;
                        case NUMBER:
                            children.add(ctx.nextGroup(GroupType.NUMBER));
                            break;
                        case STRING:
                            children.add(ctx.nextGroup(GroupType.STRING));
                            break;
                        case KEYWORD: {
                            Keyword keyword = keywords.fromToken(peek);
                            switch (keyword) {
                                case KEYWORD_HANDLE:
                                case KEYWORD_TYPE:
                                    children.add(ctx.parseNext());
                                    break;
                                default:
                                    throw new UnexpectedKeywordException(peek.getLocation(), peek.getContent(), Keyword.KEYWORD_HANDLE, Keyword.KEYWORD_TYPE);
                            }
                            break;
                        }
                        default:
                            throw new UnexpectedTokenException(peek.getLocation(), peek.getContent(), IDENTIFIER, NUMBER, STRING, KEYWORD);
                    }
                    break;
                }
                case TYPE: {
                    IdentifierGroup identifier = ctx.nextGroup(GroupType.IDENTIFIER);
                    if(ctx.isVerifyInstructions() && !ArrayTypes.isType(identifier.content())) {
                        throw new InvalidArgumentException(
                                identifier.getStartLocation(),
                                Argument.TYPE,
                                identifier.content(),
                                new UnexpectedIdentifierException(identifier.getStartLocation(), identifier.content(), ArrayTypes.getTypes()));
                    }
                    children.add(identifier);
                    break;
                }
                case SWITCH:
                case BOOTSTRAP_ARGUMENTS:
                    break;
            }
        }
        return new InstructionGroup(token, children);
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
                return new AnnotationGroup(token, target, !visible, classGroup, params);
            }
            Token next = ctx.peekToken();
            // enum intrinsic annotation
            if (keywords.match(Keyword.KEYWORD_ENUM, next)) {
                Token enumToken = ctx.nextToken();
                IdentifierGroup enumGroup = ctx.explicitIdentifier();
                IdentifierGroup enumValue = ctx.explicitIdentifier();
                params.add(new AnnotationParamGroup(name.getValue(), name, new EnumGroup(enumToken, enumGroup, enumValue)));
                continue;
            }
            Group param = ctx.parseNext();
            params.add(new AnnotationParamGroup(name.getValue(), name, param));
        }
        throw new UnexpectedKeywordException(ctx.getCurrentToken().getLocation(), "EOF", Keyword.KEYWORD_END);
    }

    public ModuleGroup readModule(Token token, ParserContext ctx) throws AssemblerException {
        AccessModsGroup access = readAccess(ctx);
        IdentifierGroup name = ctx.explicitIdentifier();
        VersionGroup version = ctx.maybeGroup(GroupType.VERSION_DIRECTIVE).getOrNull();
        MainClassGroup mainClassGroup = null;
        List<PackageGroup> packages = new ArrayList<>();
        List<RequireGroup> requires = new ArrayList<>();
        List<ExportGroup> exports = new ArrayList<>();
        List<OpenGroup> opens = new ArrayList<>();
        List<UseGroup> uses = new ArrayList<>();
        List<ProvideGroup> provides = new ArrayList<>();

        while (ctx.hasNextToken()) {
            Token next = ctx.nextToken();
            Keyword keyword = keywords.fromToken(next);
            if (keyword == null)
                throw new AssemblerException("Unexpected token, expected requires, exports, uses, provides, opens, mainclass, package or end", next.getLocation());
            switch (keyword) {
                case KEYWORD_EXPORTS:
                case KEYWORD_OPENS: {
                    AccessModsGroup accessMods = readAccess(ctx);
                    IdentifierGroup pkg = ctx.explicitIdentifier();
                    ToGroup to = ctx.maybeGroup(GroupType.MODULE_TO).getOrNull();
                    if (keyword == Keyword.KEYWORD_EXPORTS)
                        exports.add(new ExportGroup(next, accessMods, pkg, to));
                    else
                        opens.add(new OpenGroup(next, accessMods, pkg, to));
                    break;
                }
                case KEYWORD_USES: {
                    uses.add(new UseGroup(next, ctx.explicitIdentifier()));
                    break;
                }
                case KEYWORD_PROVIDES: {
                    IdentifierGroup service = ctx.explicitIdentifier();
                    WithGroup with = ctx.maybeGroup(GroupType.MODULE_WITH).getOrNull();
                    provides.add(new ProvideGroup(next, service, with));
                    break;
                }
                case KEYWORD_REQUIRES: {
                    AccessModsGroup accessMods = readAccess(ctx);
                    IdentifierGroup module = ctx.explicitIdentifier();
                    VersionGroup versionGroup = ctx.maybeGroup(GroupType.VERSION_DIRECTIVE).getOrNull();
                    requires.add(new RequireGroup(next, accessMods, module, versionGroup));
                    break;
                }
                case KEYWORD_PACKAGE: {
                    packages.add(new PackageGroup(next, ctx.explicitIdentifier()));
                    break;
                }
                case KEYWORD_MAIN_CLASS: {
                    mainClassGroup = new MainClassGroup(next, ctx.explicitIdentifier());
                    break;
                }
                case KEYWORD_END:
                    return new ModuleGroup(token, access, name, version, mainClassGroup, packages, requires, exports, opens, uses, provides);
                default:
                    throw new AssemblerException("Unexpected keyword, expected requires, exports, uses, provides, opens, mainclass, package or end", next.getLocation());
            }
        }
        throw new AssemblerException("Expected 'end' keyword", ctx.previousGroup().getStartLocation());
    }

    public InstructionGroup readInvokeDynamic(Token token, ParserContext ctx) throws AssemblerException {
        IdentifierGroup name = ctx.nextGroup(GroupType.IDENTIFIER);
        IdentifierGroup descriptor = ctx.nextGroup(GroupType.IDENTIFIER);
        HandleGroup bsmHandle = ctx.nextGroup(GroupType.HANDLE);
        ArgsGroup args = ctx.maybeGroup(GroupType.ARGS).getOrNull();

        return new InstructionGroup(token, Arrays.asList(name, descriptor, bsmHandle, args));
    }

    public AccessModsGroup readAccess(ParserContext ctx) throws AssemblerException {
        List<AccessModGroup> access = new ArrayList<>();
        while (keywords.isAccessModifier(ctx.peekTokenSilent())) {
            access.add(ctx.nextGroup(GroupType.ACCESS_MOD));
        }
        return wrap(ctx, new AccessModsGroup(access));
    }

    public BodyGroup readBody(ParserContext ctx) throws AssemblerException {
        List<Group> body = new ArrayList<>();
        while (ctx.hasNextToken()) {
            Group grp = ctx.parseNext();
            if (grp.isType(GroupType.END_BODY)) {
                return new BodyGroup(body);
            }
            body.add(grp);
        }
        throw new AssemblerException("Expected 'end' keyword", ctx.getCurrentLocation());
    }

    public LookupSwitchGroup readLookupSwitch(Token begin, ParserContext ctx) throws AssemblerException {
        List<CaseLabelGroup> caseLabels = new ArrayList<>();
        while (ctx.hasNextToken()) {
            Group grp = ctx.parseNext();
            if (grp.isType(GroupType.DEFAULT_LABEL)) {
                return new LookupSwitchGroup(begin, (DefaultLabelGroup) grp, caseLabels);
            }
            if (!grp.isType(GroupType.CASE_LABEL)) {
                throw new AssemblerException("Expected case label", grp.start().getLocation());
            }
            caseLabels.add((CaseLabelGroup) grp);
        }
        throw new AssemblerException("Expected 'default' label", ctx.getCurrentLocation());
    }

    private TableSwitchGroup readTableSwitch(Token begin, ParserContext ctx) throws AssemblerException {
        List<LabelGroup> caseLabels = new ArrayList<>();
        NumberGroup low = ctx.nextGroup(GroupType.NUMBER);
        NumberGroup high = ctx.nextGroup(GroupType.NUMBER);
        while (ctx.hasNextToken()) {
            Group grp = ctx.parseNext();
            if (grp.isType(GroupType.DEFAULT_LABEL)) {
                return new TableSwitchGroup(begin, low, high, (DefaultLabelGroup) grp, caseLabels);
            }
            if (!grp.isType( GroupType.IDENTIFIER)) {
                throw new AssemblerException("Expected 'default' label", grp.start().getLocation());
            }
            caseLabels.add(new LabelGroup(grp.getValue()));
        }
        throw new AssemblerException("Expected 'default' label", ctx.getCurrentLocation());
    }

    private <T extends Group> T wrap(ParserContext ctx, T group) {
        group.setFallbackLocation(ctx.getCurrentLocation());
        return group;
    }
}

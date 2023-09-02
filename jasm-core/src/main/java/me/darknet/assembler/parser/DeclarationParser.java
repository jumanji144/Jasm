package me.darknet.assembler.parser;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Parser for parsing generic structures from tokens to ast elements.
 *
 * @see me.darknet.assembler.ast.primitive
 */
public class DeclarationParser {

    private ParserContext ctx;

    /**
     * Parse all declarations from the given tokens, this will only try to parse
     * declarations. Result for this method will always be a list of
     * {@link ASTDeclaration} or null if parsing that element failed.
     *
     * @param tokens
     *               the tokens to parse
     *
     * @return {@link ParsingResult} of the parsing
     */
    public ParsingResult<List<@Nullable ASTElement>> parseDeclarations(Collection<Token> tokens) {
        if(tokens.isEmpty()) {
            return new ParsingResult<>(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        Pair<List<ASTComment>, Collection<Token>> filtered = filterComments(tokens);
        this.ctx = new ParserContext(this, new ArrayList<>(filtered.getSecond()));
        List<ASTElement> declarations = new ArrayList<>();
        while (!this.ctx.done()) {
            declarations.add(parseDeclaration());
        }
        return new ParsingResult<>(declarations, ctx.errorCollector.getErrors(), filtered.getFirst());
    }

    /**
     * Parse any element from the given tokens, this will try to parse any element.
     * Results for this method can only include objects from
     * {@link me.darknet.assembler.ast.primitive} and {@link ASTDeclaration}
     *
     * @param tokens
     *               the tokens to parse
     *
     * @return {@link ParsingResult} of the parsing
     */
    public ParsingResult<List<@Nullable ASTElement>> parseAny(Collection<Token> tokens) {
        if(tokens.isEmpty()) {
            return new ParsingResult<>(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        Pair<List<ASTComment>, Collection<Token>> filtered = filterComments(tokens);
        this.ctx = new ParserContext(this, new ArrayList<>(filtered.getSecond()));
        List<ASTElement> result = new ArrayList<>();
        while (!this.ctx.done()) {
            ASTElement element = parse();
            result.add(element);
        }
        return new ParsingResult<>(result, ctx.errorCollector.getErrors(), filtered.getFirst());
    }

    private Pair<List<ASTComment>, Collection<Token>> filterComments(Collection<Token> tokens) {
        List<Token> filtered = new ArrayList<>();
        List<ASTComment> comments = new ArrayList<>();
        for (Token token : tokens) {
            if (token.getType().equals(TokenType.COMMENT)) {
                comments.add(new ASTComment(token));
            } else {
                filtered.add(token);
            }
        }
        return new Pair<>(comments, filtered);
    }

    private @Nullable ASTElement parse() {
        Token token = ctx.peek();

        switch (token.getType()) {
            case IDENTIFIER: {
                String content = token.getContent();
                if (content.charAt(0) == '.') {
                    // begin of declaration
                    return parseDeclaration();
                } else
                    return new ASTIdentifier(ctx.takeAny());
            }
            case NUMBER: {
                return new ASTNumber(ctx.takeAny());
            }
            case STRING: {
                return new ASTString(ctx.takeAny());
            }
            case OPERATOR: {
                char operator = token.getContent().charAt(0);
                if (operator == '{') {
                    if (ctx.isCurrentState(State.IN_OBJECT)) {
                        Token objectKey = ctx.peek(-2);
                        if (objectKey != null && objectKey.getContent().equals("code")) {
                            // this is the only way I could easily sneak in the code format into the parser
                            return parseCode();
                        }
                    }
                    Token next = ctx.peek(1);
                    if (next == null) {
                        ctx.take("{");
                        ctx.throwEofError("identifier");
                        return null;
                    }
                    if (next.getType().equals(TokenType.OPERATOR)) {
                        if (next.getContent().equals("}")) { // empty object
                            return parseEmpty();
                        }
                    }
                    if (next.getContent().startsWith(".")) {
                        return parseArrayOrNestedDeclaration();
                    }
                    // now we need to determine if it's an array or an object
                    // it is an object if they there will be a : after the identifier
                    Token peek = ctx.peek(2);
                    if (peek == null) {
                        ctx.throwEofError(":, } or ,");
                        return null;
                    }
                    if (peek.getType().equals(TokenType.OPERATOR)) {
                        if (peek.getContent().equals(":")) {
                            return parseObject();
                        }
                    }
                    return parseArray();
                } else {
                    ctx.takeAny();
                    ctx.throwExpectedError("{", token.getContent());
                }
            }
            default: {
                ctx.errorCollector.addError(new Error("Unexpected token " + token.getContent(), token.getLocation()));
            }
        }
        return null;
    }

    private ASTEmpty parseEmpty() {
        ctx.take("{");
        ctx.take("}");
        return new ASTEmpty();
    }

    private ASTArray parseArray() {
        if (ctx.take("{") == null)
            return null;
        List<ASTElement> elements = new ArrayList<>();
        return parseHalfArray(elements);
    }

    private ASTArray parseHalfArray(List<ASTElement> elements) {
        ctx.enterState(State.IN_ARRAY);
        Token peek = ctx.peek();
        while (!peek.getContent().equals("}")) {
            elements.add(parse());
            peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError(", or {");
                return null;
            }
            if (!peek.getContent().equals("}")) {
                if (ctx.take(",") == null)
                    return null;
            }
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState(State.IN_ARRAY);
        return new ASTArray(elements);
    }

    private ASTObject parseObject() {
        ctx.enterState(State.IN_OBJECT);
        if (ctx.take("{") == null)
            return null;
        ElementMap<ASTIdentifier, ASTElement> elements = new ElementMap<>();
        Token peek = ctx.peek();
        while (!peek.getContent().equals("}")) {
            ASTIdentifier identifier = ctx.literal();
            if (identifier == null)
                return null;
            if (ctx.take(":") == null)
                return null;
            ASTElement element = parse();
            if (element == null) {
                ctx.throwExpectedError("element", peek.getContent());
                return null;
            }
            elements.put(identifier, element);
            peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError(", or }");
                return null;
            }
            if (!peek.getContent().equals("}")) {
                if (ctx.take(",") == null)
                    return null;
            }
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState(State.IN_OBJECT);
        return new ASTObject(elements);
    }

    private ASTDeclaration parseDeclaration() {
        ASTIdentifier identifier = new ASTIdentifier(ctx.takeAny());
        if (identifier.getContent().charAt(0) != '.') {
            ctx.throwExpectedError("identifier starting with '.'", identifier.getContent());
            return null;
        }
        State state = ctx.getState();
        Token peek = ctx.peek();
        if(peek == null) {
            ctx.throwEofError("content");
            return null;
        }
        List<ASTElement> elements = new ArrayList<>();
        while (!peek.getContent().startsWith(".")) {
            elements.add(parse());
            peek = ctx.peek();
            if (peek == null)
                break; // declarations are the top level elements, so we can just stop here
            if (state == State.IN_NESTED_DECLARATION_OR_ARRAY) {
                if (peek.getContent().equals("}") || peek.getContent().equals(",")) {
                    break;
                }
            }
            if ((state == State.IN_NESTED_DECLARATION) && peek.getContent().equals("}")) {
                break;
            } else if (state == State.IN_OBJECT) {
                // detection is a bit hacky, but it works
                // check if over next token is a : or next token is a }
                if (peek.getContent().equals("}") || peek.getContent().equals(",")) {
                    break;
                }
                Token next = ctx.peek(1);
                if (next == null) {
                    ctx.throwEofError("end of declaration");
                    return null;
                }
                if (next.getContent().equals(":")) {
                    break;
                }
            } else if ((state == State.IN_ARRAY) && (peek.getContent().equals(",") || peek.getContent().equals("}"))) { // arrays are a bit easier
                break;
            }
        }
        return new ASTDeclaration(identifier, elements);
    }

    private ASTElement parseArrayOrNestedDeclaration() {
        if (ctx.take("{") == null)
            return null;
        ctx.enterState(State.IN_NESTED_DECLARATION_OR_ARRAY);
        ASTElement element = parse();
        if (element == null)
            return null;
        Token peek = ctx.peek();
        if (peek == null) {
            ctx.throwEofError(", or } or element");
            return null;
        }
        List<ASTElement> elements = new ArrayList<>();
        elements.add(element);
        if (peek.getContent().equals("}")) {
            ctx.take("}");
            return new ASTDeclaration(null, elements);
        }
        if (peek.getContent().equals(",")) {
            ctx.take(",");
            return parseHalfArray(elements);
        }
        ctx.leaveState(State.IN_NESTED_DECLARATION_OR_ARRAY);
        return parseHalfNestedDeclaration(elements);
    }

    private ASTDeclaration parseNestedDeclaration() {
        if (ctx.take("{") == null)
            return null;
        return parseHalfNestedDeclaration(new ArrayList<>());
    }

    private ASTDeclaration parseHalfNestedDeclaration(List<ASTElement> elements) {
        ctx.enterState(State.IN_NESTED_DECLARATION);
        Token peek = ctx.peek();
        while (!peek.getContent().equals("}")) {
            elements.add(parseDeclaration());
            peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError("} or declaration");
                return null;
            }
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState();
        return new ASTDeclaration(null, elements);
    }

    private ASTCode parseCode() {
        ctx.enterState(State.IN_CODE);
        if (ctx.take("{") == null)
            return null;
        Token peek = ctx.peek();
        List<ASTInstruction> instructions = new ArrayList<>();
        while (!peek.getContent().equals("}")) {
            ASTInstruction instruction = parseInstruction();
            if (instruction == null)
                return null;
            instructions.add(instruction);
            peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError("} or instruction");
                return null;
            }
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState(State.IN_CODE);
        return new ASTCode(instructions);
    }

    private ASTInstruction parseInstruction() {
        ctx.enterState(State.IN_INSTRUCTION);
        Token instruction = ctx.takeAny();
        if (instruction == null)
            return null;
        if (instruction.getType() != TokenType.IDENTIFIER) {
            ctx.throwExpectedError("instruction or label", instruction.getContent());
            return null;
        }
        Token peek = ctx.peek();
        if (peek == null) {
            ctx.throwEofError("instruction argument or label");
            return null;
        }
        if (peek.getContent().equals(":")) {
            ctx.leaveState(State.IN_INSTRUCTION);
            ctx.take(":");
            return new ASTLabel(new ASTIdentifier(instruction));
        }
        ASTIdentifier identifier = new ASTIdentifier(instruction);
        List<ASTElement> arguments = new ArrayList<>();
        // parse until peek is eof or on a different line
        while (peek.getLocation().getLine() == instruction.getLocation().getLine()) {
            arguments.add(parse());
            peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError("instruction argument");
                return null;
            }
        }
        ctx.leaveState(State.IN_INSTRUCTION);
        return new ASTInstruction(identifier, arguments);
    }

    private enum State {
        DEFAULT,
        IN_DECLARATION,
        IN_NESTED_DECLARATION,
        IN_NESTED_DECLARATION_OR_ARRAY,
        IN_OBJECT,
        IN_ARRAY,
        IN_CODE,
        IN_INSTRUCTION
    }

    private static class ParserContext extends Stateful<State> {

        private final DeclarationParser parser;
        private final List<Token> tokens;
        private final ErrorCollector errorCollector = new ErrorCollector();
        private int idx = 0;
        private Token latest;

        private ParserContext(DeclarationParser parser, List<Token> tokens) {
            this.parser = parser;
            this.tokens = Collections.unmodifiableList(tokens);
            this.latest = tokens.get(0);
        }

        private boolean done() {
            return idx >= tokens.size();
        }

        private Token next() {
            latest = tokens.get(idx++);
            return latest;
        }

        private Token peek() {
            return peek(0);
        }

        private Token peek(int offset) {
            int listIdx = idx + offset;
            if (listIdx < 0 || listIdx >= tokens.size()) {
                return null;
            }
            return tokens.get(listIdx);
        }

        private Token take(String exact) {
            if (tokens.isEmpty()) {
                throwEofError(exact);
                return null;
            }
            Token token = next();
            if (token.getContent().equals(exact)) {
                return token;
            } else {
                throwExpectedError(exact, token.getContent());
                return null;
            }
        }

        private Token takeAny() {
            if (tokens.isEmpty()) {
                throwEofError("any token");
                return null;
            }
            return next();
        }

        private ASTIdentifier literal() {
            if (tokens.isEmpty()) {
                throwEofError("literal");
                return null;
            }
            Token token = next();
            switch (token.getType()) {
                case IDENTIFIER:
                case NUMBER:
                case STRING:
                    return new ASTIdentifier(token);
            }
            throwExpectedError("literal", token.getContent());
            return null;
        }

        @SuppressWarnings("unchecked")
        private <T extends ASTElement> T parseElement(ElementType... validTypes) {
            T element = (T) parser.parse();
            if (element == null) {
                return null;
            }
            if (validTypes.length == 0) {
                return element;
            }
            boolean valid = false;
            for (ElementType validType : validTypes) {
                if (element.getType() == validType) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                errorCollector
                        .addError(
                                new Error(
                                        "Expected one of " + Arrays.toString(validTypes) + " but got "
                                                + element.getType(),
                                        element.getValue().getLocation()
                                )
                        );
                return null;
            }
            return element;
        }

        public void throwError(Error error) {
            errorCollector.addError(error);
        }

        public void throwEofError(String expected) {
            if (latest == null) {
                throwError(new Error("Expected '" + expected + "' but got EOF", new Location(-1, -1, "")));
                return;
            }
            throwError(new Error("Expected '" + expected + "' but got EOF", latest.getLocation()));
        }

        public void throwExpectedError(String expected, String got) {
            if (latest == null) {
                throwEofError(expected);
                return;
            }
            throwError(new Error("Expected '" + expected + "' but got '" + got + "'", latest.getLocation()));
        }

    }

}

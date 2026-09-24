package me.darknet.assembler.parser;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Location;

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
	 * Parse all declarations from the given tokens, preserving partial AST recovery when syntax errors occur.
	 *
	 * @param tokens
	 * 		the tokens to parse
	 *
	 * @return the parsed declarations and syntax diagnostics
	 */
    public Outcome<List<@Nullable ASTElement>> parseDeclarations(Collection<Token> tokens) {
        List<Token> filtered = filterComments(tokens);
        if (filtered.isEmpty())
            return Outcome.success(List.of());
        this.ctx = new ParserContext(this, filtered);
        List<@Nullable ASTElement> declarations = new ArrayList<>();
        while (!this.ctx.done())
            declarations.add(parseDeclaration());
        return Outcome.of(declarations, ctx.diagnostics.diagnostics());
    }

	/**
	 * Parse any supported element from the given tokens, preserving partial AST recovery.
	 *
	 * @param tokens
	 * 		the tokens to parse
	 *
	 * @return the parsed elements and syntax diagnostics
	 */
    public Outcome<List<@Nullable ASTElement>> parseAny(Collection<Token> tokens) {
        List<Token> filtered = filterComments(tokens);
        if (filtered.isEmpty())
            return Outcome.success(List.of());
        this.ctx = new ParserContext(this, filtered);
        List<@Nullable ASTElement> result = new ArrayList<>();
        while (!this.ctx.done())
            result.add(parse());
        return Outcome.of(result, ctx.diagnostics.diagnostics());
    }

    private List<Token> filterComments(Collection<Token> tokens) {
        List<Token> filtered = new ArrayList<>(tokens.size());
        for (Token token : tokens) {
            if (token.type() != TokenType.COMMENT)
                filtered.add(token);
        }
        return filtered;
    }

    private @Nullable ASTElement parseOperator(Token token) {
        char operator = token.content().charAt(0);
        if (operator != '{') {
            ctx.takeAny();
            ctx.throwUnexpectedError(token.content());
            return null;
        }
        if (ctx.isCurrentState(State.IN_OBJECT)) {
            Token objectKey = ctx.peek(-2);
            if (objectKey != null && objectKey.content().equals("code")) {
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
        if (next.type().equals(TokenType.OPERATOR)) {
            if (next.content().equals("}")) { // empty object
                return parseEmpty();
            }
        }
        if (next.type().equals(TokenType.IDENTIFIER) && next.content().startsWith(".")) {
            return parseArrayOrNestedDeclaration();
        }
        // now we need to determine if it's an array or an object
        // it is an object if they there will be a : after the identifier
        Token peek = ctx.peek(2);
        if (peek == null) {
            ctx.takeAny();
            ctx.throwEofError(":, } or ,");
            return null;
        }
        if (peek.type().equals(TokenType.OPERATOR)) {
            if (peek.content().equals(":")) {
                return parseObject();
            }
        }

        // can't determine that it's an object, must be array
        return parseArray();
    }

    private @Nullable ASTElement parse() {
        Token token = ctx.peek();
        if (token == null) {
            ctx.throwEofError("element");
            return null;
        }

        switch (token.type()) {
            case IDENTIFIER -> {
                String content = token.content();
                if (content.charAt(0) == '.') {
                    // begin of declaration
                    return parseDeclaration();
                } else
                    return new ASTIdentifier(ctx.takeAny());
            }
            case NUMBER -> {
                return new ASTNumber(ctx.takeAny());
            }
            case STRING -> {
                return new ASTString(ctx.takeAny());
            }
            case CHARACTER -> {
                return new ASTCharacter(ctx.takeAny());
            }
            case OPERATOR -> {
                return parseOperator(token);
            }
            default -> ctx.throwError(DiagnosticCode.UNEXPECTED_TOKEN, "Unexpected token " + token.content(), token.location());
        }
        return null;
    }

    private ASTEmpty parseEmpty() {
        Token begin = ctx.take("{");
        ctx.take("}");
        return new ASTEmpty(begin);
    }

    private @Nullable ASTArray parseArray() {
        if (ctx.take("{") == null)
            return null;
        List<ASTElement> elements = new ArrayList<>();
        return parseHalfArray(elements);
    }

    private @Nullable ASTArray parseHalfArray(List<ASTElement> elements) {
        ctx.enterState(State.IN_ARRAY);
        while (true) {
            Token peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError(", or {");
                return null;
            }
            if (peek.content().equals("}")) {
                break;
            }
            ASTElement element = parse();
            if (element == null) {
                return null;
            }
            elements.add(element);
            peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError(", or }");
                return null;
            }
            if (!peek.content().equals("}")) {
                if (ctx.take(",") == null)
                    return null;
            }
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState(State.IN_ARRAY);
        return new ASTArray(elements);
    }

    private @Nullable ASTObject parseObject() {
        ctx.enterState(State.IN_OBJECT);
        if (ctx.take("{") == null)
            return null;
        ElementMap<ASTIdentifier, ASTElement> elements = new ElementMap<>();
        while (true) {
            Token peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError("identifier or }");
                return null;
            }
            if (peek.content().equals("}")) {
                break;
            }
            ASTIdentifier identifier = ctx.literal();
            if (identifier == null)
                return null;
            if (ctx.take(":") == null)
                return null;
            ASTElement element = parse();
            if (element == null) {
                ctx.throwExpectedError("element", peek.content());
                return null;
            }
            elements.put(identifier, element);
            peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError(", or }");
                return null;
            }
            if (!peek.content().equals("}")) {
                if (ctx.take(",") == null)
                    return null;
            }
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState(State.IN_OBJECT);
        return new ASTObject(elements);
    }

    private @Nullable ASTDeclaration parseDeclaration() {
        ASTIdentifier identifier = new ASTIdentifier(ctx.takeAny());
        if (identifier.content().charAt(0) != '.') {
            ctx.throwExpectedError("identifier starting with '.'", identifier.content());
            return null;
        }
        State state = ctx.getState();
        Token peek = ctx.peek();
        if (peek == null) {
            return new ASTDeclaration(identifier, List.of());
        }
        List<ASTElement> elements = new ArrayList<>();
        while (!peek.content().startsWith(".")) {
            ASTElement element = parse();
            if (element == null) {
                return null;
            }
            elements.add(element);
            peek = ctx.peek();
            if (peek == null)
                break; // declarations are the top level elements, so we can just stop here
            if (state == State.IN_NESTED_DECLARATION_OR_ARRAY) {
                if (peek.content().equals("}") || peek.content().equals(",")) {
                    break;
                }
            }
            if ((state == State.IN_NESTED_DECLARATION) && peek.content().equals("}")) {
                break;
            } else if (state == State.IN_OBJECT) {
                // detection is a bit hacky, but it works
                // check if over next token is a : or next token is a }
                if (peek.content().equals("}") || peek.content().equals(",")) {
                    break;
                }
                Token next = ctx.peek(1);
                if (next == null) {
                    ctx.throwEofError("end of declaration");
                    return null;
                }
                if (next.content().equals(":")) {
                    break;
                }
            } else if ((state == State.IN_ARRAY) && (peek.content().equals(",") || peek.content().equals("}"))) { // arrays are a bit easier
                break;
            }
        }
        return new ASTDeclaration(identifier, elements);
    }

    private @Nullable ASTElement parseArrayOrNestedDeclaration() {
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
        if (peek.content().equals("}")) {
            ctx.take("}");
            ctx.leaveState(State.IN_NESTED_DECLARATION_OR_ARRAY);
            return new ASTDeclaration(null, elements);
        }
        if (peek.content().equals(",")) {
            ctx.take(",");
            ctx.leaveState(State.IN_NESTED_DECLARATION_OR_ARRAY);
            return parseHalfArray(elements);
        }
        ctx.leaveState(State.IN_NESTED_DECLARATION_OR_ARRAY);
        return parseHalfNestedDeclaration(elements);
    }

    private @Nullable ASTDeclaration parseNestedDeclaration() {
        if (ctx.take("{") == null)
            return null;
        return parseHalfNestedDeclaration(new ArrayList<>());
    }

    private @Nullable ASTDeclaration parseHalfNestedDeclaration(List<ASTElement> elements) {
        ctx.enterState(State.IN_NESTED_DECLARATION);
        while (true) {
            Token peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError("} or declaration");
                return null;
            }
            if (peek.content().equals("}")) {
                break;
            }
            ASTDeclaration declaration = parseDeclaration();
            if (declaration == null) {
                return null;
            }
            elements.add(declaration);
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState();
        return new ASTDeclaration(null, elements);
    }

    private @Nullable ASTCode parseCode() {
        ctx.enterState(State.IN_CODE);
        if (ctx.take("{") == null)
            return null;
        List<ASTInstruction> instructions = new ArrayList<>();
        while (true) {
            Token peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError("} or instruction");
                return null;
            }
            if (peek.content().equals("}")) {
                break;
            }
            ASTInstruction instruction = parseInstruction();
            if (instruction == null)
                return null;
            instructions.add(instruction);
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState(State.IN_CODE);
        return new ASTCode(instructions);
    }

    private @Nullable ASTInstruction parseInstruction() {
        ctx.enterState(State.IN_INSTRUCTION);
        Token instruction = ctx.takeAny();
        if (instruction == null)
            return null;
        if (instruction.type() != TokenType.IDENTIFIER) {
            ctx.throwExpectedError("instruction or label", instruction.content());
            return null;
        }
        Token peek = ctx.peek();
        if (peek == null) {
            ctx.throwEofError("instruction argument or label");
            return null;
        }
        if (peek.type() == TokenType.OPERATOR && peek.content().equals(":")) {
            ctx.leaveState(State.IN_INSTRUCTION);
            ctx.take(":");
            return new ASTLabel(new ASTIdentifier(instruction));
        }
        ASTIdentifier identifier = new ASTIdentifier(instruction);
        List<ASTElement> arguments = new ArrayList<>();
        // parse until peek is eof or on a different line
        while (peek.location().line() == instruction.location().line()) {
            ASTElement argument = parse();
            if (argument == null) {
                return null;
            }
            arguments.add(argument);
            peek = ctx.peek();
            if (peek == null) {
                break;
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
        private final DiagnosticSink diagnostics = new DiagnosticSink(DiagnosticPhase.SYNTAX);
        private int idx = 0;
        private Token latest;

        private ParserContext(DeclarationParser parser, List<Token> tokens) {
            this.parser = parser;
            this.tokens = Collections.unmodifiableList(tokens);
            this.latest = tokens.getFirst();
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
            if (done()) {
                throwEofError(exact);
                return null;
            }
            Token token = next();
            if (token.content().equals(exact)) {
                return token;
            } else {
                throwExpectedError(exact, token.content());
                return null;
            }
        }

        private Token takeAny() {
            if (done()) {
                throwEofError("any token");
                return null;
            }
            return next();
        }

        private ASTIdentifier literal() {
            if (done()) {
                throwEofError("literal");
                return null;
            }
            Token token = next();
            switch (token.type()) {
                case IDENTIFIER:
                case NUMBER:
                case STRING:
                    return new ASTIdentifier(token);
            }
            throwExpectedError("literal", token.content());
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
                if (element.type() == validType) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                diagnostics.error(DiagnosticCode.UNEXPECTED_TOKEN,
                        "Expected one of " + Arrays.toString(validTypes) + " but got " + element.type(),
                        element.value().location());
                return null;
            }
            return element;
        }

        public void throwError(DiagnosticCode code, String message, Location location) {
            diagnostics.error(code, message, location);
        }

        public void throwEofError(String expected) {
            Location location = latest == null ? new Location(-1, -1, 0, "") : latest.location();
            throwError(DiagnosticCode.UNEXPECTED_TOKEN, "Expected '" + expected + "' but got EOF", location);
        }

        public void throwExpectedError(String expected, String got) {
            if (latest == null) {
                throwEofError(expected);
                return;
            }
            throwError(DiagnosticCode.UNEXPECTED_TOKEN, "Expected '" + expected + "' but got '" + got + "'", latest.location());
        }

        public void throwUnexpectedError(String got) {
            if (latest == null) {
                throwEofError("any token");
                return;
            }
            throwError(DiagnosticCode.UNEXPECTED_TOKEN, "Unexpected token '" + got + "'", latest.location());
        }
    }
}

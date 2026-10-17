package me.darknet.assembler.parser;

import me.darknet.assembler.ast.MetaComment;
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
        if (tokens.isEmpty())
            return Outcome.success(List.of());
        this.ctx = new ParserContext(this, new ArrayList<>(tokens));
        List<@Nullable ASTElement> declarations = new ArrayList<>();
        List<MetaComment> pending = new ArrayList<>();
        ASTElement previous = null;
        while (!this.ctx.done()) {
            collectTrivia(previous, pending);
            if (this.ctx.done())
                break;
            ASTElement declaration = parseDeclaration();
            declarations.add(declaration);
            if (declaration != null) {
                applyPendingLeading(declaration, pending);
                previous = declaration;
            }
        }
        attachEnd(previous, pending);
        return Outcome.of(declarations, ctx.diagnostics.diagnostics());
    }

    private void collectTrivia(@Nullable ASTElement previous, List<MetaComment> pending) {
        routeTrivia(ctx.takeComments(), previous, pending);
    }

    private static void attachEnd(@Nullable ASTElement element, List<MetaComment> pending) {
        if (element != null)
            pending.forEach(element::addEndComment);
    }

    private static void attachEndComments(ASTElement element, List<MetaComment> comments) {
        comments.forEach(element::addEndComment);
    }

    private void routeTrivia(List<Token> comments, @Nullable ASTElement previous,
                             List<MetaComment> pending) {
        Location previousLocation = ctx.latestSyntaxLocation();
        if (previousLocation == null && previous != null)
            previousLocation = previous.location();
        for (Token comment : comments) {
            MetaComment trivia = new MetaComment(comment);
            if (previousLocation != null && previousLocation.line() == trivia.location().line()) {
                if (previous != null)
                    previous.addTrailingComment(trivia);
                else
                    pending.add(trivia);
            } else {
                pending.add(trivia);
            }
        }
    }

    private static void applyPendingLeading(ASTElement element, List<MetaComment> pending) {
        pending.forEach(element::addLeadingComment);
        pending.clear();
    }

    private static void applyPendingEnd(ASTElement element, List<MetaComment> pending) {
        attachEndComments(element, pending);
        pending.clear();
    }

    private static boolean isComment(Token token) {
        return token.type() == TokenType.COMMENT;
    }

    private boolean declarationBoundary(State state, @Nullable Token peek) {
        if (peek == null || peek.content().startsWith("."))
            return true;
        if (state == State.IN_NESTED_DECLARATION_OR_ARRAY)
            return peek.content().equals("}") || peek.content().equals(",");
        if (state == State.IN_NESTED_DECLARATION)
            return peek.content().equals("}");
        if (state == State.IN_ARRAY)
            return peek.content().equals(",") || peek.content().equals("}");
        if (state == State.IN_OBJECT) {
            if (peek.content().equals("}") || peek.content().equals(","))
                return true;
            Token next = ctx.peek(1);
            return next != null && next.content().equals(":");
        }
        return false;
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
        if (tokens.isEmpty())
            return Outcome.success(List.of());
        this.ctx = new ParserContext(this, new ArrayList<>(tokens));
        List<@Nullable ASTElement> result = new ArrayList<>();
        List<MetaComment> pending = new ArrayList<>();
        ASTElement previous = null;
        while (!this.ctx.done()) {
            collectTrivia(previous, pending);
            if (this.ctx.done())
                break;
            ASTElement element = parse();
            result.add(element);
            if (element != null) {
                applyPendingLeading(element, pending);
                previous = element;
            }
        }
        attachEnd(previous, pending);
        return Outcome.of(result, ctx.diagnostics.diagnostics());
    }

    private @Nullable ASTElement parseOperator(Token token) {
        char operator = token.content().charAt(0);
        if (operator != '{') {
            ctx.takeAny();
            ctx.throwUnexpectedError(token.content());
            return null;
        }
        if (ctx.isCurrentState(State.IN_OBJECT) && ctx.isMethodBodyObject()) {
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
        List<MetaComment> endComments = ctx.takeComments().stream().map(MetaComment::new).toList();
        ctx.take("}");
        ASTEmpty empty = new ASTEmpty(begin);
        attachEndComments(empty, endComments);
        return empty;
    }

    private @Nullable ASTArray parseArray() {
        if (ctx.take("{") == null)
            return null;
        return parseHalfArray(new ArrayList<>(), new ArrayList<>());
    }

    private @Nullable ASTArray parseHalfArray(List<ASTElement> elements, List<MetaComment> pending) {
        ctx.enterState(State.IN_ARRAY);
        ASTElement previous = elements.isEmpty() ? null : elements.getLast();
        while (true) {
            routeTrivia(ctx.takeComments(), previous, pending);
            Token peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError(", or {");
                return null;
            }
            if (peek.content().equals("}"))
                break;
            ASTElement element = parse();
            if (element == null)
                return null;
            applyPendingLeading(element, pending);
            elements.add(element);
            previous = element;
            routeTrivia(ctx.takeComments(), previous, pending);
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
        ASTArray array = new ASTArray(elements);
        applyPendingEnd(array, pending);
        return array;
    }

    private @Nullable ASTObject parseObject() {
        // The code key has special syntax only in a method's direct body object.
        // We don't want things like local variable names of 'code' to be treated as code blocks...
        ctx.enterObject(ctx.isDirectDeclarationElement() && ctx.isCurrentDeclaration(".method"));
        try {
            ctx.enterState(State.IN_OBJECT);
            if (ctx.take("{") == null)
                return null;
            ElementMap<ASTIdentifier, ASTElement> elements = new ElementMap<>();
            List<MetaComment> pending = new ArrayList<>();
            ASTElement previous = null;
            while (true) {
                routeTrivia(ctx.takeComments(), previous, pending);
                Token peek = ctx.peek();
                if (peek == null) {
                    ctx.throwEofError("identifier or }");
                    return null;
                }
                if (peek.content().equals("}"))
                    break;
                ASTIdentifier identifier = ctx.literal();
                if (identifier == null)
                    return null;
                applyPendingLeading(identifier, pending);
                routeTrivia(ctx.takeComments(), identifier, pending);
                if (ctx.take(":") == null)
                    return null;
                routeTrivia(ctx.takeComments(), identifier, pending);
                ASTElement element = parse();
                if (element == null) {
                    ctx.throwExpectedError("element", peek.content());
                    return null;
                }
                applyPendingLeading(element, pending);
                elements.put(identifier, element);
                previous = element;
                routeTrivia(ctx.takeComments(), previous, pending);
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
            ASTObject object = new ASTObject(elements);
            applyPendingEnd(object, pending);
            return object;
        } finally {
            ctx.leaveObject();
        }
    }

    private @Nullable ASTDeclaration parseDeclaration() {
        ASTIdentifier identifier = new ASTIdentifier(ctx.takeAny());
        if (identifier.content().charAt(0) != '.') {
            ctx.throwExpectedError("identifier starting with '.'", identifier.content());
            return null;
        }
        ctx.enterDeclaration(identifier.content());
        try {
            State state = ctx.getState();
            List<ASTElement> elements = new ArrayList<>();
            List<MetaComment> pending = new ArrayList<>();
            ASTElement previous = identifier;
            while (true) {
                Token peek = ctx.peek();
                if (declarationBoundary(state, peek))
                    break;
                routeTrivia(ctx.takeComments(), previous, pending);
                ctx.enterDeclarationElement();
                ASTElement element;
                try {
                    element = parse();
                } finally {
                    ctx.leaveDeclarationElement();
                }
                if (element == null)
                    return null;
                applyPendingLeading(element, pending);
                elements.add(element);
                previous = element;
            }
            return new ASTDeclaration(identifier, elements);
        } finally {
            ctx.leaveDeclaration();
        }
    }

    private @Nullable ASTElement parseArrayOrNestedDeclaration() {
        if (ctx.take("{") == null)
            return null;
        ctx.enterState(State.IN_NESTED_DECLARATION_OR_ARRAY);
        List<MetaComment> pending = new ArrayList<>();
        routeTrivia(ctx.takeComments(), null, pending);
        ASTElement element = parse();
        if (element == null)
            return null;
        applyPendingLeading(element, pending);
        Token peek = ctx.peek();
        if (peek == null) {
            ctx.throwEofError(", or } or element");
            return null;
        }
        List<ASTElement> elements = new ArrayList<>();
        elements.add(element);
        if (peek.content().equals("}")) {
            routeTrivia(ctx.takeComments(), element, pending);
            ctx.take("}");
            ctx.leaveState(State.IN_NESTED_DECLARATION_OR_ARRAY);
            ASTDeclaration declaration = new ASTDeclaration(null, elements);
            applyPendingEnd(declaration, pending);
            return declaration;
        }
        if (peek.content().equals(",")) {
            routeTrivia(ctx.takeComments(), element, pending);
            ctx.take(",");
            ctx.leaveState(State.IN_NESTED_DECLARATION_OR_ARRAY);
            return parseHalfArray(elements, pending);
        }
        ctx.leaveState(State.IN_NESTED_DECLARATION_OR_ARRAY);
        return parseHalfNestedDeclaration(elements, pending);
    }

    private @Nullable ASTDeclaration parseNestedDeclaration() {
        if (ctx.take("{") == null)
            return null;
        return parseHalfNestedDeclaration(new ArrayList<>(), new ArrayList<>());
    }

    private @Nullable ASTDeclaration parseHalfNestedDeclaration(List<ASTElement> elements,
                                                                 List<MetaComment> pending) {
        ctx.enterState(State.IN_NESTED_DECLARATION);
        ASTElement previous = elements.isEmpty() ? null : elements.getLast();
        while (true) {
            routeTrivia(ctx.takeComments(), previous, pending);
            Token peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError("} or declaration");
                return null;
            }
            if (peek.content().equals("}"))
                break;
            ASTDeclaration declaration = parseDeclaration();
            if (declaration == null)
                return null;
            applyPendingLeading(declaration, pending);
            elements.add(declaration);
            previous = declaration;
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState();
        ASTDeclaration body = new ASTDeclaration(null, elements);
        applyPendingEnd(body, pending);
        return body;
    }

    private @Nullable ASTCode parseCode() {
        ctx.enterState(State.IN_CODE);
        if (ctx.take("{") == null)
            return null;
        List<ASTInstruction> instructions = new ArrayList<>();
        List<MetaComment> pending = new ArrayList<>();
        ASTElement previous = null;
        while (true) {
            routeTrivia(ctx.takeComments(), previous, pending);
            Token peek = ctx.peek();
            if (peek == null) {
                ctx.throwEofError("} or instruction");
                return null;
            }
            if (peek.content().equals("}"))
                break;
            ASTInstruction instruction = parseInstruction();
            if (instruction == null)
                return null;
            applyPendingLeading(instruction, pending);
            instructions.add(instruction);
            previous = instruction;
        }
        if (ctx.take("}") == null)
            return null;
        ctx.leaveState(State.IN_CODE);
        ASTCode code = new ASTCode(instructions);
        applyPendingEnd(code, pending);
        return code;
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
        if (peek == null && ctx.peekRaw() == null) {
            ctx.throwEofError("instruction argument or label");
            return null;
        }
        if (peek != null && peek.type() == TokenType.OPERATOR && peek.content().equals(":")) {
            ctx.leaveState(State.IN_INSTRUCTION);
            ASTLabel label = new ASTLabel(new ASTIdentifier(instruction));
            List<MetaComment> pending = new ArrayList<>();
            routeTrivia(ctx.takeComments(), label, pending);
            applyPendingLeading(label, pending);
            ctx.take(":");
            return label;
        }
        ASTIdentifier identifier = new ASTIdentifier(instruction);
        List<ASTElement> arguments = new ArrayList<>();
        Token raw = ctx.peekRaw();
        while (raw != null && !isComment(raw) && raw.location().line() == instruction.location().line()) {
            ASTElement argument = parse();
            if (argument == null)
                return null;
            arguments.add(argument);
            raw = ctx.peekRaw();
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
        private final LinkedList<Boolean> objectRoles = new LinkedList<>();
        private final LinkedList<String> declarations = new LinkedList<>();
        private final LinkedList<State> declarationElementStates = new LinkedList<>();
        private int idx = 0;
        private Token latest;
        private Token latestSyntax;

        private ParserContext(DeclarationParser parser, List<Token> tokens) {
            this.parser = parser;
            this.tokens = Collections.unmodifiableList(tokens);
            this.latest = tokens.getFirst();
        }

        private void enterObject(boolean methodBody) {
            objectRoles.push(methodBody);
        }

        private void leaveObject() {
            objectRoles.poll();
        }

        private boolean isMethodBodyObject() {
            return !objectRoles.isEmpty() && objectRoles.peek();
        }

        private void enterDeclaration(String keyword) {
            declarations.push(keyword);
        }

        private void leaveDeclaration() {
            declarations.poll();
        }

        private boolean isCurrentDeclaration(String keyword) {
            return keyword.equals(declarations.peek());
        }

        private void enterDeclarationElement() {
            declarationElementStates.push(getState());
        }

        private void leaveDeclarationElement() {
            declarationElementStates.poll();
        }

        private boolean isDirectDeclarationElement() {
            return !declarationElementStates.isEmpty() && getState() == declarationElementStates.peek();
        }

        private boolean done() {
            return idx >= tokens.size();
        }

        private Token next() {
            latest = tokens.get(idx++);
            if (latest.type() != TokenType.COMMENT)
                latestSyntax = latest;
            return latest;
        }

        private @Nullable Location latestSyntaxLocation() {
            return latestSyntax == null ? null : latestSyntax.location();
        }

        private Token peekRaw() {
            return idx < tokens.size() ? tokens.get(idx) : null;
        }

        private List<Token> takeComments() {
            List<Token> comments = new ArrayList<>();
            while (peekRaw() != null && peekRaw().type() == TokenType.COMMENT)
                comments.add(next());
            return comments;
        }

        private Token peek() {
            return peek(0);
        }

        private Token peek(int offset) {
            if (offset < 0) {
                int listIdx = idx - 1;
                int remaining = -offset;
                while (listIdx >= 0) {
                    Token token = tokens.get(listIdx--);
                    if (token.type() == TokenType.COMMENT)
                        continue;
                    if (--remaining == 0)
                        return token;
                }
                return null;
            }
            int listIdx = idx;
            int remaining = offset;
            while (listIdx < tokens.size()) {
                Token token = tokens.get(listIdx++);
                if (token.type() == TokenType.COMMENT)
                    continue;
                if (remaining-- == 0)
                    return token;
            }
            return null;
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

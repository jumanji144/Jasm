package me.darknet.assembler.parser;

import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Tokenizer {

    public static boolean isOperator(char c) {
        return c == '{' || c == '}' || c == ':' || c == ',';
    }

    public static boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isHex(char c) {
        return isNumber(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static boolean isNumberSuffix(char c) {
        return c == 'f' || c == 'F' || c == 'l' || c == 'L' || c == 'd' || c == 'D';
    }

    public static boolean isExponent(char c) {
        return c == 'E' || c == 'e';
    }

    public static boolean isNumberContinuation(char c) {
        return c == '.' || isExponent(c);
    }

    private void handleComment(TokenizerContext ctx) {
        while (ctx.hasCurrent()) {
            char currentChar = ctx.current();
            if (currentChar == '\n') {
                break;
            }
            ctx.forward();
        }
        ctx.collectToken();
        ctx.leaveComment();
        if (ctx.hasCurrent() && ctx.current() == '\n') {
            ctx.next();
        }
    }

    private void handleMultiLineComment(TokenizerContext ctx) {
        while (ctx.hasCurrent()) {
            char currentChar = ctx.current();
            Character nextChar = ctx.peek(1);
            if (currentChar == '*' && nextChar != null && nextChar == '/') {
                ctx.next();
                ctx.next();
                ctx.collectToken();
                ctx.leaveMultilineComment();
                return;
            }
            ctx.forward();
        }
    }

    private void handleString(TokenizerContext ctx) {
        if (!ctx.hasCurrent()) {
            return;
        }
        char currentChar = ctx.current();
        switch (currentChar) {
            case '"' -> {
                ctx.collectToken();
                ctx.leaveString();
                ctx.next();
            }
            case '\\' -> {
                ctx.next();
                if (ctx.processEscape()) {
                    ctx.leaveString();
                }
            }
            case '\n' -> {
                ctx.throwError("Unterminated string");
                ctx.discardToken();
                ctx.leaveString();
                ctx.next();
            }
            default -> ctx.forward();
        }
    }

    private void handleWhitespace(TokenizerContext ctx) {
        ctx.collectToken();
        ctx.next();
    }

    private void handleNormal(TokenizerContext ctx) {
        char currentChar = ctx.current();
        if (currentChar == '/') {
            Character nextChar = ctx.peek(1);
            if (nextChar == null) {
                ctx.collectToken();
                ctx.markTokenStart();
                ctx.throwError("Unexpected trailing '/'");
                ctx.discardToken();
                ctx.next();
                return;
            }
            if (nextChar == '/' || nextChar == '*') {
                ctx.collectToken();
                ctx.next();
                ctx.next();
                if (nextChar == '/') {
                    ctx.enterComment();
                } else {
                    ctx.enterMultilineComment();
                }
                return;
            }
        }
        if (currentChar == '"') {
            ctx.collectToken();
            ctx.next();
            ctx.enterString();
        } else if (currentChar == '\'') {
            ctx.collectToken();
            ctx.next();
            ctx.enterCharacter();
        } else if (isOperator(currentChar)) {
            ctx.collectToken();
            ctx.forward();
            ctx.collectToken();
        } else {
            ctx.forward();
        }
    }

    private void handleCharacter(TokenizerContext ctx) {
        if (!ctx.hasCurrent()) {
            return;
        }
        char currentChar = ctx.current();
        switch (currentChar) {
            case '\'' -> {
                ctx.validateCharacterLiteral();
                ctx.collectToken();
                ctx.leaveCharacter();
                ctx.next();
            }
            case '\\' -> {
                ctx.next();
                if (ctx.processEscape()) {
                    ctx.leaveCharacter();
                }
            }
            case '\n' -> {
                ctx.throwError("Unterminated character");
                ctx.discardToken();
                ctx.leaveCharacter();
                ctx.next();
            }
            default -> ctx.forward();
        }
    }

    public Result<List<Token>> tokenize(String source, String input) {
        TokenizerContext ctx = new TokenizerContext();
        ctx.input = input;
        ctx.buffer = new StringBuilder();
        ctx.source = source;
        while (ctx.hasCurrent()) {
            if (ctx.isComment()) {
                handleComment(ctx);
            } else if (ctx.isMultilineComment()) {
                handleMultiLineComment(ctx);
            } else if (ctx.isString()) {
                handleString(ctx);
            } else if (ctx.isCharacter()) {
                handleCharacter(ctx);
            } else if (Character.isWhitespace(ctx.current())) {
                handleWhitespace(ctx);
            } else {
                handleNormal(ctx);
            }
        }

        if (ctx.isComment()) {
            ctx.collectToken();
            ctx.leaveComment();
        } else if (ctx.isMultilineComment()) {
            ctx.throwError("Unterminated multiline comment");
            ctx.discardToken();
            ctx.leaveMultilineComment();
        } else if (ctx.isString()) {
            ctx.throwError("Unterminated string");
            ctx.discardToken();
            ctx.leaveString();
        } else if (ctx.isCharacter()) {
            ctx.throwError("Unterminated character");
            ctx.discardToken();
            ctx.leaveCharacter();
        }

        ctx.collectToken();

        return new Result<>(ctx.tokens, ctx.errors.getErrors(), ctx.errors.getWarns());
    }

    private static class TokenizerContext {

        private int line = 1;
        private int column = 1;
        private int index;
        private int tokenStartIndex = -1;
        private int tokenStartLine = -1;
        private int tokenStartColumn = -1;
        private boolean tokenInvalid;
        private boolean inString;
        private boolean inCharacter;
        private boolean inMultilineComment;
        private boolean inComment;
        private StringBuilder buffer;
        private final ErrorCollector errors = new ErrorCollector();
        private final List<Token> tokens = new ArrayList<>();

        private String input, source;

        public boolean hasCurrent() {
            return index < input.length();
        }

        public char current() {
            return input.charAt(index);
        }

        public Character peek(int offset) {
            int peekIndex = index + offset;
            if (peekIndex < 0 || peekIndex >= input.length()) {
                return null;
            }
            return input.charAt(peekIndex);
        }

        public void markTokenStart() {
            if (tokenStartIndex < 0) {
                tokenStartIndex = index;
                tokenStartLine = line;
                tokenStartColumn = column;
            }
        }

        public void forward() {
            markTokenStart();
            buffer.append(current());
            next();
        }

        public void next() {
            if (!hasCurrent()) {
                return;
            }
            char currentChar = current();
            index++;
            if (currentChar == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        public void enterComment() {
            markTokenStart();
            inComment = true;
        }

        public void enterMultilineComment() {
            markTokenStart();
            inMultilineComment = true;
        }

        public void leaveComment() {
            inComment = false;
        }

        public void leaveMultilineComment() {
            inMultilineComment = false;
        }

        public void enterString() {
            markTokenStart();
            inString = true;
        }

        public void leaveString() {
            inString = false;
        }

        public void enterCharacter() {
            markTokenStart();
            inCharacter = true;
        }

        public void leaveCharacter() {
            inCharacter = false;
        }

        public boolean isString() {
            return inString;
        }

        public boolean isCharacter() {
            return inCharacter;
        }

        public boolean isComment() {
            return inComment;
        }

        public boolean isMultilineComment() {
            return inMultilineComment;
        }

        public void throwError(String message) {
            errors.addError(message, new Location(line, column, 0, source));
        }

        public void throwError(String message, int errorLine, int errorColumn) {
            errors.addError(message, new Location(errorLine, errorColumn, 0, source));
        }

        private static final String DECIMAL_DIGITS = "\\d(?:[\\d_]*\\d)?";
        private static final String HEX_DIGITS = "[\\dA-Fa-f](?:[\\dA-Fa-f_]*[\\dA-Fa-f])?";
        private static final String BINARY_DIGITS = "[01](?:[01_]*[01])?";
        static final Pattern NUMBER_PATTERN = Pattern.compile(
                "-?(?:"
                        + "(?:"
                        + DECIMAL_DIGITS + "\\.(?:" + DECIMAL_DIGITS + ")?(?:[eE]-?" + DECIMAL_DIGITS + ")?"
                        + "|\\." + DECIMAL_DIGITS + "(?:[eE]-?" + DECIMAL_DIGITS + ")?"
                        + "|" + DECIMAL_DIGITS + "[eE]-?" + DECIMAL_DIGITS
                        + "|0[xX]" + HEX_DIGITS + "(?:\\." + HEX_DIGITS + ")?[pP]-?" + DECIMAL_DIGITS
                        + ")[fFdD]?"
                        + "|0[xX]" + HEX_DIGITS + "[Ll]?"
                        + "|0[bB]" + BINARY_DIGITS + "[Ll]?"
                        + "|" + DECIMAL_DIGITS + "[LlFfDd]?"
                        + ")"
        );

        private static final Pattern MALFORMED_HEX_FLOAT_PATTERN = Pattern.compile("-?0[xX].*[pP].*");

        boolean checkIfNumber(String content) {
            return NUMBER_PATTERN.matcher(content).matches();
        }

        public TokenType getType(String content) {
            if (content.length() == 1 && isOperator(content.charAt(0))) {
                return TokenType.OPERATOR;
            }
            return checkIfNumber(content) ? TokenType.NUMBER : TokenType.IDENTIFIER;
        }

        public void collectToken() {
            boolean specialToken = inString || inCharacter || inComment || inMultilineComment;
            if (!specialToken && buffer.isEmpty()) {
                discardToken();
                return;
            }
            if (tokenInvalid) {
                discardToken();
                return;
            }

            String content = buffer.toString();
            int startIndex = tokenStartIndex >= 0 ? tokenStartIndex : index;
            int startLine = tokenStartLine >= 0 ? tokenStartLine : line;
            int startColumn = tokenStartColumn >= 0 ? tokenStartColumn : column;
            Range range = new Range(startIndex, index);
            Location location = new Location(startLine, startColumn, Math.max(0, index - startIndex), source);

            if (inString) {
                tokens.add(new Token(range, location, TokenType.STRING, content));
            } else if (inCharacter) {
                tokens.add(new Token(range, location, TokenType.CHARACTER, content));
            } else if (inComment || inMultilineComment) {
                tokens.add(new Token(range, location, TokenType.COMMENT, content));
            } else {
                TokenType type = getType(content);
                if (type == TokenType.IDENTIFIER && MALFORMED_HEX_FLOAT_PATTERN.matcher(content).matches()) {
                    errors.addError("Invalid hexadecimal floating-point literal", location);
                    discardToken();
                    return;
                }
                tokens.add(new Token(range, location, type, content));
            }

            discardToken();
        }

        public void discardToken() {
            buffer.setLength(0);
            tokenStartIndex = -1;
            tokenStartLine = -1;
            tokenStartColumn = -1;
            tokenInvalid = false;
        }

        public void validateCharacterLiteral() {
            if (buffer.length() != 1) {
                errors.addError("Character literal must contain exactly one character",
                        new Location(tokenStartLine, tokenStartColumn, Math.max(0, index - tokenStartIndex), source));
                tokenInvalid = true;
            }
        }

        /**
         * @return {@code true} when the current string/character token should be aborted immediately.
         */
        public boolean processEscape() {
            int escapeLine = line;
            int escapeColumn = Math.max(1, column - 1);
            if (!hasCurrent()) {
                throwError("Incomplete escape sequence", escapeLine, escapeColumn);
                tokenInvalid = true;
                return true;
            }

            char escapeChar = current();
            next();
            switch (escapeChar) {
                case 'n' -> buffer.append('\n');
                case 'r' -> buffer.append('\r');
                case 't' -> buffer.append('\t');
                case 'b' -> buffer.append('\b');
                case 'f' -> buffer.append('\f');
                case '"' -> buffer.append('"');
                case '\'' -> buffer.append('\'');
                case '\\' -> buffer.append('\\');
                case 'u' -> {
                    int value = 0;
                    int digits = 0;
                    while (digits < 4 && hasCurrent()) {
                        char digit = current();
                        if (!isHex(digit)) {
                            break;
                        }
                        value = (value << 4) + Character.digit(digit, 16);
                        next();
                        digits++;
                    }
                    if (digits != 4) {
                        throwError("Invalid unicode escape", escapeLine, escapeColumn);
                        if (!hasCurrent()) {
                            tokenInvalid = true;
                            return true;
                        }
                        tokenInvalid = true;
                        return false;
                    }
                    buffer.append((char) value);
                }
                default -> {
                    throwError("Invalid escape sequence", escapeLine, escapeColumn);
                    tokenInvalid = true;
                }
            }
            return false;
        }

    }

}

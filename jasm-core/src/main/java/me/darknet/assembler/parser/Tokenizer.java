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

    private void handleComment(TokenizerContext ctx, char currentChar) {
        if (currentChar == '\n') {
            ctx.collectToken();
            ctx.enterComment();
            ctx.nextLine();
            ctx.next();
        }
    }

    private void handleString(TokenizerContext ctx, char currentChar) {
        switch (currentChar) {
            case '"' -> {
                ctx.collectToken();
                ctx.leaveString();
                ctx.next();
            }
            case '\\' -> {
                ctx.next();
                ctx.processEscape();
            }
            case '\n' -> {
                ctx.collectToken();
                ctx.throwError("Unterminated string");
                ctx.leaveString();
                ctx.nextLine();
                ctx.next();
            }
            default -> ctx.forward();
        }
    }

    private void handleWhitespace(TokenizerContext ctx, char currentChar) {
        // always collect the token
        ctx.collectToken();
        if (currentChar == '\n') {
            ctx.nextLine();
        }
        ctx.next();
    }

    private void handleNormal(TokenizerContext ctx, char currentChar) {
        if (currentChar == '/' && ctx.peek() == '/') {
            ctx.next();
            ctx.next();
            ctx.enterComment();
        } else if (currentChar == '"') {
            ctx.next();
            ctx.enterString();
        } else if (currentChar == '\'') {
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

    private void handleCharacter(TokenizerContext ctx, char currentChar) {
        switch (currentChar) {
            case '\'' -> {
                ctx.collectToken();
                ctx.leaveCharacter();
                ctx.next();
            }
            case '\\' -> {
                ctx.next();
                ctx.processEscape();
            }
            case '\n' -> {
                ctx.collectToken();
                ctx.throwError("Unterminated character");
                ctx.leaveCharacter();
                ctx.nextLine();
                ctx.next();
            }
            default -> ctx.forward();
        }
    }

    public Result<List<Token>> tokenize(String source, String input) {
        TokenizerContext ctx = new TokenizerContext();
        ctx.input = input;
        ctx.buffer = new StringBuffer();
        ctx.source = source;
        int length = input.length();
        while (ctx.index < length) {
            char c = input.charAt(ctx.index);
            if (ctx.isComment()) {
                handleComment(ctx, c);
            }
            if (ctx.isString()) {
                handleString(ctx, c);
            } else if(ctx.isCharacter()) {
                handleCharacter(ctx, c);
            } else if (Character.isWhitespace(c)) {
                handleWhitespace(ctx, c);
            } else {
                handleNormal(ctx, c);
            }
        }

        ctx.collectToken();

        return new Result<>(ctx.tokens, ctx.errors.getErrors());
    }

    private static class TokenizerContext {

        private int line = 1;
        private int column = 1;
        private int index;
        private boolean inString;
        private boolean inCharacter;
        private boolean inComment;
        private StringBuffer buffer;
        private final ErrorCollector errors = new ErrorCollector();
        private final List<Token> tokens = new ArrayList<>();

        private String input, source;

        public void forward() {
            buffer.append(input.charAt(index));
            next();
        }

        public void nextLine() {
            line++;
            column = 1;
        }

        public void next() {
            index++;
            column++;
        }

        public void enterComment() {
            inComment = true;
        }

        public void leaveComment() {
            inComment = false;
        }

        public void enterString() {
            inString = true;
        }

        public void leaveString() {
            inString = false;
        }

        public void enterCharacter() {
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

        public char peek() {
            return input.charAt(index + 1);
        }

        public void throwError(String message) {
            errors.addError(message, new Location(line, column, source));
        }

        static final Pattern NUMBER_PATTERN = Pattern.compile(
                "-?(?:(?:(?:(?:(?:\\d[\\d_]*\\.(?:\\d[\\d_]*)?([eE]-?\\d[\\d_]*)?)|(?:\\.(?:\\d[\\d_]*)(?:[eE]-?\\d[\\d_]*)?)|(?:(?:\\d[\\d_]*)(?:[eE]-?\\d[\\d_]*))|(?:0[xX][\\dA-Fa-f_]*(\\.[\\dA-Fa-f_]*)?[pP]-?\\d[\\d_]*))[fFdD]?)|(?:(?:(?:0[xX][\\dA-fa-f_]+)|(?:\\d[\\d_]*))[LlFfDd]?)))"
        );

        boolean checkIfNumber(String content) {
            switch (content.toLowerCase()) { // floating point numbers
                case "nan", "infinity", "+infinity", "-infinity" -> {
                    return true;
                }
            }
            // note: in this case, a regex is easier to implement than a state machine
            return NUMBER_PATTERN.matcher(content).matches();
        }

        public TokenType getType(String content) {
            if (content.length() == 1) {
                if (isOperator(content.charAt(0)))
                    return TokenType.OPERATOR;
            }
            TokenType type = TokenType.IDENTIFIER;
            // check if all the characters in the token are digits (and the '-' sign)
            if (checkIfNumber(content))
                type = TokenType.NUMBER;
            return type;
        }

        public void collectToken() {

            String content = buffer.toString();
            Range range = new Range(index - content.length(), index);
            Location location = new Location(line, column, source);

            if (inString) {
                tokens.add(new Token(range, location, TokenType.STRING, content));
            } else if(inCharacter) {
                tokens.add(new Token(range, location, TokenType.CHARACTER, content));
            } else if (!buffer.isEmpty()) {
                TokenType type = getType(content);
                tokens.add(new Token(range, location, type, content));
            }

            // clear buffer
            buffer.setLength(0); // reset buffer
        }

        public void processEscape() {
            switch (input.charAt(index++)) {
                case 'n' -> buffer.append('\n');
                case 'r' -> buffer.append('\r');
                case 't' -> buffer.append('\t');
                case 'b' -> buffer.append('\b');
                case 'f' -> buffer.append('\f');
                case '"' -> buffer.append('"');
                case '\'' -> buffer.append('\'');
                case 'u' -> {
                    buffer.append((char) Integer.parseInt(input.substring(index, index + 4), 16));
                    index += 4;
                }
                default -> buffer.append('\\');
            }
        }

    }

}

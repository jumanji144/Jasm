package me.darknet.assembler.parser;

import java.util.ArrayList;
import java.util.List;

import static me.darknet.assembler.parser.Token.TokenType.*;

public class TokenizerContext {
    private final Keywords keywords;
    private final char[] stream;
    final Location currentLocation;
    int i = 0;
    final List<Token> tokens = new ArrayList<>();

    public TokenizerContext(Keywords keywords, char[] stream, Location currentLocation) {
        this.keywords = keywords;
        this.stream = stream;
        this.currentLocation = currentLocation;
    }

    public void add(Token token) {
        tokens.add(token);
    }

    public char next() {
        char c = stream[i];
        currentLocation.column++;
        if (c == '\n') {
            currentLocation.line++;
            currentLocation.column = 1;
        }
        currentLocation.position = i;
        i++;
        return c;
    }

    public boolean canRead() {
        return i < stream.length;
    }

    public char peek() {
        return stream[i];
    }

    public void finishToken(String content, Location location) {
        // special case '.expr' keyword
        if (keywords.match(Keyword.KEYWORD_EXPR, content)) {
            add(new Token(content, location.sub(content.length()), KEYWORD));
            StringBuilder sb = new StringBuilder();
            StringBuilder expr = new StringBuilder();
            while(true) {
                char c = next();
                if(c == '\n' || c == ' ' || c == '\t'){
                    expr.append(sb).append(c);
                    sb = new StringBuilder();
                    continue;
                }
                sb.append(c);
                if(keywords.match(Keyword.KEYWORD_END, sb.toString())){
                    i++;
                    location.column++;
                    break;
                }
            }
            location.position = i;
            add(new Token(expr.toString(), location.sub(expr.length() + keywords.toString(Keyword.KEYWORD_END).length()), TEXT));
            return;
        }
        // determine the type of token
        Token.TokenType type = IDENTIFIER;

        // check if all the characters in the token are digits (and the '-' sign)
        boolean isNumber = true;
        boolean numberAppeared = false;
        boolean isHex = false;
        for (int j = 0; j < content.length(); j++) {
            char c2 = content.charAt(j);
            if (c2 == '-') {
                if (j == 0)
                    continue;
                else
                    if(content.charAt(j - 1) == 'E')
                        continue;
            }
            if(c2 < '0' || c2 > '9') { // is not number
                if(numberAppeared) { // check if there was a number before
                    if(c2 == 'x') { // hex number
                        isHex = true; // toggle state
                        continue;
                    }
                    if (c2 != '.'
                            && c2 != 'f'
                            && c2 != 'F'
                            && c2 != 'L'
                            && c2 != 'D'
                            && c2 != 'E') { // is not one of the suffixes
                        if(!isHex) { // if not hex, then it is not a number
                            isNumber = false;
                            break;
                        } else { // if hex check if it is a valid hex number
                            if(c2 < 'a' || c2 > 'f') { // lowercase
                                if(c2 < 'A' || c2 > 'F') { // uppercase
                                    isNumber = false;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    isNumber = false;
                    break;
                }
            }
            numberAppeared = true;
        }
        if (isNumber) {
            type = NUMBER;
        } else {
            // check if the token is a keyword
            if (keywords.fromString(content) != null) {
                type = KEYWORD;
            }
        }
        add(new Token(content, location.sub(content.length()), type));
    }

}

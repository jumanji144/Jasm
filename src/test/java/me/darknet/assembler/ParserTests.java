package me.darknet.assembler;

import me.darknet.assembler.parser.Keywords;
import me.darknet.assembler.parser.Parser;
import me.darknet.assembler.parser.Token;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static me.darknet.assembler.parser.Token.TokenType;

public class ParserTests {
    private static Parser parser;
    private static Parser altParser;

    @BeforeAll
    public static void setup() {
        parser = new Parser();
        altParser = new Parser(new Keywords("."));
    }

    @Nested
    class Default {
        @Test
        public void testParserTokenizeClass() {
            String input = "class Test extends java/lang/Object";

            List<Token> tokens = parser.tokenize("stdin", input);

            assertEquals(4, tokens.size());

            assertEquals(TokenType.KEYWORD, tokens.get(0).getType(), "class");
            assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType(), "class's name");
            assertEquals(TokenType.KEYWORD, tokens.get(2).getType(), "extends");
            assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType(), "extended name");
        }

        @Test
        public void testParserTokenizeField() {
            String input = "field public number I";

            List<Token> tokens = parser.tokenize("stdin", input);

            assertEquals(4, tokens.size());

            assertEquals(TokenType.KEYWORD, tokens.get(0).getType(), "field");
            assertEquals(TokenType.KEYWORD, tokens.get(1).getType(), "access modifier");
            assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType(), "field name");
            assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType(), "field desc");
        }

        @Test
        public void testParserTokenizeMethod() {
            String input = "method public static main([Ljava/lang/String; args)V";

            List<Token> tokens = parser.tokenize("stdin", input);

            assertEquals(5, tokens.size());

            assertEquals(TokenType.KEYWORD, tokens.get(0).getType(), "method");
            assertEquals(TokenType.KEYWORD, tokens.get(1).getType(), "access modifier 1");
            assertEquals(TokenType.KEYWORD, tokens.get(2).getType(), "access modifier 2");
            assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType(), "method declaration");
            assertEquals(TokenType.IDENTIFIER, tokens.get(4).getType(), "method declaration");
        }
    }

    @Nested
    class AltPrefix {
        @Test
        public void testParserTokenizeClass() {
            String input = ".class Test .extends java/lang/Object";

            List<Token> tokens = altParser.tokenize("stdin", input);

            assertEquals(4, tokens.size());

            assertEquals(TokenType.KEYWORD, tokens.get(0).getType(), "class");
            assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType(), "class's name");
            assertEquals(TokenType.KEYWORD, tokens.get(2).getType(), "extends");
            assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType(), "extended name");
        }

        @Test
        public void testParserTokenizeField() {
            String input = ".field .public number I";

            List<Token> tokens = altParser.tokenize("stdin", input);

            assertEquals(4, tokens.size());

            assertEquals(TokenType.KEYWORD, tokens.get(0).getType(), "field");
            assertEquals(TokenType.KEYWORD, tokens.get(1).getType(), "access modifier");
            assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType(), "field name");
            assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType(), "field desc");
        }

        @Test
        public void testParserTokenizeMethod() {
            String input = ".method .public .static main([Ljava/lang/String; args)V";

            List<Token> tokens = altParser.tokenize("stdin", input);

            assertEquals(5, tokens.size());

            assertEquals(TokenType.KEYWORD, tokens.get(0).getType(), "method");
            assertEquals(TokenType.KEYWORD, tokens.get(1).getType(), "access modifier 1");
            assertEquals(TokenType.KEYWORD, tokens.get(2).getType(), "access modifier 2");
            assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType(), "method declaration");
            assertEquals(TokenType.IDENTIFIER, tokens.get(4).getType(), "method declaration");
        }
    }
}

package me.darknet.assembler;

import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.parser.Tokenizer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

public class TokenizerTest {

    @Test
    public void testStringTokenizer() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", "{ \"Hello World\", type: \"java/lang/HelloWorld\" }").get();
        Assertions.assertNotNull(tokens);
        Assertions.assertEquals(7, tokens.size());
        Assertions.assertEquals("{", tokens.get(0).content());
        Assertions.assertEquals("Hello World", tokens.get(1).content());
        Assertions.assertEquals(",", tokens.get(2).content());
        Assertions.assertEquals("type", tokens.get(3).content());
        Assertions.assertEquals(":", tokens.get(4).content());
        Assertions.assertEquals("java/lang/HelloWorld", tokens.get(5).content());
        Assertions.assertEquals("}", tokens.get(6).content());
    }

    @Test
    public void testStringEscaping() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", "\"Hello \\u0020World\\\"\\\\").get();
        Assertions.assertNotNull(tokens);
        Assertions.assertEquals(1, tokens.size());
        Assertions.assertEquals("Hello  World\"\\", tokens.getFirst().content());
    }

    @Test
    public void testNumbers() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer
                .tokenize("<stdin>", "0 -10 10f 10.16F 10.161616D 10L 0xDEADBEEF 0E10 0.3e10f 6.02214076e23").get();
        Assertions.assertNotNull(tokens);
        Assertions.assertEquals(10, tokens.size());
        Assertions.assertEquals("0", tokens.get(0).content());
        Assertions.assertEquals("-10", tokens.get(1).content());
        Assertions.assertEquals("10f", tokens.get(2).content());
        Assertions.assertEquals("10.16F", tokens.get(3).content());
        Assertions.assertEquals("10.161616D", tokens.get(4).content());
        Assertions.assertEquals("10L", tokens.get(5).content());
        Assertions.assertEquals("0xDEADBEEF", tokens.get(6).content());
        Assertions.assertEquals("0E10", tokens.get(7).content());
        Assertions.assertEquals("0.3e10f", tokens.get(8).content());
        Assertions.assertEquals("6.02214076e23", tokens.get(9).content());
        for (Token token : tokens) {
            Assertions.assertSame(TokenType.NUMBER, token.type());
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = { ".class public java/lang/HelloWorld",
                    ".visible-annotation Annotate {\n\tvalue: \"java/lang/HelloWorld\", \n\ttype: .enum java/lang/TargetType, METHOD\n}",
                    ".method add (II)I { \n" + "\t.parameters: {a, b}\n" + "\t.code: {\n" + "\t\tiload a\n"
                            + "\t\tiload b\n" + "\t\tiadd\n" + "\t\tireturn\t\n" + "\t}\n" + "}" }
    )
    public void testTokenizer(String input) {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", input).get();
        Assertions.assertNotNull(tokens);
        Assertions.assertFalse(tokens.isEmpty());
    }

    @Test
    public void testSingleLineComment() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", "// This is a comment\n").get();
        Assertions.assertNotNull(tokens);
        Assertions.assertEquals(1, tokens.size());
        Assertions.assertEquals(" This is a comment", tokens.getFirst().content());
        Assertions.assertSame(TokenType.COMMENT, tokens.getFirst().type());
    }

    @Test
    public void testMultiLineComment() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", "/* This is a comment\n * with multiple lines\n */").get();
        Assertions.assertNotNull(tokens);
        Assertions.assertEquals(1, tokens.size());
        Assertions.assertEquals(" This is a comment\n * with multiple lines\n ", tokens.getFirst().content());
        Assertions.assertSame(TokenType.COMMENT, tokens.getFirst().type());
    }

}

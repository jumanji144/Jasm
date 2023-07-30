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
        List<Token> tokens = tokenizer.tokenize("<stdin>", "{ \"Hello World\", type: \"java/lang/HelloWorld\" }");
        Assertions.assertNotNull(tokens);
        Assertions.assertEquals(7, tokens.size());
        Assertions.assertEquals("{", tokens.get(0).getContent());
        Assertions.assertEquals("Hello World", tokens.get(1).getContent());
        Assertions.assertEquals(",", tokens.get(2).getContent());
        Assertions.assertEquals("type", tokens.get(3).getContent());
        Assertions.assertEquals(":", tokens.get(4).getContent());
        Assertions.assertEquals("java/lang/HelloWorld", tokens.get(5).getContent());
        Assertions.assertEquals("}", tokens.get(6).getContent());
    }

    @Test
    public void testStringEscaping() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", "\"Hello \\u0020World\\\"\\\\");
        Assertions.assertNotNull(tokens);
        Assertions.assertEquals(1, tokens.size());
        Assertions.assertEquals("Hello  World\"\\", tokens.get(0).getContent());
    }

    @Test
    public void testNumbers() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", "0 -10 10f 10.16F 10.161616D 10L 0xDEADBEEF 0E10");
        Assertions.assertNotNull(tokens);
        Assertions.assertEquals(8, tokens.size());
        Assertions.assertEquals("0", tokens.get(0).getContent());
        Assertions.assertEquals("-10", tokens.get(1).getContent());
        Assertions.assertEquals("10f", tokens.get(2).getContent());
        Assertions.assertEquals("10.16F", tokens.get(3).getContent());
        Assertions.assertEquals("10.161616D", tokens.get(4).getContent());
        Assertions.assertEquals("10L", tokens.get(5).getContent());
        Assertions.assertEquals("0xDEADBEEF", tokens.get(6).getContent());
        Assertions.assertEquals("0E10", tokens.get(7).getContent());
        for (Token token : tokens) {
            Assertions.assertSame(token.getType(), TokenType.NUMBER);
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = { ".class public java/lang/HelloWorld",
                    ".annotation Annotate {\n\tvalue: \"java/lang/HelloWorld\", \n\ttype: .enum java/lang/TargetType, METHOD\n}",
                    ".method add (II)I { \n" + "\t.parameters: {a, b}\n" + "\t.code: {\n" + "\t\tiload a\n"
                            + "\t\tiload b\n" + "\t\tiadd\n" + "\t\tireturn\t\n" + "\t}\n" + "}" }
    )
    public void testTokenizer(String input) {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", input);
        Assertions.assertNotNull(tokens);
        Assertions.assertTrue(tokens.size() > 0);
    }

}

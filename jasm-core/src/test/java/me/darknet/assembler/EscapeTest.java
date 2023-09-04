package me.darknet.assembler;

import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.util.EscapeUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EscapeTest {

    @ParameterizedTest
    @ValueSource(
            strings = { "\\n", "\\r", "\\t", "\\b", "\\f", "\\\"", "\t", "\u0020", "\u002C", "\\u0031\\\\\\\\\\\\\\",
                    "\"{ \\\"Hello World\\\", type: \\\"java/lang/HelloWorld\\\" }\",", "\n\n\n\n\n\n\n" }
    )
    public void testStringEscape(String input) {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", "\"" + EscapeUtil.escapeString(input) + "\"");
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals(input, tokens.get(0).content());
    }

    @ParameterizedTest
    @ValueSource(
            strings = { "epic\u0020obfuscated\u0020name", "{ \"Hello World\", type: \"java/lang/HelloWorld\" }",
                    "0 -10 10f 10.16F 10.161616D 10L 0xDEADBEEF 0E10", ".class public java/lang/HelloWorld", """
                            @ParameterizedTest
                            @ValueSource(
                            	strings = {
                            		"epic\\u0020obfuscated\\u0020name",
                            		"{ \\"Hello World\\", type: \\"java/lang/HelloWorld\\" }",
                            		"0 -10 10f 10.16F 10.161616D 10L 0xDEADBEEF 0E10",
                            		".class public java/lang/HelloWorld"
                            	}
                            )
                            """ }
    )
    public void testLiteralEscape(String input) {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", EscapeUtil.escapeLiteral(input));
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        System.out.println(tokens.get(0).content());
        assertEquals(input, EscapeUtil.unescape(tokens.get(0).content()));
    }

}

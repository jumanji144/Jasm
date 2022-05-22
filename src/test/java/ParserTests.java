import me.darknet.assembler.parser.Parser;
import me.darknet.assembler.parser.Token;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static me.darknet.assembler.parser.Token.TokenType;

public class ParserTests {

    static Parser parser;

    public static void setup() {
        parser = new Parser();
    }

    public void testParserTokenize() {
        String input1 = "class Test extends java/lang/Object";
        String input2 = "field public number I";
        String input3 = "method public static main([Ljava/lang/String;)V";

        List<Token> tokens1 = parser.tokenize("stdin", input1);
        List<Token> tokens2 = parser.tokenize("stdin", input2);
        List<Token> tokens3 = parser.tokenize("stdin", input3);

        assertEquals(4, tokens1.size());
        assertEquals(4, tokens2.size());
        assertEquals(4, tokens3.size());

        TokenType[] expected1 = {TokenType.KEYWORD, TokenType.IDENTIFIER, TokenType.KEYWORD, TokenType.IDENTIFIER};
        TokenType[] expected2 = {TokenType.KEYWORD, TokenType.KEYWORD, TokenType.IDENTIFIER, TokenType.IDENTIFIER};
        TokenType[] expected3 = {TokenType.KEYWORD, TokenType.KEYWORD, TokenType.KEYWORD, TokenType.IDENTIFIER};

        for (int i = 0; i < 4; i++) {
            assertEquals(tokens1.get(i).getType(), expected1[i]);
            assertEquals(tokens2.get(i).getType(), expected2[i]);
            assertEquals(tokens3.get(i).getType(), expected3[i]);
        }
    }

}

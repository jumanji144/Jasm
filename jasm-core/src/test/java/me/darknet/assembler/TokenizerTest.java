package me.darknet.assembler;

import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

public class TokenizerTest {

    @Test
    public void testStringTokenizer() {
        List<Token> tokens = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize("{ \"Hello World\", type: \"java/lang/HelloWorld\" }"),
                "Failed to tokenize string input"
        );
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
        List<Token> tokens = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize("\"Hello \\u0020World\\\"\\\\\""),
                "Failed to tokenize escaped string input"
        );
        Assertions.assertEquals(1, tokens.size());
        Assertions.assertEquals("Hello  World\"\\", tokens.getFirst().content());
    }

    @Test
    public void testNumbers() {
        List<Token> tokens = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize("0 -10 10f 10.16F 10.161616D 10L 0xDEADBEEF 0E10 0.3e10f 6.02214076e23"),
                "Failed to tokenize numeric input"
        );
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

    @Test
    public void testBinaryNumberWithSeparators() {
        var result = AssemblyParseFixture.parse("0b1_0");
        List<me.darknet.assembler.ast.ASTElement> elements = DiagnosticAssertions.requireSuccess(
                result,
                "Failed to parse binary number with separators"
        );
        Assertions.assertEquals(1, elements.size());
        ASTNumber number = Assertions.assertInstanceOf(ASTNumber.class, elements.getFirst());
        Assertions.assertEquals(2, number.asInt());
    }

    @Test
    public void testRawFloatingPointBitPatterns() {
        List<String> expected = List.of(
                "#0x7FC00000",
                "#0X7FF8000000000001",
                "#0B01111111110000000000000000000000",
                "#0b0111111111111000000000000000000000000000000000000000000000000001"
        );
        List<Token> tokens = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize(String.join(" ", expected)),
                "Failed to tokenize raw floating-point bit patterns"
        );
        Assertions.assertEquals(expected.size(), tokens.size());
        for (int index = 0; index < expected.size(); index++) {
            Assertions.assertEquals(expected.get(index), tokens.get(index).content());
            Assertions.assertSame(TokenType.NUMBER, tokens.get(index).type());
        }
    }

    @Test
    public void testRawFloatingPointBitPatternSeparators() {
        List<String> expected = List.of(
                "#0x7FC0__0000",
                "#0x7FF8_0000_0000_0001",
                "#0b01111111_11000000_00000000_00000000",
                "#0b0111111111111000_0000000000000000_0000000000000000_0000000000000001"
        );
        List<Token> tokens = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize(String.join(" ", expected)),
                "Failed to tokenize raw floating-point bit-pattern separators"
        );
        Assertions.assertEquals(expected.size(), tokens.size());
        for (int index = 0; index < expected.size(); index++) {
            Assertions.assertEquals(expected.get(index), tokens.get(index).content());
            Assertions.assertSame(TokenType.NUMBER, tokens.get(index).type());
        }
    }

    @Test
    public void testMalformedRawFloatingPointBitPatternsRetainIdentifiersWithErrors() {
        for (String input : List.of(
                "#0x",
                "#0b",
                "#0x7FC0000",
                "#0x7FC000000",
                "#0x" + "0".repeat(15),
                "#0x" + "0".repeat(17),
                "#0b" + "0".repeat(31),
                "#0b" + "0".repeat(33),
                "#0b" + "0".repeat(63),
                "#0b" + "0".repeat(65),
                "#0x_7FC00000",
                "#0x7FC00000_",
                "#0x7FC00000f",
                "#0d7FC00000",
                "#0b" + "0".repeat(31) + "2"
        )) {
            var result = AssemblyParseFixture.tokenize(input);
            DiagnosticAssertions.assertHasError(
                    result,
                    DiagnosticCode.INVALID_LITERAL,
                    "Malformed raw floating-point bit pattern should produce an error: " + input
            );
            DiagnosticAssertions.assertPhase(
                    result.diagnostics(),
                    DiagnosticPhase.LEXER,
                    "Malformed raw floating-point bit pattern should be a lexer diagnostic"
            );
            List<Token> recovered = result.requireValue();
            Assertions.assertEquals(1, recovered.size());
            Assertions.assertSame(TokenType.IDENTIFIER, recovered.getFirst().type());
            Assertions.assertEquals(input, recovered.getFirst().content());
        }
    }

    @Test
    public void testMalformedHexadecimalFloatsReportStructuredErrors() {
        for (String input : List.of("0xp1", "0x.p1")) {
            var result = AssemblyParseFixture.tokenize(input);
            DiagnosticAssertions.assertHasError(
                    result,
                    DiagnosticCode.INVALID_LITERAL,
                    "Malformed hexadecimal float should produce an error: " + input
            );
            Assertions.assertTrue(result.requireValue().isEmpty());
        }
    }

    @Test
    public void testCharacterLiteralMustContainExactlyOneCharacter() {
        List<Token> valid = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize("'a'"),
                "Failed to tokenize valid character literal"
        );
        Assertions.assertEquals("a", valid.getFirst().content());

        for (String input : List.of("''", "'ab'")) {
            var result = AssemblyParseFixture.tokenize(input);
            DiagnosticAssertions.assertHasError(
                    result,
                    DiagnosticCode.INVALID_LITERAL,
                    "Malformed character literal should produce an error: " + input
            );
            Assertions.assertTrue(result.requireValue().isEmpty());
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
        List<Token> tokens = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize(input),
                "Failed to tokenize parameterized input"
        );
        Assertions.assertFalse(tokens.isEmpty());
    }

    @Test
    public void testSingleLineComment() {
        List<Token> tokens = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize("// This is a comment\n"),
                "Failed to tokenize single-line comment"
        );
        Assertions.assertEquals(1, tokens.size());
        Assertions.assertEquals(" This is a comment", tokens.getFirst().content());
        Assertions.assertSame(TokenType.COMMENT, tokens.getFirst().type());
    }

    @Test
    public void testMultiLineComment() {
        List<Token> tokens = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize("/* This is a comment\n * with multiple lines\n */"),
                "Failed to tokenize multi-line comment"
        );
        Assertions.assertEquals(1, tokens.size());
        Assertions.assertEquals(" This is a comment\n * with multiple lines\n ", tokens.getFirst().content());
        Assertions.assertSame(TokenType.COMMENT, tokens.getFirst().type());
    }

    @Test
    public void testSingleLineCommentAtEof() {
        List<Token> tokens = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.tokenize("// This is a comment"),
                "Failed to tokenize single-line comment at EOF"
        );
        Assertions.assertEquals(1, tokens.size());
        Assertions.assertEquals(" This is a comment", tokens.getFirst().content());
        Assertions.assertSame(TokenType.COMMENT, tokens.getFirst().type());
    }

    @Test
    public void testTrailingSlashReportsStructuredError() {
        var result = AssemblyParseFixture.tokenize("/");
        DiagnosticAssertions.assertHasError(result, DiagnosticCode.UNEXPECTED_TOKEN,
                "Trailing slash should produce an error");
        DiagnosticAssertions.assertPhase(result.diagnostics(), DiagnosticPhase.LEXER,
                "Trailing slash should be a lexer diagnostic");
        Assertions.assertTrue(result.requireValue().isEmpty());
    }

    @Test
    public void testUnterminatedMultilineCommentReportsStructuredError() {
        var result = AssemblyParseFixture.tokenize("/* This comment never ends");
        DiagnosticAssertions.assertHasError(result, DiagnosticCode.UNTERMINATED_LITERAL,
                "Unterminated multiline comment should produce an error");
        DiagnosticAssertions.assertPhase(result.diagnostics(), DiagnosticPhase.LEXER,
                "Unterminated multiline comment should be a lexer diagnostic");
        Assertions.assertTrue(result.requireValue().isEmpty());
    }

    @Test
    public void testUnterminatedStringAtEofReportsStructuredError() {
        var result = AssemblyParseFixture.tokenize("\"Hello");
        DiagnosticAssertions.assertHasError(result, DiagnosticCode.UNTERMINATED_LITERAL,
                "Unterminated string should produce an error");
        Assertions.assertTrue(result.requireValue().isEmpty());
    }

    @Test
    public void testUnterminatedCharacterAtEofReportsStructuredError() {
        var result = AssemblyParseFixture.tokenize("'a");
        DiagnosticAssertions.assertHasError(result, DiagnosticCode.UNTERMINATED_LITERAL,
                "Unterminated character should produce an error");
        Assertions.assertTrue(result.requireValue().isEmpty());
    }

    @Test
    public void testInvalidUnicodeEscapeReportsStructuredError() {
        var truncated = AssemblyParseFixture.tokenize("\"\\u12\"");
        DiagnosticAssertions.assertHasError(truncated, DiagnosticCode.INVALID_ESCAPE,
                "Truncated unicode escape should produce an error");
        Assertions.assertTrue(truncated.requireValue().isEmpty());

        var nonHex = AssemblyParseFixture.tokenize("\"\\u00ZZ\"");
        DiagnosticAssertions.assertHasError(nonHex, DiagnosticCode.INVALID_ESCAPE,
                "Non-hex unicode escape should produce an error");
        Assertions.assertTrue(nonHex.requireValue().isEmpty());
    }

}

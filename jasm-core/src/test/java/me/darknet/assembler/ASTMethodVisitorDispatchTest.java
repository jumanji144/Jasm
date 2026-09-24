package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import me.darknet.assembler.visitor.Modifiers;
import org.junit.jupiter.api.Test;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ASTMethodVisitorDispatchTest {
    @Test
    void retainsSourceMethodDataWithoutFormatSpecificVisitorDispatch() {
        ASTIdentifier parameter = identifier("value", 10);
        ASTAnnotation parameterAnnotation = annotation(
                AnnotationVisibility.VISIBLE,
                "parameter/Visible",
                20
        );
        Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations = new IdentityHashMap<>();
        parameterAnnotations.put(parameter, List.of(parameterAnnotation));

        ASTIdentifier attributeKey = identifier("registers", 40);
        ASTIdentifier attributeValue = identifier("2", 50);
        ASTIdentifier secondAttributeKey = identifier("raw-attribute", 55);
        ASTIdentifier secondAttributeValue = identifier("raw-value", 68);
        ElementMap<ASTIdentifier, ASTElement> methodAttributes = new ElementMap<>();
        methodAttributes.put(attributeKey, attributeValue);
        methodAttributes.put(secondAttributeKey, secondAttributeValue);

        ASTString defaultValue = string("default", 60);
        ASTInstruction sourceInstruction = new ASTInstruction(
                identifier("raw-op", 70),
                List.of(identifier("raw-operand", 77))
        );
        ASTCode code = new ASTCode(List.of(sourceInstruction));

        ASTMethod method = new ASTMethod(
                new Modifiers(),
                identifier("test", 0),
                identifier("(I)V", 5),
                List.of(parameter),
                parameterAnnotations,
                List.of(identifier("java/lang/Exception", 90)),
                defaultValue,
                List.of(),
                code,
                methodAttributes
        );

        assertEquals(List.of("value"), method.getParameters().stream().map(ASTIdentifier::content).toList());
        assertSame(parameterAnnotation, method.getParameterAnnotations().get(parameter).getFirst());
        assertEquals(List.of("java/lang/Exception"),
                method.getDeclaredExceptions().stream().map(ASTIdentifier::content).toList());
        assertSame(defaultValue, method.getAnnotationDefaultValue());
        assertSame(code, method.getCode());
        assertEquals(2, method.getMethodAttributes().size());
        assertSame(attributeKey, method.getMethodAttributes().key(0));
        assertSame(attributeValue, method.getMethodAttributes().get(0));
        assertSame(secondAttributeKey, method.getMethodAttributes().key(1));
        assertSame(secondAttributeValue, method.getMethodAttributes().get(1));

        assertSame(method, parameter.parent());
        assertSame(method, parameterAnnotation.parent());
        assertSame(method, defaultValue.parent());
        assertSame(method, code.parent());
        assertSame(code, sourceInstruction.parent());
        assertSame(method, attributeKey.parent());
        assertSame(method, attributeValue.parent());
        assertSame(method, secondAttributeKey.parent());
        assertSame(method, secondAttributeValue.parent());
        assertTrue(code.children().contains(sourceInstruction));
    }

    private static ASTAnnotation annotation(AnnotationVisibility visibility, String classType, int start) {
        return new ASTAnnotation(visibility, identifier(classType, start), new ElementMap<>());
    }

    private static ASTIdentifier identifier(String content, int start) {
        return new ASTIdentifier(token(TokenType.IDENTIFIER, content, start));
    }

    private static ASTString string(String content, int start) {
        return new ASTString(token(TokenType.STRING, content, start));
    }

    private static Token token(TokenType type, String content, int start) {
        return new Token(
                new Range(start, start + content.length() - 1),
                new Location(1, start + 1, content.length(), "ASTMethodVisitorDispatchTest"),
                type,
                content
        );
    }
}

package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.ast.specific.ASTRecordComponent;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.visitor.Modifiers;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ASTModelTest {
    @Test
    void typeAnnotationHelpersUseTypeAnnotationCollections() {
        ASTField field = new ASTField(modifiers(), id("field", 5), id("I", 11), null);
        ASTRecordComponent component = new ASTRecordComponent(id("name", 20), id("I", 25));
        ASTAnnotation annotation = annotation("pkg/Anno", 30);

        field.addVisibleTypeAnnotation(annotation);
        component.addInvisibleTypeAnnotation(annotation("pkg/Other", 40));

        assertEquals(1, field.getVisibleTypeAnnotations().size());
        assertTrue(field.getVisibleAnnotations().isEmpty());
        assertEquals(1, component.getInvisibleTypeAnnotations().size());
        assertTrue(component.getInvisibleAnnotations().isEmpty());
    }

    @Test
    void classSettersUpdateChildrenAndParentLinks() {
        ASTClass clazz = new ASTClass(modifiers(), id("Example", 0), List.of());
        ASTIdentifier permitted = id("pkg/Sub", 15);
        ASTRecordComponent component = new ASTRecordComponent(id("value", 25), id("I", 31));

        clazz.setPermittedSubclasses(List.of(permitted));
        clazz.setRecordComponents(List.of(component));

        assertSame(clazz, permitted.parent());
        assertSame(clazz, component.parent());
        assertTrue(clazz.children().contains(permitted));
        assertTrue(clazz.children().contains(component));

        clazz.setPermittedSubclasses(List.of());
        clazz.setRecordComponents(List.of());

        assertNull(permitted.parent());
        assertNull(component.parent());
        assertFalse(clazz.children().contains(permitted));
        assertFalse(clazz.children().contains(component));
    }

    @Test
    void collectionsAreOwnedAndReadOnly() {
        List<ASTElement> contents = new ArrayList<>();
        ASTField field = new ASTField(modifiers(), id("field", 5), id("I", 11), num("1", 13));
        contents.add(field);

        ASTClass clazz = new ASTClass(modifiers(), id("Example", 0), contents);
        contents.clear();

        assertEquals(1, clazz.contents().size());
        assertThrows(UnsupportedOperationException.class, () -> clazz.contents().add(field));
        assertThrows(UnsupportedOperationException.class, () -> clazz.children().add(field));
        assertThrows(UnsupportedOperationException.class, () -> field.getModifiers().modifiers().add(id("static", 20)));

        ElementMap<ASTIdentifier, ASTElement> values = new ElementMap<>();
        values.put(id("value", 30), num("1", 37));
        ASTAnnotation annotation = new ASTAnnotation(true, id("pkg/Anno", 45), values);
        values.put(id("other", 55), num("2", 61));

        assertEquals(1, annotation.values().size());
        assertThrows(UnsupportedOperationException.class, () -> annotation.values().pairs().clear());
    }

    @Test
    void treeContainsDescriptorFieldValueAndParameterAnnotations() {
        ASTAnnotation parameterAnnotation = annotation("pkg/ParamAnno", 40);
        ASTIdentifier parameter = id("param", 22);
        Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations = new IdentityHashMap<>();
        parameterAnnotations.put(parameter, List.of(parameterAnnotation));

        ASTField field = new ASTField(modifiers(), id("field", 5), id("I", 11), num("1", 13));
        ASTMethod method = new ASTMethod(
                modifiers(),
                id("method", 18),
                id("(I)V", 28),
                List.of(parameter),
                parameterAnnotations,
                null,
                List.of(),
                new ASTCode(List.of()),
                List.of(),
                BytecodeFormat.JVM
        );

        List<ASTElement> visited = new ArrayList<>();
        field.walk(element -> {
            visited.add(element);
            return true;
        });
        method.walk(element -> {
            visited.add(element);
            return true;
        });

        assertTrue(visited.contains(field.getDescriptor()));
        assertTrue(visited.contains(field.getFieldValue()));
        assertTrue(visited.contains(parameterAnnotation));
        assertSame(method, parameterAnnotation.parent());
    }

    @Test
    void rangeAndPickReflectSetterBasedMutationAcrossAncestors() {
        ASTField field = new ASTField(modifiers(), id("field", 5), id("I", 11), null);
        ASTClass clazz = new ASTClass(modifiers(), id("Example", 0), List.of(field));

        assertEquals(11, clazz.range().end());

        ASTString signature = string("\"sig\"", 25);
        field.setSignature(signature);

        assertEquals(29, field.range().end());
        assertEquals(29, clazz.range().end());
        assertSame(signature, clazz.pick(26));
    }

    @Test
    void keyedViewsRemainReadableWithoutExposingBuilderMutation() {
        ElementMap<ASTIdentifier, ASTElement> values = new ElementMap<>();
        ASTIdentifier key = id("value", 10);
        ASTNumber number = num("1", 16);
        values.put(key, number);

        ASTAnnotation annotation = new ASTAnnotation(true, id("pkg/Anno", 0), values);

        assertEquals(1, annotation.values().size());
        assertSame(key, annotation.values().key("value"));
        assertSame(number, annotation.values().get("value"));
        assertEquals(2, annotation.values().elements().size());
    }

    private static ASTAnnotation annotation(String type, int start) {
        ElementMap<ASTIdentifier, ASTElement> values = new ElementMap<>();
        values.put(id("value", start + 2), num("1", start + 8));
        return new ASTAnnotation(true, id(type, start), values);
    }

    private static Modifiers modifiers() {
        return new Modifiers();
    }

    private static ASTIdentifier id(String content, int start) {
        return new ASTIdentifier(token(TokenType.IDENTIFIER, content, start));
    }

    private static ASTNumber num(String content, int start) {
        return new ASTNumber(token(TokenType.NUMBER, content, start));
    }

    private static ASTString string(String content, int start) {
        return new ASTString(token(TokenType.STRING, content, start));
    }

    private static Token token(TokenType type, String content, int start) {
        return new Token(
                new me.darknet.assembler.util.Range(start, start + content.length() - 1),
                new Location(1, start + 1, content.length(), "ASTModelTest"),
                type,
                content
        );
    }
}

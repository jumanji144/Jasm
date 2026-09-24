package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTEmpty;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.ast.specific.ASTRecordComponent;
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
        ASTAnnotation annotation = annotation(AnnotationVisibility.VISIBLE, "pkg/Anno", 30);

        field.addVisibleTypeAnnotation(annotation);
        component.addInvisibleTypeAnnotation(annotation(AnnotationVisibility.INVISIBLE, "pkg/Other", 40));

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
        ASTNumber version = num("21", 40);

        clazz.setPermittedSubclasses(List.of(permitted));
        clazz.setRecordComponents(List.of(component));
        clazz.setVersion(version);

        assertSame(clazz, permitted.parent());
        assertSame(clazz, component.parent());
        assertSame(clazz, version.parent());
        assertTrue(clazz.children().contains(permitted));
        assertTrue(clazz.children().contains(component));
        assertTrue(clazz.children().contains(version));
        assertSame(version, clazz.getVersion());

        clazz.setPermittedSubclasses(List.of());
        clazz.setRecordComponents(List.of());
        clazz.setVersion(null);

        assertNull(permitted.parent());
        assertNull(component.parent());
        assertNull(version.parent());
        assertFalse(clazz.children().contains(permitted));
        assertFalse(clazz.children().contains(component));
        assertFalse(clazz.children().contains(version));
        assertNull(clazz.getVersion());
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
        assertThrows(UnsupportedOperationException.class, () -> field.getModifiers().getModifiers().add(id("static", 20)));

        ElementMap<ASTIdentifier, ASTElement> values = new ElementMap<>();
        values.put(id("value", 30), num("1", 37));
        ASTAnnotation annotation = new ASTAnnotation(AnnotationVisibility.VISIBLE, id("pkg/Anno", 45), values);
        values.put(id("other", 55), num("2", 61));

        assertEquals(1, annotation.getValueMap().size());
        assertThrows(UnsupportedOperationException.class, () -> annotation.getValueMap().pairs().clear());
    }

    @Test
    void treeContainsDescriptorFieldValueAndParameterAnnotations() {
        ASTAnnotation parameterAnnotation = annotation(AnnotationVisibility.VISIBLE, "pkg/ParamAnno", 40);
        ASTIdentifier parameter = id("param", 22);
        Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations = new IdentityHashMap<>();
        parameterAnnotations.put(parameter, List.of(parameterAnnotation));

        ASTField field = new ASTField(modifiers(), id("field", 5), id("I", 11), num("1", 13));
        ElementMap<ASTIdentifier, ASTElement> methodAttributes = new ElementMap<>();
        ASTIdentifier attributeKey = id("registers", 35);
        ASTNumber attributeValue = num("2", 45);
        methodAttributes.put(attributeKey, attributeValue);
        ASTMethod method = new ASTMethod(
                modifiers(),
                id("method", 18),
                id("(I)V", 28),
                List.of(parameter),
                parameterAnnotations,
                List.of(),
                null,
                List.of(),
                new ASTCode(List.of()),
                methodAttributes
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
        assertTrue(visited.contains(attributeKey));
        assertTrue(visited.contains(attributeValue));
        assertSame(method, parameterAnnotation.parent());
        assertSame(method, attributeKey.parent());
        assertSame(method, attributeValue.parent());
        assertEquals(1, method.getMethodAttributes().size());
        assertSame(attributeKey, method.getMethodAttributes().key(0));
        assertSame(attributeValue, method.getMethodAttributes().get(0));
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

        ASTAnnotation annotation = new ASTAnnotation(AnnotationVisibility.VISIBLE, id("pkg/Anno", 0), values);

        assertEquals(1, annotation.getValueMap().size());
        assertSame(key, annotation.getValueMap().key("value"));
        assertSame(number, annotation.getValueMap().get("value"));
        assertEquals(2, annotation.getValueMap().elements().size());
    }

    @Test
    void argumentAccessorKeepsTheDocumentedAdaptations() {
        ASTNumber numericOperand = num("42", 10);
        ASTEmpty emptyArrayOperand = new ASTEmpty(token(TokenType.OPERATOR, "{}", 20));
        ASTEmpty emptyObjectOperand = new ASTEmpty(token(TokenType.OPERATOR, "{}", 24));
        ASTInstruction instruction = new ASTInstruction(
                id("test", 0), List.of(numericOperand, emptyArrayOperand, emptyObjectOperand)
        );

        ASTIdentifier adaptedNumber = instruction.argument(0, ASTIdentifier.class);
        assertEquals("42", adaptedNumber.content());
        assertNotSame(numericOperand, adaptedNumber);
        assertSame(ASTEmpty.EMPTY_ARRAY, instruction.argumentArray(1));
        assertSame(ASTEmpty.EMPTY_OBJECT, instruction.argumentObject(2));

        List<ASTElement> nullArgument = new ArrayList<>();
        nullArgument.add(null);
        assertNull(new ASTInstruction(id("test", 30), nullArgument).argument(0, ASTIdentifier.class));
    }

    @Test
    void argumentAccessorRejectsAnElementOfTheWrongType() {
        ASTInstruction instruction = new ASTInstruction(id("test", 0), List.of(string("\"value\"", 5)));

        assertThrows(IllegalStateException.class, () -> instruction.argumentArray(0));
    }

    @Test
    void annotationRejectsNullVisibility() {
        assertThrows(NullPointerException.class,
                () -> new ASTAnnotation(null, id("pkg/Anno", 0), new ElementMap<>()));
    }

    @Test
    void numberParsingDistinguishesHexDigitsFromSuffixes() {
        ASTNumber decimal = num("127", 0);
        ASTNumber hexLower = num("0x7f", 4);
        ASTNumber hexUpper = num("0X7F", 4);
        ASTNumber binaryLower = num("0b01111111", 9);
        ASTNumber binaryUpper = num("0B01111111", 9);

        assertEquals(127, decimal.asInt());
        assertEquals(127, hexLower.asInt());
        assertEquals(127, hexUpper.asInt());
        assertEquals(127, binaryLower.asInt());
        assertEquals(127, binaryUpper.asInt());
        assertFalse(hexLower.isFloatingPoint());
        assertFalse(binaryLower.isFloatingPoint());
        assertFalse(hexLower.isWide());
        assertFalse(binaryLower.isWide());

        ASTNumber negDecimal = num("-127", 0);
        ASTNumber negHexLower = num("-0x7f", 4);
        ASTNumber negHexUpper = num("-0X7F", 4);
        ASTNumber negBinaryLower = num("-0b01111111", 9);
        ASTNumber negBinaryUpper = num("-0B01111111", 9);
        assertEquals(-127, negDecimal.asInt());
        assertEquals(-127, negHexLower.asInt());
        assertEquals(-127, negHexUpper.asInt());
        assertEquals(-127, negBinaryLower.asInt());
        assertEquals(-127, negBinaryUpper.asInt());
    }

    @Test
    void rawFloatingPointBitPatternsPreserveAllBits() {
        ASTNumber floatPattern = num("#0x7FC00000", 0);
        assertInstanceOf(Float.class, floatPattern.number());
        assertEquals(0x7FC00000, Float.floatToRawIntBits(floatPattern.asFloat()));
        assertTrue(floatPattern.isFloatingPoint());
        assertFalse(floatPattern.isWide());
        assertTrue(floatPattern.isNaN());

        ASTNumber nonCanonicalFloat = num("#0x7FC00001", 12);
        assertEquals(0x7FC00001, Float.floatToRawIntBits(nonCanonicalFloat.asFloat()));
        assertTrue(nonCanonicalFloat.isNaN());
        assertNotEquals(
                Float.floatToRawIntBits(Float.NaN),
                Float.floatToRawIntBits(nonCanonicalFloat.asFloat())
        );

        ASTNumber doublePattern = num("#0x7FF8000000000001", 24);
        assertInstanceOf(Double.class, doublePattern.number());
        assertEquals(0x7FF8000000000001L, Double.doubleToRawLongBits(doublePattern.asDouble()));
        assertTrue(doublePattern.isFloatingPoint());
        assertTrue(doublePattern.isWide());
        assertTrue(doublePattern.isNaN());

        ASTNumber binaryFloatPattern = num("#0B01111111110000000000000000000000", 42);
        assertInstanceOf(Float.class, binaryFloatPattern.number());
        assertFalse(binaryFloatPattern.isWide());
        assertEquals(
                Float.floatToRawIntBits(floatPattern.asFloat()),
                Float.floatToRawIntBits(binaryFloatPattern.asFloat())
        );

        ASTNumber binaryDoublePattern = num(
                "#0b0111111111111000000000000000000000000000000000000000000000000001", 78);
        assertInstanceOf(Double.class, binaryDoublePattern.number());
        assertTrue(binaryDoublePattern.isWide());
        assertEquals(
                Double.doubleToRawLongBits(doublePattern.asDouble()),
                Double.doubleToRawLongBits(binaryDoublePattern.asDouble())
        );

        ASTNumber separated = num("#0x7FC0__0000", 150);
        assertEquals(0x7FC00000, Float.floatToRawIntBits(separated.asFloat()));
        assertTrue(separated.isNaN());

        ASTNumber zeroFloat = num("#0x00000000", 166);
        ASTNumber zeroDouble = num("#0x0000000000000000", 178);
        assertInstanceOf(Float.class, zeroFloat.number());
        assertFalse(zeroFloat.isWide());
        assertInstanceOf(Double.class, zeroDouble.number());
        assertTrue(zeroDouble.isWide());

        ASTNumber negativeFloatZero = num("#0x80000000", 198);
        ASTNumber negativeFloatOne = num("#0xBF800000", 210);
        ASTNumber negativeDoubleZero = num("#0x8000000000000000", 222);
        assertEquals(0x80000000, Float.floatToRawIntBits(negativeFloatZero.asFloat()));
        assertEquals(-1.0f, negativeFloatOne.asFloat());
        assertEquals(0xBF800000, Float.floatToRawIntBits(negativeFloatOne.asFloat()));
        assertEquals(0x8000000000000000L, Double.doubleToRawLongBits(negativeDoubleZero.asDouble()));

        ASTNumber positiveFloatInfinity = num("#0x7F800000", 244);
        ASTNumber negativeFloatInfinity = num("#0xFF800000", 256);
        ASTNumber positiveDoubleInfinity = num("#0x7FF0000000000000", 268);
        ASTNumber negativeDoubleInfinity = num("#0xFFF0000000000000", 288);
        assertTrue(positiveFloatInfinity.isInfinity());
        assertTrue(negativeFloatInfinity.isInfinity());
        assertTrue(positiveDoubleInfinity.isInfinity());
        assertTrue(negativeDoubleInfinity.isInfinity());
        assertEquals(0x7F800000, Float.floatToRawIntBits(positiveFloatInfinity.asFloat()));
        assertEquals(0xFF800000, Float.floatToRawIntBits(negativeFloatInfinity.asFloat()));
        assertEquals(0x7FF0000000000000L, Double.doubleToRawLongBits(positiveDoubleInfinity.asDouble()));
        assertEquals(0xFFF0000000000000L, Double.doubleToRawLongBits(negativeDoubleInfinity.asDouble()));

        ASTNumber readableNaN = num("NaNF", 316);
        assertEquals(
                Float.floatToRawIntBits(Float.NaN),
                Float.floatToRawIntBits(readableNaN.asFloat())
        );
        assertNotEquals(
                Float.floatToRawIntBits(readableNaN.asFloat()),
                Float.floatToRawIntBits(nonCanonicalFloat.asFloat())
        );
    }

    @Test
    void preExistingNumberSpellingsKeepTheirValues() {
        ASTNumber decimalInteger = num("127", 0);
        ASTNumber longInteger = num("127L", 4);
        ASTNumber hexadecimalInteger = num("0x7FFFFFFF", 9);
        ASTNumber decimalFloat = num("100.0f", 20);
        ASTNumber decimalExponent = num("1e3", 28);
        ASTNumber hexadecimalFloat = num("0x1.8p1", 32);

        assertInstanceOf(Integer.class, decimalInteger.number());
        assertEquals(127, decimalInteger.asInt());
        assertInstanceOf(Long.class, longInteger.number());
        assertEquals(127L, longInteger.asLong());
        assertEquals(0x7FFFFFFF, hexadecimalInteger.asInt());
        assertEquals(100.0f, decimalFloat.asFloat());
        assertFalse(decimalFloat.isWide());
        assertEquals(1000.0d, decimalExponent.asDouble());
        assertTrue(decimalExponent.isWide());
        assertEquals(3.0d, hexadecimalFloat.asDouble());
        assertTrue(hexadecimalFloat.isWide());
    }

    @Test
    void specialNumberFormsCoverFloatAndDoubleVariants() {
        ASTNumber nan = num("NaN", 0);
        ASTNumber nanDouble = num("NaND", 4);
        ASTNumber nanFloat = num("NaNF", 9);
        ASTNumber infinity = num("Infinity", 14);
        ASTNumber posInfinityDouble = num("+InfinityD", 23);
        ASTNumber negInfinityDouble = num("-InfinityD", 34);
        ASTNumber posInfinityFloat = num("+InfinityF", 45);
        ASTNumber negInfinityFloat = num("-InfinityF", 56);

        assertTrue(Double.isNaN(nan.asDouble()));
        assertTrue(Double.isNaN(nanDouble.asDouble()));
        assertTrue(Float.isNaN(nanFloat.asFloat()));
        assertTrue(nan.isNaN());
        assertTrue(nanDouble.isNaN());
        assertTrue(nanFloat.isNaN());
        assertTrue(nan.isFloatingPoint());
        assertTrue(nanDouble.isWide());
        assertFalse(nanFloat.isWide());

        assertEquals(Double.POSITIVE_INFINITY, infinity.asDouble());
        assertEquals(Double.POSITIVE_INFINITY, posInfinityDouble.asDouble());
        assertEquals(Double.NEGATIVE_INFINITY, negInfinityDouble.asDouble());
        assertEquals(Float.POSITIVE_INFINITY, posInfinityFloat.asFloat());
        assertEquals(Float.NEGATIVE_INFINITY, negInfinityFloat.asFloat());
        assertTrue(infinity.isInfinity());
        assertTrue(posInfinityDouble.isInfinity());
        assertTrue(negInfinityDouble.isInfinity());
        assertTrue(posInfinityFloat.isInfinity());
        assertTrue(negInfinityFloat.isInfinity());
        assertTrue(infinity.isWide());
        assertTrue(posInfinityDouble.isWide());
        assertFalse(posInfinityFloat.isWide());
    }

    @Test
    void exponentNumberFormsParseAsFloatingPoint() {
        ASTNumber decimalExponent = num("1e3", 0);
        ASTNumber decimalNegativeExponent = num("2.5E-2", 4);
        ASTNumber floatExponent = num("1e3f", 12);
        ASTNumber hexExponent = num("0x1.8p1", 18);
        ASTNumber hexFloatExponent = num("0x1.0p2f", 26);

        assertEquals(1000.0d, decimalExponent.asDouble());
        assertEquals(0.025d, decimalNegativeExponent.asDouble());
        assertEquals(1000.0f, floatExponent.asFloat());
        assertEquals(3.0d, hexExponent.asDouble());
        assertEquals(4.0f, hexFloatExponent.asFloat());

        assertTrue(decimalExponent.isFloatingPoint());
        assertTrue(decimalNegativeExponent.isFloatingPoint());
        assertTrue(floatExponent.isFloatingPoint());
        assertTrue(hexExponent.isFloatingPoint());
        assertTrue(hexFloatExponent.isFloatingPoint());

        assertTrue(decimalExponent.isWide());
        assertTrue(decimalNegativeExponent.isWide());
        assertFalse(floatExponent.isWide());
        assertTrue(hexExponent.isWide());
        assertFalse(hexFloatExponent.isWide());
    }

    private static ASTAnnotation annotation(AnnotationVisibility visibility, String type, int start) {
        ElementMap<ASTIdentifier, ASTElement> values = new ElementMap<>();
        values.put(id("value", start + 2), num("1", start + 8));
        return new ASTAnnotation(visibility, id(type, start), values);
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

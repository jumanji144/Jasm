package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.registry.MethodValueRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BasicMethodValueLookup}.
 */
class BasicMethodValueLookupTest {
    private static final BasicMethodValueLookup LOOKUP = new BasicMethodValueLookup();

    @Test
    void parsesLongFromCharSequenceSlices() {
        Value value = requireStatic(
                "java/lang/Long",
                "parseLong",
                "(Ljava/lang/CharSequence;III)J",
                Values.valueOfString("0f"),
                Values.valueOf(0),
                Values.valueOf(2),
                Values.valueOf(16)
        );

        Value.KnownLongValue known = assertInstanceOf(Value.KnownLongValue.class, value);
        assertEquals(15L, known.value());
    }

    @Test
    void resolvesByteToUnsignedLongWithCorrectDescriptor() {
        Value value = requireStatic(
                "java/lang/Byte",
                "toUnsignedLong",
                "(B)J",
                Values.valueOf((byte) -1)
        );

        Value.KnownLongValue known = assertInstanceOf(Value.KnownLongValue.class, value);
        assertEquals(Byte.toUnsignedLong((byte) -1), known.value());
    }

    @Test
    void resolvesMathGetExponentWithIntReturnType() {
        Value value = requireStatic(
                "java/lang/Math",
                "getExponent",
                "(D)I",
                Values.valueOf(8.0D)
        );

        Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
        assertEquals(Math.getExponent(8.0D), known.value());
    }

    @Test
    void resolvesMathRoundWithCorrectReturnTypes() {
        Value floatValue = requireStatic(
                "java/lang/Math",
                "round",
                "(F)I",
                Values.valueOf(1.6F)
        );
        Value doubleValue = requireStatic(
                "java/lang/Math",
                "round",
                "(D)J",
                Values.valueOf(1.6D)
        );

        assertEquals(Math.round(1.6F), assertInstanceOf(Value.KnownIntValue.class, floatValue).value());
        assertEquals(Math.round(1.6D), assertInstanceOf(Value.KnownLongValue.class, doubleValue).value());
    }

    @Test
    void usesStringFallbackForDoubleStringConversions() {
        Value toStringValue = requireStatic(
                "java/lang/Double",
                "toString",
                "(D)Ljava/lang/String;",
                Values.DOUBLE_VALUE
        );
        Value toHexStringValue = requireStatic(
                "java/lang/Double",
                "toHexString",
                "(D)Ljava/lang/String;",
                Values.DOUBLE_VALUE
        );

        assertSame(Values.STRING_VALUE, toStringValue);
        assertSame(Values.STRING_VALUE, toHexStringValue);
    }

    @Test
    void resolvesCharacterCodePointOfByCorrectSignature() {
        Value value = requireStatic(
                "java/lang/Character",
                "codePointOf",
                "(Ljava/lang/String;)I",
                Values.valueOfString("LATIN CAPITAL LETTER A")
        );

        Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
        assertEquals(Character.codePointOf("LATIN CAPITAL LETTER A"), known.value());
    }

    @Test
    void resolvesStringContainsAndMatchesByCorrectKeys() {
        assertTrue(stringBool("hello", "contains", "(Ljava/lang/CharSequence;)Z", Values.valueOfString("ell")));
        assertTrue(stringBool("hello", "matches", "(Ljava/lang/String;)Z", Values.valueOfString("h.*o")));
    }

    @Test
    void allowsStringBoundaryCasesThatEndAtLength() {
        assertEquals("", string("hello", "substring", "(II)Ljava/lang/String;", Values.valueOf(5), Values.valueOf(5)));
        assertEquals(5, stringInt("hello", "codePointCount", "(II)I", Values.valueOf(0), Values.valueOf(5)));
    }

    @Test
    void resolvesKnownStringInstanceCallsEvenWhenOwnerIsWider() {
        Value value = requireInstance(
                "java/lang/Object",
                "toString",
                "()Ljava/lang/String;",
                Values.valueOfString("hello")
        );

        Value.KnownStringValue known = assertInstanceOf(Value.KnownStringValue.class, value);
        assertEquals("hello", known.value());
    }

    @Test
    void resolvesAdditionalStringTransformationsAndComparisons() {
        assertEquals("hello", string("  hello  ", "trim", "()Ljava/lang/String;"));
        assertEquals("hello", string("  hello  ", "strip", "()Ljava/lang/String;"));
        assertEquals("hello  ", string("  hello  ", "stripLeading", "()Ljava/lang/String;"));
        assertEquals("  hello", string("  hello  ", "stripTrailing", "()Ljava/lang/String;"));
        assertEquals("HELLO", string("hello", "toUpperCase", "()Ljava/lang/String;"));
        assertEquals("hello", string("HELLO", "toLowerCase", "()Ljava/lang/String;"));
        assertEquals("haha", string("ha", "repeat", "(I)Ljava/lang/String;", Values.valueOf(2)));
        assertTrue(stringBool("hello", "equals", "(Ljava/lang/Object;)Z", Values.valueOfString("hello")));
        assertTrue(stringBool("hello", "equalsIgnoreCase", "(Ljava/lang/String;)Z", Values.valueOfString("HELLO")));
        assertTrue(stringBool("hello", "contentEquals", "(Ljava/lang/CharSequence;)Z", Values.valueOfString("hello")));
        assertTrue(stringBool("hello", "startsWith", "(Ljava/lang/String;)Z", Values.valueOfString("he")));
        assertTrue(stringBool("hello", "startsWith", "(Ljava/lang/String;I)Z", Values.valueOfString("ell"), Values.valueOf(1)));
        assertTrue(stringBool("hello", "endsWith", "(Ljava/lang/String;)Z", Values.valueOfString("lo")));
        assertEquals(0, stringInt("hello", "compareTo", "(Ljava/lang/String;)I", Values.valueOfString("hello")));
        assertEquals(0, stringInt("hello", "compareToIgnoreCase", "(Ljava/lang/String;)I", Values.valueOfString("HELLO")));
    }

    @Test
    void resolvesAdditionalStringIndexesAndCodePointQueries() {
        assertEquals('e', stringInt("hello", "charAt", "(I)C", Values.valueOf(1)));
        assertEquals(2, stringInt("hello", "indexOf", "(I)I", Values.valueOf('l')));
        assertEquals(3, stringInt("hello", "indexOf", "(II)I", Values.valueOf('l'), Values.valueOf(3)));
        assertEquals(2, stringInt("hello", "indexOf", "(Ljava/lang/String;)I", Values.valueOfString("ll")));
        assertEquals(3, stringInt("hello", "indexOf", "(Ljava/lang/String;I)I", Values.valueOfString("l"), Values.valueOf(3)));
        assertEquals("llo", string("hello", "substring", "(I)Ljava/lang/String;", Values.valueOf(2)));
        assertEquals((int) 'h', stringInt("hello", "codePointAt", "(I)I", Values.valueOf(0)));
        assertEquals((int) 'o', stringInt("hello", "codePointBefore", "(I)I", Values.valueOf(5)));
        assertEquals(3, stringInt("hello", "offsetByCodePoints", "(II)I", Values.valueOf(1), Values.valueOf(2)));
    }

    @Test
    void resolvesStringValueOfOverloadsAndSimplePredicates() {
        assertEquals("42", staticString("java/lang/String", "valueOf", "(I)Ljava/lang/String;", Values.valueOf(42)));
        assertEquals("1.5", staticString("java/lang/String", "valueOf", "(F)Ljava/lang/String;", Values.valueOf(1.5F)));
        assertEquals("1.5", staticString("java/lang/String", "valueOf", "(D)Ljava/lang/String;", Values.valueOf(1.5D)));
        assertEquals("42", staticString("java/lang/String", "valueOf", "(J)Ljava/lang/String;", Values.valueOf(42L)));
        assertEquals("true", staticString("java/lang/String", "valueOf", "(Z)Ljava/lang/String;", Values.INT_1));
        assertTrue(stringBool(" ", "isBlank", "()Z"));
        assertTrue(stringBool("", "isEmpty", "()Z"));
        assertEquals("hello", string("hello", "intern", "()Ljava/lang/String;"));
        assertEquals("hello", string("hello", "toString", "()Ljava/lang/String;"));
        assertEquals("hello".hashCode(), stringInt("hello", "hashCode", "()I"));
    }

    @Test
    void resolvesBooleanSystemAndInjectedRegistryLookups() {
        assertEquals(1, knownInt(requireStatic("java/lang/Boolean", "logicalXor", "(ZZ)Z", Values.INT_1, Values.INT_0)));
        assertEquals(Boolean.hashCode(true), knownInt(requireStatic("java/lang/Boolean", "hashCode", "(Z)I", Values.INT_1)));
        assertEquals("false", knownString(requireStatic("java/lang/Boolean", "toString", "(Z)Ljava/lang/String;", Values.INT_0)));
        assertEquals(System.lineSeparator(), knownString(requireStatic("java/lang/System", "lineSeparator", "()Ljava/lang/String;")));
        assertEquals(
                System.getProperty("java.version"),
                knownString(requireStatic("java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", Values.valueOfString("java.version")))
        );
        assertEquals(
                "fallback",
                knownString(requireStatic(
                        "java/lang/System",
                        "getProperty",
                        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                        Values.valueOfString("jasm.test.missing.property"),
                        Values.valueOfString("fallback")
                ))
        );

        BasicMethodValueLookup customLookup = new BasicMethodValueLookup(
                MethodValueRegistry.builder()
                        .registerStatic("test/Owner", "value", "()I", params -> Values.valueOf(7))
                        .build()
        );
        Value value = customLookup.accept(instruction(Opcodes.INVOKESTATIC, "test/Owner", "value", "()I"), null, List.of());
        assertNotNull(value);
        assertEquals(7, knownInt(value));
    }

    @Test
    void resolvesNumericAndStrictMathRegistries() {
        assertEquals(127, knownInt(requireStatic("java/lang/Byte", "parseByte", "(Ljava/lang/String;I)I", Values.valueOfString("7f"), Values.valueOf(16))));
        assertEquals(Byte.toUnsignedInt((byte) -1), knownInt(requireStatic("java/lang/Byte", "toUnsignedInt", "(B)I", Values.valueOf((byte) -1))));
        assertEquals(Short.reverseBytes((short) 0x1234), knownInt(requireStatic("java/lang/Short", "reverseBytes", "(S)S", Values.valueOf(0x1234))));
        assertEquals(
                Integer.toUnsignedString(-1, 16),
                knownString(requireStatic("java/lang/Integer", "toUnsignedString", "(II)Ljava/lang/String;", Values.valueOf(-1), Values.valueOf(16)))
        );
        assertEquals(
                Long.rotateLeft(1L, 63),
                knownLong(requireStatic("java/lang/Long", "rotateLeft", "(JI)J", Values.valueOf(1L), Values.valueOf(63)))
        );
        assertEquals(1.0F, knownFloat(requireStatic("java/lang/Float", "intBitsToFloat", "(I)F", Values.valueOf(0x3f800000))));
        assertEquals(1.0D, knownDouble(requireStatic("java/lang/Double", "longBitsToDouble", "(J)D", Values.valueOf(0x3ff0_0000_0000_0000L))));
        assertEquals(Math.floorMod(-7L, 3), knownInt(requireStatic("java/lang/Math", "floorMod", "(JI)I", Values.valueOf(-7L), Values.valueOf(3))));
        assertEquals(StrictMath.abs(-2.5D), knownDouble(requireStatic("java/lang/StrictMath", "abs", "(D)D", Values.valueOf(-2.5D))));
        assertEquals(Character.codePointAt("ab", 1), knownInt(requireStatic("java/lang/Character", "codePointAt", "(Ljava/lang/CharSequence;I)I", Values.valueOfString("ab"), Values.valueOf(1))));
        assertEquals(
                Character.offsetByCodePoints("hello", 1, 2),
                knownInt(requireStatic("java/lang/Character", "offsetByCodePoints", "(Ljava/lang/CharSequence;II)I", Values.valueOfString("hello"), Values.valueOf(1), Values.valueOf(2)))
        );
    }

    @Test
    void returnsFallbackValuesForInvalidInputsAndNullForUnknownMethods() {
        assertSame(Values.INT_VALUE, requireStatic("java/lang/Byte", "parseByte", "(Ljava/lang/String;)I", Values.valueOfString("999")));
        assertSame(Values.LONG_VALUE, requireStatic("java/lang/Long", "parseLong", "(Ljava/lang/String;)J", Values.valueOfString("nope")));
        assertSame(Values.INT_VALUE, requireStatic("java/lang/Math", "addExact", "(II)I", Values.INT_MAX, Values.INT_1));
        assertSame(Values.STRING_VALUE, requireStatic("java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", Values.INT_0));
        assertSame(Values.STRING_VALUE, requireInstance("java/lang/String", "repeat", "(I)Ljava/lang/String;", Values.valueOfString("ha"), Values.valueOf(-1)));
        assertSame(Values.STRING_VALUE, requireInstance("java/lang/String", "substring", "(I)Ljava/lang/String;", Values.valueOfString("hi"), Values.valueOf(5)));
        assertSame(Values.INT_VALUE, requireStatic("java/lang/Character", "codePointAt", "(Ljava/lang/CharSequence;I)I", Values.valueOfString("a"), Values.valueOf(5)));
        assertSame(Values.INT_VALUE, requireInstance("java/lang/String", "equals", "(Ljava/lang/Object;)Z", Values.valueOfString("a"), Values.INT_1));
        assertNull(findStatic("missing/Owner", "value", "()I"));
    }

    private static @NotNull Value requireStatic(String owner, String name, String descriptor, Value... params) {
        Value value = findStatic(owner, name, descriptor, params);
        assertNotNull(value, "Expected method lookup to produce a value");
        return value;
    }

    private static @Nullable Value findStatic(String owner, String name, String descriptor, Value... params) {
        return LOOKUP.accept(instruction(Opcodes.INVOKESTATIC, owner, name, descriptor), null, List.of(params));
    }

    private static @NotNull Value requireInstance(String owner, String name, String descriptor, Value.ObjectValue context, Value... params) {
        Value value = findInstance(owner, name, descriptor, context, params);
        assertNotNull(value, "Expected method lookup to produce a value");
        return value;
    }

    private static @Nullable Value findInstance(String owner, String name, String descriptor, Value.ObjectValue context, Value... params) {
        return LOOKUP.accept(instruction(Opcodes.INVOKEVIRTUAL, owner, name, descriptor), context, List.of(params));
    }

    private static @NotNull Value requireStringInstance(String value, String name, String descriptor, Value... params) {
        return requireInstance("java/lang/String", name, descriptor, Values.valueOfString(value), params);
    }

    private static MethodInsnNode instruction(int opcode, String owner, String name, String descriptor) {
        return new MethodInsnNode(opcode, owner, name, descriptor, false);
    }

    private static int knownInt(Value value) {
        return assertInstanceOf(Value.KnownIntValue.class, value).value();
    }

    private static long knownLong(Value value) {
        return assertInstanceOf(Value.KnownLongValue.class, value).value();
    }

    private static float knownFloat(Value value) {
        return assertInstanceOf(Value.KnownFloatValue.class, value).value();
    }

    private static double knownDouble(Value value) {
        return assertInstanceOf(Value.KnownDoubleValue.class, value).value();
    }

    private static String knownString(Value value) {
        return assertInstanceOf(Value.KnownStringValue.class, value).value();
    }

    private static boolean stringBool(String value, String name, String descriptor, Value... params) {
        return knownInt(requireStringInstance(value, name, descriptor, params)) != 0;
    }

    private static int stringInt(String value, String name, String descriptor, Value... params) {
        return knownInt(requireStringInstance(value, name, descriptor, params));
    }

    private static String string(String value, String name, String descriptor, Value... params) {
        return knownString(requireStringInstance(value, name, descriptor, params));
    }

    private static String staticString(String owner, String name, String descriptor, Value... params) {
        return knownString(requireStatic(owner, name, descriptor, params));
    }
}

package me.darknet.assembler;

import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.instruction.MethodInstruction;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link BasicMethodValueLookup}.
 */
class BasicMethodValueLookupTest {
    // We don't really have a great way of doing full test coverage for all defined methods.
    // There's a load of them and doing that is a lot of work.
    private static final BasicMethodValueLookup LOOKUP = new BasicMethodValueLookup();

    @Test
    void parsesLongFromCharSequenceSlices() {
        Value value = invokeStatic(
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
        Value value = invokeStatic(
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
        Value value = invokeStatic(
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
        Value floatValue = invokeStatic(
                "java/lang/Math",
                "round",
                "(F)I",
                Values.valueOf(1.6F)
        );
        Value doubleValue = invokeStatic(
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
        Value toStringValue = invokeStatic(
                "java/lang/Double",
                "toString",
                "(D)Ljava/lang/String;",
                Values.DOUBLE_VALUE
        );
        Value toHexStringValue = invokeStatic(
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
        Value value = invokeStatic(
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
        Value contains = invokeInstance(
                "java/lang/String",
                "contains",
                "(Ljava/lang/CharSequence;)Z",
                Values.valueOfString("hello"),
                Values.valueOfString("ell")
        );
        Value matches = invokeInstance(
                "java/lang/String",
                "matches",
                "(Ljava/lang/String;)Z",
                Values.valueOfString("hello"),
                Values.valueOfString("h.*o")
        );

        assertEquals(1, assertInstanceOf(Value.KnownIntValue.class, contains).value());
        assertEquals(1, assertInstanceOf(Value.KnownIntValue.class, matches).value());
    }

    @Test
    void allowsStringBoundaryCasesThatEndAtLength() {
        Value substring = invokeInstance(
                "java/lang/String",
                "substring",
                "(II)Ljava/lang/String;",
                Values.valueOfString("hello"),
                Values.valueOf(5),
                Values.valueOf(5)
        );
        Value codePointCount = invokeInstance(
                "java/lang/String",
                "codePointCount",
                "(II)I",
                Values.valueOfString("hello"),
                Values.valueOf(0),
                Values.valueOf(5)
        );

        assertEquals("", assertInstanceOf(Value.KnownStringValue.class, substring).value());
        assertEquals(5, assertInstanceOf(Value.KnownIntValue.class, codePointCount).value());
    }

    @Test
    void resolvesKnownStringInstanceCallsEvenWhenOwnerIsWider() {
        Value value = invokeInstance(
                "java/lang/Object",
                "toString",
                "()Ljava/lang/String;",
                Values.valueOfString("hello")
        );

        Value.KnownStringValue known = assertInstanceOf(Value.KnownStringValue.class, value);
        assertEquals("hello", known.value());
    }

    private static Value invokeStatic(String owner, String name, String descriptor, Value... params) {
        Value value = LOOKUP.accept(instruction(JavaOpcodes.INVOKESTATIC, owner, name, descriptor), null, List.of(params));
        assertNotNull(value, "Expected method lookup to produce a value");
        return value;
    }

    private static Value invokeInstance(String owner, String name, String descriptor, Value.ObjectValue context, Value... params) {
        Value value = LOOKUP.accept(instruction(JavaOpcodes.INVOKEVIRTUAL, owner, name, descriptor), context, List.of(params));
        assertNotNull(value, "Expected method lookup to produce a value");
        return value;
    }

    private static MethodInstruction instruction(int opcode, String owner, String name, String descriptor) {
        return new MethodInstruction(
                opcode,
                Types.instanceTypeFromInternalName(owner),
                name,
                Types.methodType(descriptor),
                false
        );
    }
}

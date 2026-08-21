package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.func.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bunch of nonsense tests to verify that the various static function adapters work as expected.
 * These are mostly just sanity checks to make sure the adapters are doing what we expect them to do.
 */
class StaticFuncAdapterTest {

    @Test
    void adaptersProduceKnownValuesForCompatibleArguments() {
        assertKnownInt(((B2IStaticFunc) Byte::toUnsignedInt).apply(List.of(Values.valueOf((byte) -1))), 255);
        assertKnownLong(((B2JStaticFunc) Byte::toUnsignedLong).apply(List.of(Values.valueOf((byte) -1))), 255L);
        assertKnownString(((B2StringStaticFunc) Byte::toString).apply(List.of(Values.valueOf((byte) 12))), "12");
        assertKnownInt(((BB2IStaticFunc) Byte::compare).apply(List.of(Values.valueOf((byte) 1), Values.valueOf((byte) 2))), -1);
        assertKnownInt(((C2CStaticFunc) Character::toUpperCase).apply(List.of(Values.valueOf('a'))), 'A');
        assertKnownInt(((C2IStaticFunc) Character::getNumericValue).apply(List.of(Values.valueOf('9'))), 9);
        assertKnownBoolean(((C2ZStaticFunc) Character::isUpperCase).apply(List.of(Values.valueOf('A'))), true);
        assertKnownInt(((CC2IStaticFunc) Character::compare).apply(List.of(Values.valueOf('a'), Values.valueOf('b'))), -1);
        assertKnownBoolean(((CC2ZStaticFunc) Character::isSurrogatePair).apply(List.of(Values.valueOf('\uD83D'), Values.valueOf('\uDE00'))), true);
        assertKnownDouble(((D2DStaticFunc) Math::abs).apply(List.of(Values.valueOf(-4.5D))), 4.5D);
        assertKnownInt(((D2IStaticFunc) Double::hashCode).apply(List.of(Values.valueOf(4.5D))), Double.hashCode(4.5D));
        assertKnownLong(((D2JStaticFunc) Double::doubleToLongBits).apply(List.of(Values.valueOf(4.5D))), Double.doubleToLongBits(4.5D));
        assertKnownBoolean(((D2ZStaticFunc) Double::isNaN).apply(List.of(Values.valueOf(Double.NaN))), true);
        assertKnownDouble(((DD2DStaticFunc) Math::max).apply(List.of(Values.valueOf(1.5D), Values.valueOf(4.5D))), 4.5D);
        assertKnownInt(((DD2IStaticFunc) Double::compare).apply(List.of(Values.valueOf(1.5D), Values.valueOf(4.5D))), -1);
        assertKnownFloat(((F2FStaticFunc) Math::abs).apply(List.of(Values.valueOf(-4.5F))), 4.5F);
        assertKnownInt(((F2IStaticFunc) Float::hashCode).apply(List.of(Values.valueOf(4.5F))), Float.hashCode(4.5F));
        assertKnownBoolean(((F2ZStaticFunc) Float::isNaN).apply(List.of(Values.valueOf(Float.NaN))), true);
        assertKnownFloat(((FF2FStaticFunc) Math::max).apply(List.of(Values.valueOf(1.5F), Values.valueOf(4.5F))), 4.5F);
        assertKnownInt(((FF2IStaticFunc) Float::compare).apply(List.of(Values.valueOf(1.5F), Values.valueOf(4.5F))), -1);
        assertKnownInt(((I2CStaticFunc) Character::lowSurrogate).apply(List.of(Values.valueOf(0x1F600))), Character.lowSurrogate(0x1F600));
        assertKnownFloat(((I2FStaticFunc) Float::intBitsToFloat).apply(List.of(Values.valueOf(0x3f800000))), 1.0F);
        assertKnownInt(((I2IStaticFunc) Integer::bitCount).apply(List.of(Values.valueOf(0b1011))), 3);
        assertKnownLong(((I2JStaticFunc) Integer::toUnsignedLong).apply(List.of(Values.valueOf(-1))), Integer.toUnsignedLong(-1));
        assertKnownString(((I2StringStaticFunc) Integer::toHexString).apply(List.of(Values.valueOf(255))), "ff");
        assertKnownBoolean(((I2ZStaticFunc) Character::isBmpCodePoint).apply(List.of(Values.valueOf(65))), true);
        assertKnownInt(((II2IStaticFunc) Integer::compare).apply(List.of(Values.valueOf(4), Values.valueOf(9))), -1);
        assertKnownInt(((II2IThrowingStaticFunc) Math::floorDiv).apply(List.of(Values.valueOf(9), Values.valueOf(4))), 2);
        assertKnownDouble(((J2DStaticFunc) Double::longBitsToDouble).apply(List.of(Values.valueOf(0x3ff0000000000000L))), 1.0D);
        assertKnownInt(((J2IStaticFunc) Long::bitCount).apply(List.of(Values.valueOf(0b1011L))), 3);
        assertKnownInt(((J2IThrowingStaticFunc) Math::toIntExact).apply(List.of(Values.valueOf(9L))), 9);
        assertKnownLong(((J2JStaticFunc) Long::reverse).apply(List.of(Values.valueOf(1L))), Long.reverse(1L));
        assertKnownLong(((JI2JThrowingStaticFunc) Long::rotateLeft).apply(List.of(Values.valueOf(1L), Values.valueOf(3))), Long.rotateLeft(1L, 3));
        assertKnownInt(((JJ2IThrowingStaticFunc) Long::compareUnsigned).apply(List.of(Values.valueOf(1L), Values.valueOf(2L))), -1);
        assertKnownLong(((JJ2JStaticFunc) Long::sum).apply(List.of(Values.valueOf(1L), Values.valueOf(2L))), 3L);
        assertKnownLong(((JJ2JThrowingStaticFunc) Long::divideUnsigned).apply(List.of(Values.valueOf(9L), Values.valueOf(3L))), 3L);
    }

    @Test
    void adaptersFallBackToUnknownValuesWhenArgumentsDoNotMatch() {
        assertSame(Values.STRING_VALUE, ((B2StringStaticFunc) Byte::toString).apply(List.of(Values.valueOfString("bad"))));
        assertSame(Values.DOUBLE_VALUE, ((DD2DStaticFunc) Math::max).apply(List.of(Values.valueOf(1.0D), Values.valueOfString("bad"))));
        assertSame(Values.INT_VALUE, ((I2IStaticFunc) Integer::bitCount).apply(List.of(Values.valueOfString("bad"))));
        assertSame(Values.LONG_VALUE, ((JJ2JThrowingStaticFunc) Long::divideUnsigned).apply(List.of(Values.valueOf(1L), Values.valueOf(0L))));
    }

    private static void assertKnownBoolean(Value value, boolean expected) {
        assertKnownInt(value, expected ? 1 : 0);
    }

    private static void assertKnownInt(Value value, int expected) {
        Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
        assertEquals(expected, known.value());
    }

    private static void assertKnownLong(Value value, long expected) {
        Value.KnownLongValue known = assertInstanceOf(Value.KnownLongValue.class, value);
        assertEquals(expected, known.value());
    }

    private static void assertKnownFloat(Value value, float expected) {
        Value.KnownFloatValue known = assertInstanceOf(Value.KnownFloatValue.class, value);
        assertEquals(expected, known.value());
    }

    private static void assertKnownDouble(Value value, double expected) {
        Value.KnownDoubleValue known = assertInstanceOf(Value.KnownDoubleValue.class, value);
        assertEquals(expected, known.value());
    }

    private static void assertKnownString(Value value, String expected) {
        Value.KnownStringValue known = assertInstanceOf(Value.KnownStringValue.class, value);
        assertEquals(expected, known.value());
    }
}

package me.darknet.assembler.analysis;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.analysis.registry.BooleanMethodValueRegistry;
import me.darknet.assembler.analysis.registry.ByteMethodValueRegistry;
import me.darknet.assembler.analysis.registry.CharacterMethodValueRegistry;
import me.darknet.assembler.analysis.registry.DoubleMethodValueRegistry;
import me.darknet.assembler.analysis.registry.FloatMethodValueRegistry;
import me.darknet.assembler.analysis.registry.IntegerMethodValueRegistry;
import me.darknet.assembler.analysis.registry.LongMethodValueRegistry;
import me.darknet.assembler.analysis.registry.MathMethodValueRegistry;
import me.darknet.assembler.analysis.registry.MethodValueRegistry;
import me.darknet.assembler.analysis.registry.ShortMethodValueRegistry;
import me.darknet.assembler.analysis.registry.StringMethodValueRegistry;
import me.darknet.assembler.analysis.registry.SystemMethodValueRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link MethodValueRegistry} implementations, ensuring that they resolve known
 * method calls to known values and fall back to unknown values when inputs are not known.
 */
class MethodValueRegistryTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("registryHappyPaths")
    void resolvesRepresentativeRegistryEntries(String name, MethodValueRegistry registry, MethodReference instruction,
                                               Value.ObjectValue context, List<Value> params, Consumer<Value> assertion) {
        Value value = registry.accept(instruction, context, params);
        assertNotNull(value, name);
        assertion.accept(value);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("registryFallbackPaths")
    void fallsBackToUnknownValuesWhenInputsAreNotKnown(String name, MethodValueRegistry registry, MethodReference instruction,
                                                       Value.ObjectValue context, List<Value> params, Consumer<Value> assertion) {
        Value value = registry.accept(instruction, context, params);
        assertNotNull(value, name);
        assertion.accept(value);
    }

    @Test
    void mergesStaticAndStringRegistries() {
        MethodValueRegistry registry = MethodValueRegistry.builder()
                .merge(StringMethodValueRegistry.create())
                .merge(MathMethodValueRegistry.create())
                .build();

        Value stringValue = registry.accept(
                reference("java/lang/Object", "toString", "()Ljava/lang/String;"),
                Values.valueOfString("hello"),
                List.of()
        );
        MethodReference maxReference = new MethodReference("java/lang/Math", "max", "(II)I");
        Value mathValue = registry.accept(
                maxReference,
                null,
                List.of(Values.valueOf(2), Values.valueOf(9))
        );
        Value unknownMathValue = registry.accept(maxReference, null, List.of(Values.INT_VALUE, Values.valueOf(9)));

        assertEquals("hello", assertInstanceOf(Value.KnownStringValue.class, stringValue).value());
        assertEquals(9, assertInstanceOf(Value.KnownIntValue.class, mathValue).value());
        assertSame(Values.INT_VALUE, unknownMathValue);
    }

    private static Stream<Arguments> registryHappyPaths() {
        return Stream.of(
                arguments(
                        "Boolean registry",
                        BooleanMethodValueRegistry.create(),
                        reference("java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z"),
                        null,
                        List.of(Values.valueOfString("true")),
                        (Consumer<Value>) value -> assertEquals(1, assertInstanceOf(Value.KnownIntValue.class, value).value())
                ),
                arguments(
                        "Byte registry",
                        ByteMethodValueRegistry.create(),
                        reference("java/lang/Byte", "toUnsignedLong", "(B)J"),
                        null,
                        List.of(Values.valueOf((byte) -1)),
                        (Consumer<Value>) value -> assertEquals(255L, assertInstanceOf(Value.KnownLongValue.class, value).value())
                ),
                arguments(
                        "Character registry",
                        CharacterMethodValueRegistry.create(),
                        reference("java/lang/Character", "codePointOf", "(Ljava/lang/String;)I"),
                        null,
                        List.of(Values.valueOfString("LATIN CAPITAL LETTER A")),
                        (Consumer<Value>) value -> assertEquals(65, assertInstanceOf(Value.KnownIntValue.class, value).value())
                ),
                arguments(
                        "Double registry",
                        DoubleMethodValueRegistry.create(),
                        reference("java/lang/Double", "toString", "(D)Ljava/lang/String;"),
                        null,
                        List.of(Values.valueOf(1.5D)),
                        (Consumer<Value>) value -> assertEquals("1.5", assertInstanceOf(Value.KnownStringValue.class, value).value())
                ),
                arguments(
                        "Float registry",
                        FloatMethodValueRegistry.create(),
                        reference("java/lang/Float", "hashCode", "(F)I"),
                        null,
                        List.of(Values.valueOf(1.5F)),
                        (Consumer<Value>) value -> assertEquals(Float.hashCode(1.5F), assertInstanceOf(Value.KnownIntValue.class, value).value())
                ),
                arguments(
                        "Integer registry",
                        IntegerMethodValueRegistry.create(),
                        reference("java/lang/Integer", "toHexString", "(I)Ljava/lang/String;"),
                        null,
                        List.of(Values.valueOf(255)),
                        (Consumer<Value>) value -> assertEquals("ff", assertInstanceOf(Value.KnownStringValue.class, value).value())
                ),
                arguments(
                        "Long registry",
                        LongMethodValueRegistry.create(),
                        reference("java/lang/Long", "parseLong", "(Ljava/lang/CharSequence;III)J"),
                        null,
                        List.of(Values.valueOfString("0f"), Values.valueOf(0), Values.valueOf(2), Values.valueOf(16)),
                        (Consumer<Value>) value -> assertEquals(15L, assertInstanceOf(Value.KnownLongValue.class, value).value())
                ),
                arguments(
                        "Math registry",
                        MathMethodValueRegistry.create(),
                        reference("java/lang/StrictMath", "round", "(D)J"),
                        null,
                        List.of(Values.valueOf(1.6D)),
                        (Consumer<Value>) value -> assertEquals(2L, assertInstanceOf(Value.KnownLongValue.class, value).value())
                ),
                arguments(
                        "Short registry",
                        ShortMethodValueRegistry.create(),
                        reference("java/lang/Short", "toUnsignedLong", "(S)J"),
                        null,
                        List.of(Values.valueOf((short) -1)),
                        (Consumer<Value>) value -> assertEquals(65535L, assertInstanceOf(Value.KnownLongValue.class, value).value())
                ),
                arguments(
                        "String registry",
                        StringMethodValueRegistry.create(),
                        reference("java/lang/String", "substring", "(II)Ljava/lang/String;"),
                        Values.valueOfString("hello"),
                        List.of(Values.valueOf(5), Values.valueOf(5)),
                        (Consumer<Value>) value -> assertEquals("", assertInstanceOf(Value.KnownStringValue.class, value).value())
                ),
                arguments(
                        "System registry",
                        SystemMethodValueRegistry.create(),
                        reference("java/lang/System", "lineSeparator", "()Ljava/lang/String;"),
                        null,
                        List.of(),
                        (Consumer<Value>) value -> assertEquals(System.lineSeparator(), assertInstanceOf(Value.KnownStringValue.class, value).value())
                )
        );
    }

    private static Stream<Arguments> registryFallbackPaths() {
        return Stream.of(
                arguments(
                        "Boolean fallback",
                        BooleanMethodValueRegistry.create(),
                        reference("java/lang/Boolean", "toString", "(Z)Ljava/lang/String;"),
                        null,
                        List.of(Values.STRING_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.STRING_VALUE, value)
                ),
                arguments(
                        "Byte fallback",
                        ByteMethodValueRegistry.create(),
                        reference("java/lang/Byte", "toUnsignedLong", "(B)J"),
                        null,
                        List.of(Values.INT_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.LONG_VALUE, value)
                ),
                arguments(
                        "Character fallback",
                        CharacterMethodValueRegistry.create(),
                        reference("java/lang/Character", "codePointOf", "(Ljava/lang/String;)I"),
                        null,
                        List.of(Values.STRING_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.INT_VALUE, value)
                ),
                arguments(
                        "Double fallback",
                        DoubleMethodValueRegistry.create(),
                        reference("java/lang/Double", "toHexString", "(D)Ljava/lang/String;"),
                        null,
                        List.of(Values.DOUBLE_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.STRING_VALUE, value)
                ),
                arguments(
                        "Float fallback",
                        FloatMethodValueRegistry.create(),
                        reference("java/lang/Float", "parseFloat", "(Ljava/lang/String;)F"),
                        null,
                        List.of(Values.STRING_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.FLOAT_VALUE, value)
                ),
                arguments(
                        "Integer fallback",
                        IntegerMethodValueRegistry.create(),
                        reference("java/lang/Integer", "toString", "(II)Ljava/lang/String;"),
                        null,
                        List.of(Values.INT_VALUE, Values.INT_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.STRING_VALUE, value)
                ),
                arguments(
                        "Long fallback",
                        LongMethodValueRegistry.create(),
                        reference("java/lang/Long", "parseLong", "(Ljava/lang/String;)J"),
                        null,
                        List.of(Values.STRING_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.LONG_VALUE, value)
                ),
                arguments(
                        "Math fallback",
                        MathMethodValueRegistry.create(),
                        reference("java/lang/Math", "round", "(D)J"),
                        null,
                        List.of(Values.DOUBLE_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.LONG_VALUE, value)
                ),
                arguments(
                        "Short fallback",
                        ShortMethodValueRegistry.create(),
                        reference("java/lang/Short", "toUnsignedLong", "(S)J"),
                        null,
                        List.of(Values.INT_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.LONG_VALUE, value)
                ),
                arguments(
                        "String fallback",
                        StringMethodValueRegistry.create(),
                        reference("java/lang/String", "substring", "(II)Ljava/lang/String;"),
                        Values.valueOfString("hello"),
                        List.of(Values.INT_VALUE, Values.INT_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.STRING_VALUE, value)
                ),
                arguments(
                        "System fallback",
                        SystemMethodValueRegistry.create(),
                        reference("java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;"),
                        null,
                        List.of(Values.STRING_VALUE),
                        (Consumer<Value>) value -> assertSame(Values.STRING_VALUE, value)
                )
        );
    }

    private static MethodReference reference(String owner, String name, String descriptor) {
        return new MethodReference(owner, name, descriptor);
    }
}

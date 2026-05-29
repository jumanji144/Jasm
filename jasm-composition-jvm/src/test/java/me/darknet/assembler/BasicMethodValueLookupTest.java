package me.darknet.assembler;

import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.instruction.MethodInstruction;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    void resolvesLongNumberOfLeadingZeros() {
        Value value = invokeStatic(
                "java/lang/Long",
                "numberOfLeadingZeros",
                "(J)I",
                Values.valueOf(1L)
        );

        Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
        assertEquals(Long.numberOfLeadingZeros(1L), known.value());
    }

    @Test
    void resolvesMathMaxForKnownDoubles() {
        Value value = invokeStatic(
                "java/lang/Math",
                "max",
                "(DD)D",
                Values.valueOf(1.5D),
                Values.valueOf(9.0D)
        );

        Value.KnownDoubleValue known = assertInstanceOf(Value.KnownDoubleValue.class, value);
        assertEquals(9.0D, known.value());
    }

    @Test
    void resolvesMathFmaForKnownDoubles() {
        Value value = invokeStatic(
                "java/lang/Math",
                "fma",
                "(DDD)D",
                Values.valueOf(2.0D),
                Values.valueOf(3.0D),
                Values.valueOf(4.0D)
        );

        Value.KnownDoubleValue known = assertInstanceOf(Value.KnownDoubleValue.class, value);
        assertEquals(Math.fma(2.0D, 3.0D, 4.0D), known.value());
    }

    @Test
    void resolvesBooleanCompare() {
        Value value = invokeStatic(
                "java/lang/Boolean",
                "compare",
                "(ZZ)I",
                Values.INT_1,
                Values.INT_0
        );

        Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
        assertEquals(Boolean.compare(true, false), known.value());
    }

    @Test
    void resolvesBooleanHashCode() {
        Value value = invokeStatic(
                "java/lang/Boolean",
                "hashCode",
                "(Z)I",
                Values.INT_1
        );

        Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
        assertEquals(Boolean.hashCode(true), known.value());
    }

    @Test
    void resolvesKnownStringInstanceCalls() {
        Value value = LOOKUP.accept(
                new MethodInstruction(
                        JavaOpcodes.INVOKEVIRTUAL,
                        Types.instanceType(String.class),
                        "length",
                        Types.methodType("()I"),
                        false
                ),
                Values.valueOfString("hello"),
                List.of()
        );

        Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
        assertEquals(5, known.value());
    }

    private static Value invokeStatic(String owner, String name, String descriptor, Value... params) {
        MethodInstruction instruction = new MethodInstruction(
                JavaOpcodes.INVOKESTATIC,
                Types.instanceTypeFromInternalName(owner),
                name,
                Types.methodType(descriptor),
                false
        );
        Value value = LOOKUP.accept(instruction, null, List.of(params));
        assertNotNull(value, "Expected method lookup to produce a value");
        return value;
    }
}

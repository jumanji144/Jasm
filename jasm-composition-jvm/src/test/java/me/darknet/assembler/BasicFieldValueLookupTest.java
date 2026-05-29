package me.darknet.assembler;

import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.instruction.FieldInstruction;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.BasicFieldValueLookup;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BasicFieldValueLookup}.
 */
class BasicFieldValueLookupTest {
    private static final BasicFieldValueLookup LOOKUP = new BasicFieldValueLookup();

    @Test
    void resolvesKnownStaticConstantFields() {
        // Proper field lookup should give us a value with a known value
        Value value = LOOKUP.accept(field(JavaOpcodes.GETSTATIC, "java/lang/Integer", "MAX_VALUE"), null);

        Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
        assertEquals(Integer.MAX_VALUE, known.value());
    }

    @Test
    void ignoresConstantRegistryForInstanceFieldReads() {
        // This is an instance lookup, not a static lookup, so the static field registry should not give us a value here.
        Value value = LOOKUP.accept(
                field(JavaOpcodes.GETFIELD, "java/lang/Integer", "MAX_VALUE"),
                Values.valueOfInstance(Types.instanceType(Integer.class))
        );

        assertNull(value);
    }

    @Test
    void returnsNullForUnknownStaticFields() {
        Value value = LOOKUP.accept(field(JavaOpcodes.GETSTATIC, "java/lang/String", "DOES_NOT_EXIST"), null);

        assertNull(value);
    }

    private static FieldInstruction field(int opcode, String owner, String name) {
        return new FieldInstruction(opcode, Types.instanceTypeFromInternalName(owner), name, Types.INT);
    }
}

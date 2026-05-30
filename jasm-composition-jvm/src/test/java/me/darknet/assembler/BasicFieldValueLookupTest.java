package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.BasicFieldValueLookup;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BasicFieldValueLookup}.
 */
class BasicFieldValueLookupTest {
    private static final BasicFieldValueLookup LOOKUP = new BasicFieldValueLookup();

    @Test
    void resolvesKnownStaticConstantFields() {
        // Proper field lookup should give us a value with a known value
        Value value = LOOKUP.accept(field(Opcodes.GETSTATIC, "java/lang/Integer", "MAX_VALUE"), null);

        Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
        assertEquals(Integer.MAX_VALUE, known.value());
    }

    @Test
    void ignoresConstantRegistryForInstanceFieldReads() {
        // This is an instance lookup, not a static lookup, so the static field registry should not give us a value here.
        Value value = LOOKUP.accept(
                field(Opcodes.GETFIELD, "java/lang/Integer", "MAX_VALUE"),
                Values.valueOfInstance(Type.getType(Integer.class))
        );

        assertNull(value);
    }

    @Test
    void returnsNullForUnknownStaticFields() {
        Value value = LOOKUP.accept(field(Opcodes.GETSTATIC, "java/lang/String", "DOES_NOT_EXIST"), null);

        assertNull(value);
    }

    private static FieldInsnNode field(int opcode, String owner, String name) {
        return new FieldInsnNode(opcode, owner, name, Type.INT_TYPE.getDescriptor());
    }
}

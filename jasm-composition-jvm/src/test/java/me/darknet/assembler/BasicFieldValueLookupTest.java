package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.BasicFieldValueLookup;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BasicFieldValueLookup}.
 */
class BasicFieldValueLookupTest {
	private static final BasicFieldValueLookup LOOKUP = new BasicFieldValueLookup();

	@Test
	void resolvesKnownStaticConstantFields() {
		// Proper field lookup should give us a value with a known value
		Value value = requireStaticField("java/lang/Integer", "MAX_VALUE");
		Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value);
		assertEquals(Integer.MAX_VALUE, known.value());
	}

	@Test
	void ignoresConstantRegistryForInstanceFieldReads() {
		// This is an instance lookup, not a static lookup, so the static field registry should not give us a value here.
		Value value = findInstanceField(instanceOf(Integer.class), "java/lang/Integer", "MAX_VALUE");
		assertNull(value);
	}

	@Test
	void returnsNullForUnknownStaticFields() {
		Value value = findStaticField("java/lang/String", "DOES_NOT_EXIST");
		assertNull(value);
	}

	@Test
	void resolvesFloatingPointAndFileConstants() {
		Value.KnownDoubleValue pi = assertInstanceOf(Value.KnownDoubleValue.class,
				requireStaticField("java/lang/Math", "PI"));
		Value.KnownStringValue separator = assertInstanceOf(Value.KnownStringValue.class,
				requireStaticField("java/io/File", "separator"));
		Value.KnownIntValue separatorChar = assertInstanceOf(Value.KnownIntValue.class,
				requireStaticField("java/io/File", "separatorChar"));

		assertEquals(Math.PI, pi.value());
		assertEquals(File.separator, separator.value());
		assertEquals(File.separatorChar, separatorChar.value());
	}

	@Test
	void resolvesCharacterAndFloatMetadataConstants() {
		Value.KnownIntValue highSurrogate = assertInstanceOf(Value.KnownIntValue.class,
				requireStaticField("java/lang/Character", "MIN_HIGH_SURROGATE"));
		Value.KnownIntValue directionality = assertInstanceOf(Value.KnownIntValue.class,
				requireStaticField("java/lang/Character", "DIRECTIONALITY_LEFT_TO_RIGHT"));
		Value.KnownFloatValue nan = assertInstanceOf(Value.KnownFloatValue.class,
				requireStaticField("java/lang/Float", "NaN"));

		assertEquals(Character.MIN_HIGH_SURROGATE, highSurrogate.value());
		assertEquals(Character.DIRECTIONALITY_LEFT_TO_RIGHT, directionality.value());
		assertTrue(Float.isNaN(nan.value()));
	}

	private static @NotNull Value requireInstanceField(Value.ObjectValue context, String owner, String name) {
		Value value = findInstanceField(context, owner, name);
		assertNotNull(value, "Expected to find instance field " + owner + "." + name);
		return value;
	}

	private static @Nullable Value findInstanceField(Value.ObjectValue context, String owner, String name) {
		return LOOKUP.accept(field(Opcodes.GETFIELD, owner, name), context);
	}

	private static @NotNull Value requireStaticField(String owner, String name) {
		Value value = findStaticField(owner, name);
		assertNotNull(value, "Expected to find static field " + owner + "." + name);
		return value;
	}

	private static @Nullable Value findStaticField(String owner, String name) {
		return LOOKUP.accept(field(Opcodes.GETSTATIC, owner, name), null);
	}

	private static FieldInsnNode field(int opcode, String owner, String name) {
		return new FieldInsnNode(opcode, owner, name, Type.INT_TYPE.getDescriptor());
	}

	private static Value.ObjectValue instanceOf(Class<?> clazz) {
		return Values.valueOfInstance(Type.getType(clazz));
	}
}

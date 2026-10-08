package me.darknet.assembler;

import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.assembler.descriptor.Descriptors;
import me.darknet.assembler.descriptor.PrimitiveType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescriptorsTest {
	@Test
	void returnsComputationalPrimitiveCategories() {
		assertEquals(PrimitiveType.INT, Descriptors.primitiveCategory("Z"));
		assertEquals(PrimitiveType.INT, Descriptors.primitiveCategory("B"));
		assertEquals(PrimitiveType.INT, Descriptors.primitiveCategory("S"));
		assertEquals(PrimitiveType.INT, Descriptors.primitiveCategory("C"));
		assertEquals(PrimitiveType.INT, Descriptors.primitiveCategory("I"));
		assertEquals(PrimitiveType.FLOAT, Descriptors.primitiveCategory("F"));
		assertEquals(PrimitiveType.LONG, Descriptors.primitiveCategory("J"));
		assertEquals(PrimitiveType.DOUBLE, Descriptors.primitiveCategory("D"));

		assertNull(Descriptors.primitiveCategory("V"));
		assertNull(Descriptors.primitiveCategory("[I"));
		assertNull(Descriptors.primitiveCategory("Ljava/lang/String;"));
		assertNull(Descriptors.primitiveCategory(""));
	}

	@Test
	void identifiesWideDescriptorTypes() {
		assertTrue(Descriptors.isWideType(PrimitiveType.LONG));
		assertTrue(Descriptors.isWideType(PrimitiveType.DOUBLE));
		assertFalse(Descriptors.isWideType(PrimitiveType.INT));
		assertFalse(Descriptors.isWideType((DescriptorType) null));

		assertTrue(Descriptors.isWideType("J"));
		assertTrue(Descriptors.isWideType("D"));
		assertFalse(Descriptors.isWideType("I"));
		assertFalse(Descriptors.isWideType("[J"));
		assertFalse(Descriptors.isWideType("JJ"));
	}

	@Test
	void returnsOnlyArrayDescriptors() {
		assertEquals("[F", Descriptors.arrayDescriptor("[F"));
		assertEquals("[[Ljava/lang/Object;", Descriptors.arrayDescriptor("[[Ljava/lang/Object;"));
		assertNull(Descriptors.arrayDescriptor("F"));
		assertNull(Descriptors.arrayDescriptor("Ljava/lang/Object;"));
	}
}

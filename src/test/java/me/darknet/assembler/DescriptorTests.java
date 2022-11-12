package me.darknet.assembler;

import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DescriptorTests {
	@Nested
	class Methods {
		@Test
		void testRunWithoutOwner() {
			MethodDescriptor descriptor = new MethodDescriptor("run", "()V");
			assertFalse(descriptor.hasDeclaredOwner());
			assertNull(descriptor.getOwner());
			assertEquals("run", descriptor.getName());
			assertEquals("V", descriptor.getReturnType());
		}

		@Test
		void testRunWithOwner() {
			MethodDescriptor descriptor = new MethodDescriptor("java/lang/Runnable.run", "()V");
			assertTrue(descriptor.hasDeclaredOwner());
			assertEquals("java/lang/Runnable", descriptor.getOwner());
			assertEquals("run", descriptor.getName());
			assertEquals("V", descriptor.getReturnType());
		}
	}

	@Nested
	class Fields {

		@Test
		void testRunWithoutOwner() {
			FieldDescriptor descriptor = new FieldDescriptor("out", "Ljava/io/PrintStream;");
			assertFalse(descriptor.hasDeclaredOwner());
			assertNull(descriptor.getOwner());
			assertEquals("out", descriptor.getName());
			assertEquals("Ljava/io/PrintStream;", descriptor.getDesc());
		}

		@Test
		void testRunWithOwner() {
			FieldDescriptor descriptor = new FieldDescriptor("java/lang/System.out", "Ljava/io/PrintStream;");
			assertTrue(descriptor.hasDeclaredOwner());
			assertEquals("java/lang/System", descriptor.getOwner());
			assertEquals("out", descriptor.getName());
			assertEquals("Ljava/io/PrintStream;", descriptor.getDesc());
		}
	}
}

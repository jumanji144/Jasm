package me.darknet.assembler;

import me.darknet.assembler.compile.MethodVariableLayout;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link MethodVariableLayout}.
 */
class MethodVariableLayoutTest {
	@Test
	void instanceMethodsKeepReceiverInSourceIndexSpace() {
		MethodVariableLayout layout = MethodVariableLayout.fromMethod(
				Type.getObjectType("example/Owner"),
				new MethodNode(Opcodes.ACC_PUBLIC, "test", "(J)V", null, null)
		);

		// Source syntax keeps the implicit receiver as an explicit first parameter: { this, count }.
		assertEquals(2, layout.sourceParameterCount());

		// The receiver does not exist in JVM MethodParameters / parameter-annotation arrays.
		assertEquals(-1, layout.sourceIndexToJvmParameterIndex(0));

		// The receiver still occupies local slot 0 in the method frame.
		assertEquals(0, layout.sourceIndexToLocalSlot(0));

		// The long argument is the first real JVM parameter once 'this' is excluded.
		assertEquals(0, layout.sourceIndexToJvmParameterIndex(1));

		// In an instance method the first explicit argument starts after local slot 0.
		assertEquals(1, layout.sourceIndexToLocalSlot(1));

		// Looking up JVM parameter 0 should map back to the source parameter after 'this'.
		assertEquals(1, layout.jvmParameterIndexToSourceIndex(0));
	}

	@Test
	void staticMethodsUseSourceAndJvmIndicesDirectly() {
		MethodVariableLayout layout = MethodVariableLayout.fromMethod(
				Type.getObjectType("example/Owner"),
				new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "(DJ)V", null, null)
		);

		// Static methods have no receiver, so the source list is just { doubleArg, longArg }.
		assertEquals(2, layout.sourceParameterCount());

		// Without 'this', the first source parameter is also JVM parameter 0.
		assertEquals(0, layout.sourceIndexToJvmParameterIndex(0));

		// And it starts at local slot 0 because there is no receiver occupying that slot.
		assertEquals(0, layout.sourceIndexToLocalSlot(0));

		// The second source parameter is likewise JVM parameter 1.
		assertEquals(1, layout.sourceIndexToJvmParameterIndex(1));

		// The first parameter is a double, so it consumes local slots 0 and 1.
		// That makes the following long start at local slot 2.
		assertEquals(2, layout.sourceIndexToLocalSlot(1));

		// Reverse mappings are also direct when there is no receiver offset.
		assertEquals(0, layout.jvmParameterIndexToSourceIndex(0));
		assertEquals(1, layout.jvmParameterIndexToSourceIndex(1));
	}

	@Test
	void wideParametersAdvanceLocalSlotsWithoutChangingJvmParameterIndices() {
		MethodVariableLayout layout = MethodVariableLayout.fromMethod(
				Type.getObjectType("example/Owner"),
				new MethodNode(Opcodes.ACC_PUBLIC, "test", "(Ljava/lang/String;J)V", null, null)
		);

		// Source syntax is { this, name, count } for an instance method with two explicit arguments.
		assertEquals(3, layout.sourceParameterCount());

		// As before, the receiver is not a JVM parameter.
		assertEquals(-1, layout.sourceIndexToJvmParameterIndex(0));

		// But it still owns local slot 0.
		assertEquals(0, layout.sourceIndexToLocalSlot(0));

		// The String is the first explicit argument, so it becomes JVM parameter 0.
		assertEquals(0, layout.sourceIndexToJvmParameterIndex(1));

		// It starts at local slot 1 because slot 0 is reserved for 'this'.
		assertEquals(1, layout.sourceIndexToLocalSlot(1));

		// The following long is only the second JVM parameter even though it is wide.
		// Width affects local slots, not JVM parameter numbering.
		assertEquals(1, layout.sourceIndexToJvmParameterIndex(2));

		// The long starts at slot 2 because the receiver uses slot 0 and the String uses slot 1.
		assertEquals(2, layout.sourceIndexToLocalSlot(2));
	}
}

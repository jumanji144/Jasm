package me.darknet.assembler;

import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.Descriptor;
import me.darknet.assembler.descriptor.DescriptorForm;
import me.darknet.assembler.descriptor.DescriptorParser;
import me.darknet.assembler.descriptor.DescriptorSyntaxException;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.assembler.descriptor.MethodDescriptor;
import me.darknet.assembler.descriptor.PrimitiveType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the shared descriptor grammar: what it accepts, what it rejects, and the model it produces.
 */
public class DescriptorParserTest {

	@Test
	void parsesPrimitivesClassesAndArrays() {
		assertEquals(PrimitiveType.INT, DescriptorParser.parseFieldDescriptor("I"));
		assertEquals(PrimitiveType.LONG, DescriptorParser.parseFieldDescriptor("J"));

		ClassDescriptor type = assertInstanceOf(ClassDescriptor.class, DescriptorParser.parseFieldDescriptor("Ljava/lang/String;"));
		assertEquals("java/lang/String", type.internalName());
		assertEquals("Ljava/lang/String;", type.descriptor());

		// The component is the record's own datum, so a nested array is read by unwrapping once per
		// dimension rather than through a derived accessor.
		ArrayDescriptor array = assertInstanceOf(
				ArrayDescriptor.class, DescriptorParser.parseFieldDescriptor("[[Ljava/lang/String;"));
		ArrayDescriptor inner = assertInstanceOf(ArrayDescriptor.class, array.component());
		assertEquals(new ClassDescriptor("java/lang/String"), inner.component());
		assertEquals("[[Ljava/lang/String;", array.descriptor());
		assertEquals("Ljava/lang/String;", inner.component().descriptor());
	}

	@Test
	void modelsMethodSignatures() {
		MethodDescriptor signature = DescriptorParser.parseMethodDescriptor("(I[JLjava/lang/String;)V");
		assertEquals(List.of(PrimitiveType.INT, arrayOf(PrimitiveType.LONG), new ClassDescriptor("java/lang/String")),
				signature.parameters());
		assertEquals(PrimitiveType.VOID, signature.returnType());
		assertEquals("(I[JLjava/lang/String;)V", signature.descriptor());
	}

	@Test
	void reportsWhereAndWhyADescriptorIsMalformed() {
		assertEquals("missing terminating ';' at index 4", reasonFor("Lbad", DescriptorForm.FIELD));
		assertEquals("missing ')' at index 2", reasonFor("(I", DescriptorForm.METHOD));
		assertEquals("missing type descriptor at index 2", reasonFor("()", DescriptorForm.METHOD));
		assertEquals("unknown type descriptor character 'X' at index 0", reasonFor("X", DescriptorForm.FIELD));
		assertEquals("void is only valid as a method return type at index 0", reasonFor("V", DescriptorForm.FIELD));
		assertEquals("unexpected trailing 'I' at index 1", reasonFor("II", DescriptorForm.FIELD));
		assertEquals("void is only valid as a method return type at index 1", reasonFor("[V", DescriptorForm.FIELD));
		assertEquals("expected a class or array type, not a primitive", reasonFor("I", DescriptorForm.CLASS_OR_ARRAY));
		assertEquals("expected a class type, not an array", reasonFor("[I", DescriptorForm.CLASS_TYPE));
		assertEquals("method descriptor is not a value type", reasonFor("(I)V", DescriptorForm.TYPE_REFERENCE));
	}

	@Test
	void distinguishesInternalNamesFromClassDescriptors() {
		// 'Lbad' has no terminator, so it names the class 'Lbad' rather than the class 'bad'.
		assertEquals(new ClassDescriptor("Lbad"),
				DescriptorParser.parseTypeReference("Lbad"));
		assertEquals(new ClassDescriptor("bad"),
				DescriptorParser.parseTypeReference("Lbad;"));
		assertEquals(new ClassDescriptor("java/lang/String"),
				DescriptorParser.parseTypeReference("java/lang/String"));
		assertEquals(arrayOf(PrimitiveType.INT),
				DescriptorParser.parseTypeReference("[I"));

		// A method signature is never a value type, however it is spelled.
		assertThrows(DescriptorSyntaxException.class,
				() -> DescriptorParser.parseTypeReference("(I)V"));
	}

	@Test
	void rejectsNamesThatCannotBeClassNames() {
		assertThrows(IllegalArgumentException.class, () -> DescriptorParser.requireValidInternalName(""));
		assertThrows(IllegalArgumentException.class, () -> DescriptorParser.requireValidInternalName("a b"));
		// A space is not a legal internal name character, so this cannot be read as an internal name.
		assertNull(DescriptorParser.tryParse("a b", DescriptorForm.TYPE_REFERENCE));
		assertEquals("character ' ' is not allowed in a class name at index 1",
				reasonFor("a b", DescriptorForm.TYPE_REFERENCE));
	}

	@Test
	void acceptsEverySampleDescriptorShape() {
		// Shapes actually present in the bundled JVM and Dalvik samples, plus the alias spellings.
		for (String descriptor : List.of("()I", "()LExample;", "()Ljava/lang/Class;", "()V", "()Z",
				"(LExample;)LExample;", "([Ljava/lang/String;)V", "(I[JLjava/lang/String;)V")) {
			assertTrue(DescriptorParser.isValidMethodDescriptor(descriptor), descriptor);
		}
		for (String descriptor : List.of("I", "J", "Z", "[I", "[Ljava/lang/String;", "Ljava/lang/Object;")) {
			assertTrue(DescriptorParser.isValidFieldDescriptor(descriptor), descriptor);
		}
	}

	@Test
	void reportsVoidAsAnInvalidFieldType() {
		// The previous grammar walker accepted 'V' as a field descriptor, which no stack can hold.
		assertFalse(DescriptorParser.isValidFieldDescriptor("V"));
		assertFalse(DescriptorParser.isValidFieldDescriptor("[V"));
		assertTrue(DescriptorParser.isValidMethodDescriptor("()V"));
	}

	@Test
	void internalNameFormRejectsDescriptorSpellings() {
		// A class name is written straight into a class reference, where a descriptor does not fail
		// loudly: it produces a class literally named "Ljava/lang/Object;".
		assertEquals(new ClassDescriptor("java/lang/String"),
				DescriptorParser.parse("java/lang/String", DescriptorForm.INTERNAL_NAME));

		assertNull(DescriptorParser.tryParse("Ljava/lang/String;", DescriptorForm.INTERNAL_NAME));
		assertNull(DescriptorParser.tryParse("[I", DescriptorForm.INTERNAL_NAME));
		assertNull(DescriptorParser.tryParse("a b", DescriptorForm.INTERNAL_NAME));
		assertNull(DescriptorParser.tryParse("", DescriptorForm.INTERNAL_NAME));

		assertEquals("character ';' is not allowed in a class name at index 17",
				reasonFor("Ljava/lang/String;", DescriptorForm.INTERNAL_NAME));
		assertEquals("missing class name", reasonFor("", DescriptorForm.INTERNAL_NAME));
	}

	@Test
	void parsesTheSameTextIntoTheModelItRendersBack() {
		// Round-tripping the model through its own descriptor() is what makes the model safe to store
		// instead of the original text.
		for (String source : List.of("I", "J", "Z", "[I", "[[J", "Ljava/lang/Object;", "[Ljava/util/List;",
				"()V", "(I)V", "(IJ)Ljava/lang/String;", "([Ljava/lang/String;)Z")) {
			Descriptor parsed = DescriptorParser.parseAny(source);
			assertEquals(source, parsed.descriptor(), source);
		}
	}

	private static String reasonFor(String source, DescriptorForm form) {
		DescriptorSyntaxException failure = assertThrows(DescriptorSyntaxException.class, () -> DescriptorParser.parse(source, form));
		return failure.detail();
	}

	private static ArrayDescriptor arrayOf(DescriptorType component) {
		return new ArrayDescriptor(component);
	}

}

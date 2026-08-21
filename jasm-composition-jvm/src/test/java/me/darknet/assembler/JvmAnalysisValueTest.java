package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.BasicFieldValueLookup;
import me.darknet.assembler.compile.analysis.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.MethodAnalysisLookup;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.test.JvmAssemblerFixture;
import me.darknet.assembler.test.JvmCompilation;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate basic engine value propagation and merging behavior.
 */
public class JvmAnalysisValueTest {
	private static final TestJvmCompilerOptions VALUE_OPTS = new TestJvmCompilerOptions();
	private static final TestJvmCompilerOptions TYPE_OPTS = new TestJvmCompilerOptions();
	private static final String TEMPLATE_INT_SUPPLIER = """
			.super java/lang/Object
			.class Example {
			    .method public static example ()I {
			        code: {
			        BEGIN:
			        \0
				    END:
			        }
			    }
			}
			""";
	private static final String TEMPLATE_OBJECT_SUPPLIER = """
			.super java/lang/Object
			.class Example {
			    .method public static example ()Ljava/lang/Object; {
			        code: {
			        BEGIN:
			        \0
				    END:
			        }
			    }
			}
			""";

	static {
		VALUE_OPTS.engineProvider(varCache -> {
			ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(varCache);
			engine.setFieldValueLookup(new BasicFieldValueLookup());
			engine.setMethodValueLookup(new BasicMethodValueLookup());
			return engine;
		});
		TYPE_OPTS.engineProvider(TypedJvmAnalysisEngine::new);
	}

	@Test
	void testIntAdd() {
		String source = TEMPLATE_INT_SUPPLIER.replace("\0", """
				iconst_2
				istore a
				iload a
				dup
				iadd
				ireturn
				"""
		);
		assertKnownIntValue(4, getReturnValue(source));
	}

	@Test
	void testIntAddWithOpaqueFlow() {
		String source = TEMPLATE_INT_SUPPLIER.replace("\0", """
				invokestatic Example.getOpaqueBoolean ()Z
				ifeq FOO
				iconst_5
				istore a
				goto CONTINUE
				  FOO:
				iconst_2
				istore a
				goto CONTINUE
				  CONTINUE:
				iload a
				dup
				iadd
				ireturn
				"""
		);

		// In this case we don't know which path will be taken.
		// But also, even if it were a constant, our analysis engine doesn't track which paths will or wont be taken...
		assertUnknownInt(getReturnValue(source));
	}

	@Test
	void testIntDivide() {
		String source = TEMPLATE_INT_SUPPLIER.replace("\0", """
				bipush 100
				iconst_5
				idiv
				ireturn
				"""
		);
		assertKnownIntValue(20, getReturnValue(source));
	}

	@Test
	void testDoubleCompareNanUsesOneForDcmpg() {
		String source = TEMPLATE_INT_SUPPLIER.replace("\0", """
				ldc NaND
				dconst_1
				dcmpg
				ireturn
				"""
		);
		assertKnownIntValue(1, getReturnValue(source));
	}

	@Test
	void testFloatCompareNanUsesMinusOneForFcmpl() {
		String source = TEMPLATE_INT_SUPPLIER.replace("\0", """
				ldc NaNF
				fconst_1
				fcmpl
				ireturn
				"""
		);
		assertKnownIntValue(-1, getReturnValue(source));
	}


	@Test
	void testPrimitiveConversionsPreserveKnownValues() {
		String source = TEMPLATE_INT_SUPPLIER.replace("\0", """
				iconst_5
				i2l
				lconst_1
				ladd
				l2i
				ireturn
				"""
		);
		assertKnownIntValue(6, getReturnValue(source));
	}

	@Test
	void testArrayLengthTracksKnownSize() {
		String source = TEMPLATE_INT_SUPPLIER.replace("\0", """
				iconst_4
				newarray int
				arraylength
				ireturn
				"""
		);
		assertKnownIntValue(4, getReturnValue(source));
	}

	@Test
	void testMethodLookupUsesKnownContextAndParameters() {
		String source = TEMPLATE_INT_SUPPLIER.replace("\0", """
				ldc "abc"
				invokevirtual java/lang/String.length ()I
				ireturn
				"""
		);
		assertKnownIntValue(3, getReturnValue(source));
	}

	@Test
	void testFieldLookupUsesRegisteredValue() {
		String source = TEMPLATE_INT_SUPPLIER.replace("\0", """
				getstatic Dummy.answer I
				ireturn
				"""
		);
		assertUnknownInt(getReturnValue(source));
	}

	@Test
	void testLdcClassTypePushesClassReference() {
		String source = TEMPLATE_OBJECT_SUPPLIER.replace("\0", """
				ldc Ljava/lang/String;
				areturn
				"""
		);
		assertValue(getReturnValue(source), "Expected a class reference value", value -> value instanceof Value.ObjectValue);
	}

	@Test
	void testLdcTypePushesKnownClassReference() {
		String source = TEMPLATE_OBJECT_SUPPLIER.replace("\0", """
				ldc "test"
				areturn
				"""
		);
		assertKnownStringValue("test", getReturnValue(source));
	}

	@Test
	void testMethodRegistryOperatesOnInputsAndGivesExpectedOutput() {
		String source = TEMPLATE_OBJECT_SUPPLIER.replace("\0", """
				ldc "hello "
				ldc "world"
				invokevirtual java/lang/String.concat (Ljava/lang/String;)Ljava/lang/String;
				areturn
				"""
		);
		assertKnownStringValue("hello world", getReturnValue(source));
	}

	private static void assertValue(Value value, String message, Predicate<Value> condition) {
		assertTrue(condition.test(value), message);
	}

	private static void assertUnknownInt(Value value) {
		assertInstanceOf(Value.UnknownIntValue.class, value, "Expected an UnknownIntValue");
	}

	private static void assertKnownIntValue(int expected, Value value) {
		Value.KnownIntValue known = assertInstanceOf(Value.KnownIntValue.class, value, "Expected a KnownIntValue");
		assertEquals(expected, known.value(), "KnownIntValue had unexpected value");
	}

	private static void assertKnownStringValue(String expected, Value value) {
		Value.KnownStringValue known = assertInstanceOf(Value.KnownStringValue.class, value, "Expected a KnownStringValue");
		assertEquals(expected, known.value(), "KnownStringValue had unexpected value");
	}

	private static Value getReturnValue(String source) {
		JvmCompilation compilation = JvmAssemblerFixture.compileJvm(source, VALUE_OPTS);
		MethodAnalysisLookup lookup = compilation.requireSuccess().analysisLookup();

		for (var entry : lookup.allResults().entrySet()) {
			if (entry.getKey().name.equals("example")) {
				AnalysisResults analysis = entry.getValue();
				assertNotNull(analysis, "Expected analysis results");
				Frame frame = analysis.terminalFrames().lastEntry().getValue();
				ValuedFrame valuedFrame = assertInstanceOf(ValuedFrame.class, frame, "Expected terminal frame to be a ValuedFrame");
				return assertDoesNotThrow(() -> valuedFrame.getStack().peek());
			}
		}

		fail("Expected to find analysis results for method 'example'");
		throw new IllegalStateException();
	}
}

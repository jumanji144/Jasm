package me.darknet.assembler.cli;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompilerOptions;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.backend.jvm.compile.JavaClassRepresentation;
import me.darknet.assembler.backend.jvm.compile.JavaCompileResult;
import me.darknet.assembler.backend.jvm.compile.JvmCompiler;
import me.darknet.assembler.backend.jvm.compile.JvmCompilerOptions;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.target.TargetContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the typed boundary between the target-neutral compiler contract and each backend.
 */
class TypedCompilerContractTest {
	private static final String CLASS_SOURCE = ".class public super Example {}";

	@Test
	void compilersExposeTheirExactCompilerBindings() {
		// The compiler interface is generic, but each backend must bind the type parameters to its own types.
		assertCompilerBinding(JvmCompiler.class, JvmCompilerOptions.class,
				JavaClassRepresentation.class, JavaCompileResult.class);
		assertCompilerBinding(DalvikCompiler.class, DalvikCompilerOptions.class,
				DalvikClassRepresentation.class, DalvikCompileResult.class);
	}

	@Test
	void eachCompilerHasOnlyItsTypedCompileOverload() {
		// Same idea for the compile() method.
		assertCompileMethod(JvmCompiler.class, JvmCompilerOptions.class);
		assertCompileMethod(DalvikCompiler.class, DalvikCompilerOptions.class);
	}

	@Test
	void compilerOptionsKeepOverlayTypesTargetSpecific() throws NoSuchMethodException {
		// The withOverlay() method is target-specific, and the type of the overlay must be target-specific as well.
		Method jvmOverlay = JvmCompilerOptions.class.getMethod("withOverlay", JavaClassRepresentation.class);
		assertEquals(JvmCompilerOptions.class, jvmOverlay.getReturnType());
		assertThrows(NoSuchMethodException.class, () -> JvmCompilerOptions.class.getMethod("withOverlay", DalvikClassRepresentation.class));

		// Same for Dalvik.
		Method dalvikOverlay = DalvikCompilerOptions.class.getMethod("withOverlay", DalvikClassRepresentation.class);
		assertEquals(DalvikCompilerOptions.class, dalvikOverlay.getReturnType());
		assertThrows(NoSuchMethodException.class, () -> DalvikCompilerOptions.class.getMethod("withOverlay", JavaClassRepresentation.class));
	}

	@Test
	void defaultJvmOptionsProduceTypedSuccessfulOutput() {
		ValidatedUnit unit = validated(CLASS_SOURCE, JvmTargetContext.INSTANCE);
		Outcome<JavaCompileResult> result = new JvmCompiler().compile(unit, new JvmCompilerOptions());

		// The compiler must produce a successful result with a representation that is target-specific.
		assertFalse(result.hasErrors(), diagnostics(result));
		JavaCompileResult typedResult = result.requireValue();
		JavaClassRepresentation representation = assertInstanceOf(JavaClassRepresentation.class, typedResult.representation());
		assertNotNull(representation.classFile());
		assertTrue(representation.classFile().length > 0, "Successful JVM compilation must emit bytes");
	}

	@Test
	void defaultDalvikOptionsProduceTypedSuccessfulOutput() {
		ValidatedUnit unit = validated(CLASS_SOURCE, DalvikTargetContext.INSTANCE);
		Outcome<DalvikCompileResult> result = new DalvikCompiler().compile(unit, new DalvikCompilerOptions());

		// Same as the other test, but for Dalvik.
		assertFalse(result.hasErrors(), diagnostics(result));
		DalvikCompileResult typedResult = result.requireValue();
		DalvikClassRepresentation representation = assertInstanceOf(DalvikClassRepresentation.class, typedResult.representation());
		assertNotNull(representation.definition());
		assertEquals("Example", representation.definition().getType().internalName());
	}

	@Test
	void failedTypedCompilationsCarryNoRepresentation() {
		ValidatedUnit dalvikUnit = validated(CLASS_SOURCE, DalvikTargetContext.INSTANCE);
		ValidatedUnit jvmUnit = validated(CLASS_SOURCE, JvmTargetContext.INSTANCE);
		Outcome<JavaCompileResult> jvmResult = new JvmCompiler().compile(dalvikUnit, new JvmCompilerOptions());
		Outcome<DalvikCompileResult> dalvikResult = new DalvikCompiler().compile(jvmUnit, new DalvikCompilerOptions());

		// The compiler must reject a Dalvik-processed unit, and the result must carry no representation.
		assertTrue(jvmResult.hasErrors(), "JVM compiler must reject a Dalvik-processed unit");
		assertNull(jvmResult.requireValue().representation());

		// Same in reverse for a JVM-processed unit.
		assertTrue(dalvikResult.hasErrors(), "Dalvik compiler must reject a JVM-processed unit");
		assertNull(dalvikResult.requireValue().representation());
	}

	private static ValidatedUnit validated(String source, TargetContext target) {
		Outcome<List<me.darknet.assembler.ast.ASTElement>> parsed = Processor.processSourceResult(source, "<typed-contract-test>", target);
		assertFalse(parsed.hasErrors(), diagnostics(parsed));

		Outcome<ValidatedUnit> processed = SemanticProcessor.process(parsed.requireValue(), target);
		assertFalse(processed.hasErrors(), diagnostics(processed));
		return processed.requireValue();
	}

	private static void assertCompilerBinding(Class<?> compilerType, Class<?> optionsType,
	                                          Class<?> representationType, Class<?> resultType) {
		ParameterizedType compilerInterface = Arrays.stream(compilerType.getGenericInterfaces())
				.filter(ParameterizedType.class::isInstance)
				.map(ParameterizedType.class::cast)
				.filter(type -> type.getRawType() == Compiler.class)
				.findFirst()
				.orElseThrow(() -> new AssertionError(compilerType.getName()
						+ " must directly implement Compiler with concrete type arguments"));

		assertArrayEquals(new Type[]{optionsType, representationType, resultType},
				compilerInterface.getActualTypeArguments(),
				compilerType.getSimpleName() + " Compiler binding changed");
	}

	private static void assertCompileMethod(Class<?> compilerType, Class<?> optionsType) {
		List<Method> compileMethods = Arrays.stream(compilerType.getDeclaredMethods())
				.filter(method -> method.getName().equals("compile"))
				.filter(method -> !method.isBridge() && !method.isSynthetic())
				.toList();
		assertEquals(1, compileMethods.size(), compilerType.getSimpleName()
				+ " must expose one non-bridge compile method");

		Method compile = compileMethods.getFirst();
		assertEquals(Outcome.class, compile.getReturnType());
		assertArrayEquals(new Class<?>[]{ValidatedUnit.class, optionsType}, compile.getParameterTypes(),
				compilerType.getSimpleName() + " compile overload must remain target typed");
	}

	private static String diagnostics(Outcome<?> outcome) {
		return outcome.diagnostics().toString();
	}
}

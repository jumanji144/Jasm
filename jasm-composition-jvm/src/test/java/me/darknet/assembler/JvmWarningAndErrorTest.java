package me.darknet.assembler;

import me.darknet.assembler.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.compiler.ReflectiveInheritanceChecker;
import me.darknet.assembler.test.BinarySampleFixture;
import me.darknet.assembler.test.JvmAnalysisAssertions;
import me.darknet.assembler.test.JvmAssemblerFixture;
import me.darknet.assembler.test.JvmCompilation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmWarningAndErrorTest {
    @Test
    void wrongReturn() {
        warnOnBothEngines("Example-wrong-return.jasm");
    }

    @Test
    void int2Object() {
        warnOnBothEngines("Example-int2obj-cast.jasm");
        warnOnBothEngines("Example-int2obj-instanceof.jasm");
    }

    @Test
    void null2Int() {
        warnOnBothEngines("Example-null2int-ineg.jasm");
    }

    @Test
    void object2Int() {
        warnOnBothEngines("Example-obj2int-iadd.jasm");
        warnOnBothEngines("Example-obj2int-f2i.jasm");
    }

    @Test
    void switchOnNull() {
        warnOnBothEngines("Example-tswitch-null.jasm");
        warnOnBothEngines("Example-lswitch-null.jasm");
    }

    @Test
    void switchOnObj() {
        warnOnBothEngines("Example-tswitch-obj.jasm");
        warnOnBothEngines("Example-lswitch-obj.jasm");
    }

    @Test
    void getField() {
        warnOnBothEngines("Example-getfield-null.jasm");
        warnOnBothEngines("Example-getfield-prim.jasm");
    }

    @Test
    void putField() {
        warnOnBothEngines("Example-putfield-null.jasm");
        warnOnBothEngines("Example-putfield-prim.jasm");
        warnOnBothEngines("Example-putstatic-array-to-prim.jasm");
        warnOnBothEngines("Example-putstatic-array-to-wrong-component-type.jasm");
        warnOnBothEngines("Example-putstatic-array-to-wrong-dim-array.jasm");
        warnOnBothEngines("Example-putstatic-prim-to-array.jasm");
        warnOnBothEngines("Example-putstatic-prim-to-obj.jasm");
        warnOnBothEngines("Example-putstatic-obj-to-prim.jasm");
        warnOnBothEngines("Example-putstatic-obj-to-array.jasm");
    }

    @Test
    void invokeContext() {
        warnOnBothEngines("Example-invokevirtual-null.jasm");
        warnOnBothEngines("Example-invokevirtual-prim.jasm");
    }

    @Test
    void storeTypeIncompatibility() {
        warnOnBothEngines("Example-istore-obj.jasm");
        warnOnBothEngines("Example-istore-null.jasm");
        warnOnBothEngines("Example-astore-int.jasm");
    }

    @Test
    void arrays() {
        warnOnBothEngines("Example-arraylen-null.jasm");
        warnOnBothEngines("Example-arraylen-int.jasm");
        warnOnBothEngines("Example-arraylen-obj.jasm");
        warnOnBothEngines("Example-arrayload-null.jasm");
        warnOnBothEngines("Example-arrayload-int.jasm");
        warnOnBothEngines("Example-arrayload-obj.jasm");
        warnOnBothEngines("Example-arrayload-index.jasm");
        warnOnBothEngines("Example-arraystore-null.jasm");
        warnOnBothEngines("Example-arraystore-int.jasm");
        warnOnBothEngines("Example-arraystore-obj.jasm");
        warnOnBothEngines("Example-arraystore-index.jasm");
    }

    @Test
    void math() {
        warnOnBothEngines("Example-fneg-null.jasm");
        warnOnBothEngines("Example-fcmpl-null-a.jasm");
        warnOnBothEngines("Example-fcmpl-null-b.jasm");
    }

    @Test
    void putfieldWithoutContext() {
        assertAnalysisFailure("Example-putfield-no-context.jasm");
    }

    @Test
    void intAndObjectStackMerge() {
        assertAnalysisFailure("Example-object-int-stack-merge.jasm");
    }

    @Test
    void intAndObjectLocalMerge() {
        assertAnalysisFailure("Example-int-object-var-merge.jasm");
    }

	@Test
	void loadUninitializedVariable() {
		warnOnBothEngines("Example-load-not-initialized.jasm");
	}

	@Test
	void frameComputationFailureReportsAstLocation() {
		String source = """
				.super java/lang/Object
				.class public Example {
				    .field public foo Ljava/lang/String;
				    .method public example ()Ljava/lang/String; {
				        parameters: { this },
				        code: {
				        A:
				            aload this
				            getfield Example.foo Ljava/lang/String;
				            pop
				            astore foo
				            aload foo
				            areturn
				        B:
				        }
				    }
				}
				""";

		TestJvmCompilerOptions options = new TestJvmCompilerOptions();
		JvmCompilation compilation = JvmAssemblerFixture.compileJvm("frame-computation-failure.jasm", source, options);

		assertTrue(compilation.hasErrors(), "Expected invalid bytecode to produce a compilation error");
		assertTrue(compilation.warnings().stream().anyMatch(warning ->
				warning.getMessage().contains("Cannot store into local 'foo'")
						&& warning.getMessage().contains("found 0")),
				"Expected a warning explaining why foo is invalid: " + compilation.warnings());
		assertEquals(11, compilation.errors().getFirst().getLocation().line(),
				"Expected the error to point at the invalid astore instruction: " + compilation.errors());
	}

	@Test
	void stackUnderflowAfterLineDirectivesReportsFailingInstructionLocation() {
		String source = """
				.super java/lang/Object
				.class public Example {
				    .field public mosaic Ljava/lang/Object;
				    .method public suspend ()V {
				        parameters: { this },
				        code: {
				        A:
				            line 368
				            aload this
				            getfield Example.mosaic Ljava/lang/Object;
				            astore mosaic
				            line 369
				            pop
				            aload mosaic
				            ifnull D
				        D:
				        }
				    }
				}
				""";

		JvmCompilation compilation = JvmAssemblerFixture.compileJvm("late-stack-underflow.jasm", source,
				new TestJvmCompilerOptions());

		var error = compilation.errors().stream()
				.filter(candidate -> candidate.getMessage().contains("Cannot peek from empty stack"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected stack-underflow error: " + compilation.errors()));
		assertEquals(13, error.getLocation().line(),
				"Expected the error to point at the later pop instruction: " + compilation.errors());
	}

	private static void warnOnBothEngines(String sampleName) {
        String source = BinarySampleFixture.jvmSample(sampleName).read();

        TestJvmCompilerOptions typed = new TestJvmCompilerOptions();
        typed.inheritanceChecker(new ReflectiveInheritanceChecker(JvmWarningAndErrorTest.class.getClassLoader()));
        typed.engineProvider(TypedJvmAnalysisEngine::new);
        JvmAnalysisAssertions.assertCompileWarning(JvmAssemblerFixture.compileJvm(source, typed));

        TestJvmCompilerOptions valued = new TestJvmCompilerOptions();
        valued.inheritanceChecker(new ReflectiveInheritanceChecker(JvmWarningAndErrorTest.class.getClassLoader()));
        valued.engineProvider(ValuedJvmAnalysisEngine::new);
        JvmAnalysisAssertions.assertCompileWarning(JvmAssemblerFixture.compileJvm(source, valued));
    }

    private static void assertAnalysisFailure(String sampleName) {
        String source = BinarySampleFixture.jvmSample(sampleName).read();
        TestJvmCompilerOptions options = new TestJvmCompilerOptions();
        options.inheritanceChecker(new ReflectiveInheritanceChecker(JvmWarningAndErrorTest.class.getClassLoader()));
        options.engineProvider(ValuedJvmAnalysisEngine::new);
        JvmAnalysisAssertions.assertAnalysisFailure(JvmAssemblerFixture.compileJvm(source, options));
    }
}

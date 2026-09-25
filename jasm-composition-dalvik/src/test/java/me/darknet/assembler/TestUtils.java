package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compile.DalvikClassResult;
import me.darknet.assembler.compile.DalvikCompiler;
import me.darknet.assembler.compile.DalvikCompilerOptions;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.compiler.EmptyInheritanceChecker;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DalvikDexFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.test.SourceNormalization;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.type.Types;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.fail;

// TODO: We've been adding more test utilities to the JVM and core modules but not quite yet here for dalvik.
//  - We'll probably break down this class and add a number of extra utilities soon.

public class TestUtils {

    public static void processDalvik(String source, DalvikCompilerOptions options,
                                     ThrowingConsumer<DalvikClassResult> outputConsumer,
                                     Consumer<List<Diagnostic>> warningConsumer) {
        Outcome<List<ASTElement>> astResult =
                AssemblyParseFixture.processDeclarations("<test>", source, DalvikTargetContext.INSTANCE);
        if (astResult.hasErrors()) {
            fail("Failed to parse Dalvik class\n" + DiagnosticAssertions.formatErrors(astResult.errors()));
        }

        var unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(astResult.requireValue(), DalvikTargetContext.INSTANCE),
                "Failed to process Dalvik semantic unit"
        );
        Outcome<DalvikClassResult> compilation = new DalvikCompiler().compile(unit, options);
        if (compilation.hasErrors()) {
            fail("Failed to compile Dalvik class\n" + DiagnosticAssertions.formatErrors(compilation.errors()));
        }

        if (warningConsumer != null && compilation.hasWarnings()) {
            warningConsumer.accept(compilation.warnings());
        }

        try {
            if (outputConsumer != null) {
                outputConsumer.accept(compilation.requireValue());
            }
        } catch (Throwable t) {
            fail("Error processing compiled Dalvik class: " + t.getMessage(), t);
        }
    }

    public static void processDalvik(String source, DalvikCompilerOptions options,
                                     ThrowingConsumer<DalvikClassResult> outputConsumer) {
        processDalvik(source, options, outputConsumer, null);
    }

    public static void processSample(byte[] dexFile, String className, ThrowingConsumer<String> outputConsumer,
                                     Consumer<List<Diagnostic>> warningConsumer) {
        try {
            DexFile file = DalvikDexFixture.readDex(dexFile);
            ClassDefinition classDef = file.definitions()
                    .stream()
                    .filter(def -> def.getType().internalName().equals(className))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Class not found: " + className));

            if (outputConsumer != null) {
                outputConsumer.accept(normalize(DalvikDexFixture.print(classDef)));
            }
        } catch (IOException e) {
            fail("Failed to read dex header", e);
        } catch (Throwable e) {
            // Consumer should fail instead of us handling it generically here.
            fail("Error processing dex file: " + e.getMessage(), e);
        }
    }

    public static void processSampleFile(byte[] dexFile, ThrowingConsumer<String> outputConsumer) {
        try {
            DexFile file = DalvikDexFixture.readDex(dexFile);
            for (ClassDefinition definition : file.definitions()) {
                if (outputConsumer != null) {
                    outputConsumer.accept(normalize(DalvikDexFixture.print(definition)));
                }
            }
        } catch (IOException e) {
            fail("Failed to read dex header", e);
        } catch (Throwable e) {
            // Consumer should fail instead of us handling it generically here.
            fail("Error processing dex file: " + e.getMessage(), e);
        }
    }

    public static DalvikCompilerOptions options() {
        return new DalvikCompilerOptions()
                .withVersion(35)
                .withInheritanceChecker(EmptyInheritanceChecker.INSTANCE);
    }

    public static DalvikCompilerOptions overlayOptions(String internalName) {
        ClassDefinition overlay = new ClassDefinition(
                Types.instanceTypeFromInternalName(internalName),
                Types.instanceTypeFromInternalName("java/lang/Object"),
                DalvikModifiers.ACC_PUBLIC
        );
        return options().withOverlay(new DalvikClassRepresentation(overlay));
    }

    public static String normalize(String input) {
        return SourceNormalization.normalize(input);
    }

    public static void assertParsesDalvik(String source) {
        var result = AssemblyParseFixture.processDeclarations("<test>", source, DalvikTargetContext.INSTANCE);
        if (result.hasErrors()) {
            fail("Failed to parse Dalvik class\n" + DiagnosticAssertions.formatErrors(result.errors()));
        }
    }

}

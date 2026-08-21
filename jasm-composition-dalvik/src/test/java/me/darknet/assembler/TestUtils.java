package me.darknet.assembler;

import me.darknet.assembler.compile.DalvikClassResult;
import me.darknet.assembler.compile.DalvikCompiler;
import me.darknet.assembler.compiler.ClassResult;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.error.Warn;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.printer.DalvikClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.test.SourceNormalization;
import me.darknet.dex.file.DexHeader;
import me.darknet.dex.io.Input;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.fail;

// TODO: We've been adding more test utilities to the JVM and core modules but not quite yet here for dalvik.
//  - We'll probably break down this class and add a number of extra utilities soon.

public class TestUtils {

    public static void processDalvik(String source, CompilerOptions<?> options,
                                     ThrowingConsumer<DalvikClassResult> outputConsumer,
                                     Consumer<List<Warn>> warningConsumer) {
        Result<List<me.darknet.assembler.ast.ASTElement>> astResult =
                AssemblyParseFixture.processDeclarations("<test>", source, BytecodeFormat.DALVIK);
        if (astResult.hasErr()) {
            fail("Failed to parse Dalvik class\n" + DiagnosticAssertions.formatErrors(astResult.errors()));
        }

        Result<? extends ClassResult> compilation = new DalvikCompiler().compile(astResult.get(), options);
        if (compilation.hasErr()) {
            fail("Failed to compile Dalvik class\n" + DiagnosticAssertions.formatErrors(compilation.errors()));
        }

        if (warningConsumer != null && compilation.hasWarn()) {
            warningConsumer.accept(compilation.getWarns());
        }

        try {
            if (outputConsumer != null) {
                outputConsumer.accept((DalvikClassResult) compilation.get());
            }
        } catch (Throwable t) {
            fail("Error processing compiled Dalvik class: " + t.getMessage(), t);
        }
    }

    public static void processDalvik(String source, CompilerOptions<?> options,
                                     ThrowingConsumer<DalvikClassResult> outputConsumer) {
        processDalvik(source, options, outputConsumer, null);
    }

    public static void processSample(byte[] dexFile, String className, ThrowingConsumer<String> outputConsumer,
                                     Consumer<List<Warn>> warningConsumer) {
        Input input = Input.wrap(dexFile);
        try {
            DexHeader header = DexHeader.CODEC.read(input);
            DexFile file = DexFile.CODEC.map(header, header.map());

            ClassDefinition classDef = file.definitions()
                    .stream()
                    .filter(def -> def.getType().internalName().equals(className))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Class not found: " + className));

            DalvikClassPrinter printer = new DalvikClassPrinter(classDef);
            PrintContext<?> ctx = new PrintContext<>("\t");
            printer.print(ctx);

            String output = ctx.toString();

            if (outputConsumer != null) {
                outputConsumer.accept(normalize(output));
            }

        } catch (IOException e) {
            fail("Failed to read dex header", e);
        } catch (Throwable e) {
            // Consumer should fail instead of us handling it generically here
            fail("Error processing dex file: " + e.getMessage(), e);
        }
    }

    public static void processSampleFile(byte[] dexFile, ThrowingConsumer<String> outputConsumer) {
        Input input = Input.wrap(dexFile);
        try {
            DexHeader header = DexHeader.CODEC.read(input);
            DexFile file = DexFile.CODEC.map(header, header.map());

            for (ClassDefinition definition : file.definitions()) {
                DalvikClassPrinter printer = new DalvikClassPrinter(definition);
                PrintContext<?> ctx = new PrintContext<>("\t");
                printer.print(ctx);

                String output = ctx.toString();

                if (outputConsumer != null) {
                    outputConsumer.accept(normalize(output));
                }
            }

        } catch (IOException e) {
            fail("Failed to read dex header", e);
        } catch (Throwable e) {
            // Consumer should fail instead of us handling it generically here
            fail("Error processing dex file: " + e.getMessage(), e);
        }
    }

    public static String normalize(String input) {
        return SourceNormalization.normalize(input);
    }

    public static void assertParsesDalvik(String source) {
        var result = AssemblyParseFixture.processDeclarations("<test>", source, BytecodeFormat.DALVIK);
        if (result.hasErr()) {
            fail("Failed to parse Dalvik class\n" + DiagnosticAssertions.formatErrors(result.errors()));
        }
    }

}

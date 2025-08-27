package me.darknet.assembler;

import me.darknet.assembler.compile.DalvikClassResult;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Warn;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.printer.DalvikClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.file.DexHeader;
import me.darknet.dex.io.Input;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {

    private static final Pattern DUPLICATE_NEWLINES = Pattern.compile("\\n\\s*\\n");
    private static final Pattern END_LINE_PADDING = Pattern.compile("[ \\t]+\\n");
    private static final Pattern COMMENTS = Pattern.compile("(?:^|\\n)\\s*//.+");

    public static void processDalvik(String source, CompilerOptions<?> options,
                                     ThrowingConsumer<DalvikClassResult> outputConsumer,
                                     Consumer<List<Warn>> warningConsumer) {
        Processor.processSource(source, "<test>", ast -> {

        }, errors -> {
            for (Error error : errors) {
                System.err.println(error);
            }
            fail("Failed to parse class");
        }, BytecodeFormat.DALVIK);
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
        input = input.replace("\r", "");
        while (input.contains("  "))
            input = input.replace("  ", " ");

        input = COMMENTS.matcher(input).replaceAll("");
        input = END_LINE_PADDING.matcher(input).replaceAll("\n");
        input = DUPLICATE_NEWLINES.matcher(input).replaceAll("\n");
        return input.trim();
    }

}

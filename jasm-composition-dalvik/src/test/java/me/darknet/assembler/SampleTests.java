package me.darknet.assembler;

import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.parser.BytecodeFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SampleTests {

    private static final String PATH_PREFIX = "src/test/resources/samples/jasm/";
    private static final String PATH_BIN_PREFIX = "src/test/resources/samples/binary/";
    private static final String PATH_ILLEGAL_PREFIX = "src/test/resources/samples/jasm-illegal/";

    @Test
    public void testSimpleSample() throws Throwable {
        var arg = BinaryTestArgument.fromName("HelloWorld.sample");

        TestUtils.processSample(arg.source.get(), "Main",
                output -> {
            System.out.println("Output: " + output);
                    Processor.processSource(output, "<test>", ast -> {
                        // This is a simple test, we just want to ensure it compiles without errors
                        var a = ast;
                    }, errors -> {
                        for (var error : errors) {
                            System.err.println(error);
                        }
                        throw new AssertionError("Failed to parse class");
                    }, BytecodeFormat.DALVIK);
                }, warnings -> {});

    }

    @Test
    public void testAllOpcodes() throws Throwable {
        var arg = BinaryTestArgument.fromName("OmniBus.sample");

        TestUtils.processSample(arg.source.get(), "Array",
                output -> {
                    System.out.println("Output: " + output);
                    Processor.processSource(output, "<test>", ast -> {
                        // This is a simple test, we just want to ensure it compiles without errors
                        var a = ast;
                    }, errors -> {
                        for (var error : errors) {
                            System.err.println(error);
                        }
                        throw new AssertionError("Failed to parse class");
                    }, BytecodeFormat.DALVIK);
                }, warns -> {});
    }

    record BinaryTestArgument(Path path, String name, ThrowingSupplier<byte[]> source) {
        public static BinaryTestArgument fromName(String name) {
            Path path = Paths.get(System.getProperty("user.dir")).resolve(PATH_BIN_PREFIX).resolve(name);
            return from(path);
        }

        public static BinaryTestArgument from(Path path) {
            return new BinaryTestArgument(path, path.getFileName().toString(), () -> Files.readAllBytes(path));
        }

        @Override
        public String toString() {
            return name;
        }
    }

}

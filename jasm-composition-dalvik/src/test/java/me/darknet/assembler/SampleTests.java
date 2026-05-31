package me.darknet.assembler;

import me.darknet.assembler.compile.DalvikCompilerOptions;
import me.darknet.assembler.compiler.EmptyInheritanceChecker;
import me.darknet.assembler.test.SampleSourceFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SampleTests {

    @Test
    public void testSimpleSample() {
        byte[] dex = SampleSourceFixture.readBytes(binarySample("HelloWorld.sample"));
        TestUtils.processSample(dex, "Main", TestUtils::assertParsesDalvik, warnings -> {});
    }

    @Test
    public void testAllOpcodes() {
        byte[] dex = SampleSourceFixture.readBytes(binarySample("OmniBus.sample"));
        TestUtils.processSample(dex, "Array", TestUtils::assertParsesDalvik, warns -> {});
    }

    @Test
    public void testSimpleSampleCompiles() {
        byte[] dex = SampleSourceFixture.readBytes(binarySample("HelloWorld.sample"));
        TestUtils.processSample(dex, "Main", source ->
                TestUtils.processDalvik(source, options(), result -> assertNotNull(result.representation())), warns -> {});
    }

    @Test
    public void testAllOpcodesCompiles() {
        byte[] dex = SampleSourceFixture.readBytes(binarySample("OmniBus.sample"));
        TestUtils.processSample(dex, "Array", source ->
                TestUtils.processDalvik(source, options(), result -> assertNotNull(result.representation())), warns -> {});
    }

    private static DalvikCompilerOptions options() {
        return new DalvikCompilerOptions()
                .version(35)
                .inheritanceChecker(EmptyInheritanceChecker.INSTANCE);
    }

    private static Path binarySample(String name) {
        return SampleSourceFixture.requireExisting(Path.of("src", "test", "resources", "samples", "binary", name));
    }
}

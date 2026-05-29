package me.darknet.assembler;

import me.darknet.assembler.test.SampleSourceFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

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

    private static Path binarySample(String name) {
        return SampleSourceFixture.requireExisting(Path.of("src", "test", "resources", "samples", "binary", name));
    }
}

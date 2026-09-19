package me.darknet.assembler;

import me.darknet.assembler.test.DalvikSampleFixture;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SampleTests {

    @Test
    public void testSimpleSample() {
        byte[] dex = DalvikSampleFixture.dexSample("HelloWorld.sample").read();
        TestUtils.processSample(dex, "Main", TestUtils::assertParsesDalvik, warnings -> {});
    }

    @Test
    public void testAllOpcodes() {
        byte[] dex = DalvikSampleFixture.dexSample("OmniBus.sample").read();
        TestUtils.processSample(dex, "Array", TestUtils::assertParsesDalvik, warns -> {});
    }

    @Test
    public void testSimpleSampleCompiles() {
        byte[] dex = DalvikSampleFixture.dexSample("HelloWorld.sample").read();
        TestUtils.processSample(dex, "Main", source ->
                TestUtils.processDalvik(source, TestUtils.options(), result -> assertNotNull(result.representation())), warns -> {});
    }

    @Test
    public void testAllOpcodesCompiles() {
        byte[] dex = DalvikSampleFixture.dexSample("OmniBus.sample").read();
        TestUtils.processSample(dex, "Array", source ->
                TestUtils.processDalvik(source, TestUtils.options(), result -> assertNotNull(result.representation())), warns -> {});
    }

}

package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.backend.dalvik.io.DalvikDexIO;
import me.darknet.dex.tree.DexFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikDexIOTest {

    @Test
    void readsDexPathsAndOrdersApkEntriesByNumericIndex(@TempDir Path temporaryDirectory) throws IOException {
        byte[] helloWorld = DalvikSampleFixture.dexSample("HelloWorld.sample").read();
        byte[] omniBus = DalvikSampleFixture.dexSample("OmniBus.sample").read();

        Path standalone = temporaryDirectory.resolve("classes.dex");
        Files.write(standalone, helloWorld);
        List<DexFile> standaloneFiles = DalvikDexIO.read(standalone);
        assertEquals(1, standaloneFiles.size());
        assertTrue(standaloneFiles.getFirst().definitions().stream()
                .anyMatch(definition -> definition.getType().internalName().equals("Main")));

        Path apk = DalvikSampleFixture.writeZip(
                temporaryDirectory,
                "multiple-dex.apk",
                Map.of("classes10.dex", helloWorld, "classes2.dex", omniBus)
        );
        List<DexFile> apkFiles = DalvikDexIO.read(apk);

        assertEquals(2, apkFiles.size());
        assertTrue(apkFiles.get(0).definitions().stream()
                .anyMatch(definition -> definition.getType().internalName().equals("Array")));
        assertTrue(apkFiles.get(1).definitions().stream()
                .anyMatch(definition -> definition.getType().internalName().equals("Main")));
    }
}

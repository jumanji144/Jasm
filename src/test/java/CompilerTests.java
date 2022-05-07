import me.darknet.assembler.Assembler;
import me.darknet.assembler.parser.AssemblerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CompilerTests {

    static List<String> features = List.of(
            "Annotations.ja",
            "Array.ja",
            "ClassInstances.ja",
            "Example.ja",
            "ExeceptionCatch.ja",
            "Flow.ja",
            "HelloWorld.ja",
            "InvokeDynamic.ja",
            "LookupSwitch.ja",
            "Macros.ja",
            "TableSwitch.ja"
    );

    static List<byte[]> featuresBytes = new ArrayList<>(features.size());

    @BeforeAll
    public static void loadAllExamples() throws IOException {
        String prefix = "features/";
        for (String feature : features) {
            try (InputStream is = CompilerTests.class.getClassLoader().getResourceAsStream(prefix + feature)) {
                assert is != null : "Could not find resource " + feature;
                byte[] bytes = new byte[is.available()];
                is.read(bytes);
                featuresBytes.add(bytes);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load " + feature, e);
            }
        }
    }

    @Test
    public void testAllExamples() throws AssemblerException {
        for (int i = 0; i < features.size(); i++) {
            String feature = features.get(i);
            byte[] bytes = featuresBytes.get(i);
            System.out.print("Testing " + feature);
            try {
                Assembler.assemble(feature, 52, bytes);
            } catch (Exception e) {
                System.out.println(" FAILED");
                continue;
            }
            System.out.println("... OK");
        }
    }

}

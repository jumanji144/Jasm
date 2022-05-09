import me.darknet.assembler.Assembler;
import me.darknet.assembler.parser.AssemblerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompilerTests {

    static List<String> features = Arrays.asList(
            "Annotations.ja",
            "Array.ja",
            "ClassInstances.ja",
            "ConstFieldAssign.ja",
            "Example.ja",
            "Exceptions.ja",
            "ExeceptionCatch.ja",
            "Flow.ja",
            "HelloWorld.ja",
            "InvokeDynamic.ja",
            "LookupSwitch.ja",
            "Macros.ja",
            "TableSwitch.ja",
            "Signatures.ja"
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

    public static List<Arguments> getFeatures() {
        List<Arguments> args = new ArrayList<>(features.size());
        for (int i = 0; i < features.size(); i++) {
            args.add(Arguments.of(features.get(i), featuresBytes.get(i)));
        }
        return args;
    }

    @ParameterizedTest
    @MethodSource("getFeatures")
    public void testExample(String feature, byte[] bytes) throws AssemblerException {
        System.out.print("Testing " + feature);
        try {
            Assembler.assemble(feature, 52, bytes);
        } catch (AssemblerException e) {
            throw new RuntimeException(e.describe(), e.getCause());
        }
        System.out.println("... OK");
    }

}

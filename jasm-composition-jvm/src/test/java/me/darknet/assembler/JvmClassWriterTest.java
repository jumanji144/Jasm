package me.darknet.assembler;

import me.darknet.assembler.compile.JvmClassWriter;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.TypeAwareness;
import me.darknet.assembler.error.ErrorCollector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class JvmClassWriterTest {

    @Test
    void unknownFrameTypesWarnAndFallBackToObject() {
        ErrorCollector collector = new ErrorCollector();
        TypeAwareness awareness = new TypeAwareness() {
            @Override
            public boolean isAwareOf(String type) {
                return type.startsWith("java/");
            }

            @Override
            public String notifyUnknownType(String type) {
                return "Unknown type: " + type;
            }
        };
        InheritanceChecker checker = new InheritanceChecker() {
            @Override
            public boolean isSubclassOf(String child, String parent) {
                return false;
            }

            @Override
            public String getCommonSuperclass(String type1, String type2) {
                fail("Unknown frame types must not reach the inheritance checker");
                return null;
            }
        };

        ExposedClassWriter writer = new ExposedClassWriter(collector, awareness, checker);

        assertEquals("java/lang/Object", writer.commonSuperclass("missing/A", "missing/B"));
        assertFalse(collector.getWarns().isEmpty());
        assertEquals(2, collector.getWarns().size());
    }

    private static final class ExposedClassWriter extends JvmClassWriter {
        private ExposedClassWriter(ErrorCollector collector, TypeAwareness awareness, InheritanceChecker checker) {
            super(0, collector, awareness, checker);
        }

        private String commonSuperclass(String type1, String type2) {
            return getCommonSuperClass(type1, type2);
        }
    }
}

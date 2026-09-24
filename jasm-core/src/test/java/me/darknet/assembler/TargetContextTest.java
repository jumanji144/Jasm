package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.assembler.processing.MethodTargetData;
import me.darknet.assembler.target.AnnotationCapability;
import me.darknet.assembler.target.MethodAttributeParser;
import me.darknet.assembler.target.MethodAttributeRegistry;
import me.darknet.assembler.target.TargetId;
import me.darknet.assembler.target.TargetRegistry;
import me.darknet.assembler.test.FixtureTarget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the target-service and registry boundaries.
 */
class TargetContextTest {
    @Test
    void fixtureTargetsOwnIndependentInstructionRegistries() {
        assertSame(FixtureTarget.JVM.context(), FixtureTarget.JVM.context());
        assertSame(FixtureTarget.DALVIK.context(), FixtureTarget.DALVIK.context());

        Instructions<?> jvmInstructions = FixtureTarget.JVM.context().instructions();
        Instructions<?> dalvikInstructions = FixtureTarget.DALVIK.context().instructions();
        assertFalse(jvmInstructions.getInstructionNames().contains("move"));
        assertTrue(dalvikInstructions.getInstructionNames().contains("move"));
        assertTrue(jvmInstructions.getInstructionNames().contains("aconst_null"));
        assertFalse(dalvikInstructions.getInstructionNames().contains("aconst_null"));
    }

    @Test
    void registryPreservesOrderAndResolvesCaseInsensitively() {
        TargetRegistry registry = TargetRegistry.of(List.of(FixtureTarget.JVM, FixtureTarget.DALVIK));
        assertEquals(List.of(new TargetId("JVM"), new TargetId("DALVIK")), registry.ids());
        assertSame(FixtureTarget.JVM, registry.find(new TargetId("jVm")));
        assertSame(FixtureTarget.DALVIK, registry.find(new TargetId("dalvik")));
        assertNull(registry.find(new TargetId("unknown")));
        assertThrows(UnsupportedOperationException.class, () -> registry.ids().clear());
        assertThrows(IllegalArgumentException.class,
                () -> TargetRegistry.of(List.of(FixtureTarget.JVM, FixtureTarget.JVM)));
    }

    @Test
    void targetIdsRejectNullAndBlankValuesWithoutNormalizingValidIds() {
        assertThrows(NullPointerException.class, () -> new TargetId(null));
        assertThrows(IllegalArgumentException.class, () -> new TargetId(" \t "));
        assertEquals("jVm", new TargetId("jVm").value());
    }

    @Test
    void keywordMetadataUnifiesTargetMnemonicsAndAttributes() {
        Set<String> jvmKeywords = FixtureTarget.JVM.context().keywordNames();
        Set<String> dalvikKeywords = FixtureTarget.DALVIK.context().keywordNames();

        assertTrue(jvmKeywords.contains("goto"));
        assertTrue(jvmKeywords.contains("goto_w"));
        assertFalse(jvmKeywords.contains("registers"));
        assertTrue(dalvikKeywords.contains("move"));
        assertTrue(dalvikKeywords.contains("registers"));
        assertFalse(dalvikKeywords.contains("aconst_null"));
        assertThrows(UnsupportedOperationException.class, () -> jvmKeywords.add("new-keyword"));
    }

    @Test
    void methodAttributeRegistryUsesExactKeysAndImmutableKeywordSnapshots() {
        MethodAttributeRegistry registry = new MethodAttributeRegistry();
        MethodAttributeParser first = parser("first");
        MethodAttributeParser second = parser("second");
        registry.register(first);
        Set<String> firstSnapshot = registry.getKeywords();
        registry.register(second);

        assertSame(first, registry.get("first"));
        assertNull(registry.get("FIRST"));
        assertNull(registry.get("missing"));
        assertEquals(List.of("first"), new ArrayList<>(firstSnapshot));
        assertEquals(List.of("first", "second"), new ArrayList<>(registry.getKeywords()));
        assertThrows(UnsupportedOperationException.class, () -> firstSnapshot.add("third"));
        assertThrows(IllegalArgumentException.class, () -> registry.register(parser("first")));
        assertThrows(UnsupportedOperationException.class,
                () -> MethodAttributeRegistry.empty().register(parser("immutable")));
    }

    @Test
    void annotationCapabilitiesMatchFixtureTargetSupport() {
        var jvm = FixtureTarget.JVM.context().annotationCapabilities();
        var dalvik = FixtureTarget.DALVIK.context().annotationCapabilities();

        assertFalse(jvm.supports(AnnotationCapability.SYSTEM_VISIBILITY));
        assertTrue(jvm.supports(AnnotationCapability.TYPE_ANNOTATIONS));
        assertTrue(jvm.supports(AnnotationCapability.PARAMETER_ANNOTATIONS));
        assertTrue(jvm.supports(AnnotationCapability.ANNOTATION_DEFAULT_VALUES));
        assertTrue(dalvik.supports(AnnotationCapability.SYSTEM_VISIBILITY));
        assertFalse(dalvik.supports(AnnotationCapability.TYPE_ANNOTATIONS));
        assertFalse(dalvik.supports(AnnotationCapability.PARAMETER_ANNOTATIONS));
        assertTrue(dalvik.supports(AnnotationCapability.ANNOTATION_DEFAULT_VALUES));
        assertEquals("system visibility", AnnotationCapability.SYSTEM_VISIBILITY.displayName());
    }

    private static MethodAttributeParser parser(String key) {
        return new MethodAttributeParser() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public MethodTargetData parse(ProcessorContext context, ASTElement value, ASTElement declaration) {
                return null;
            }
        };
    }
}
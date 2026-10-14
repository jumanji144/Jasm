package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.backend.dalvik.DalvikTarget;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.target.AnnotationCapability;
import me.darknet.assembler.target.TargetId;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikTargetSupportTest {

    @Test
    void exposesDalvikTargetIdentityAndCapabilities() {
        DalvikTarget target = DalvikTarget.INSTANCE;

        assertEquals(new TargetId("DALVIK"), target.id());
        assertEquals("Dalvik", target.displayName());
        assertSame(DalvikTargetContext.INSTANCE, target.context());
        assertTrue(target.context().annotationCapabilities().supports(AnnotationCapability.SYSTEM_VISIBILITY));
        assertFalse(target.context().annotationCapabilities().supports(AnnotationCapability.TYPE_ANNOTATIONS));
        assertTrue(target.context().annotationCapabilities().supports(AnnotationCapability.PARAMETER_ANNOTATIONS));
        assertTrue(target.context().annotationCapabilities().supports(AnnotationCapability.ANNOTATION_DEFAULT_VALUES));
    }

    @Test
    void rejectsRegisterCountsThatCannotFitInTheDalvikMethodModel() {
        var result = AssemblyParseFixture.processDeclarations(
                "<test>",
                ".method public test ()V { registers: 2147483648 }",
                DalvikTargetContext.INSTANCE
        );

        DiagnosticAssertions.assertHasError(
                result,
                DiagnosticCode.MALFORMED_DECLARATION,
                "An out-of-range Dalvik register count should be rejected"
        );
        DiagnosticAssertions.assertPhase(
                result.diagnostics(),
                DiagnosticPhase.TARGET_VALIDATION,
                "Register-count validation belongs to the target-validation phase"
        );
    }
}

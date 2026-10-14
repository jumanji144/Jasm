package me.darknet.assembler;

import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Outcome}'s variants and the invariants each of them enforces.
 * <p>
 * These pin the contract a caller relies on when it reads a value without checking flags first: a failure
 * has no value, and a variant that would contradict itself cannot be constructed at all.
 */
class OutcomeTest {
    private static final Diagnostic ERROR = Diagnostic.error(DiagnosticPhase.SEMANTIC_LOWERING, DiagnosticCode.OPERAND_SHAPE, "an error", null);
    private static final Diagnostic WARNING = Diagnostic.warning(DiagnosticPhase.SEMANTIC_LOWERING, DiagnosticCode.UNCLASSIFIED, "a warning", null);

    @Test
    void valueWithoutDiagnosticsIsASuccess() {
        Outcome<String> outcome = Outcome.of("value", List.of());

        assertTrue(outcome.isSuccess());
        assertFalse(outcome.isFailure());
        assertFalse(outcome.isPartial());
        assertEquals("value", outcome.value());
        assertEquals("value", outcome.requireValue());
        assertTrue(outcome.diagnostics().isEmpty());
        assertFalse(outcome.hasErrors());
        assertFalse(outcome.hasWarnings());
    }

    @Test
    void valueWithOnlyWarningsIsStillASuccess() {
        Outcome<String> outcome = Outcome.of("value", List.of(WARNING));

        // A warning does not prevent output, so the variant stays a success and carries the warning with it.
        assertTrue(outcome.isSuccess());
        assertFalse(outcome.hasErrors());
        assertTrue(outcome.hasWarnings());
        assertEquals(List.of(WARNING), outcome.warnings());
        assertEquals("value", outcome.requireValue());
    }

    @Test
    void valueWithErrorsIsPartialAndStillReadable() {
        Outcome<String> outcome = Outcome.of("value", List.of(WARNING, ERROR));

        // The value exists, but errors make it unusable as output; it remains inspectable for recovery.
        assertTrue(outcome.isPartial());
        assertTrue(outcome.hasErrors());
        assertEquals("value", outcome.requireValue());
        assertEquals(List.of(ERROR), outcome.errors());
        assertEquals(List.of(WARNING), outcome.warnings());
    }

    @Test
    void noValueWithErrorsIsAFailure() {
        Outcome<String> outcome = Outcome.of(null, List.of(ERROR));

        // A failure has no value, so callers cannot read it. The errors are still available for reporting.
        assertTrue(outcome.isFailure());
        assertFalse(outcome.hasValue());
        assertNull(outcome.value());
        assertEquals(List.of(ERROR), outcome.errors());
        assertThrows(IllegalStateException.class, outcome::requireValue);
    }

    @Test
    void anOutcomeWithNeitherValueNorErrorIsRejected() {
        // No value and no error leaves callers unable to distinguish success from a missing result.
        assertThrows(IllegalArgumentException.class, () -> Outcome.of(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> Outcome.of(null, List.of(WARNING)));
    }

    @Test
    void aVariantThatContradictsItsOwnDiagnosticsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Outcome.success("value", List.of(ERROR)));
        assertThrows(IllegalArgumentException.class, () -> Outcome.partial("value", List.of()));
        assertThrows(IllegalArgumentException.class, () -> Outcome.partial("value", List.of(WARNING)));
        assertThrows(IllegalArgumentException.class, () -> Outcome.failure(List.of()));
        assertThrows(IllegalArgumentException.class, () -> Outcome.failure(List.of(WARNING)));
        assertThrows(IllegalArgumentException.class, () -> Outcome.failure(WARNING));
    }

    @Test
    void factoriesBuildTheVariantTheirDiagnosticsDescribe() {
        assertSame("value", Outcome.success("value").requireValue());
        assertTrue(Outcome.success("value").diagnostics().isEmpty());
        assertTrue(Outcome.success("value", List.of(WARNING)).isSuccess());
        assertTrue(Outcome.partial("value", List.of(ERROR)).isPartial());
        assertTrue(Outcome.failure(List.of(ERROR)).isFailure());
        assertTrue(Outcome.failure(ERROR).isFailure());
        assertEquals(List.of(ERROR), Outcome.failure(ERROR).errors());
    }

    @Test
    void everyVariantKeepsAnImmutableCopyOfItsDiagnostics() {
        List<Diagnostic> successInput = new ArrayList<>(List.of(WARNING));
        List<Diagnostic> partialInput = new ArrayList<>(List.of(WARNING, ERROR));
        List<Diagnostic> failureInput = new ArrayList<>(List.of(ERROR));
        Outcome<String> success = Outcome.success("success", successInput);
        Outcome<String> partial = Outcome.partial("partial", partialInput);
        Outcome<String> failure = Outcome.failure(failureInput);

        successInput.add(ERROR);
        partialInput.clear();
        failureInput.clear();

        assertEquals(List.of(WARNING), success.diagnostics());
        assertEquals(List.of(WARNING, ERROR), partial.diagnostics());
        assertEquals(List.of(ERROR), failure.diagnostics());
        assertThrows(UnsupportedOperationException.class, () -> success.diagnostics().clear());
        assertThrows(UnsupportedOperationException.class, () -> partial.diagnostics().clear());
        assertThrows(UnsupportedOperationException.class, () -> failure.diagnostics().clear());
    }
}

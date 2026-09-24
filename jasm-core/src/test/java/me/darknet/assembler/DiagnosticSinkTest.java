package me.darknet.assembler;

import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.error.Severity;
import me.darknet.assembler.util.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the {@link DiagnosticSink} invariants that the pipeline relies on.
 */
class DiagnosticSinkTest {

    private static final Location FIRST = new Location(3, 5, 2, "<test>");
    private static final Location SAME_LINE_LATER_COLUMN = new Location(3, 9, 2, "<test>");

    @Test
    void errorAndWarningAtTheSamePlaceBothSurvive() {
        DiagnosticSink sink = new DiagnosticSink();

        sink.error(DiagnosticCode.UNCLASSIFIED, "Same text", FIRST);
        sink.warning(DiagnosticCode.UNCLASSIFIED, "Same text", FIRST);

        // Severity is a field, so two diagnostics that differ only in severity are not duplicates.
        assertEquals(2, sink.diagnostics().size());
        assertEquals(1, sink.errors().size());
        assertEquals(1, sink.warnings().size());
    }

    @Test
    void identicalDiagnosticsCollapse() {
        DiagnosticSink sink = new DiagnosticSink();

        sink.error(DiagnosticCode.UNCLASSIFIED, "Same text", FIRST);
        sink.error(DiagnosticCode.UNCLASSIFIED, "Same text", FIRST);

        assertEquals(1, sink.diagnostics().size());
    }

    @Test
    void removeAtOnlyRemovesDiagnosticsAtThatExactLocation() {
        DiagnosticSink sink = new DiagnosticSink();

        sink.warning(DiagnosticCode.UNCLASSIFIED, "On the same line", SAME_LINE_LATER_COLUMN);
        sink.warning(DiagnosticCode.UNCLASSIFIED, "At the target location", FIRST);

        sink.removeAt(FIRST, DiagnosticPhase.TARGET_VALIDATION);

        // A line-based removal would have taken the other column with it.
        assertEquals(1, sink.diagnostics().size());
        assertEquals("On the same line", sink.diagnostics().getFirst().message());
    }

    @Test
    void removeAtLeavesOtherPhasesAlone() {
        DiagnosticSink sink = new DiagnosticSink();
        sink.error(DiagnosticPhase.OUTPUT_VERIFICATION, DiagnosticCode.VERIFICATION_WARNING,
                "Analysis said something", FIRST);
        sink.warning(DiagnosticPhase.BACKEND_EMISSION, DiagnosticCode.UNEXPECTED_ELEMENT,
                "Emitter said something", FIRST);

        sink.removeAt(FIRST, DiagnosticPhase.OUTPUT_VERIFICATION);

        assertEquals(1, sink.diagnostics().size());
        assertEquals("Emitter said something", sink.diagnostics().getFirst().message());
    }

    @Test
    void removeAtNullIsANoOp() {
        DiagnosticSink sink = new DiagnosticSink();
        sink.error(DiagnosticCode.UNCLASSIFIED, "No location", null);
        sink.error(DiagnosticCode.UNCLASSIFIED, "With location", FIRST);

        assertDoesNotThrow(() -> sink.removeAt(null, DiagnosticPhase.TARGET_VALIDATION));
        assertEquals(2, sink.diagnostics().size());
    }

    @Test
    void viewsRejectMutation() {
        DiagnosticSink sink = new DiagnosticSink();
        sink.error(DiagnosticCode.UNCLASSIFIED, "An error", FIRST);
        sink.warning(DiagnosticCode.UNCLASSIFIED, "A warning", FIRST);

        assertThrows(UnsupportedOperationException.class, () -> sink.diagnostics().clear());
        assertThrows(UnsupportedOperationException.class, () -> sink.errors().clear());
        assertThrows(UnsupportedOperationException.class, () -> sink.warnings().clear());
    }

    @Test
    void diagnosticCopiesRelatedLocations() {
        java.util.ArrayList<Location> related = new java.util.ArrayList<>(java.util.List.of(FIRST));
        Diagnostic diagnostic = new Diagnostic(Severity.ERROR, DiagnosticPhase.SYNTAX, DiagnosticCode.UNEXPECTED_TOKEN,
                "Unexpected token", SAME_LINE_LATER_COLUMN, related);

        related.clear();

        assertEquals(java.util.List.of(FIRST), diagnostic.related());
        assertThrows(UnsupportedOperationException.class, () -> diagnostic.related().clear());
    }

    @Test
    void toStringRendersTheLocationOnlyWhenThereIsOne() {
        Diagnostic located = Diagnostic.error(DiagnosticPhase.SYNTAX, DiagnosticCode.UNEXPECTED_TOKEN, "Boom", FIRST);
        Diagnostic unlocated = Diagnostic.error(DiagnosticPhase.SYNTAX, DiagnosticCode.UNEXPECTED_TOKEN, "Boom", null);

        assertEquals(FIRST + ": Boom", located.toString());
        assertEquals("Boom", unlocated.toString());
    }

    @Test
    void formatAddsTheCodeAndRelatedLocations() {
        Diagnostic diagnostic = new Diagnostic(Severity.ERROR, DiagnosticPhase.BACKEND_EMISSION,
                DiagnosticCode.REGISTER_LIMIT, "Register exceeds declared register count: v2", FIRST,
                java.util.List.of(SAME_LINE_LATER_COLUMN));

        assertEquals(FIRST + ": [REGISTER_LIMIT] Register exceeds declared register count: v2"
                + " (related: " + SAME_LINE_LATER_COLUMN + ")", diagnostic.format());

        Diagnostic withoutRelated = Diagnostic.error(DiagnosticPhase.LEXER, DiagnosticCode.UNTERMINATED_LITERAL,
                "Unterminated string", null);
        assertEquals("[UNTERMINATED_LITERAL] Unterminated string", withoutRelated.format());
    }
}

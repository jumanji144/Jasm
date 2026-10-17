package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.printer.DisassemblyCommentExtractor;
import me.darknet.assembler.printer.DisassemblyCommentMap;
import me.darknet.assembler.printer.DisassemblyCommentProvider;
import me.darknet.assembler.printer.DisassemblyCommentSite;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates comment API supports round-tripping of comments from source -> AST -> disassembly.
 * A library consumer would implement their own provider for their own persistent comment storage.
 */
class DisassemblyCommentRoundTripTest {
    @Test
    void printsExtractedCommentsAtClassMemberAndCodeSites() {
        String source = """
                // class note
                .super java/lang/Object
                .visible-annotation java/lang/Deprecated {}
                .class public roundtrip/Comments {
                    // field note
                    .field public static final value I { value: 7 }
                    // method note
                    .method public static run ()V {
                        code: {
                            // first instruction note
                            nop // inline instruction note
                            // dropped-label note
                            dropped:
                            nop
                            return
                            // method end note
                        }
                    }
                    // class end note
                }
                """;
        var declarations = process(source);
        var unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(declarations, JvmTargetContext.INSTANCE),
                "Annotated JVM source should validate");
        byte[] classBytes = JvmAssemblerFixture.compileJvm(source, new TestJvmCompilerOptions())
                .requireClassBytes();
        DisassemblyCommentMap comments = DisassemblyCommentExtractor.extract(unit.declarations());
        var method = new DisassemblyCommentSite.MethodSite("roundtrip/Comments", "run", "()V");
        var generated = new DisassemblyCommentSite.InstructionSite(method, 2);
        DisassemblyCommentProvider provider = site -> {
            List<String> bodies = new ArrayList<>(comments.commentsFor(site));
            if (site.equals(generated))
                bodies.add(" callback generated");
            return bodies;
        };

        String printed = JvmDisassemblyFixture.disassembleJvm(classBytes, ctx ->
                ctx.setDisassemblyCommentProvider(provider));

        assertBefore(printed, "// class note", ".visible-annotation java/lang/Deprecated");
        assertBefore(printed, "// class note", ".class public roundtrip/Comments");
        assertBefore(printed, "// field note", ".field public static final value I");
        assertBefore(printed, "// method note", ".method public static run ()V");
        assertTrue(printed.contains("// first instruction note"), printed);
        assertTrue(printed.contains("// inline instruction note"), printed);
        assertTrue(printed.contains("// dropped-label note"), printed);
        assertTrue(printed.contains("// callback generated"), printed);
        assertTrue(printed.contains("// method end note"), printed);
        assertTrue(printed.contains("// class end note"), printed);
        assertTrue(printed.indexOf("// dropped-label note") < printed.indexOf("nop", printed.indexOf("nop") + 1),
                "The dropped label's comment should stay at its executable position\n" + printed);
    }

    @Test
    void keepsGeneratedTryDiagnosticsVisibleWithoutReingestingThem() {
        // We emit try-catch range comments before relevant labels so users don't have to remember
        // where the ranges are in the top of the method definition. But we also don't want
        // these to persist in the comments map, as they are generated and not user-provided.
        String source = """
                .super java/lang/Object
                .class public roundtrip/TryComments {
                    .method public static guarded ()V {
                        exceptions: { { Start, End, Handler, java/lang/RuntimeException } },
                        code: {
                            // try-start: range=[UserStart-UserEnd] handler=UserHandler
                            Start:
                            nop
                            End:
                            return
                            Handler:
                            athrow
                        }
                    }
                }
                """;
        var declarations = process(source);
        var unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(declarations, JvmTargetContext.INSTANCE),
                "Annotated try-catch source should validate");
        byte[] classBytes = JvmAssemblerFixture.compileJvm(source, new TestJvmCompilerOptions())
                .requireClassBytes();
        DisassemblyCommentMap comments = DisassemblyCommentExtractor.extract(unit.declarations());
        var method = new DisassemblyCommentSite.MethodSite("roundtrip/TryComments", "guarded", "()V");
        assertTrue(Stream.of(
                        new DisassemblyCommentSite.LabelSite(method, 0),
                        new DisassemblyCommentSite.InstructionSite(method, 0),
                        method)
                .flatMap(site -> comments.commentsFor(site).stream())
                .anyMatch(body -> body.contains("try-start: range=[UserStart-UserEnd] handler=UserHandler")));

        String printed = JvmDisassemblyFixture.disassembleJvm(classBytes, ctx -> {
            ctx.setDebugTryCatchRanges(true);
            ctx.setDisassemblyCommentProvider(comments);
        });
        assertTrue(printed.contains("// try-start:"), printed);
        assertTrue(printed.contains("// try-end:"), printed);
        assertTrue(printed.contains("// try-handler:"), printed);

        var printedDeclarations = process(printed);
        DisassemblyCommentMap reextracted = DisassemblyCommentExtractor.extract(printedDeclarations);
        List<String> userBodies = Stream.of(
                        new DisassemblyCommentSite.ClassSite("roundtrip/TryComments"),
                        new DisassemblyCommentSite.ClassEndSite("roundtrip/TryComments"),
                        method,
                        new DisassemblyCommentSite.LabelSite(method, 0),
                        new DisassemblyCommentSite.LabelSite(method, 1),
                        new DisassemblyCommentSite.LabelSite(method, 2),
                        new DisassemblyCommentSite.InstructionSite(method, 0),
                        new DisassemblyCommentSite.InstructionSite(method, 1),
                        new DisassemblyCommentSite.InstructionSite(method, 2),
                        new DisassemblyCommentSite.MethodEndSite(method))
                .flatMap(site -> reextracted.commentsFor(site).stream())
                .toList();
        assertTrue(userBodies.stream().anyMatch(body -> body.contains("handler=UserHandler")), userBodies.toString());
        assertFalse(userBodies.stream().anyMatch(body -> body.matches(
                "(?s).*try-(?:start|end|handler):\\s+range=\\[[^\\r\\n]+\\]\\s+handler=\\S+:\\S+.*")),
                userBodies.toString());
    }

    private static List<ASTElement> process(String source) {
        Outcome<List<ASTElement>> result = AssemblyParseFixture.processDeclarations(
                "<comment-round-trip>", source, JvmTargetContext.INSTANCE);
        return DiagnosticAssertions.requireSuccess(result, "JVM comments should parse and process");
    }

    private static void assertBefore(String source, String first, String second) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        assertTrue(firstIndex >= 0, "Missing: " + first + "\n" + source);
        assertTrue(secondIndex > firstIndex, first + " must precede " + second + "\n" + source);
    }
}

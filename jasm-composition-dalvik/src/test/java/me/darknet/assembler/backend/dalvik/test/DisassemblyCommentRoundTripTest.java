package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.ast.MetaComment;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.printer.DisassemblyCommentExtractor;
import me.darknet.assembler.printer.DisassemblyCommentMap;
import me.darknet.assembler.printer.DisassemblyCommentProvider;
import me.darknet.assembler.printer.DisassemblyCommentSite;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
                .class public roundtrip/DalvikComments {
                    // field note
                    .field public static final value I { value: 7 }
                    // method note
                    .method public static run ()V {
                        registers: 1,
                        code: {
                            // first instruction note
                            const v0 1
                            // dropped-label note
                            dropped:
                            return-void
                            // method end note
                        }
                        // method object end note
                    }
                    // class end note
                }
                """;
        var parsed = DiagnosticAssertions.requireSuccess(AssemblyParseFixture.parse(source),
                "Dalvik source should retain generic comments");
        assertEquals(List.of(" class note"), parsed.getFirst().leadingComments().stream()
                .map(MetaComment::content).toList());
        Prepared prepared = compile(source);
        DisassemblyCommentMap comments = DisassemblyCommentExtractor.extract(prepared.unit().declarations());
        assertEquals(List.of(" class note"), comments.commentsFor(
                new DisassemblyCommentSite.ClassSite("roundtrip/DalvikComments")));
        var method = new DisassemblyCommentSite.MethodSite("roundtrip/DalvikComments", "run", "()V");
        var generated = new DisassemblyCommentSite.InstructionSite(method, 1);
        var missingLabel = new DisassemblyCommentSite.LabelSite(method, 0);
        DisassemblyCommentProvider provider = site -> {
            List<String> bodies = new ArrayList<>(comments.commentsFor(site));
            if (site.equals(generated))
                bodies.add(" callback generated");
            if (site.equals(missingLabel))
                bodies.add(" callback label fallback");
            return bodies;
        };
        PrintContext<?> context = new PrintContext<>("\t");
        context.setDisassemblyCommentProvider(provider);
        new DalvikClassPrinter(prepared.definition()).print(context);
        String printed = context.toString();

        assertBefore(printed, "// class note", ".visible-annotation java/lang/Deprecated");
        assertBefore(printed, "// class note", ".class public roundtrip/DalvikComments");
        assertBefore(printed, "// field note", ".field public static final value I");
        assertBefore(printed, "// method note", ".method public static run ()V");
        assertTrue(printed.contains("// first instruction note"), printed);
        assertTrue(printed.contains("// dropped-label note"), printed);
        assertTrue(printed.contains("// callback generated"), printed);
        assertBefore(printed, "// callback label fallback", "const v0");
        assertTrue(printed.contains("// method end note"), printed);
        assertTrue(printed.contains("// method object end note"), printed);
        assertTrue(printed.contains("// class end note"), printed);
        assertTrue(printed.indexOf("// dropped-label note") < printed.indexOf("return-void"),
                "The label comment should resolve to the following instruction position\n" + printed);
    }

    private static Prepared compile(String source) {
        Outcome<List<ASTElement>> parsed = AssemblyParseFixture.processDeclarations(
                "<dalvik-comment-round-trip>", source, DalvikTargetContext.INSTANCE);
        List<ASTElement> declarations = DiagnosticAssertions.requireSuccess(parsed,
                "Dalvik comments should parse and process");
        var unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(declarations, DalvikTargetContext.INSTANCE),
                "Dalvik comments should validate");
        Outcome<DalvikCompileResult> result = new DalvikCompiler().compile(unit, TestUtils.options());
        DalvikClassRepresentation representation = DiagnosticAssertions.requireSuccess(result,
                "Dalvik comments should compile").representation();
        return new Prepared(unit, representation.definition());
    }

    private static void assertBefore(String source, String first, String second) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        assertTrue(firstIndex >= 0, "Missing: " + first + "\n" + source);
        assertTrue(secondIndex > firstIndex, first + " must precede " + second + "\n" + source);
    }

    private record Prepared(ValidatedUnit unit, ClassDefinition definition) {}
}

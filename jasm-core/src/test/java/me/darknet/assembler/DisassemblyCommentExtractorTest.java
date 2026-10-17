package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.printer.DisassemblyCommentExtractor;
import me.darknet.assembler.printer.DisassemblyCommentMap;
import me.darknet.assembler.printer.DisassemblyCommentSite;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.assembler.test.FixtureTarget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DisassemblyCommentExtractorTest {
    @Test
    void extractsClassMemberCodeLabelAndEndCommentsWithoutChangingSemanticChildren() {
        String source = """
                // class note
                .class public sample/Comments {
                    /* field note */
                    .field public static final value I { value:
                        /* nested value note */
                        7 }
                    // method note
                    .method public static run ()V {
                        code: {
                            // label note
                            start:
                            line 12
                            // first instruction
                            nop // inline instruction note
                            nop
                            return
                            // code end note
                        }
                        // method end note
                    }
                    // class end note
                }
                """;
        List<ASTElement> declarations = process(source);
        ASTClass clazz = assertInstanceOf(ASTClass.class, declarations.getFirst());
        ASTField field = clazz.contents().stream().filter(ASTField.class::isInstance)
                .map(ASTField.class::cast).findFirst().orElseThrow();
        ASTMethod method = clazz.contents().stream().filter(ASTMethod.class::isInstance)
                .map(ASTMethod.class::cast).findFirst().orElseThrow();
        ASTCode code = method.getCode();
        assertNotNull(code);
        assertEquals(5, code.getInstructions().size(), "Labels and line pseudo-instructions remain in the AST code view");
        assertInstanceOf(ASTLabel.class, code.getInstructions().getFirst());
        var parsedWithComments = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.parse(source), "Commented source should parse");
        var parsedWithoutComments = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.parse(stripComments(source)), "Comment-free source should parse");
        assertEquals(treeShape(parsedWithoutComments.getFirst()), treeShape(parsedWithComments.getFirst()),
                "Comment trivia must not change AST children");

        var semantic = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(declarations, FixtureTarget.JVM.context()),
                "Comments must not change semantic compilation");
        assertNotNull(semantic.methods().get(method));

        DisassemblyCommentMap comments = DisassemblyCommentExtractor.extract(declarations);
        var methodSite = new DisassemblyCommentSite.MethodSite("sample/Comments", "run", "()V");
        assertEquals(List.of(" class note"), comments.commentsFor(
                new DisassemblyCommentSite.ClassSite("sample/Comments")));
        assertTrue(String.join("\n", comments.commentsFor(
                new DisassemblyCommentSite.FieldSite("sample/Comments", "value", "I")))
                .contains("field note"));
        assertEquals(List.of(" method note"), comments.commentsFor(methodSite));
        assertEquals(List.of(" label note"), comments.commentsFor(
                new DisassemblyCommentSite.LabelSite(methodSite, 0)));
        assertTrue(String.join("\n", comments.commentsFor(
                new DisassemblyCommentSite.InstructionSite(methodSite, 0)))
                .contains("first instruction"));
        assertTrue(String.join("\n", comments.commentsFor(
                new DisassemblyCommentSite.InstructionSite(methodSite, 0)))
                .contains("inline instruction note"));
        assertTrue(String.join("\n", comments.commentsFor(
                new DisassemblyCommentSite.FieldSite("sample/Comments", "value", "I")))
                .contains("nested value note"));
        assertTrue(String.join("\n", comments.commentsFor(
                new DisassemblyCommentSite.MethodEndSite(methodSite))).contains("code end note"));
        assertTrue(String.join("\n", comments.commentsFor(
                new DisassemblyCommentSite.MethodEndSite(methodSite))).contains("method end note"));
        assertEquals(List.of(" class end note"), comments.commentsFor(
                new DisassemblyCommentSite.ClassEndSite("sample/Comments")));
    }

    @Test
    void groupsAdjacentCommentsAndUsesExecutableOrdinals() {
        String source = """
                .class public sample/Ordinals {
                    .method public static run ()V {
                        code: {
                            // foo
                            // iconst_1
                            line 23
                            pop
                            return
                        }
                    }
                }
                """;
        ASTClass clazz = assertInstanceOf(ASTClass.class, process(source).getFirst());
        ASTMethod method = clazz.contents().stream().filter(ASTMethod.class::isInstance)
                .map(ASTMethod.class::cast).findFirst().orElseThrow();
        var site = new DisassemblyCommentSite.MethodSite("sample/Ordinals", "run", "()V");
        DisassemblyCommentMap comments = DisassemblyCommentExtractor.extract(List.of(clazz));

        assertEquals(List.of(" foo\n iconst_1"), comments.commentsFor(
                new DisassemblyCommentSite.InstructionSite(site, 0)));
        assertNotNull(method.getCode());

        String withPriorInstruction = """
                .class public sample/Shifted {
                    .method public static run ()V {
                        code: {
                            nop
                            // foo
                            // iconst_1
                            line 23
                            return
                        }
                    }
                }
                """;
        DisassemblyCommentMap shifted = DisassemblyCommentExtractor.extract(process(withPriorInstruction));
        var shiftedMethod = new DisassemblyCommentSite.MethodSite("sample/Shifted", "run", "()V");
        assertEquals(List.of(" foo\n iconst_1"), shifted.commentsFor(
                new DisassemblyCommentSite.InstructionSite(shiftedMethod, 1)));

        String terminalLabel = """
                .class public sample/TerminalLabel {
                    .method public static run ()V {
                        code: {
                            return
                            // final label note
                            End:
                        }
                    }
                }
                """;
        DisassemblyCommentMap terminal = DisassemblyCommentExtractor.extract(process(terminalLabel));
        var terminalMethod = new DisassemblyCommentSite.MethodSite("sample/TerminalLabel", "run", "()V");
        assertEquals(List.of(" final label note"), terminal.commentsFor(
                new DisassemblyCommentSite.MethodEndSite(terminalMethod)));

        String blankLines = """
                .class public sample/Spacing {
                    .method public static run ()V {
                        code: {
                            // first

                            // second
                            return
                        }
                    }
                }
                """;
        DisassemblyCommentMap spaced = DisassemblyCommentExtractor.extract(process(blankLines));
        var spacedMethod = new DisassemblyCommentSite.MethodSite("sample/Spacing", "run", "()V");
        assertEquals(List.of(" first\n\n second"), spaced.commentsFor(
                new DisassemblyCommentSite.InstructionSite(spacedMethod, 0)));
    }

    @Test
    void resolvesCommentsInsideNestedInstructionOperandsToTheirInstruction() {
        List<ASTElement> declarations = process("""
                .class public sample/NestedOperands {
                    .method public static run ()V {
                        code: {
                            mystery { value:
                                // nested operand note
                                1 }
                            return
                        }
                    }
                }
                """);
        var method = new DisassemblyCommentSite.MethodSite("sample/NestedOperands", "run", "()V");
        DisassemblyCommentMap comments = DisassemblyCommentExtractor.extract(declarations);

        assertEquals(List.of(" nested operand note"), comments.commentsFor(
                new DisassemblyCommentSite.InstructionSite(method, 0)));
    }

    @Test
    void mapsTopLevelOverlayMembersOnlyWhenAnOwnerIsProvided() {
        List<ASTElement> declarations = process("""
                // field comment
                .field public value I
                // method comment
                .method public run ()V {}
                .field public finalField I
                // overlay eof
                """);
        assertThrows(IllegalArgumentException.class, () -> DisassemblyCommentExtractor.extract(declarations));

        DisassemblyCommentMap comments = DisassemblyCommentExtractor.extract(declarations, "sample/Overlay");
        assertEquals(List.of(" field comment"), comments.commentsFor(
                new DisassemblyCommentSite.FieldSite("sample/Overlay", "value", "I")));
        assertEquals(List.of(" method comment"), comments.commentsFor(
                new DisassemblyCommentSite.MethodSite("sample/Overlay", "run", "()V")));
        assertEquals(List.of(" overlay eof"), comments.commentsFor(
                new DisassemblyCommentSite.ClassEndSite("sample/Overlay")));
    }

    @Test
    void filtersOnlyExactGeneratedTryRangeBodiesAndKeepsRootEndComments() {
        List<ASTElement> declarations = process("""
                .class public sample/Tries {
                    // try-start: range=[L0-L1] handler=L2:Ljava/lang/Exception;
                    // try-start: range=[L0-L1] handler=L2
                    .method public static run ()V {}
                } // root inline
                // root eof
                """);
        DisassemblyCommentMap comments = DisassemblyCommentExtractor.extract(declarations);
        assertEquals(List.of(" try-start: range=[L0-L1] handler=L2"), comments.commentsFor(
                new DisassemblyCommentSite.MethodSite("sample/Tries", "run", "()V")));
        assertEquals(List.of(" root inline\n root eof"), comments.commentsFor(
                new DisassemblyCommentSite.ClassEndSite("sample/Tries")));
    }

    @Test
    void immutableMapDefensivelyCopiesKeysAndBodies() {
        var site = new DisassemblyCommentSite.ClassSite("sample/Immutable");
        List<String> bodies = new ArrayList<>(List.of(" first"));
        Map<DisassemblyCommentSite, List<String>> input = new LinkedHashMap<>();
        input.put(site, bodies);
        DisassemblyCommentMap map = new DisassemblyCommentMap(input);
        bodies.add(" second");
        input.clear();

        assertEquals(List.of(" first"), map.commentsFor(site));
        assertThrows(UnsupportedOperationException.class, () -> map.commentsFor(site).add("not allowed"));
    }

    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/|//[^\\r\\n]*", "");
    }

    private static String treeShape(ASTElement element) {
        return element.type().name() + element.children().stream().map(DisassemblyCommentExtractorTest::treeShape).toList();
    }

    private static List<ASTElement> process(String source) {
        return DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.processDeclarations(source),
                "Comment test source should process successfully");
    }
}

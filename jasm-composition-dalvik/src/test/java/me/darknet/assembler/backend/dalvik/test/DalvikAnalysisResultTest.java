package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompilerOptions;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikAnalysisFailure;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikAnalysisResults;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikMethodAnalysisLookup;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikRegisterState;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikAnalysisResultTest {
    private static final String ANALYSIS_SOURCE = """
            .super java/lang/Object
            .class public AnalysisSourceMap {
                .method public static value ()I {
                    registers: 1,
                    code: {
                    Entry:
                        const v0 7
                        return v0
                    }
                }
                .method public static value (I)I {
                    registers: 2,
                    parameters: { input },
                    code: {
                        const v0 9
                        return v0
                    }
                }
            }
            """;

    @Test
    void mapsOverloadedDexInstructionsToOriginalAstNodes() {
        Compilation compilation = compile(ANALYSIS_SOURCE, null);
        ASTClass sourceClass = assertInstanceOf(ASTClass.class, compilation.parsedAst().getFirst());
        DalvikMethodAnalysisLookup lookup = compilation.result().analysisLookup();

        MethodReference noArgumentKey = new MethodReference("AnalysisSourceMap", "value", "()I");
        MethodReference oneArgumentKey = new MethodReference("AnalysisSourceMap", "value", "(I)I");
        DalvikAnalysisResults noArgument = lookup.getResults(noArgumentKey);
        DalvikAnalysisResults oneArgument = lookup.getResults(oneArgumentKey);

        assertNotNull(noArgument);
        assertNotNull(oneArgument);
        assertSame(noArgument, lookup.getResults("value", "()I"));
        assertSame(oneArgument, lookup.getResults("value", "(I)I"));
        assertEquals(2, lookup.getAllResults().size());
        assertEquals(List.of(noArgumentKey, oneArgumentKey), new ArrayList<>(lookup.getAllResults().keySet()));

        assertSourceMapping(method(sourceClass, "()I"), noArgument);
        assertSourceMapping(method(sourceClass, "(I)I"), oneArgument);

        Instruction entryLabel = noArgument.getCodeInstructions().getFirst();
        assertInstanceOf(Label.class, entryLabel);
        assertEquals(0, noArgument.getInstructionIndex(entryLabel), "Entry must occupy a dex code position");
        assertNull(noArgument.getSource(entryLabel), "Labels do not have executable source mappings");
    }

    @Test
    void excludesBodylessAndOverlayOnlyMethods() {
        Compilation emptyClass = compile("""
                .super java/lang/Object
                .class public EmptyAnalysis {}
                """, null);
        assertTrue(emptyClass.result().analysisLookup().getAllResults().isEmpty());

        Compilation noCode = compile("""
                .super java/lang/Object
                .class public abstract interface NoCode {
                    .method public abstract missing ()I {
                        default-value: 42
                    }
                }
                """, null);
        assertTrue(noCode.result().analysisLookup().getAllResults().isEmpty());

        Compilation overlay = compile("""
                .super java/lang/Object
                .class public OverlayExample {
                    .method public static kept ()V {
                        code: {
                            return-void
                        }
                    }
                }
                """, null);
        DalvikClassRepresentation overlayRepresentation = overlay.result().representation();
        assertNotNull(overlayRepresentation);

        Compilation added = compile("""
                .method public static added ()V {
                    code: {
                        return-void
                    }
                }
                """, overlayRepresentation);
        assertNotNull(added.result().analysisLookup().getResults("added", "()V"));
        assertNull(added.result().analysisLookup().getResults("kept", "()V"));
        assertEquals(1, added.result().analysisLookup().getAllResults().size());
    }

    private static void assertSourceMapping(ASTMethod astMethod, DalvikAnalysisResults results) {
        List<ASTInstruction> executableAst = astMethod.getCode().getInstructions().stream()
                .filter(instruction -> !(instruction instanceof ASTLabel))
                .toList();
        int executableIndex = 0;
        List<Instruction> code = results.getCodeInstructions();
        for (int codeIndex = 0; codeIndex < code.size(); codeIndex++) {
            Instruction instruction = code.get(codeIndex);
            assertEquals(codeIndex, results.getInstructionIndex(instruction));
            if (instruction instanceof Label) {
                assertNull(results.getSource(instruction));
                continue;
            }
            assertSame(executableAst.get(executableIndex++), results.getSource(instruction));
        }
        assertEquals(executableAst.size(), executableIndex);
    }

    private static ASTMethod method(ASTClass sourceClass, String descriptor) {
        return sourceClass.contents().stream()
                .filter(ASTMethod.class::isInstance)
                .map(ASTMethod.class::cast)
                .filter(candidate -> candidate.getName().literal().equals("value")
                        && candidate.getDescriptor().literal().equals(descriptor))
                .findFirst()
                .orElseThrow();
    }

    private static Compilation compile(String source, DalvikClassRepresentation overlay) {
        List<ASTElement> parsedAst = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.processDeclarations("<dalvik-analysis-result-test>", source,
                        DalvikTargetContext.INSTANCE),
                "Dalvik analysis source should parse");
        ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(parsedAst, DalvikTargetContext.INSTANCE),
                "Dalvik analysis source should process");
        DalvikCompileResult result = DiagnosticAssertions.requireSuccess(
                new DalvikCompiler().compile(unit, new DalvikCompilerOptions().withOverlay(overlay)),
                "Dalvik analysis source should compile");
        return new Compilation(parsedAst, result);
    }

    private record Compilation(List<ASTElement> parsedAst, DalvikCompileResult result) {
    }
}

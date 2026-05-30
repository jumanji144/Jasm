package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.CodeElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.frame.Frame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Mutable analysis result for a single method.
 */
public class MethodAnalysisResult implements AnalysisResults {
    private final NavigableMap<Integer, Frame> frames = new TreeMap<>();
    private final NavigableMap<Integer, Frame> terminalFrames = new TreeMap<>();
    private final Map<ASTInstruction, CodeElement> astToElement = new IdentityHashMap<>();
    private final Map<CodeElement, ASTInstruction> elementToAst = new IdentityHashMap<>();
    private AnalysisException analysisFailure;

    @Override
    public @NotNull NavigableMap<Integer, Frame> frames() {
        return frames;
    }

    @Override
    public @NotNull NavigableMap<Integer, Frame> terminalFrames() {
        return terminalFrames;
    }

    @Override
    public @Nullable AnalysisException getAnalysisFailure() {
        return analysisFailure;
    }

    public void setAnalysisFailure(@Nullable AnalysisException analysisFailure) {
        this.analysisFailure = analysisFailure;
    }

    public void recordInstructionMapping(@Nullable ASTInstruction instruction, @NotNull CodeElement element) {
        elementToAst.put(element, instruction);
        if (instruction != null) {
            astToElement.put(instruction, element);
        }
    }

    @Override
    public @NotNull Map<ASTInstruction, CodeElement> getAstToCodeMap() {
        return astToElement;
    }

    @Override
    public @NotNull Map<CodeElement, ASTInstruction> getCodeToAstMap() {
        return elementToAst;
    }
}

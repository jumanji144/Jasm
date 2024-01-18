package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.type.ArrayType;
import dev.xdark.blw.type.ObjectType;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.VariableNameLookup;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.FrameOps;

import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.simulation.ExecutionEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Base outline for an engine intended for use in proper stack/local analysis.
 *
 * @see TypedJvmAnalysisEngine For basic type-tracking of stack/locals.
 * @see ValuedJvmAnalysisEngine For basic value-tracking of stack/locals.
 */
public abstract class JvmAnalysisEngine<F extends Frame> implements ExecutionEngine, AnalysisResults, JavaOpcodes {
    protected final NavigableMap<Integer, F> frames = new TreeMap<>();
    protected final NavigableMap<Integer, F> terminalFrames = new TreeMap<>();
    private final Map<ASTInstruction, CodeElement> astToElement = new IdentityHashMap<>();
    private final Map<CodeElement, ASTInstruction> elementToAst = new IdentityHashMap<>();
    protected final VariableNameLookup variableNameLookup;
    protected AnalysisException analysisFailure;
    protected F frame;

    public JvmAnalysisEngine(@NotNull VariableNameLookup variableNameLookup) {
        this.variableNameLookup = variableNameLookup;
    }

    public abstract FrameOps<?> newFrameOps();

    /**
     * @param index
     *              Key.
     *
     * @return Frame at index, or {@code null} if not present.
     */
    @Nullable
    public F getFrame(int index) {
        return frames.get(index);
    }

    /**
     * @param frame
     *              Frame to set.
     */
    public void setActiveFrame(@NotNull F frame) {
        this.frame = frame;
    }

    /**
     * @param index
     *              Key.
     * @param frame
     *              Frame to put.
     */
    public void putFrame(int index, @NotNull F frame) {
        frames.put(index, frame);
    }

    /**
     * @param index
     *              Key.
     * @param frame
     *              Frame to put.
     */
    public void markTerminal(int index, @NotNull F frame) {
        terminalFrames.put(index, frame);
    }

    @Override
    public void recordInstructionMapping(@NotNull ASTInstruction instruction, @NotNull CodeElement element) {
        elementToAst.put(element, instruction);
        astToElement.put(instruction, element);
    }

    @Override
    public @NotNull Map<ASTInstruction, CodeElement> getAstToCodeMap() {
        return astToElement;
    }

    @Override
    public @NotNull Map<CodeElement, ASTInstruction> getCodeToAstMap() {
        return elementToAst;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull NavigableMap<Integer, Frame> frames() {
        return (NavigableMap<Integer, Frame>) frames;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull NavigableMap<Integer, Frame> terminalFrames() {
        return (NavigableMap<Integer, Frame>) terminalFrames;
    }

    @Override
    public @Nullable AnalysisException getAnalysisFailure() {
        return analysisFailure;
    }

    @Override
    public void setAnalysisFailure(@Nullable AnalysisException analysisFailure) {
        this.analysisFailure = analysisFailure;
    }

    @Override
    public void execute(ConditionalJumpInstruction instruction) {
        switch (instruction.opcode()) {
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> frame.pop(1);
            case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> frame.pop(2);
        }
    }

    @Override
    public void execute(AllocateInstruction instruction) {
        ObjectType type = instruction.type();
        if (type instanceof ArrayType)
            frame.pop(1); // pop array size off stack
        frame.pushType(type);
    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {
        frame.pop(1);
    }

    @Override
    public void execute(TableSwitchInstruction instruction) {
        frame.pop(1);
    }

    @Override
    public void execute(ImmediateJumpInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(Instruction instruction) {
        // no-op, nothing should hit here
    }

    @Override
    public void label(Label label) {
        //no-op
    }
}

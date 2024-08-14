package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.type.*;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.VariableNameLookup;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.FrameMergeException;
import me.darknet.assembler.compile.analysis.frame.FrameOps;

import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.simulation.ExecutionEngine;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.ErrorCollector;
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
    protected static final InstanceType METHOD_TYPE = Types.instanceType(MethodType.class);
    protected static final InstanceType METHOD_HANDLE = Types.instanceType(MethodHandle.class);
    protected static final InstanceType CLASS = Types.instanceType(Class.class);

    protected final NavigableMap<Integer, F> frames = new TreeMap<>();
    protected final NavigableMap<Integer, F> terminalFrames = new TreeMap<>();
    private final Map<ASTInstruction, CodeElement> astToElement = new IdentityHashMap<>();
    private final Map<CodeElement, ASTInstruction> elementToAst = new IdentityHashMap<>();
    protected final VariableNameLookup variableNameLookup;
    protected InheritanceChecker checker;
    protected ErrorCollector errorCollector;

    protected AnalysisException analysisFailure;
    protected F frame;

    public JvmAnalysisEngine(@NotNull VariableNameLookup variableNameLookup) {
        this.variableNameLookup = variableNameLookup;
    }

    public abstract FrameOps<?> newFrameOps();

    /**
     * @param checker
     *                Inheritance checker to use. Can be {@code null} to disable
     *                capabilities surrounding {@code instanceof} and casting.
     */
    public void setChecker(InheritanceChecker checker) {
        this.checker = checker;
    }

    /**
     * @return Inheritance checker used. May be {@code null} if not assigned.
     */
    public @Nullable InheritanceChecker getChecker() {
        return checker;
    }

    /**
     * @param errorCollector
     *         Collector to dump error/warnings into.
     */
    public void setErrorCollector(ErrorCollector errorCollector) {
        this.errorCollector = errorCollector;
    }

    /**
     * @param element
     *         Origin of the warning.
     * @param message
     *         Warning message content.
     */
    public void warn(@NotNull CodeElement element, @NotNull String message) {
        if (errorCollector == null) return;
        ASTInstruction ast = getCodeToAstMap().get(element);
        if (ast != null) errorCollector.addWarn(message, ast.location());
    }

    /**
     * @param element
     *         Origin of the warning.
     * @param message
     *         Error message content.
     */
    protected void error(@NotNull CodeElement element, @NotNull String message) {
        if (errorCollector == null) return;
        ASTInstruction ast = getCodeToAstMap().get(element);
        if (ast != null) errorCollector.addError(message, ast.location());
    }

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
     * @param checker
     *              Inheritance checker to use for determining common super-types.
     * @param index
     *              Key.
     * @param frame
     *              Frame to put.
     *
     * @return {@code true} when the frame merge resulted in a change.
     * {@code false} when the frame merge resulted in no change.
     */
    @SuppressWarnings("unchecked")
    public boolean putAndMergeFrame(@NotNull InheritanceChecker checker, int index, @NotNull F frame) throws FrameMergeException {
        F old = getFrame(index);

        if (old == null) {
            putFrame(index, frame);
            return true;
        }

        F merged = (F) old.copy();
        boolean changed = merged.merge(checker, frame);
        putFrame(index, merged);
        return changed;
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
    public void execute(AllocateMultiDimArrayInstruction instruction) {
        int dimensions = instruction.dimensions();
        if (dimensions <= 0)
            warn(instruction, "multianewarray must have > 0 dimensions");
        frame.pop(dimensions); // pop n values off the stack that fill in the dimension sizes
        frame.pushType(instruction.type());
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

    /**
     * @param element Element being validated.
     * @param inputType Type use as input.
     * @param destinationType Type use as destination. Example cases being a field or method parameter.
     * @param verb Type use action.
     * @param noun Destination name.
     */
    protected void validateTypeUse(@NotNull CodeElement element, @Nullable ClassType inputType,
                                   @NotNull ClassType destinationType, @NotNull String verb, @NotNull String noun) {
        if (inputType instanceof PrimitiveType) {
            if (destinationType instanceof InstanceType)
                warn(element, "Cannot " + verb + " primitive value into instance " + noun);
            else if (destinationType instanceof ArrayType)
                warn(element, "Cannot " + verb + " primitive value into array " + noun);
        } else if (inputType instanceof ArrayType arrayValueType) {
            if (destinationType instanceof InstanceType instanceDestinationType && !Types.OBJECT.equals(instanceDestinationType))
                warn(element, "Cannot " + verb + " array value into " + noun + " that is not 'java/lang/Object'");
            else if (destinationType instanceof PrimitiveType)
                warn(element, "Cannot " + verb + " array value into primitive " + noun);
            else if (destinationType instanceof ArrayType arrayDestinationType && !arrayDestinationType.equals(arrayValueType))
                warn(element, "Cannot " + verb + " array value into array " + noun + " of different component type or dimension");
        }  else if (inputType instanceof InstanceType) {
            if (destinationType instanceof PrimitiveType)
                warn(element, "Cannot " + verb + " instance value into primitive " + noun);
            else if (destinationType instanceof ArrayType)
                warn(element, "Cannot " + verb + " instance value into array " + noun);
        } else if (inputType == null) {
            if (destinationType instanceof PrimitiveType)
                warn(element, "Cannot " + verb + " null value into primitive " + noun);
        }
    }
}

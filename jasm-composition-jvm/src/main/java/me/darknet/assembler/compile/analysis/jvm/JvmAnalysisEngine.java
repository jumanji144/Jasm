package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.*;
import dev.xdark.blw.type.*;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.FrameOps;

import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.simulation.ExecutionEngine;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.ErrorCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base outline for an engine intended for use in proper stack/local analysis.
 *
 * @see TypedJvmAnalysisEngine For basic type-tracking of stack/locals.
 * @see ValuedJvmAnalysisEngine For basic value-tracking of stack/locals.
 */
public abstract class JvmAnalysisEngine<F extends Frame> implements ExecutionEngine, JavaOpcodes {
    protected static final InstanceType METHOD_TYPE = Types.instanceType(MethodType.class);
    protected static final InstanceType METHOD_HANDLE = Types.instanceType(MethodHandle.class);
    protected static final InstanceType CLASS = Types.instanceType(Class.class);

    protected final VarCache varCache;
    protected InheritanceChecker checker;
    protected ErrorCollector errorCollector;
    private MethodAnalysisResult result;
    private AnalysisSession<F> session;

    public JvmAnalysisEngine(@NotNull VarCache varCache) {
        this.varCache = varCache;
        this.result = new MethodAnalysisResult();
    }

    public abstract FrameOps<?> newFrameOps();

    @NotNull
    public VarCache getVarCache() {
        return varCache;
    }

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

    public void setResult(@NotNull MethodAnalysisResult result) {
        this.result = result;
    }

    public void bindSession(@Nullable AnalysisSession<F> session) {
        this.session = session;
    }

    public final @NotNull MethodAnalysisResult result() {
        return result;
    }

    public final @NotNull AnalysisSession<F> session() {
        if (session == null)
            throw new IllegalStateException("Analysis session not bound");
        return session;
    }

    public final @NotNull F frame() {
        F frame = session().frame();
        if (frame == null)
            throw new IllegalStateException("Active frame not bound");
        return frame;
    }

    /**
     * @param errorCollector
     *         Collector to dump error/warnings into.
     */
    public void setErrorCollector(ErrorCollector errorCollector) {
        this.errorCollector = errorCollector;
    }

    /**
     * Clears errors and warnings at the location of the given code element.
     * When we revisit code after frame merges, we want to clear old problems that
     * may no longer be relevant with the new frame state.
     *
     * @param element
     *         Element with a location to clear errors/warnings at.
     */
    public void clearErrorsAt(@NotNull CodeElement element) {
        if (errorCollector == null)
            return;
        ASTInstruction ast = result().getCodeToAstMap().get(element);
        if (ast != null)
            errorCollector.removeAt(ast.location());
    }

    /**
     * @param element
     *         Origin of the warning.
     * @param message
     *         Warning message content.
     */
    public void warn(@NotNull CodeElement element, @NotNull String message) {
        if (errorCollector == null)
            return;
        ASTInstruction ast = result().getCodeToAstMap().get(element);
        if (ast != null)
            errorCollector.addWarn(message, ast.location());
    }

    /**
     * @param element
     *         Origin of the warning.
     * @param message
     *         Error message content.
     */
    protected void error(@NotNull CodeElement element, @NotNull String message) {
        if (errorCollector == null)
            return;
        ASTInstruction ast = result().getCodeToAstMap().get(element);
        if (ast != null)
            errorCollector.addError(message + " @ " + ast.content(), ast.location());
    }

    @Override
    public void execute(ConditionalJumpInstruction instruction) {
        switch (instruction.opcode()) {
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> frame().pop(1);
            case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> frame().pop(2);
        }
    }

    @Override
    public void execute(AllocateInstruction instruction) {
        ObjectType type = instruction.type();
        if (type instanceof ArrayType)
            frame().pop(1); // pop array size off stack
        frame().pushType(type);
    }

    @Override
    public void execute(AllocateMultiDimArrayInstruction instruction) {
        int dimensions = instruction.dimensions();
        if (dimensions <= 0)
            warn(instruction, "multianewarray must have > 0 dimensions");
        frame().pop(dimensions); // pop n values off the stack that fill in the dimension sizes
        frame().pushType(instruction.type());
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
        switch (inputType) {
            case PrimitiveType ignored -> {
                if (destinationType instanceof InstanceType)
                    warn(element, "Cannot " + verb + " primitive value into instance " + noun);
                else if (destinationType instanceof ArrayType)
                    warn(element, "Cannot " + verb + " primitive value into array " + noun);
            }
            case ArrayType arrayValueType -> {
                switch (destinationType) {
                    case InstanceType instanceDestinationType when !Types.OBJECT.equals(instanceDestinationType) ->
                            warn(element, "Cannot " + verb + " array value into " + noun + " that is not 'java/lang/Object'");
                    case PrimitiveType ignored ->
                            warn(element, "Cannot " + verb + " array value into primitive " + noun);
                    case ArrayType arrayDestinationType when !arrayDestinationType.equals(arrayValueType) ->
                            warn(element, "Cannot " + verb + " array value into array " + noun + " of different component type or dimension");
                    default -> {
                    }
                }
            }
            case InstanceType ignored -> {
                if (destinationType instanceof PrimitiveType)
                    warn(element, "Cannot " + verb + " instance value into primitive " + noun);
                else if (destinationType instanceof ArrayType)
                    warn(element, "Cannot " + verb + " instance value into array " + noun);
            }
            case null -> {
                if (destinationType instanceof PrimitiveType)
                    warn(element, "Cannot " + verb + " null value into primitive " + noun);
            }
            default -> {
            }
        }
    }
}

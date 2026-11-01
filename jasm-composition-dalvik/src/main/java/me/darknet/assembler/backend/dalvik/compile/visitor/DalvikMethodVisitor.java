package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.registry.FieldValueLookup;
import me.darknet.assembler.analysis.registry.MethodValueLookup;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.backend.dalvik.DalvikParameterNames;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikAnalysisEngine;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikAnalysisResults;
import me.darknet.assembler.backend.dalvik.instructions.RegisterOperands;
import me.darknet.assembler.backend.dalvik.instructions.RegisterRef;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.processing.ProcessedInstruction;
import me.darknet.assembler.processing.ProcessedMethod;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Pair;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.CodeBuilder;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.type.ClassType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Visits a Dalvik method declaration and builds its dex member, code, registers, and debug information.
 */
public class DalvikMethodVisitor extends DalvikMemberVisitor<MethodMember> implements ASTMethodVisitor {
    private final List<String> parameterNames = new ArrayList<>();
    private final List<Location> parameterLocations = new ArrayList<>();
    private final ASTMethod source;
    private final ProcessedMethod processed;
    private final MethodReference analysisMethod;
    private final Map<MethodReference, DalvikAnalysisResults> analysisResults;
    private final DiagnosticSink sink;
    private final InheritanceChecker inheritanceChecker;
    private final MethodValueLookup methodValueLookup;
    private final FieldValueLookup fieldValueLookup;
    private final Set<Integer> explicitRegisters;
    private int incomingRegisterCount;
    private CodeBuilder codeBuilder;
    private @Nullable DalvikRegisterLayout.Layout registerLayout;
    private DalvikCodeVisitor codeVisitor;
    private boolean codeRejected;

    /**
     * @param member
     *         Method being built.
     * @param processed
     *         Semantic method view carrying source and target extensions.
     * @param analysisMethod
     *         Full key for this source-emitted method.
     * @param analysisResults
     *         Insertion-ordered collector for emitted method results.
     * @param sink
     *         Sink reporting forms and register layouts this backend cannot encode.
     */
    public DalvikMethodVisitor(@NotNull MethodMember member, 
                               @NotNull ProcessedMethod processed,
                               @NotNull MethodReference analysisMethod,
                               @NotNull Map<MethodReference, DalvikAnalysisResults> analysisResults,
                               @NotNull DiagnosticSink sink,
                               @NotNull InheritanceChecker inheritanceChecker,
                               @NotNull MethodValueLookup methodValueLookup,
                               @NotNull FieldValueLookup fieldValueLookup) {
        super(member);
        this.source = processed.source();
        this.processed = processed;
        this.analysisMethod = analysisMethod;
        this.analysisResults = analysisResults;
        this.sink = sink;
        this.inheritanceChecker = inheritanceChecker;
        this.methodValueLookup = methodValueLookup;
        this.fieldValueLookup = fieldValueLookup;
        this.explicitRegisters = collectExplicitRegisters(processed);
    }

    @Override
    public void visitParameter(int index, @NotNull ASTIdentifier name) {
        while (parameterNames.size() <= index)
            parameterNames.add(null);
        parameterNames.set(index, name.literal());
        while (parameterLocations.size() <= index)
            parameterLocations.add(null);
        parameterLocations.set(index, name.location());
    }

    @Override
    public void visitDeclaredException(@NotNull ASTIdentifier exceptionType) {
        member.addThrownType(exceptionType.literal());
    }

    @Override
    public void visitAnnotationDefaultValue(@NotNull DiagnosticSink sink, ASTElement defaultValue) {
        List<Constant> captured = new ArrayList<>(1);
        DalvikAnnotationVisitor visitor = new DalvikAnnotationVisitor(
                DalvikAnnotationVisitor.RUNTIME,
                ASTIdentifier.STUB,
                annotation -> captured.addAll(annotation.annotation().elements().values())
        );
        ASTAnnotationVisitor.accept(visitor, List.of(new Pair<>(ASTIdentifier.STUB, defaultValue)), sink);

        // A value the annotation visitor could not build leaves the element without a default fallback.
        // The visitor is expected to report the problem, so this method does not need to.
        if (sink.hasErrors() || captured.size() != 1)
            return;

        member.setDefaultValue(captured.getFirst());
    }

    @Override
    public ASTAnnotationVisitor visitParameterAnnotation(@NotNull AnnotationVisibility visibility, int index,
                                                         @NotNull ASTIdentifier classType) {
        int parameterCount = member.getType().parameterTypes().size();
        if (index < 0 || index >= parameterCount)
            return null;
        byte mapped = switch (visibility) {
            case VISIBLE -> DalvikAnnotationVisitor.RUNTIME;
            case INVISIBLE -> DalvikAnnotationVisitor.BUILD;
            case SYSTEM -> DalvikAnnotationVisitor.SYSTEM;
        };
        return new DalvikAnnotationVisitor(mapped, classType, annotation -> {
            List<List<Annotation>> parameterAnnotations = new ArrayList<>(member.getParameterAnnotations());
            while (parameterAnnotations.size() <= index)
                parameterAnnotations.add(List.of());

            List<Annotation> forParameter = new ArrayList<>(parameterAnnotations.get(index));
            forParameter.add(annotation);
            parameterAnnotations.set(index, List.copyOf(forParameter));
            member.setParameterAnnotations(parameterAnnotations);
        });
    }

    @Override
    public ASTInstructionVisitor visitCode(@NotNull DiagnosticSink sink) {
        if (codeVisitor == null && !codeRejected) {
            incomingRegisterCount = incomingRegisterWords();

            // Calculate the register layout for the method, which includes the parameter registers and any additional registers
            // required for the method's code. If the layout cannot be calculated, the method is rejected and no code is emitted.
            Set<String> parameterAliases = Set.copyOf(parameterRegisters(0).keySet());
            registerLayout = DalvikRegisterLayout.calculate(processed, incomingRegisterCount, explicitRegisters, parameterAliases, sink);
            if (registerLayout == null) {
                codeRejected = true;
                return null;
            }

            // Create the visitor that builds the actual output code.
            int parameterBase = registerLayout.parameterBase();
            codeBuilder = new CodeBuilder();
            codeVisitor = new DalvikCodeVisitor(
                    sink,
                    parameterRegisters(parameterBase),
                    registerLayout.registerCount(),
                    parameterBase,
                    explicitRegisters
            );
        }
        return codeVisitor;
    }

    @Override
    public void visitEnd() {
        member.setParameterNames(parameterNames);
        boolean collided = reportParameterNameCollisions();
        if (codeVisitor != null && registerLayout != null) {
            int parameterBase = registerLayout.parameterBase();
            if (!collided)
                codeVisitor.reportNumericReferences(declaredParameterSlots(parameterBase),
                        wideParameterSlots(parameterBase), receiverSlot(parameterBase));

            // The named locals span the whole body, so the body has to start and end with a label. The labels go into a
            // copy before the code is built, because a built Code's instruction list is read-only.
            List<Instruction> body = new ArrayList<>(codeVisitor.body());
            Map<Integer, String> names = codeVisitor.localNames();
            List<DebugInformation.LocalVariable> locals = List.of();
            if (!names.isEmpty()) {
                Label entry = entryLabel(body);
                Label exit = exitLabel(body);
                locals = localVariables(names, entry, exit);
            }
            for (Instruction instruction : body)
                codeBuilder.add(instruction);
            Code code = codeBuilder
                    .arguments(incomingRegisterCount, codeVisitor.getOutRegisters())
                    .registers(registerLayout.registerCount())
                    .build();
            codeVisitor.getTryCatches().forEach(code::addTryCatch);
            DebugInformation debugInfo = debugInformation(code, locals);
            if (debugInfo != null)
                code.setDebugInfo(debugInfo);
            member.setCode(code);

            Map<Instruction, ASTInstruction> instructionToSource = codeVisitor.buildSourceMap(code.getInstructions());
            if (!instructionToSource.isEmpty()) {
                analysisResults.put(analysisMethod, DalvikAnalysisEngine.analyze(
                        analysisMethod, member, code, instructionToSource, inheritanceChecker,
                        methodValueLookup, fieldValueLookup));
            }
        }
    }

    /**
     * Rejects each parameter name that would silently refer to another binding. The error fails the compile, so no output
     * is written for the method.
     *
     * @return {@code true} when any parameter name collides.
     */
    private boolean reportParameterNameCollisions() {
        int count = member.getType().parameterTypes().size();
        boolean instance = (member.getAccess() & DalvikModifiers.ACC_STATIC) == 0;
        Map<Integer, String> collisions = DalvikParameterNames.collisions(parameterNames, count, instance);
        for (Map.Entry<Integer, String> collision : collisions.entrySet()) {
            int index = collision.getKey();
            Location location = index < parameterLocations.size() && parameterLocations.get(index) != null
                    ? parameterLocations.get(index)
                    : source.location();
            sink.error(DiagnosticCode.PARAMETER_NAME_COLLISION,
                    "Parameter name cannot be used: " + collision.getValue(), location);
        }
        return !collisions.isEmpty();
    }

    /**
     * @return Slot of the receiver, which is the first slot of the parameters, or {@code null} for a static method.
     */
    private @Nullable Integer receiverSlot(int parameterBase) {
        boolean instance = (member.getAccess() & DalvikModifiers.ACC_STATIC) == 0;
        return instance ? parameterBase : null;
    }

    /**
     * @return Number of Dalvik registers required for the incoming parameters of this method.
     */
    private int incomingRegisterWords() {
        int words = (member.getAccess() & DalvikModifiers.ACC_STATIC) == 0 ? 1 : 0;
        for (ClassType parameter : member.getType().parameterTypes())
            words += registerWords(parameter.descriptor());
        return words;
    }

    /**
     * @param parameterBase
     *         Base register index for the first parameter.
     *
     * @return Map of parameter names to their corresponding Dalvik register indices.
     */
    private @NotNull Map<String, Integer> parameterRegisters(int parameterBase) {
        Map<String, Integer> registers = new HashMap<>();
        int aliasBase = parameterBase;
        int offset = 0;
        if ((member.getAccess() & DalvikModifiers.ACC_STATIC) == 0) {
            registers.put("this", aliasBase);
            offset++;
        }
        for (int index = 0; index < member.getType().parameterTypes().size(); index++) {
            int register = aliasBase + offset;
            String name = index < parameterNames.size() ? parameterNames.get(index) : null;
            if (name != null)
                registers.put(name, register);
            registers.put("p" + index, register);
            offset += registerWords(member.getType().parameterTypes().get(index).descriptor());
        }
        return registers;
    }

    /**
     * @param parameterBase
     *         Base register index for the first parameter.
     *
     * @return Declared parameter names keyed by the slot of each parameter's first word. The receiver and unnamed
     * parameters are absent.
     */
    private @NotNull Map<Integer, String> declaredParameterSlots(int parameterBase) {
        Map<Integer, String> slots = new HashMap<>();
        int register = parameterBase;
        if ((member.getAccess() & DalvikModifiers.ACC_STATIC) == 0)
            register++;
        for (int index = 0; index < member.getType().parameterTypes().size(); index++) {
            String name = index < parameterNames.size() ? parameterNames.get(index) : null;
            if (name != null)
                slots.put(register, name);
            register += registerWords(member.getType().parameterTypes().get(index).descriptor());
        }
        return slots;
    }

    /**
     * @param parameterBase
     *         Base register index for the first parameter.
     *
     * @return Slots of the wide parameters, whose high word follows the slot of the parameter.
     */
    private @NotNull Set<Integer> wideParameterSlots(int parameterBase) {
        Set<Integer> slots = new HashSet<>();
        int register = parameterBase;
        if ((member.getAccess() & DalvikModifiers.ACC_STATIC) == 0)
            register++;
        for (ClassType parameter : member.getType().parameterTypes()) {
            if (registerWords(parameter.descriptor()) == 2)
                slots.add(register);
            register += registerWords(parameter.descriptor());
        }
        return slots;
    }

    /**
     * @param code
     *         Code to build debug information for.
     * @param locals
     *         Named locals of the code, each covering the whole method.
     *
     * @return Debug information for the code, or {@code null} if no debug information is available.
     */
    private @Nullable DebugInformation debugInformation(@NotNull Code code,
                                                        @NotNull List<DebugInformation.LocalVariable> locals) {
        List<DebugInformation.LineNumber> lineNumbers = new ArrayList<>();
        for (Instruction instruction : code.getInstructions())
            if (instruction instanceof Label label && label.lineNumber() != Label.UNASSIGNED)
                lineNumbers.add(new DebugInformation.LineNumber(label, label.lineNumber()));
        if (lineNumbers.isEmpty() && parameterNames.isEmpty() && locals.isEmpty())
            return null;
        return new DebugInformation(lineNumbers, List.of(), List.copyOf(parameterNames), locals);
    }

    /**
     * Describes every named local over the whole method, from its entry to its exit, with no type. The range is bounded by
     * the given labels, which the encoder places at the start and end of the method.
     *
     * @param names
     *         Names of the locals, keyed by slot.
     * @param entry
     *         Label at the start of the body.
     * @param exit
     *         Label at the end of the body.
     *
     * @return One local per named slot, in ascending slot order.
     */
    private static @NotNull List<DebugInformation.LocalVariable> localVariables(@NotNull Map<Integer, String> names,
                                                                              @NotNull Label entry, @NotNull Label exit) {
        List<DebugInformation.LocalVariable> locals = new ArrayList<>(names.size());
        for (Map.Entry<Integer, String> local : names.entrySet())
            locals.add(new DebugInformation.LocalVariable(local.getKey(), local.getValue(), null, null, entry, exit));
        return locals;
    }

    /**
     * Returns the label the named locals start at: the body's first label when it has one, and otherwise a new label
     * placed before the first instruction.
     *
     * @param body
     *         Instructions and labels of the method, in code order. A new label is added to its front.
     *
     * @return The entry label.
     */
    private static @NotNull Label entryLabel(@NotNull List<Instruction> body) {
        if (!body.isEmpty() && body.getFirst() instanceof Label first)
            return first;
        Label entry = new Label();
        body.addFirst(entry);
        return entry;
    }

    /**
     * Returns the label the named locals end at: the body's last label when it has one, and otherwise a new label
     * placed after the last instruction.
     *
     * @param body
     *         Instructions and labels of the method, in code order. A new label is added to its end.
     *
     * @return The exit label.
     */
    private static @NotNull Label exitLabel(@NotNull List<Instruction> body) {
        if (!body.isEmpty() && body.getLast() instanceof Label last)
            return last;
        Label exit = new Label();
        body.add(exit);
        return exit;
    }

    /**
     * @param descriptor
     *         Type descriptor of the parameter.
     *
     * @return Number of Dalvik registers required for the parameter (1 or 2).
     */
    private static int registerWords(@NotNull String descriptor) {
        return descriptor.length() == 1 && (descriptor.charAt(0) == 'J' || descriptor.charAt(0) == 'D') ? 2 : 1;
    }

    /**
     * Collects all registers explicitly referenced in the method's code.
     * <p>
     * This is used to ensure that the backend does not allocate a register that is already used by the source code.
     *
     * @param processed
     *         Processed method containing the code to analyze.
     *
     * @return Set of register indices explicitly referenced in the method's code.
     */
    private static @NotNull Set<Integer> collectExplicitRegisters(@NotNull ProcessedMethod processed) {
        Set<Integer> registers = new HashSet<>();
        for (var entry : processed.code()) {
            if (!(entry instanceof ProcessedInstruction instruction))
                continue;
            for (var operand : instruction.operands()) {
                if (operand.value() instanceof RegisterRef register) {
                    reserveExplicitRegister(registers, register);
                } else if (operand.value() instanceof RegisterOperands operands) {
                    for (RegisterRef register : operands.registers())
                        reserveExplicitRegister(registers, register);
                }
            }
        }
        return Set.copyOf(registers);
    }

    /**
     * Reserves a single register in the given set, and if the register is wide, also reserves the next register.
     *
     * @param registers
     *         Set of register indices to reserve.
     * @param register
     *         Register reference to reserve.
     */
    private static void reserveExplicitRegister(@NotNull Set<Integer> registers, @NotNull RegisterRef register) {
        Integer index = register.index();
        if (index == null || index < 0)
            return;
        registers.add(index);
        if (register.width() == OperandRole.WidthPolicy.WIDE
                && index < Integer.MAX_VALUE) {
            registers.add(index + 1);
        }
    }
}

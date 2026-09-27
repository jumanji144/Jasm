package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.instructions.DalvikMethodData;
import me.darknet.assembler.processing.ProcessedMethod;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticSink;
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
import java.util.List;
import java.util.Map;

/**
 * Visits a Dalvik method declaration and builds its dex member, code, registers, and debug information.
 */
public class DalvikMethodVisitor extends DalvikMemberVisitor<MethodMember> implements ASTMethodVisitor {
    private final List<String> parameterNames = new ArrayList<>();
    private final ASTMethod source;
    private final @Nullable DalvikMethodData methodData;
    private final DiagnosticSink sink;
    private Integer declaredRegisterCount;
    private int incomingRegisterCount;
    private CodeBuilder codeBuilder;
    private DalvikCodeVisitor codeVisitor;
    private boolean codeRejected;

    /**
     * @param member
     * 		Method being built.
     * @param processed
     * 		Semantic method view carrying source and target extensions.
     * @param sink
     * 		Sink reporting forms and register layouts this backend cannot encode.
     */
    public DalvikMethodVisitor(MethodMember member, @NotNull ProcessedMethod processed,
                               @NotNull DiagnosticSink sink) {
        super(member);
        this.source = processed.source();
        this.methodData = processed.extensions().get(DalvikMethodData.class);
        this.sink = sink;
    }

    @Override
    public void visitParameter(int index, @NotNull ASTIdentifier name) {
        while (parameterNames.size() <= index)
            parameterNames.add(null);
        parameterNames.set(index, name.literal());
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
            declaredRegisterCount = methodData == null ? null : methodData.registers();

            // The declaration is what every register-count problem is measured against, so it is used as the
            // report location whenever the method declared one.
            Location registerDeclaration = methodData == null ? null : methodData.source().location();
            Location mismatchLocation = registerDeclaration == null ? source.location() : registerDeclaration;
            codeBuilder = new CodeBuilder();
            incomingRegisterCount = incomingRegisterWords();
            if (declaredRegisterCount == null && incomingRegisterCount > 0) {
                codeRejected = true;
                sink.error(DiagnosticCode.REGISTER_DECLARATION, "Dalvik methods with parameters require registers", mismatchLocation);
                return null;
            }
            int registerLimit = declaredRegisterCount == null ? -1 : declaredRegisterCount;
            int parameterBase = declaredRegisterCount == null ? -1 : declaredRegisterCount - incomingRegisterCount;
            if (declaredRegisterCount != null && parameterBase < 0) {
                codeRejected = true;
                sink.error(DiagnosticCode.REGISTER_DECLARATION, "Declared register count is smaller than incoming parameters", mismatchLocation);
                return null;
            }
            codeVisitor = new DalvikCodeVisitor(
                    codeBuilder,
                    sink,
                    parameterRegisters(parameterBase),
                    registerLimit,
                    parameterBase,
                    registerDeclaration
            );
        }
        return codeVisitor;
    }

    private int incomingRegisterWords() {
        int words = (member.getAccess() & DalvikModifiers.ACC_STATIC) == 0 ? 1 : 0;
        for (ClassType parameter : member.getType().parameterTypes())
            words += registerWords(parameter.descriptor());
        return words;
    }

    private Map<String, Integer> parameterRegisters(int parameterBase) {
        Map<String, Integer> registers = new HashMap<>();
        int aliasBase = Math.max(parameterBase, 0);
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

    private static int registerWords(String descriptor) {
        return descriptor.length() == 1 && (descriptor.charAt(0) == 'J' || descriptor.charAt(0) == 'D') ? 2 : 1;
    }

    @Override
    public void visitEnd() {
        member.setParameterNames(parameterNames);
        if (codeVisitor != null) {
            int totalRegisters = Math.max(incomingRegisterCount, codeVisitor.getRegisterCount());
            if (declaredRegisterCount != null && totalRegisters > declaredRegisterCount) {
                // The visitor has already reported the register limit problem, so this method does not need to.
                // What we do want to report is if the method declared a register count but the code used more than that, which is a separate problem.
                if (!codeVisitor.hasReportedRegisterLimit())
                    sink.error(DiagnosticCode.REGISTER_LIMIT, "Code uses more registers than declared", source.location());
                return;
            }

            Code code = codeBuilder
                    .arguments(incomingRegisterCount, codeVisitor.getOutRegisters())
                    .registers(declaredRegisterCount == null ? totalRegisters : declaredRegisterCount)
                    .build();
            codeVisitor.getTryCatches().forEach(code::addTryCatch);
            DebugInformation debugInfo = debugInformation(code);
            if (debugInfo != null)
                code.setDebugInfo(debugInfo);
            member.setCode(code);
        }
    }

    private DebugInformation debugInformation(Code code) {
        List<DebugInformation.LineNumber> lineNumbers = new ArrayList<>();
        for (Instruction instruction : code.getInstructions())
            if (instruction instanceof Label label && label.lineNumber() != Label.UNASSIGNED)
                lineNumbers.add(new DebugInformation.LineNumber(label, label.lineNumber()));
        if (lineNumbers.isEmpty() && parameterNames.isEmpty())
            return null;
        return new DebugInformation(lineNumbers, List.copyOf(parameterNames), List.of());
    }
}

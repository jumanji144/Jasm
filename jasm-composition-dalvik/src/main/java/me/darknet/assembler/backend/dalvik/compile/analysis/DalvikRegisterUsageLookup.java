package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.instructions.ArrayData;
import me.darknet.assembler.backend.dalvik.instructions.DalvikLowering;
import me.darknet.assembler.backend.dalvik.instructions.PackedSwitchPayload;
import me.darknet.assembler.backend.dalvik.instructions.RegisterOperands;
import me.darknet.assembler.backend.dalvik.instructions.RegisterRange;
import me.darknet.assembler.backend.dalvik.instructions.RegisterRef;
import me.darknet.assembler.backend.dalvik.instructions.SignedLiteral;
import me.darknet.assembler.backend.dalvik.instructions.SparseSwitchPayload;
import me.darknet.assembler.backend.dalvik.visitor.ASTDalvikInstructionVisitor;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.instructions.SemanticInstruction;
import me.darknet.assembler.processing.ProcessedCodeEntry;
import me.darknet.assembler.processing.ProcessedInstruction;
import me.darknet.assembler.processing.ProcessedMethod;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.dex.file.instructions.Opcodes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable register-usage summaries keyed by method name and descriptor.
 * <p>
 * A compiler can build this lookup from its {@link ValidatedUnit}, reusing the processed methods that it
 * already has instead of making each consumer run semantic processing again.
 */
public final class DalvikRegisterUsageLookup {
    private static final DalvikRegisterUsageLookup EMPTY = new DalvikRegisterUsageLookup(Map.of());

    private final Map<MethodKey, List<DalvikRegisterUsage>> usagesByMethod;

    private DalvikRegisterUsageLookup(Map<MethodKey, List<DalvikRegisterUsage>> usagesByMethod) {
        Map<MethodKey, List<DalvikRegisterUsage>> copy = new LinkedHashMap<>(usagesByMethod.size());
        usagesByMethod.forEach((method, usages) -> copy.put(method, List.copyOf(usages)));
        this.usagesByMethod = Collections.unmodifiableMap(copy);
    }

    /**
     * @return An empty register-usage lookup.
     */
    public static @NotNull DalvikRegisterUsageLookup empty() {
        return EMPTY;
    }

    /**
     * Collects register accesses from the processed methods in a validated unit.
     *
     * @param unit
     *        Unit already processed for the Dalvik target.
     *
     * @return Immutable register usages keyed by method name and descriptor.
     */
    public static @NotNull DalvikRegisterUsageLookup from(@NotNull ValidatedUnit unit) {
        if (unit.methods().isEmpty())
            return empty();

        Map<MethodKey, List<DalvikRegisterUsage>> usages = new LinkedHashMap<>(unit.methods().size());
        for (ProcessedMethod method : unit.methods().values()) {
            ASTMethod source = method.source();
            MethodKey key = new MethodKey(source.getName().literal(), source.getDescriptor().literal());
            usages.put(key, collect(method));
        }
        return new DalvikRegisterUsageLookup(usages);
    }

    /**
     * @param method
     *        Source method AST to look up. Its name and descriptor identify the compiled method, so a
     *        reparsed source node with the same signature can also be used.
     *
     * @return Register access summaries in first-operand encounter order, or an empty list if absent.
     */
    public @NotNull List<DalvikRegisterUsage> getUsages(@NotNull ASTMethod method) {
        MethodKey key = new MethodKey(method.getName().literal(), method.getDescriptor().literal());
        List<DalvikRegisterUsage> usages = usagesByMethod.get(key);
        return usages == null ? List.of() : usages;
    }

    private static List<DalvikRegisterUsage> collect(@NotNull ProcessedMethod method) {
        Map<String, MutableUsage> usages = new LinkedHashMap<>(); // Maintain encounter order

        // Visit instructions and collect register accesses.
        RegisterCollector collector = new RegisterCollector(usages);
        for (ProcessedCodeEntry entry : method.code()) {
            if (entry instanceof ProcessedInstruction instruction) {
                collector.source = instruction.source();
                instruction.definition().transform(instruction, collector);
            }
        }

        // Wrap to final usage model.
        List<DalvikRegisterUsage> result = new ArrayList<>(usages.size());
        usages.forEach((name, usage) -> result.add(new DalvikRegisterUsage(name, usage.readCount, usage.writeCount, usage.references)));
        return List.copyOf(result);
    }

    /**
     * Key for a method in the register-usage lookup.
     *
     * @param name
     *        Method name.
     * @param descriptor
     *        Method descriptor.
     */
    private record MethodKey(@NotNull String name, @NotNull String descriptor) {}

    private static final class MutableUsage {
        private int readCount;
        private int writeCount;
        private final List<ASTInstruction> references = new ArrayList<>();
        private final Set<ASTInstruction> seen = Collections.newSetFromMap(new IdentityHashMap<>());

        private void addReference(ASTInstruction source) {
            if (seen.add(source))
                references.add(source);
        }
    }

    private static final class RegisterCollector implements ASTDalvikInstructionVisitor {
        private final Map<String, MutableUsage> usages;
        private ASTInstruction source;

        private RegisterCollector(Map<String, MutableUsage> usages) {
            this.usages = usages;
        }

        private void read(RegisterRef register) {
            access(register.name(), true, false);
        }

        private void write(RegisterRef register) {
            access(register.name(), false, true);
        }

        private void readWrite(RegisterRef register) {
            access(register.name(), true, true);
        }

        private void read(RegisterOperands operands) {
            if (operands.isRange() && operands instanceof RegisterRange range) {
                access("{" + range.first().name() + ", " + range.last().name() + "}", true, false);
            } else {
                for (RegisterRef register : operands.registers())
                    read(register);
            }
        }

        private void access(String name, boolean read, boolean write) {
            MutableUsage usage = usages.computeIfAbsent(name, ignored -> new MutableUsage());
            if (read)
                usage.readCount++;
            if (write)
                usage.writeCount++;
            usage.addReference(source);
        }

        @Override
        public void visitInstruction(SemanticInstruction instruction) {}

        @Override
        public void visitLabel(ASTIdentifier label) {}

        @Override
        public void visitLineNumber(ASTNumber line) {}

        @Override
        public void visitException(ASTIdentifier start, ASTIdentifier end,
                                   ASTIdentifier handler, ASTIdentifier type) {}

        @Override
        public void visitEnd() {}

        @Override
        public void visitNop() {}

        @Override
        public void visitMove(DalvikLowering lowering, RegisterRef to, RegisterRef from) {
            read(from);
            write(to);
        }

        @Override
        public void visitMoveResult(DalvikLowering lowering, RegisterRef to) {
            write(to);
        }

        @Override
        public void visitMoveException(RegisterRef to) {
            write(to);
        }

        @Override
        public void visitReturn(DalvikLowering lowering, RegisterRef returnValue) {
            read(returnValue);
        }

        @Override
        public void visitReturnVoid() {}

        @Override
        public void visitConst(DalvikLowering lowering, RegisterRef to, ASTElement value) {
            write(to);
        }

        @Override
        public void visitMonitorEnter(RegisterRef register) {
            read(register);
        }

        @Override
        public void visitMonitorExit(RegisterRef register) {
            read(register);
        }

        @Override
        public void visitCheckCast(RegisterRef register, ASTIdentifier type) {
            readWrite(register);
        }

        @Override
        public void visitInstanceOf(RegisterRef result, RegisterRef check, ASTIdentifier type) {
            read(check);
            write(result);
        }

        @Override
        public void visitArrayLength(RegisterRef result, RegisterRef array) {
            read(array);
            write(result);
        }

        @Override
        public void visitNewInstance(RegisterRef result, ASTIdentifier type) {
            write(result);
        }

        @Override
        public void visitNewArray(RegisterRef result, RegisterRef size, ASTIdentifier type) {
            read(size);
            write(result);
        }

        @Override
        public void visitFilledNewArray(RegisterOperands args, ASTIdentifier type) {
            read(args);
        }

        @Override
        public void visitFillArrayData(RegisterRef to, ArrayData data) {
            read(to);
        }

        @Override
        public void visitThrow(RegisterRef exception) {
            read(exception);
        }

        @Override
        public void visitGoto(ASTIdentifier label) {}

        @Override
        public void visitPackedSwitch(RegisterRef register, PackedSwitchPayload payload) {
            read(register);
        }

        @Override
        public void visitSparseSwitch(RegisterRef register, SparseSwitchPayload payload) {
            read(register);
        }

        @Override
        public void visitCmp(DalvikLowering lowering, RegisterRef to, RegisterRef from1, RegisterRef from2) {
            read(from1);
            read(from2);
            write(to);
        }

        @Override
        public void visitIf(DalvikLowering lowering, RegisterRef a, RegisterRef b, ASTIdentifier label) {
            read(a);
            read(b);
        }

        @Override
        public void visitIfZero(DalvikLowering lowering, RegisterRef a, ASTIdentifier label) {
            read(a);
        }

        @Override
        public void visitBinaryOperation(DalvikLowering lowering, RegisterRef to, RegisterRef from1, RegisterRef from2) {
            read(from1);
            read(from2);
            write(to);
        }

        @Override
        public void visitBinary2AddrOperation(DalvikLowering lowering, RegisterRef a, RegisterRef b) {
            readWrite(a);
            read(b);
        }

        @Override
        public void visitBinaryLiteralOperation(DalvikLowering lowering, RegisterRef to, RegisterRef from,
                                                SignedLiteral constant) {
            read(from);
            write(to);
        }

        @Override
        public void visitArrayOperation(DalvikLowering lowering, RegisterRef array, RegisterRef index, RegisterRef value) {
            switch (lowering.opcode()) {
                case Opcodes.AGET, Opcodes.AGET_WIDE, Opcodes.AGET_OBJECT, Opcodes.AGET_BOOLEAN,
                        Opcodes.AGET_BYTE, Opcodes.AGET_CHAR, Opcodes.AGET_SHORT -> {
                    read(array);
                    read(index);
                    write(value);
                }
                case Opcodes.APUT, Opcodes.APUT_WIDE, Opcodes.APUT_OBJECT, Opcodes.APUT_BOOLEAN,
                        Opcodes.APUT_BYTE, Opcodes.APUT_CHAR, Opcodes.APUT_SHORT -> {
                    read(array);
                    read(index);
                    read(value);
                }
                default -> throw new IllegalArgumentException("Unexpected Dalvik array opcode: " + lowering.opcode());
            }
        }

        @Override
        public void visitVirtualFieldOperation(DalvikLowering lowering, RegisterRef value, RegisterRef instance,
                                               MemberPath path, ASTIdentifier descriptor) {
            switch (lowering.opcode()) {
                case Opcodes.IGET, Opcodes.IGET_WIDE, Opcodes.IGET_OBJECT, Opcodes.IGET_BOOLEAN,
                        Opcodes.IGET_BYTE, Opcodes.IGET_CHAR, Opcodes.IGET_SHORT -> {
                    read(instance);
                    write(value);
                }
                case Opcodes.IPUT, Opcodes.IPUT_WIDE, Opcodes.IPUT_OBJECT, Opcodes.IPUT_BOOLEAN,
                        Opcodes.IPUT_BYTE, Opcodes.IPUT_CHAR, Opcodes.IPUT_SHORT -> {
                    read(value);
                    read(instance);
                }
                default -> throw new IllegalArgumentException("Unexpected Dalvik instance-field opcode: " + lowering.opcode());
            }
        }

        @Override
        public void visitStaticFieldOperation(DalvikLowering lowering, RegisterRef value, MemberPath path,
                                              ASTIdentifier descriptor) {
            switch (lowering.opcode()) {
                case Opcodes.SGET, Opcodes.SGET_WIDE, Opcodes.SGET_OBJECT, Opcodes.SGET_BOOLEAN,
                        Opcodes.SGET_BYTE, Opcodes.SGET_CHAR, Opcodes.SGET_SHORT -> write(value);
                case Opcodes.SPUT, Opcodes.SPUT_WIDE, Opcodes.SPUT_OBJECT, Opcodes.SPUT_BOOLEAN,
                        Opcodes.SPUT_BYTE, Opcodes.SPUT_CHAR, Opcodes.SPUT_SHORT -> read(value);
                default -> throw new IllegalArgumentException("Unexpected Dalvik static-field opcode: " + lowering.opcode());
            }
        }

        @Override
        public void visitInvoke(DalvikLowering lowering, RegisterOperands registers, MemberPath method,
                                ASTIdentifier descriptor) {
            read(registers);
        }

        @Override
        public void visitInvokePolymorphic(RegisterOperands registers, MemberPath method,
                                           ASTIdentifier descriptor, ASTIdentifier proto) {
            read(registers);
        }

        @Override
        public void visitInvokeCustom(RegisterOperands registers, ASTIdentifier name,
                                      ASTIdentifier type, ASTElement handle, ASTArray arguments) {
            read(registers);
        }

        @Override
        public void visitUnaryOperation(DalvikLowering lowering, RegisterRef to, RegisterRef from) {
            read(from);
            write(to);
        }
    }
}

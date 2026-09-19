package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.compile.DalvikConstantMapper;
import me.darknet.assembler.visitor.ASTDalvikInstructionVisitor;
import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.code.CodeBuilder;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.constant.HandleConstant;
import me.darknet.dex.tree.definitions.instructions.*;
import me.darknet.dex.tree.type.ClassType;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.ReferenceType;
import me.darknet.dex.tree.type.TypeParser;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DalvikCodeVisitor implements ASTDalvikInstructionVisitor, Opcodes {

    private final CodeBuilder codeBuilder;
    private ASTInstruction currentInstructionAst;
    private Kind currentInstructionKind = Kind.NORMAL;
    private final Map<String, Integer> registerMap = new LinkedHashMap<>();
    private final Set<Integer> usedRegisters = new HashSet<>();
    private final Map<String, Label> labels = new LinkedHashMap<>();
    private final List<TryCatch> tryCatches = new ArrayList<>();
    private final int registerLimit;
    private final int parameterBase;
    private int nextLocalRegister;
    private int registerCount;
    private int outRegisters;
    private int pendingLineNumber = Label.UNASSIGNED;

    enum Kind {
        NORMAL,
        OBJECT,
        WIDE,
        RESULT
    }

    public DalvikCodeVisitor(CodeBuilder codeBuilder, Map<String, Integer> initialRegisterMap,
                             int registerLimit, int parameterBase) {
        this.codeBuilder = codeBuilder;
        this.registerLimit = registerLimit;
        this.parameterBase = parameterBase;
        initialRegisterMap.forEach((name, index) -> {
            registerMap.put(name, index);
            usedRegisters.add(index);
            registerCount = Math.max(registerCount, index + 1);
        });
        if (registerLimit >= 0) {
            registerCount = Math.max(registerCount, registerLimit);
        }
    }

    public int getRegisterIndex(String registerName) {
        Integer numericRegister = parseNumericRegister(registerName);
        if (numericRegister != null) {
            ensureRegisterAllowed(numericRegister);
            usedRegisters.add(numericRegister);
            registerCount = Math.max(registerCount, numericRegister + 1);
            return numericRegister;
        }

        Integer existing = registerMap.get(registerName);
        if (existing != null) {
            return existing;
        }

        int index = nextLocalRegister;
        while (usedRegisters.contains(index) || (parameterBase >= 0 && index >= parameterBase)) {
            index++;
        }
        ensureRegisterAllowed(index);
        registerMap.put(registerName, index);
        usedRegisters.add(index);
        nextLocalRegister = index + 1;
        registerCount = Math.max(registerCount, index + 1);
        return index;
    }

    private static Integer parseNumericRegister(String registerName) {
        if (registerName.length() < 2 || registerName.charAt(0) != 'v') {
            return null;
        }
        for (int i = 1; i < registerName.length(); i++) {
            if (!Character.isDigit(registerName.charAt(i))) {
                return null;
            }
        }
        try {
            return Integer.parseInt(registerName.substring(1));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid register name: " + registerName, exception);
        }
    }

    private void ensureRegisterAllowed(int index) {
        if (index < 0 || (registerLimit >= 0 && index >= registerLimit)) {
            throw new IllegalStateException("Register exceeds declared register count: v" + index);
        }
    }

    public int getRegisterCount() {
        return registerCount;
    }

    public int getOutRegisters() {
        return outRegisters;
    }

    private void updateOutRegisters(int count) {
        outRegisters = Math.max(outRegisters, count);
    }

    public List<TryCatch> getTryCatches() {
        return List.copyOf(tryCatches);
    }

    private void addInstruction(Instruction instruction) {
        if (pendingLineNumber != Label.UNASSIGNED) {
            Label label = new Label();
            label.lineNumber(pendingLineNumber);
            codeBuilder.add(label);
            pendingLineNumber = Label.UNASSIGNED;
        }
        codeBuilder.add(instruction);
    }

    private @NotNull Label getLabel(@NotNull String name) {
        return labels.computeIfAbsent(name, ignored -> new Label());
    }

    private @NotNull String getCurrentOpName() {
        return currentInstructionAst.identifier().content();
    }

    private static @NotNull ClassType parseClassType(@NotNull ASTIdentifier type) {
        return new TypeParser(type.literal()).requireClassType();
    }

    private static @NotNull ReferenceType parseReferenceType(@NotNull String owner) {
        return owner.startsWith("[")
                ? Types.referenceTypeFromDescriptor(owner)
                : Types.instanceTypeFromInternalName(owner);
    }

    private static @NotNull MethodType parseMethodType(@NotNull ASTIdentifier descriptor) {
        return Types.methodTypeFromDescriptor(descriptor.literal());
    }

    private static @NotNull InstanceType parseInstanceType(@NotNull ASTIdentifier type) {
        String literal = type.literal();
        if (literal.startsWith("L")) {
            ClassType classType = new TypeParser(literal).requireClassType();
            if (classType instanceof InstanceType instanceType) {
                return instanceType;
            }
            throw new IllegalStateException("Expected instance type, got: " + literal);
        }
        return Types.instanceTypeFromInternalName(literal);
    }

    private static int argumentWordCount(int[] registers, boolean range) {
        if (range) {
            return registers[1] - registers[0] + 1;
        }
        return registers.length;
    }

    private int[] parseRegisters(@NotNull ASTArray registers, boolean range) {
        List<ASTElement> values = registers.values();
        int[] parsed = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            parsed[i] = parseRegister(values.get(i));
        }

        if (range && parsed.length != 2) {
            throw new IllegalStateException("Range instructions require first and last register bounds");
        }
        if (range && parsed[1] < parsed[0]) {
            throw new IllegalStateException("Range instruction register bounds are reversed");
        }

        return parsed;
    }

    private int parseRegister(@NotNull ASTElement element) {
        if (!(element instanceof ASTIdentifier identifier)) {
            throw new IllegalStateException("Expected register identifier");
        }

        return getRegisterIndex(identifier.literal());
    }

    private static @NotNull MemberPath parseMemberPath(@NotNull ASTIdentifier path) {
        String literal = path.literal();
        int separator = literal.lastIndexOf('.');
        if (separator <= 0 || separator == literal.length() - 1) {
            throw new IllegalStateException("Expected member path in owner.name form: " + literal);
        }

        String owner = literal.substring(0, separator);
        String name = literal.substring(separator + 1);
        return new MemberPath(owner, name);
    }

    private static int parseSpecialNumber(@NotNull String literal) {
        String normalized = literal.toLowerCase();
        if (normalized.endsWith("f")) {
            return Float.floatToIntBits(switch (normalized) {
                case "nanf", "nan" -> Float.NaN;
                case "infinityf", "+infinityf", "infinity", "+infinity" -> Float.POSITIVE_INFINITY;
                case "-infinityf", "-infinity" -> Float.NEGATIVE_INFINITY;
                default -> throw new IllegalStateException("Unsupported const literal: " + literal);
            });
        }

        float value = switch (normalized) {
            case "nan" -> Float.NaN;
            case "infinity", "+infinity" -> Float.POSITIVE_INFINITY;
            case "-infinity" -> Float.NEGATIVE_INFINITY;
            default -> throw new IllegalStateException("Unsupported const literal: " + literal);
        };
        return Float.floatToIntBits(value);
    }

    private static long parseSpecialWideNumber(@NotNull String literal) {
        String normalized = literal.toLowerCase();
        return Double.doubleToLongBits(switch (normalized) {
            case "nan", "nand" -> Double.NaN;
            case "infinity", "+infinity", "infinityd", "+infinityd" -> Double.POSITIVE_INFINITY;
            case "-infinity", "-infinityd" -> Double.NEGATIVE_INFINITY;
            default -> throw new IllegalStateException("Unsupported const-wide literal: " + literal);
        });
    }

    private static boolean isFloatingArrayLiteral(@NotNull ASTNumber number) {
        String literal = number.content().toLowerCase();
        if (literal.startsWith("0x") || literal.startsWith("-0x") || literal.startsWith("0b") || literal.startsWith("-0b")) {
            return false;
        }
        return number.isFloatingPoint();
    }

    private static long parseIntegralArrayLiteral(@NotNull ASTNumber number) {
        String literal = number.content().toLowerCase();
        boolean negative = literal.startsWith("-");
        if (negative) {
            literal = literal.substring(1);
        }

        int radix = 10;
        if (literal.startsWith("0x")) {
            radix = 16;
            literal = literal.substring(2);
        } else if (literal.startsWith("0b")) {
            radix = 2;
            literal = literal.substring(2);
        }

        if (literal.endsWith("l")) {
            literal = literal.substring(0, literal.length() - 1);
        }

        if (literal.isEmpty()) {
            throw new IllegalStateException("Expected integral literal");
        }

        if (negative) {
            return -Long.parseLong(literal, radix);
        }

        if (radix != 10) {
            return Long.parseUnsignedLong(literal, radix);
        }

        return Long.parseLong(literal, radix);
    }

    private static int inferArrayElementWidth(@NotNull ASTArray array) {
        int elementWidth = 1;
        for (ASTElement element : array.values()) {
            if (!(element instanceof ASTNumber number)) {
                throw new IllegalStateException("fill-array-data requires numeric literals");
            }

            if (isFloatingArrayLiteral(number)) {
                elementWidth = Math.max(elementWidth, number.isWide() ? Double.BYTES : Float.BYTES);
                continue;
            }

            long value = parseIntegralArrayLiteral(number);
            if (number.isWide()) {
                elementWidth = Math.max(elementWidth, Long.BYTES);
            } else if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                elementWidth = Math.max(elementWidth, Integer.BYTES);
            } else if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                elementWidth = Math.max(elementWidth, Short.BYTES);
            }
        }
        return elementWidth;
    }

    private static byte[] encodeArrayData(@NotNull ASTArray array, int elementWidth) {
        ByteBuffer buffer = ByteBuffer.allocate(array.values().size() * elementWidth).order(ByteOrder.LITTLE_ENDIAN);
        for (ASTElement element : array.values()) {
            ASTNumber number = (ASTNumber) element;
            if (isFloatingArrayLiteral(number)) {
                switch (elementWidth) {
                    case 4 -> buffer.putInt(Float.floatToRawIntBits(number.isWide() ? (float) number.asDouble() : number.asFloat()));
                    case 8 -> buffer.putLong(Double.doubleToRawLongBits(number.asDouble()));
                    default -> throw new IllegalStateException("Floating-point array data requires 4-byte or 8-byte elements");
                }
                continue;
            }

            long value = parseIntegralArrayLiteral(number);
            switch (elementWidth) {
                case 1 -> buffer.put((byte) value);
                case 2 -> buffer.putShort((short) value);
                case 4 -> buffer.putInt((int) value);
                case 8 -> buffer.putLong(value);
                default -> throw new IllegalStateException("Unsupported array element width: " + elementWidth);
            }
        }
        return buffer.array();
    }

    private static int parseSwitchKey(@NotNull String literal) {
        String normalized = literal.toLowerCase();
        boolean negative = normalized.startsWith("-");
        if (negative) {
            normalized = normalized.substring(1);
        }

        int radix = 10;
        if (normalized.startsWith("0x")) {
            radix = 16;
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("0b")) {
            radix = 2;
            normalized = normalized.substring(2);
        }

        if (normalized.isEmpty()) {
            throw new IllegalStateException("Expected integer switch key");
        }

        long parsed = radix == 10
                ? Long.parseLong(normalized, radix)
                : Long.parseUnsignedLong(normalized, radix);
        if (negative) {
            parsed = -parsed;
        }
        return (int) parsed;
    }

    private static @NotNull me.darknet.dex.tree.definitions.constant.Handle parseHandle(@NotNull ASTElement handle) {
        Constant constant = DalvikConstantMapper.fromConstant(handle);
        if (constant instanceof HandleConstant handleConstant) {
            return handleConstant.handle();
        }
        throw new IllegalStateException("Expected method handle constant");
    }

    private static @NotNull List<Constant> parseConstants(@NotNull ASTArray arguments) {
        List<Constant> constants = new ArrayList<>(arguments.values().size());
        for (ASTElement argument : arguments.values()) {
            constants.add(DalvikConstantMapper.fromConstant(argument));
        }
        return List.copyOf(constants);
    }

    @Override
    public void visitInstruction(@NotNull ASTInstruction instruction) {
        currentInstructionAst = instruction;
        // parse optional suffix
        currentInstructionKind = Kind.NORMAL;
        String content = instruction.content();
        int indexOfDash = content.lastIndexOf('-');
        if (indexOfDash == -1) {
            return;
        }

        String suffix = content.substring(indexOfDash + 1);
        switch (suffix) {
            case "object" -> currentInstructionKind = Kind.OBJECT;
            case "wide" -> currentInstructionKind = Kind.WIDE;
            case "result" -> currentInstructionKind = Kind.RESULT;
        }
    }

    @Override
    public void visitNop() {
        addInstruction(new NopInstruction());
    }

    @Override
    public void visitMove(ASTIdentifier to, ASTIdentifier from) {
        int toIndex = getRegisterIndex(to.literal());
        int fromIndex = getRegisterIndex(from.literal());
        addInstruction(switch (currentInstructionKind) {
            case OBJECT -> new MoveObjectInstruction(toIndex, fromIndex);
            case WIDE -> new MoveWideInstruction(toIndex, fromIndex);
            default -> new MoveInstruction(toIndex, fromIndex);
        });
    }

    @Override
    public void visitMoveResult(ASTIdentifier to) {
        int toIndex = getRegisterIndex(to.literal());
        addInstruction(switch (currentInstructionKind) {
            case OBJECT -> new MoveResultInstruction(Result.OBJECT, toIndex);
            case WIDE -> new MoveResultInstruction(Result.WIDE, toIndex);
            default -> new MoveResultInstruction(Result.NORMAL, toIndex);
        });
    }

    @Override
    public void visitMoveException(ASTIdentifier to) {
        int toIndex = getRegisterIndex(to.literal());
        addInstruction(new MoveExceptionInstruction(toIndex));
    }

    @Override
    public void visitReturn(ASTIdentifier returnValue) {
        int returnIndex = getRegisterIndex(returnValue.literal());
        addInstruction(switch (currentInstructionKind) {
            case OBJECT -> new ReturnInstruction(returnIndex, Return.OBJECT);
            case WIDE -> new ReturnInstruction(returnIndex, Return.WIDE);
            default -> new ReturnInstruction(returnIndex);
        });
    }

    @Override
    public void visitReturnVoid() {
        addInstruction(new ReturnInstruction());
    }

    @Override
    public void visitConst(ASTIdentifier to, ASTElement value) {
        int toIndex = getRegisterIndex(to.literal());
        String opcode = currentInstructionAst.identifier().content();
        switch (opcode) {
            case "const", "const-wide" -> {
                boolean wide = opcode.equals("const-wide");
                if (value instanceof ASTNumber constValue) {
                    if (wide) {
                        if (constValue.isFloatingPoint()) {
                            long longBits = constValue.isWide()
                                    ? Double.doubleToLongBits(constValue.asDouble())
                                    : Double.doubleToLongBits((double) constValue.asFloat());
                            addInstruction(new ConstWideInstruction(toIndex, longBits));
                        } else {
                            addInstruction(new ConstWideInstruction(toIndex, constValue.asLong()));
                        }
                    } else {
                        if (constValue.isFloatingPoint()) {
                            int intBits = constValue.isWide()
                                    ? Float.floatToIntBits((float) constValue.asDouble())
                                    : Float.floatToIntBits(constValue.asFloat());
                            addInstruction(new ConstInstruction(toIndex, intBits));
                        } else {
                            addInstruction(new ConstInstruction(toIndex, constValue.asInt()));
                        }
                    }
                } else if (value instanceof ASTIdentifier identifier) {
                    if (wide) {
                        addInstruction(new ConstWideInstruction(toIndex, parseSpecialWideNumber(identifier.literal())));
                    } else {
                        addInstruction(new ConstInstruction(toIndex, parseSpecialNumber(identifier.literal())));
                    }
                }
            }
            case "const-string" -> addInstruction(new ConstStringInstruction(toIndex, ((ASTString) value).content()));
            case "const-class" -> {
                if (!(value instanceof ASTIdentifier constValue)) {
                    throw new IllegalStateException("const-class requires a class descriptor");
                }
                addInstruction(new ConstTypeInstruction(toIndex, parseClassType(constValue)));
            }
            case "const-method-handle" -> {
                Constant constant = DalvikConstantMapper.fromConstant(value);
                if (!(constant instanceof HandleConstant handle)) {
                    throw new IllegalStateException("const-method-handle requires a method handle");
                }
                addInstruction(new ConstMethodHandleInstruction(toIndex, handle.handle()));
            }
            case "const-method-type" -> {
                if (!(value instanceof ASTIdentifier constValue)) {
                    throw new IllegalStateException("const-method-type requires a method descriptor");
                }
                addInstruction(new ConstMethodTypeInstruction(
                        toIndex,
                        Types.methodTypeFromDescriptor(constValue.literal())
                ));
            }
        }
    }

    @Override
    public void visitMonitorEnter(ASTIdentifier register) {
        addInstruction(new MonitorInstruction(getRegisterIndex(register.literal()), false));
    }

    @Override
    public void visitMonitorExit(ASTIdentifier register) {
        addInstruction(new MonitorInstruction(getRegisterIndex(register.literal()), true));
    }

    @Override
    public void visitCheckCast(ASTIdentifier register, ASTIdentifier type) {
        addInstruction(new CheckCastInstruction(getRegisterIndex(register.literal()), parseClassType(type)));
    }

    @Override
    public void visitInstanceOf(ASTIdentifier result, ASTIdentifier check, ASTIdentifier type) {
        addInstruction(new InstanceOfInstruction(
                getRegisterIndex(result.literal()),
                getRegisterIndex(check.literal()),
                parseClassType(type)
        ));
    }

    @Override
    public void visitArrayLength(ASTIdentifier result, ASTIdentifier array) {
        addInstruction(new ArrayLengthInstruction(
                getRegisterIndex(result.literal()),
                getRegisterIndex(array.literal())
        ));
    }

    @Override
    public void visitNewInstance(ASTIdentifier result, ASTIdentifier type) {
        ClassType classType = parseClassType(type);
        if (!(classType instanceof InstanceType instanceType)) {
            throw new IllegalStateException("new-instance requires an instance type");
        }
        addInstruction(new NewInstanceInstruction(getRegisterIndex(result.literal()), instanceType));
    }

    @Override
    public void visitNewArray(ASTIdentifier result, ASTIdentifier size, ASTIdentifier type) {
        addInstruction(new NewArrayInstruction(
                getRegisterIndex(result.literal()),
                getRegisterIndex(size.literal()),
                parseClassType(type)
        ));
    }

    @Override
    public void visitFilledNewArray(ASTArray args, ASTIdentifier type) {
        int[] registers = parseRegisters(args, getCurrentOpName().endsWith("/range"));
        ClassType arrayType = parseClassType(type);
        int registerCount = getCurrentOpName().endsWith("/range")
                ? registers[1] - registers[0] + 1
                : registers.length;
        updateOutRegisters(registerCount);
        if (getCurrentOpName().endsWith("/range")) {
            int first = registers[0];
            int last = registers[1];
            addInstruction(new FilledNewArrayInstruction(arrayType, last - first + 1, first));
            return;
        }
        addInstruction(new FilledNewArrayInstruction(arrayType, registers));
    }

    @Override
    public void visitFillArrayData(ASTIdentifier to, ASTElement data) {
        ArrayData arrayData = parseArrayData(data);
        addInstruction(new FillArrayDataInstruction(
                getRegisterIndex(to.literal()),
                encodeArrayData(arrayData.values(), arrayData.elementWidth()),
                arrayData.elementWidth()
        ));
    }

    private static ArrayData parseArrayData(ASTElement data) {
        if (data instanceof ASTArray array) {
            return new ArrayData(array, inferArrayElementWidth(array));
        }
        if (!(data instanceof ASTObject payload)) {
            throw new IllegalStateException("fill-array-data requires an array or payload object");
        }

        ASTElement widthElement = payload.value("width");
        ASTElement valuesElement = payload.value("values");
        if (!(widthElement instanceof ASTNumber width) || width.isFloatingPoint()) {
            throw new IllegalStateException("fill-array-data width must be an integer");
        }
        if (!(valuesElement instanceof ASTArray values)) {
            throw new IllegalStateException("fill-array-data values must be an array");
        }

        int elementWidth = width.asInt();
        if (elementWidth != 1 && elementWidth != 2 && elementWidth != 4 && elementWidth != 8) {
            throw new IllegalStateException("fill-array-data width must be 1, 2, 4, or 8");
        }
        return new ArrayData(values, elementWidth);
    }

    @Override
    public void visitThrow(ASTIdentifier exception) {
        addInstruction(new ThrowInstruction(getRegisterIndex(exception.literal())));
    }

    @Override
    public void visitGoto(ASTIdentifier label) {
        addInstruction(new GotoInstruction(getLabel(label.literal())));
    }

    @Override
    public void visitPackedSwitch(ASTIdentifier register, ASTObject packedSwitchObject) {
        ASTNumber first = packedSwitchObject.value("first");
        ASTArray targets = packedSwitchObject.value("targets");
        if (first == null || targets == null) {
            throw new IllegalStateException("packed-switch requires first and targets entries");
        }

        List<Label> targetLabels = new ArrayList<>(targets.values().size());
        for (ASTElement target : targets.values()) {
            if (!(target instanceof ASTIdentifier identifier)) {
                throw new IllegalStateException("packed-switch targets must be labels");
            }
            targetLabels.add(getLabel(identifier.literal()));
        }

        addInstruction(new PackedSwitchInstruction(
                getRegisterIndex(register.literal()),
                parseSwitchKey(first.content()),
                List.copyOf(targetLabels)
        ));
    }

    @Override
    public void visitSparseSwitch(ASTIdentifier register, ASTObject sparseSwitchObject) {
        Map<Integer, Label> targets = new LinkedHashMap<>();
        for (var pair : sparseSwitchObject.values().pairs()) {
            ASTElement target = pair.second();
            if (!(target instanceof ASTIdentifier identifier)) {
                throw new IllegalStateException("sparse-switch targets must be labels");
            }
            targets.put(parseSwitchKey(pair.first().literal()), getLabel(identifier.literal()));
        }

        addInstruction(new SparseSwitchInstruction(
                getRegisterIndex(register.literal()),
                Map.copyOf(targets)
        ));
    }

    @Override
    public void visitCmp(ASTIdentifier to, ASTIdentifier from1, ASTIdentifier from2) {
        addInstruction(new CompareInstruction(
                switch (getCurrentOpName()) {
                    case "cmpl-float" -> CMPL_FLOAT;
                    case "cmpg-float" -> CMPG_FLOAT;
                    case "cmpl-double" -> CMPL_DOUBLE;
                    case "cmpg-double" -> CMPG_DOUBLE;
                    case "cmp-long" -> CMP_LONG;
                    default -> throw new IllegalStateException("Unsupported compare opcode: " + getCurrentOpName());
                },
                getRegisterIndex(to.literal()),
                getRegisterIndex(from1.literal()),
                getRegisterIndex(from2.literal())
        ));
    }

    @Override
    public void visitBinaryOperation(ASTIdentifier to, ASTIdentifier from1, ASTIdentifier from2) {
        addInstruction(new BinaryInstruction(
                binaryOpcode(getCurrentOpName()),
                getRegisterIndex(to.literal()),
                getRegisterIndex(from1.literal()),
                getRegisterIndex(from2.literal())
        ));
    }

    @Override
    public void visitBinary2AddrOperation(ASTIdentifier a, ASTIdentifier b) {
        addInstruction(new Binary2AddrInstruction(
                binary2AddrOpcode(getCurrentOpName()),
                getRegisterIndex(a.literal()),
                getRegisterIndex(b.literal())
        ));
    }

    @Override
    public void visitBinaryLiteralOperation(ASTIdentifier to, ASTIdentifier from, ASTNumber constant) {
        String opcode = getCurrentOpName();
        long value = constant.isFloatingPoint() ? Long.MIN_VALUE : constant.asLong();
        boolean wideLiteral = opcode.endsWith("/lit16") || opcode.equals("rsub-int");
        long minimum = wideLiteral ? Short.MIN_VALUE : Byte.MIN_VALUE;
        long maximum = wideLiteral ? Short.MAX_VALUE : Byte.MAX_VALUE;
        if (value < minimum || value > maximum) {
            throw new IllegalStateException(opcode + " literal is outside its signed range");
        }

        addInstruction(new BinaryLiteralInstruction(
                binaryLiteralOpcode(opcode),
                getRegisterIndex(to.literal()),
                getRegisterIndex(from.literal()),
                (int) value
        ));
    }

    private static int binaryOpcode(String opcode) {
        return switch (opcode) {
            case "add-int" -> ADD_INT;
            case "sub-int" -> SUB_INT;
            case "mul-int" -> MUL_INT;
            case "div-int" -> DIV_INT;
            case "rem-int" -> REM_INT;
            case "and-int" -> AND_INT;
            case "or-int" -> OR_INT;
            case "xor-int" -> XOR_INT;
            case "shl-int" -> SHL_INT;
            case "shr-int" -> SHR_INT;
            case "ushr-int" -> USHR_INT;
            case "add-long" -> ADD_LONG;
            case "sub-long" -> SUB_LONG;
            case "mul-long" -> MUL_LONG;
            case "div-long" -> DIV_LONG;
            case "rem-long" -> REM_LONG;
            case "and-long" -> AND_LONG;
            case "or-long" -> OR_LONG;
            case "xor-long" -> XOR_LONG;
            case "shl-long" -> SHL_LONG;
            case "shr-long" -> SHR_LONG;
            case "ushr-long" -> USHR_LONG;
            case "add-float" -> ADD_FLOAT;
            case "sub-float" -> SUB_FLOAT;
            case "mul-float" -> MUL_FLOAT;
            case "div-float" -> DIV_FLOAT;
            case "rem-float" -> REM_FLOAT;
            case "add-double" -> ADD_DOUBLE;
            case "sub-double" -> SUB_DOUBLE;
            case "mul-double" -> MUL_DOUBLE;
            case "div-double" -> DIV_DOUBLE;
            case "rem-double" -> REM_DOUBLE;
            default -> throw new IllegalStateException("Unsupported binary opcode: " + opcode);
        };
    }

    private static int binary2AddrOpcode(String opcode) {
        return switch (opcode) {
            case "add-int/2addr" -> ADD_INT_2ADDR;
            case "sub-int/2addr" -> SUB_INT_2ADDR;
            case "mul-int/2addr" -> MUL_INT_2ADDR;
            case "div-int/2addr" -> DIV_INT_2ADDR;
            case "rem-int/2addr" -> REM_INT_2ADDR;
            case "and-int/2addr" -> AND_INT_2ADDR;
            case "or-int/2addr" -> OR_INT_2ADDR;
            case "xor-int/2addr" -> XOR_INT_2ADDR;
            case "shl-int/2addr" -> SHL_INT_2ADDR;
            case "shr-int/2addr" -> SHR_INT_2ADDR;
            case "ushr-int/2addr" -> USHR_INT_2ADDR;
            case "add-long/2addr" -> ADD_LONG_2ADDR;
            case "sub-long/2addr" -> SUB_LONG_2ADDR;
            case "mul-long/2addr" -> MUL_LONG_2ADDR;
            case "div-long/2addr" -> DIV_LONG_2ADDR;
            case "rem-long/2addr" -> REM_LONG_2ADDR;
            case "and-long/2addr" -> AND_LONG_2ADDR;
            case "or-long/2addr" -> OR_LONG_2ADDR;
            case "xor-long/2addr" -> XOR_LONG_2ADDR;
            case "shl-long/2addr" -> SHL_LONG_2ADDR;
            case "shr-long/2addr" -> SHR_LONG_2ADDR;
            case "ushr-long/2addr" -> USHR_LONG_2ADDR;
            case "add-float/2addr" -> ADD_FLOAT_2ADDR;
            case "sub-float/2addr" -> SUB_FLOAT_2ADDR;
            case "mul-float/2addr" -> MUL_FLOAT_2ADDR;
            case "div-float/2addr" -> DIV_FLOAT_2ADDR;
            case "rem-float/2addr" -> REM_FLOAT_2ADDR;
            case "add-double/2addr" -> ADD_DOUBLE_2ADDR;
            case "sub-double/2addr" -> SUB_DOUBLE_2ADDR;
            case "mul-double/2addr" -> MUL_DOUBLE_2ADDR;
            case "div-double/2addr" -> DIV_DOUBLE_2ADDR;
            case "rem-double/2addr" -> REM_DOUBLE_2ADDR;
            default -> throw new IllegalStateException("Unsupported two-address opcode: " + opcode);
        };
    }

    private static int binaryLiteralOpcode(String opcode) {
        return switch (opcode) {
            case "add-int/lit16" -> ADD_INT_LIT16;
            case "rsub-int" -> RSUB_INT;
            case "mul-int/lit16" -> MUL_INT_LIT16;
            case "div-int/lit16" -> DIV_INT_LIT16;
            case "rem-int/lit16" -> REM_INT_LIT16;
            case "and-int/lit16" -> AND_INT_LIT16;
            case "or-int/lit16" -> OR_INT_LIT16;
            case "xor-int/lit16" -> XOR_INT_LIT16;
            case "add-int/lit8" -> ADD_INT_LIT8;
            case "rsub-int/lit8" -> RSUB_INT_LIT8;
            case "mul-int/lit8" -> MUL_INT_LIT8;
            case "div-int/lit8" -> DIV_INT_LIT8;
            case "rem-int/lit8" -> REM_INT_LIT8;
            case "and-int/lit8" -> AND_INT_LIT8;
            case "or-int/lit8" -> OR_INT_LIT8;
            case "xor-int/lit8" -> XOR_INT_LIT8;
            case "shl-int/lit8" -> SHL_INT_LIT8;
            case "shr-int/lit8" -> SHR_INT_LIT8;
            case "ushr-int/lit8" -> USHR_INT_LIT8;
            default -> throw new IllegalStateException("Unsupported literal opcode: " + opcode);
        };
    }

    @Override
    public void visitIf(ASTIdentifier a, ASTIdentifier b, ASTIdentifier label) {
        int opcode = switch (getCurrentOpName()) {
            case "if-eq" -> IF_EQ;
            case "if-ne" -> IF_NE;
            case "if-lt" -> IF_LT;
            case "if-ge" -> IF_GE;
            case "if-gt" -> IF_GT;
            case "if-le" -> IF_LE;
            default -> throw new IllegalStateException("Unsupported branch opcode: " + getCurrentOpName());
        };
        addInstruction(new BranchInstruction(
                opcode - IF_EQ,
                getRegisterIndex(a.literal()),
                getRegisterIndex(b.literal()),
                getLabel(label.literal())
        ));
    }

    @Override
    public void visitIfZero(ASTIdentifier a, ASTIdentifier label) {
        int opcode = switch (getCurrentOpName()) {
            case "if-eqz" -> IF_EQZ;
            case "if-nez" -> IF_NEZ;
            case "if-ltz" -> IF_LTZ;
            case "if-gez" -> IF_GEZ;
            case "if-gtz" -> IF_GTZ;
            case "if-lez" -> IF_LEZ;
            default -> throw new IllegalStateException("Unsupported zero-branch opcode: " + getCurrentOpName());
        };
        addInstruction(new BranchZeroInstruction(
                opcode - IF_EQZ,
                getRegisterIndex(a.literal()),
                getLabel(label.literal())
        ));
    }

    @Override
    public void visitArrayOperation(ASTIdentifier array, ASTIdentifier index, ASTIdentifier value) {
        addInstruction(new ArrayInstruction(
                switch (getCurrentOpName()) {
                    case "aget" -> AGET - AGET;
                    case "aget-wide" -> AGET_WIDE - AGET;
                    case "aget-object" -> AGET_OBJECT - AGET;
                    case "aget-boolean" -> AGET_BOOLEAN - AGET;
                    case "aget-byte" -> AGET_BYTE - AGET;
                    case "aget-char" -> AGET_CHAR - AGET;
                    case "aget-short" -> AGET_SHORT - AGET;
                    case "aput" -> APUT - AGET;
                    case "aput-wide" -> APUT_WIDE - AGET;
                    case "aput-object" -> APUT_OBJECT - AGET;
                    case "aput-boolean" -> APUT_BOOLEAN - AGET;
                    case "aput-byte" -> APUT_BYTE - AGET;
                    case "aput-char" -> APUT_CHAR - AGET;
                    case "aput-short" -> APUT_SHORT - AGET;
                    default -> throw new IllegalStateException("Unsupported array opcode: " + getCurrentOpName());
                },
                getRegisterIndex(array.literal()),
                getRegisterIndex(index.literal()),
                getRegisterIndex(value.literal())
        ));
    }

    @Override
    public void visitVirtualFieldOperation(ASTIdentifier value, ASTIdentifier instance, ASTIdentifier path, ASTIdentifier descriptor) {
        MemberPath member = parseMemberPath(path);
        addInstruction(new InstanceFieldInstruction(
                switch (getCurrentOpName()) {
                    case "iget" -> IGET - IGET;
                    case "iget-wide" -> IGET_WIDE - IGET;
                    case "iget-object" -> IGET_OBJECT - IGET;
                    case "iget-boolean" -> IGET_BOOLEAN - IGET;
                    case "iget-byte" -> IGET_BYTE - IGET;
                    case "iget-char" -> IGET_CHAR - IGET;
                    case "iget-short" -> IGET_SHORT - IGET;
                    case "iput" -> IPUT - IGET;
                    case "iput-wide" -> IPUT_WIDE - IGET;
                    case "iput-object" -> IPUT_OBJECT - IGET;
                    case "iput-boolean" -> IPUT_BOOLEAN - IGET;
                    case "iput-byte" -> IPUT_BYTE - IGET;
                    case "iput-char" -> IPUT_CHAR - IGET;
                    case "iput-short" -> IPUT_SHORT - IGET;
                    default -> throw new IllegalStateException("Unsupported instance field opcode: " + getCurrentOpName());
                },
                getRegisterIndex(value.literal()),
                getRegisterIndex(instance.literal()),
                Types.instanceTypeFromInternalName(member.owner()),
                member.name(),
                new TypeParser(descriptor.literal()).requireClassType()
        ));
    }

    @Override
    public void visitStaticFieldOperation(ASTIdentifier value, ASTIdentifier path, ASTIdentifier descriptor) {
        MemberPath member = parseMemberPath(path);
        addInstruction(new StaticFieldInstruction(
                switch (getCurrentOpName()) {
                    case "sget" -> SGET - SGET;
                    case "sget-wide" -> SGET_WIDE - SGET;
                    case "sget-object" -> SGET_OBJECT - SGET;
                    case "sget-boolean" -> SGET_BOOLEAN - SGET;
                    case "sget-byte" -> SGET_BYTE - SGET;
                    case "sget-char" -> SGET_CHAR - SGET;
                    case "sget-short" -> SGET_SHORT - SGET;
                    case "sput" -> SPUT - SGET;
                    case "sput-wide" -> SPUT_WIDE - SGET;
                    case "sput-object" -> SPUT_OBJECT - SGET;
                    case "sput-boolean" -> SPUT_BOOLEAN - SGET;
                    case "sput-byte" -> SPUT_BYTE - SGET;
                    case "sput-char" -> SPUT_CHAR - SGET;
                    case "sput-short" -> SPUT_SHORT - SGET;
                    default -> throw new IllegalStateException("Unsupported static field opcode: " + getCurrentOpName());
                },
                getRegisterIndex(value.literal()),
                Types.instanceTypeFromInternalName(member.owner()),
                member.name(),
                new TypeParser(descriptor.literal()).requireClassType()
        ));
    }

    @Override
    public void visitInvoke(ASTArray registers, ASTIdentifier method, ASTIdentifier descriptor) {
        MemberPath member = parseMemberPath(method);
        int opcode = switch (getCurrentOpName()) {
            case "invoke-virtual" -> INVOKE_VIRTUAL;
            case "invoke-super" -> INVOKE_SUPER;
            case "invoke-direct" -> INVOKE_DIRECT;
            case "invoke-static" -> INVOKE_STATIC;
            case "invoke-interface" -> INVOKE_INTERFACE;
            case "invoke-virtual/range" -> INVOKE_VIRTUAL_RANGE;
            case "invoke-super/range" -> INVOKE_SUPER_RANGE;
            case "invoke-direct/range" -> INVOKE_DIRECT_RANGE;
            case "invoke-static/range" -> INVOKE_STATIC_RANGE;
            case "invoke-interface/range" -> INVOKE_INTERFACE_RANGE;
            default -> throw new IllegalStateException("Unsupported invoke opcode: " + getCurrentOpName());
        };
        boolean range = getCurrentOpName().endsWith("/range");
        int[] registerValues = parseRegisters(registers, range);
        ReferenceType owner = parseReferenceType(member.owner());
        MethodType methodType = parseMethodType(descriptor);
        updateOutRegisters(argumentWordCount(registerValues, range));
        if (range) {
            int first = registerValues[0];
            int last = registerValues[1];
            addInstruction(InvokeInstruction.range(opcode, owner, member.name(), methodType, last - first + 1, first));
        } else {
            addInstruction(new InvokeInstruction(opcode, owner, member.name(), methodType, registerValues));
        }
    }

    @Override
    public void visitInvokeCustom(ASTArray registers, ASTIdentifier name, ASTIdentifier type, ASTElement handle, ASTArray arguments) {
        boolean range = getCurrentOpName().endsWith("/range");
        int[] registerValues = parseRegisters(registers, range);
        var bootstrapHandle = parseHandle(handle);
        MethodType methodType = parseMethodType(type);
        List<Constant> bootstrapArguments = parseConstants(arguments);
        updateOutRegisters(argumentWordCount(registerValues, range));
        if (range) {
            int first = registerValues[0];
            int last = registerValues[1];
            addInstruction(new InvokeCustomInstruction(
                    bootstrapHandle,
                    name.literal(),
                    methodType,
                    bootstrapArguments,
                    last - first + 1,
                    first
            ));
            return;
        }
        addInstruction(new InvokeCustomInstruction(
                bootstrapHandle,
                name.literal(),
                methodType,
                bootstrapArguments,
                registerValues
        ));
    }

    @Override
    public void visitInvokePolymorphic(ASTArray registers, ASTIdentifier method, ASTIdentifier descriptor, ASTIdentifier proto) {
        throw new IllegalStateException("invoke-polymorphic is not supported by the current dex-core backend");
    }

    @Override
    public void visitUnaryOperation(ASTIdentifier to, ASTIdentifier from) {
        addInstruction(new UnaryInstruction(
                switch (getCurrentOpName()) {
                    case "neg-int" -> NEG_INT;
                    case "not-int" -> NOT_INT;
                    case "neg-long" -> NEG_LONG;
                    case "not-long" -> NOT_LONG;
                    case "neg-float" -> NEG_FLOAT;
                    case "neg-double" -> NEG_DOUBLE;
                    case "int-to-long" -> INT_TO_LONG;
                    case "int-to-float" -> INT_TO_FLOAT;
                    case "int-to-double" -> INT_TO_DOUBLE;
                    case "long-to-int" -> LONG_TO_INT;
                    case "long-to-float" -> LONG_TO_FLOAT;
                    case "long-to-double" -> LONG_TO_DOUBLE;
                    case "float-to-int" -> FLOAT_TO_INT;
                    case "float-to-long" -> FLOAT_TO_LONG;
                    case "float-to-double" -> FLOAT_TO_DOUBLE;
                    case "double-to-int" -> DOUBLE_TO_INT;
                    case "double-to-long" -> DOUBLE_TO_LONG;
                    case "double-to-float" -> DOUBLE_TO_FLOAT;
                    case "int-to-byte" -> INT_TO_BYTE;
                    case "int-to-char" -> INT_TO_CHAR;
                    case "int-to-short" -> INT_TO_SHORT;
                    default -> throw new IllegalStateException("Unsupported unary opcode: " + getCurrentOpName());
                },
                getRegisterIndex(from.literal()),
                getRegisterIndex(to.literal())
        ));
    }

    @Override
    public void visitLabel(@NotNull ASTIdentifier label) {
        Label target = getLabel(label.literal());
        if (pendingLineNumber != Label.UNASSIGNED && target.lineNumber() == Label.UNASSIGNED) {
            target.lineNumber(pendingLineNumber);
            pendingLineNumber = Label.UNASSIGNED;
        }
        codeBuilder.add(target);
    }

    @Override
    public void visitLineNumber(@NotNull ASTNumber line) {
        pendingLineNumber = line.asInt();
    }

    @Override
    public void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end, @NotNull ASTIdentifier handler, @NotNull ASTIdentifier type) {
        Label begin = getLabel(start.literal());
        Label finish = getLabel(end.literal());
        Handler parsedHandler = new Handler(
                getLabel(handler.literal()),
                "*".equals(type.literal()) ? null : parseInstanceType(type)
        );
        for (int i = 0; i < tryCatches.size(); i++) {
            TryCatch existing = tryCatches.get(i);
            if (existing.begin() != begin || existing.end() != finish) {
                continue;
            }
            List<Handler> handlers = new ArrayList<>(existing.handlers());
            handlers.add(parsedHandler);
            tryCatches.set(i, new TryCatch(begin, finish, List.copyOf(handlers)));
            return;
        }
        tryCatches.add(new TryCatch(begin, finish, List.of(parsedHandler)));
    }

    @Override
    public void visitEnd() {
        if (pendingLineNumber != Label.UNASSIGNED) {
            Label label = new Label();
            label.lineNumber(pendingLineNumber);
            codeBuilder.add(label);
            pendingLineNumber = Label.UNASSIGNED;
        }
    }

    private record ArrayData(ASTArray values, int elementWidth) {}

    private record MemberPath(String owner, String name) {}
}

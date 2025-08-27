package me.darknet.assembler.printer;

import dev.xdark.blw.code.*;
import me.darknet.assembler.compile.analysis.jvm.IndexedExecutionEngine;
import me.darknet.assembler.helper.Variables;

import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.type.*;
import me.darknet.assembler.util.VarNaming;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class InstructionPrinter implements IndexedExecutionEngine {
    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u[0-9a-fA-F]{4}");
    private final static String[] OPCODES = new String[256];

    static {
        for (var field : JavaOpcodes.class.getFields()) {
            try {
                OPCODES[field.getInt(null)] = field.getName().toLowerCase();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    protected PrintContext.CodePrint ctx;
    protected Code code;
    protected Map<Integer, String> labelNames;
    protected Variables variables;
    private int currentIndex = 0;

    public InstructionPrinter(PrintContext.CodePrint ctx, Code code, Variables variables, Map<Integer, String> labelNames) {
        this.ctx = ctx;
        this.code = code;
        this.variables = variables;
        this.labelNames = labelNames;
    }

    @Override
    public void index(int index) {
        currentIndex = index;
    }

    @Override
    public void label(Label label) {
        String name = labelNames.get(label.getIndex());
        ctx.label(name).next();
        if (ctx.debugTryCatchRanges) {
            for (TryCatchBlock block : code.tryCatchBlocks()) {
                Label start = block.start();
                Label end = block.end();
                Label handler = block.handler();
                InstanceType type = block.type();
                String typeName = type == null ? "*" : type.internalName();
                String range = "range=[" + labelNames.get(start.getIndex()) + "-" + labelNames.get(end.getIndex()) + "]";
                String endName = labelNames.get(handler.getIndex());
                if (label == start) {
                    ctx.instruction("// try-start:   " + range + " handler=" + endName + ":" + typeName).next();
                }
                if (label == end) {
                    ctx.instruction("// try-end:     " + range + " handler=" + endName + ":" + typeName).next();
                }
                if (label == handler) {
                    ctx.instruction("// try-handler: " + range + " handler=" + endName + ":" + typeName).next();
                }
            }
        }
        if (label.getLineNumber() != Label.UNSET) {
            ctx.instruction("line").print(Integer.toString(label.getLineNumber())).next();
        }
    }

    @Override
    public void execute(SimpleInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()]).next();
    }

    @Override
    public void execute(ConstantInstruction<?> instruction) {
        String opcode;
        switch (instruction) {
            case ConstantInstruction.Int i -> {
                int val = i.constant().value();
                if (val == -1) {
                    ctx.instruction("iconst_m1").next();
                    return;
                } else if (val >= 0 && val <= 5) {
                    ctx.instruction("iconst_" + val).next();
                    return;
                } else if (val >= -128 && val <= 127) {
                    opcode = "bipush";
                } else if (val >= -32768 && val <= 32767) {
                    opcode = "sipush";
                } else {
                    opcode = "ldc";
                }
            }
            case ConstantInstruction.Long i -> {
                long val = i.constant().value();
                if (val == 0 || val == 1) {
                    ctx.instruction("lconst_" + val).next();
                    return;
                } else {
                    opcode = "ldc"; // ldc2_w
                }
            }
            case ConstantInstruction.Float i -> {
                float val = i.constant().value();
                if (val == 0 || val == 1 || val == 2) {
                    ctx.instruction("fconst_" + (int) val).next();
                    return;
                } else {
                    opcode = "ldc";
                }
            }
            case ConstantInstruction.Double i -> {
                double val = i.constant().value();
                if (val == 0 || val == 1) {
                    ctx.instruction("dconst_" + (int) val).next();
                    return;
                } else {
                    opcode = "ldc"; // ldc2_w
                }
            }
            case null, default -> opcode = "ldc";
        }
        ctx.instruction(opcode);
        instruction.constant().accept(new ConstantPrinter(ctx));
        ctx.next();
    }

    @Override
    public void execute(VarInstruction instruction) {
        int opcode = instruction.opcode();
        int index = instruction.variableIndex();
        String varName = computeName(opcode, index, currentIndex + 1);

        ctx.instruction(OPCODES[opcode]);

        // If it has already been escaped (\\uXXXX), print the escape as-is.
        // We do not need to escape the variable name twice.
        if (varName.charAt(0) == '\\' && UNICODE_ESCAPE.matcher(varName).matches())
            ctx.print(varName);
        else
            ctx.literal(varName);

        ctx.next();
    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {
        var obj = ctx.instruction("lookupswitch").object();
        // Java has no zip function
        int[] keys = instruction.keys();
        List<Label> targets = instruction.targets();
        for (int i = 0; i < keys.length; i++) {
            printLookupCase(obj, keys[i], targets.get(i));
            obj.next();
        }
        obj.value("default").print(labelNames.get(instruction.defaultTarget().getIndex()));
        obj.end();
        ctx.next();
    }

    private void printLookupCase(PrintContext.ObjectPrint ctx, int key, Label target) {
        ctx.value(String.valueOf(key)).print(labelNames.get(target.getIndex()));
    }

    @Override
    public void execute(TableSwitchInstruction instruction) {
        var obj = ctx.instruction("tableswitch").object();
        obj.value("min").print(String.valueOf(instruction.min())).next();
        obj.value("max").print(String.valueOf(instruction.min() + instruction.targets().size())).next();
        var arr = obj.value("cases").array();
        List<Label> targets = instruction.targets();
        arr.print(targets, (print, lbl) -> print.print(labelNames.get(lbl.getIndex())));
        arr.end();
        obj.next();
        obj.value("default").print(labelNames.get(instruction.defaultTarget().getIndex())).end();
        ctx.next();
    }

    @Override
    public void execute(InstanceofInstruction instruction) {
        ctx.instruction("instanceof").literal(instruction.type().internalName()).next();
    }

    @Override
    public void execute(CheckCastInstruction instruction) {
        ctx.instruction("checkcast").literal(instruction.type().internalName()).next();
    }

    @Override
    public void execute(AllocateInstruction instruction) {
        Type type = instruction.type();
        if (type instanceof InstanceType instance) {
            ctx.instruction("new").literal(instance.internalName()).next();
        } else {
            ArrayType arrayType = (ArrayType) type;
            ClassType component = arrayType.componentType();
            if (component instanceof ObjectType objectComponent) {
                String typeName = objectComponent.internalName();
                ctx.instruction("anewarray").literal(typeName).next();
            } else if (component instanceof PrimitiveType primitiveComponent) {
                ctx.instruction("newarray").print(primitiveComponent.name()).next();
            }
        }
    }

    @Override
    public void execute(AllocateMultiDimArrayInstruction instruction) {
        String descriptor = instruction.type().descriptor();
        int dimensions = instruction.dimensions();
        ctx.instruction("multianewarray").literal(descriptor).arg().print(Integer.toString(dimensions)).next();
    }

    @Override
    public void execute(MethodInstruction instruction) {
        String opcode = OPCODES[instruction.opcode()];
        if (instruction.isInterface() && instruction.opcode() != JavaOpcodes.INVOKEINTERFACE) {
            opcode += "interface";
        }
        ctx.instruction(opcode).literal(instruction.owner().internalName()).print(".").literal(instruction.name())
                .print(" ").literal(instruction.type().descriptor()).next();
    }

    @Override
    public void execute(FieldInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()]).literal(instruction.owner().internalName()).print(".")
                .literal(instruction.name()).print(" ").literal(instruction.type().descriptor()).next();
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {
        ctx.instruction("invokedynamic").literal(instruction.name()).arg().literal(instruction.type().descriptor())
                .arg();
        ConstantPrinter.printMethodHandle(instruction.bootstrapHandle(), ctx);
        var bsmArray = ctx.arg().array();
        ConstantPrinter printer = new ConstantPrinter(bsmArray);
        bsmArray.print(instruction.args(), (__, cst) -> cst.accept(printer));
        bsmArray.end();
        ctx.next();
    }

    @Override
    public void execute(ImmediateJumpInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()]).print(labelNames.get(instruction.target().getIndex())).next();
    }

    @Override
    public void execute(ConditionalJumpInstruction instruction) {
        ctx.instruction(OPCODES[instruction.opcode()]).print(labelNames.get(instruction.target().getIndex())).next();
    }

    @Override
    public void execute(VariableIncrementInstruction instruction) {
        String variableName = computeName(JavaOpcodes.IINC, instruction.variableIndex(), currentIndex + 1);
        ctx.instruction(OPCODES[instruction.opcode()])
                .literal(variableName).arg()
                .literal(instruction.incrementBy()).next();
    }

    @Override
    public void execute(PrimitiveConversionInstruction primitiveConversionInstruction) {
        primitiveConversionInstruction.accept(new PrimitiveConversion() {
            @Override
            public void i2l() {
                ctx.instruction("i2l");
            }

            @Override
            public void i2f() {
                ctx.instruction("i2f");
            }

            @Override
            public void i2d() {
                ctx.instruction("i2d");
            }

            @Override
            public void l2i() {
                ctx.instruction("l2i");
            }

            @Override
            public void l2f() {
                ctx.instruction("l2f");
            }

            @Override
            public void l2d() {
                ctx.instruction("l2d");
            }

            @Override
            public void f2i() {
                ctx.instruction("f2i");
            }

            @Override
            public void f2l() {
                ctx.instruction("f2l");
            }

            @Override
            public void f2d() {
                ctx.instruction("f2d");
            }

            @Override
            public void d2i() {
                ctx.instruction("d2i");
            }

            @Override
            public void d2l() {
                ctx.instruction("d2l");
            }

            @Override
            public void d2f() {
                ctx.instruction("d2f");
            }

            @Override
            public void i2b() {
                ctx.instruction("i2b");
            }

            @Override
            public void i2c() {
                ctx.instruction("i2c");
            }

            @Override
            public void i2s() {
                ctx.instruction("i2s");
            }
        });
        ctx.next();
    }

    @Override
    public void execute(Instruction instruction) {

    }

    private @NotNull String computeName(int opcode, int variableIndex, int codeOffset) {
        ClassType assumedType = switch (opcode) {
            case JavaOpcodes.ALOAD, JavaOpcodes.ASTORE -> Types.OBJECT;
            case JavaOpcodes.FLOAD, JavaOpcodes.FSTORE -> Types.FLOAT;
            case JavaOpcodes.DLOAD, JavaOpcodes.DSTORE -> Types.DOUBLE;
            case JavaOpcodes.LLOAD, JavaOpcodes.LSTORE -> Types.LONG;
            case JavaOpcodes.ILOAD, JavaOpcodes.ISTORE, JavaOpcodes.IINC, JavaOpcodes.RET -> Types.INT;
            default -> Types.VOID; // Should never happen
        };

        var local = variables.get(variableIndex, codeOffset, assumedType.descriptor());
        if (local != null &&
                // Both must be non-primitives, or both primitives of the same type
                ((!local.isPrimitive() && !(assumedType instanceof PrimitiveType))
                || Variables.compatibleDescriptors(assumedType.descriptor(), local.descriptor())))
            return local.name();

        return VarNaming.name(variableIndex, assumedType);
    }

}

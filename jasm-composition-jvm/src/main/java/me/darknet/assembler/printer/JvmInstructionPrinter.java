package me.darknet.assembler.printer;

import me.darknet.assembler.helper.Variables;
import me.darknet.assembler.util.VarNaming;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class JvmInstructionPrinter {
    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u[0-9a-fA-F]{4}");

    protected final PrintContext.CodePrint ctx;
    protected final List<TryCatchBlockNode> tryCatchBlocks;
    protected final Map<LabelNode, String> labelNames;
    protected final Variables variables;
    private int currentIndex = 0;

    public JvmInstructionPrinter(PrintContext.CodePrint ctx,
                                 List<TryCatchBlockNode> tryCatchBlocks,
                                 Variables variables,
                                 Map<LabelNode, String> labelNames) {
        this.ctx = ctx;
        this.tryCatchBlocks = tryCatchBlocks;
        this.variables = variables;
        this.labelNames = labelNames;
    }

    public void index(int index) {
        currentIndex = index;
    }

    public void label(LabelNode label) {
        String name = labelNames.get(label);
        if (name == null) {
            return;
        }
        ctx.label(name).next();
        if (ctx.debugTryCatchRanges) {
            for (TryCatchBlockNode block : tryCatchBlocks) {
                String typeName = block.type == null ? "*" : block.type;
                String range = "range=[" + labelNames.get(block.start) + "-" + labelNames.get(block.end) + "]";
                String handlerName = labelNames.get(block.handler);
                if (label == block.start) {
                    ctx.instruction("// try-start:   " + range + " handler=" + handlerName + ":" + typeName).next();
                }
                if (label == block.end) {
                    ctx.instruction("// try-end:     " + range + " handler=" + handlerName + ":" + typeName).next();
                }
                if (label == block.handler) {
                    ctx.instruction("// try-handler: " + range + " handler=" + handlerName + ":" + typeName).next();
                }
            }
        }
    }

    public void execute(AbstractInsnNode instruction) {
        switch (instruction) {
            case LabelNode label -> label(label);
            case LineNumberNode lineNumber -> execute(lineNumber);
            case FrameNode ignored -> { }
            case InsnNode insn -> execute(insn);
            case IntInsnNode intInsn -> execute(intInsn);
            case LdcInsnNode ldcInsn -> execute(ldcInsn);
            case VarInsnNode varInsn -> execute(varInsn);
            case IincInsnNode iincInsn -> execute(iincInsn);
            case JumpInsnNode jumpInsn -> execute(jumpInsn);
            case TypeInsnNode typeInsn -> execute(typeInsn);
            case FieldInsnNode fieldInsn -> execute(fieldInsn);
            case MethodInsnNode methodInsn -> execute(methodInsn);
            case InvokeDynamicInsnNode invokeDynamicInsn -> execute(invokeDynamicInsn);
            case LookupSwitchInsnNode lookupSwitchInsn -> execute(lookupSwitchInsn);
            case TableSwitchInsnNode tableSwitchInsn -> execute(tableSwitchInsn);
            case MultiANewArrayInsnNode multiANewArrayInsn -> execute(multiANewArrayInsn);
            default -> throw new IllegalStateException("Unhandled instruction node: " + instruction.getClass().getName());
        }
    }

    public void execute(LineNumberNode instruction) {
        ctx.instruction("line").print(Integer.toString(instruction.line)).next();
    }

    public void execute(InsnNode instruction) {
        ctx.instruction(opcodeName(instruction.getOpcode())).next();
    }

    public void execute(IntInsnNode instruction) {
        if (instruction.getOpcode() == Opcodes.NEWARRAY) {
            ctx.instruction("newarray").print(newArrayTypeName(instruction.operand)).next();
            return;
        }
        ctx.instruction(opcodeName(instruction.getOpcode())).print(Integer.toString(instruction.operand)).next();
    }

    public void execute(LdcInsnNode instruction) {
        ctx.instruction("ldc");
        new JvmConstantPrinter(ctx).printConstant(instruction.cst);
        ctx.next();
    }

    public void execute(VarInsnNode instruction) {
        int opcode = instruction.getOpcode();
        int index = instruction.var;
        String varName = computeName(opcode, index, currentIndex + 1);
        ctx.instruction(opcodeName(opcode));
        if (varName.charAt(0) == '\\' && UNICODE_ESCAPE.matcher(varName).matches()) {
            ctx.print(varName);
        } else {
            ctx.literal(varName);
        }
        ctx.next();
    }

    public void execute(IincInsnNode instruction) {
        String variableName = computeName(Opcodes.IINC, instruction.var, currentIndex + 1);
        ctx.instruction("iinc").literal(variableName).arg().literal(instruction.incr).next();
    }

    public void execute(JumpInsnNode instruction) {
        ctx.instruction(opcodeName(instruction.getOpcode())).print(labelNames.get(instruction.label)).next();
    }

    public void execute(TypeInsnNode instruction) {
        ctx.instruction(opcodeName(instruction.getOpcode())).literal(instruction.desc).next();
    }

    public void execute(FieldInsnNode instruction) {
        ctx.instruction(opcodeName(instruction.getOpcode())).literal(instruction.owner).print(".")
                .literal(instruction.name).print(" ").literal(instruction.desc).next();
    }

    public void execute(MethodInsnNode instruction) {
        String opcode = opcodeName(instruction.getOpcode());
        if (instruction.itf && instruction.getOpcode() != Opcodes.INVOKEINTERFACE) {
            opcode += "interface";
        }
        ctx.instruction(opcode).literal(instruction.owner).print(".").literal(instruction.name)
                .print(" ").literal(instruction.desc).next();
    }

    public void execute(InvokeDynamicInsnNode instruction) {
        ctx.instruction("invokedynamic").literal(instruction.name).arg().literal(instruction.desc).arg();
        JvmConstantPrinter.printMethodHandle(instruction.bsm, ctx);
        var bsmArray = ctx.arg().array();
        for (int i = 0; i < instruction.bsmArgs.length; i++) {
            if (i > 0) {
                bsmArray.arg();
            }
            new JvmConstantPrinter(bsmArray).printConstant(instruction.bsmArgs[i]);
        }
        bsmArray.end();
        ctx.next();
    }

    public void execute(LookupSwitchInsnNode instruction) {
        var obj = ctx.instruction("lookupswitch").object();
        for (int i = 0; i < instruction.keys.size(); i++) {
            if (i > 0) {
                obj.next();
            }
            obj.value(String.valueOf(instruction.keys.get(i))).print(labelNames.get(instruction.labels.get(i)));
        }
        if (!instruction.keys.isEmpty()) {
            obj.next();
        }
        obj.value("default").print(labelNames.get(instruction.dflt));
        obj.end();
        ctx.next();
    }

    public void execute(TableSwitchInsnNode instruction) {
        var obj = ctx.instruction("tableswitch").object();
        obj.value("min").print(String.valueOf(instruction.min)).next();
        obj.value("max").print(String.valueOf(instruction.min + instruction.labels.size())).next();
        var arr = obj.value("cases").array();
        arr.print(instruction.labels, (print, lbl) -> print.print(labelNames.get(lbl)));
        arr.end();
        obj.next();
        obj.value("default").print(labelNames.get(instruction.dflt)).end();
        ctx.next();
    }

    public void execute(MultiANewArrayInsnNode instruction) {
        ctx.instruction("multianewarray").literal(instruction.desc).arg().print(Integer.toString(instruction.dims)).next();
    }

    public void print(InsnList code) {
        for (int i = 0; i < code.size(); i++) {
            index(i);
            AbstractInsnNode instruction = code.get(i);
            execute(instruction);
        }
    }

    private static @NotNull String opcodeName(int opcode) {
        return Printer.OPCODES[opcode].toLowerCase();
    }

    private static @NotNull String newArrayTypeName(int operand) {
        return switch (operand) {
            case Opcodes.T_BOOLEAN -> "boolean";
            case Opcodes.T_CHAR -> "char";
            case Opcodes.T_FLOAT -> "float";
            case Opcodes.T_DOUBLE -> "double";
            case Opcodes.T_BYTE -> "byte";
            case Opcodes.T_SHORT -> "short";
            case Opcodes.T_INT -> "int";
            case Opcodes.T_LONG -> "long";
            default -> throw new IllegalStateException("Unexpected newarray operand: " + operand);
        };
    }

    private @NotNull String computeName(int opcode, int variableIndex, int codeOffset) {
        Type assumedType = switch (opcode) {
            case Opcodes.ALOAD, Opcodes.ASTORE -> Type.getObjectType("java/lang/Object");
            case Opcodes.FLOAD, Opcodes.FSTORE -> Type.FLOAT_TYPE;
            case Opcodes.DLOAD, Opcodes.DSTORE -> Type.DOUBLE_TYPE;
            case Opcodes.LLOAD, Opcodes.LSTORE -> Type.LONG_TYPE;
            case Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.IINC, Opcodes.RET -> Type.INT_TYPE;
            default -> Type.VOID_TYPE;
        };

        var local = variables.get(variableIndex, codeOffset, assumedType.getDescriptor());
        if (local != null &&
                ((!local.isPrimitive() && assumedType.getSort() == Type.OBJECT)
                        || Variables.compatibleDescriptors(assumedType.getDescriptor(), local.descriptor()))) {
            return local.name();
        }

        return VarNaming.name(variableIndex, assumedType);
    }
}

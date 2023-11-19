package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.*;
import dev.xdark.blw.simulation.ExecutionEngine;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.IntFunction;

/**
 * An engine which is intended for use in proper stack/local analysis.
 */
public class JvmAnalysisEngine<L extends Local, S extends StackEntry, F extends AbstractFrame<L, S>>
        implements AnalysisEngine<L, S, F>, ExecutionEngine, AnalysisResults<L, S, F>, JavaOpcodes {
    private final NavigableMap<Integer, F> frames = new TreeMap<>();
    private final IntFunction<String> variableNameLookup;
    private AnalysisException analysisFailure;
    private F frame;

    public JvmAnalysisEngine(@NotNull IntFunction<String> variableNameLookup) {
        this.variableNameLookup = variableNameLookup;
    }

    @Override
    public F getFrame(int index) {
        return frames.get(index);
    }

    @Override
    public void putFrame(int index, F frame) {
        frames.put(index, frame);
        this.frame = frame;
    }

    @Override
    public @NotNull NavigableMap<Integer, F> frames() {
        return frames;
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
    public void label(Label label) {
        //no-op
    }

    @Override
    public void execute(SimpleInstruction instruction) {
        switch (instruction.opcode()) {
            case DUP -> frame.push(frame.peek());
            case DUP_X1 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                frame.push(type1, type2, type1);
            }
            case DUP_X2 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                ClassType type3 = frame.pop();
                frame.push(type1, type3, type2, type1);
            }
            case DUP2 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                frame.push(type2, type1, type2, type1);
            }
            case DUP2_X1 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                ClassType type3 = frame.pop();
                frame.push(type2, type1, type3, type2, type1);
            }
            case DUP2_X2 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                ClassType type3 = frame.pop();
                ClassType type4 = frame.pop();
                frame.push(type2, type1, type4, type3, type2, type1);
            }
            case POP, IINC, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> frame.pop();
            case POP2 -> frame.pop2();
            case SWAP -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                frame.push(type1, type2);
            }
            case IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR -> {
                frame.pop2();
                frame.push(Types.INT);
            }
            case LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR -> {
                frame.pop(4); // LONG + TOP, LONG + TOP
                frame.pushType(Types.LONG);
            }
            case FADD, FSUB, FMUL, FDIV, FREM -> {
                frame.pop2();
                frame.push(Types.FLOAT);
            }
            case DADD, DSUB, DMUL, DDIV, DREM -> {
                frame.pop(4);
                frame.pushType(Types.DOUBLE);
            }
            case INEG, LNEG, FNEG, DNEG -> {
                ClassType type = frame.pop();
                frame.pushType(type);
            }
            case ATHROW -> {
                ClassType type = frame.pop();
                if (type == Commons.NULL) {
                    frame.push(Types.type(NullPointerException.class));
                } else {
                    frame.push(type);
                }
            }
            case ACONST_NULL -> frame.pushNull();
        }
    }

    @Override
    public void execute(ConstantInstruction<?> instruction) {
        Constant constant = instruction.constant();
        if (constant instanceof OfInt) {
            frame.push(Types.INT);
        } else if (constant instanceof OfLong) {
            frame.push(Types.LONG);
            frame.push(Types.VOID); // top
        } else if (constant instanceof OfFloat) {
            frame.push(Types.FLOAT);
        } else if (constant instanceof OfDouble) {
            frame.push(Types.DOUBLE);
            frame.push(Types.VOID); // top
        } else if (constant instanceof OfString) {
            frame.push(Types.type(String.class));
        } else if (constant instanceof OfMethodHandle mh) {
            frame.push(Types.methodType(mh.value().type().descriptor()).returnType());
        } else if (constant instanceof OfDynamic dyn) {
            frame.push(dyn.value().type());
        } else if (constant instanceof OfType tp) {
            frame.push((ClassType) tp.value());
        }
    }

    @Override
    public void execute(VarInstruction instruction) {
        final int index = instruction.variableIndex();
        final int opcode = instruction.opcode();
        switch (opcode) {
            case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
                boolean has = frame.hasLocal(index);
                ClassType type;
                if (has) {
                    type = frame.getLocalType(index);
                } else {
                    type = switch (opcode) {
                        case ILOAD -> Types.INT;
                        case LLOAD -> Types.LONG;
                        case FLOAD -> Types.FLOAT;
                        case DLOAD -> Types.DOUBLE;
                        case ALOAD -> Commons.OBJECT;
                        default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
                    };
                }

                frame.push(type);
                if (opcode == LLOAD || opcode == DLOAD)
                    frame.push(Types.VOID);
            }
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                if (opcode == LSTORE || opcode == DSTORE)
                    frame.pop();

                String name = variableNameLookup.apply(index);
                frame.setLocal(index, new SimpleLocal(index, name, frame.pop()));
            }
        }
    }

    @Override
    public void execute(InstanceofInstruction instruction) {
        frame.pop();
        frame.push(Types.INT);
    }

    @Override
    public void execute(CheckCastInstruction instruction) {
        frame.pop();
        frame.push(instruction.type());
    }

    @Override
    public void execute(AllocateInstruction instruction) {
        frame.push(instruction.type());
    }

    @Override
    public void execute(MethodInstruction instruction) {
        List<ClassType> types = instruction.type().parameterTypes();
        int size = types.size();
        if (instruction.opcode() != INVOKESTATIC) {
            frame.pop();
        }
        for (int i = 0; i < size; i++) {
            frame.pop(types.get(i));
        }
        if (instruction.type().returnType() != Types.VOID)
            frame.pushType(instruction.type().returnType());
    }

    @Override
    public void execute(FieldInstruction instruction) {
        int opcode = instruction.opcode();
        switch (opcode)
        {
            case GETFIELD -> {
                frame.pop();
                frame.pushType(instruction.type());
            }
            case GETSTATIC -> frame.pushType(instruction.type());
            case PUTFIELD -> {
                frame.pop(instruction.type());
                frame.pop();
            }
            case PUTSTATIC -> frame.pop(instruction.type());
            default -> throw new IllegalStateException("Unknown field insn: " + opcode);
        }
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {
        MethodType type = (MethodType) instruction.type();
        List<ClassType> types = type.parameterTypes();
        for (ClassType classType : types) {
            frame.pop(classType);
        }
        if (type.returnType() != Types.VOID)
            frame.pushType(type.returnType());
    }

    @Override
    public void execute(ConditionalJumpInstruction instruction) {
        switch (instruction.opcode()) {
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> frame.pop();
            case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> frame.pop(2);
        }
    }

    @Override
    public void execute(PrimitiveConversionInstruction instruction) {
        frame.pop(instruction.from());
        frame.pushType(instruction.to());
    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {
        frame.pop();
    }

    @Override
    public void execute(TableSwitchInstruction instruction) {
        frame.pop();
    }

    @Override
    public void execute(ImmediateJumpInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(VariableIncrementInstruction instruction) {
        // no-op
    }

    @Override
    public void execute(Instruction instruction) {
        // no-op, nothing should hit here
    }
}

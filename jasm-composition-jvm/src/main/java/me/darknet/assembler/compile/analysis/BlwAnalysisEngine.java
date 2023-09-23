package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.*;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.Types;

import java.util.List;

public class BlwAnalysisEngine implements AnalysisEngine, JavaOpcodes {

    private Frame frame;

    public BlwAnalysisEngine(List<ClassType> parameters) {
        this.frame = new Frame();
        this.frame.locals(parameters, 0);
    }

    public void frame(Frame frame) {
        this.frame = frame;
    }

    public Frame frame() {
        return frame;
    }

    @Override
    public void label(Label label) {
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
                if(type == Frame.NULL) {
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
        switch (instruction.opcode()) {
            case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
                frame.push(frame.local(instruction.variableIndex()));
                if (instruction.opcode() == LLOAD || instruction.opcode() == DLOAD)
                    frame.push(Types.VOID);
            }
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                if (instruction.opcode() == LSTORE || instruction.opcode() == DSTORE)
                    frame.pop();
                frame.local(instruction.variableIndex(), frame.pop());
            }
        }
    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {
    }

    @Override
    public void execute(TableSwitchInstruction instruction) {
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
        if (instruction.opcode() != GETSTATIC)
            frame.pop();

        frame.pushType(instruction.type());
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {
        switch (instruction.bootstrapHandle().kind()) {
            case 0, 2, 4, 6, 7, 8, 9 -> frame.pop(); // pop `this`
        }
        if(instruction.type() instanceof MethodType mt) {
            frame.pop(mt.parameterTypes().size());
            frame.pushType(mt.returnType());
        } else {
            frame.pushType((ClassType) instruction.type());
        }
    }

    @Override
    public void execute(ImmediateJumpInstruction instruction) {
    }

    @Override
    public void execute(ConditionalJumpInstruction instruction) {
        switch (instruction.opcode()) {
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> frame.pop();
            case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> frame.pop(2);
        }
    }

    @Override
    public void execute(VariableIncrementInstruction instruction) {
    }

    @Override
    public void execute(PrimitiveConversionInstruction instruction) {
        frame.pop();
        frame.pushType(instruction.to());
    }

    @Override
    public void execute(Instruction instruction) {
    }

    public ClassType local(int index) {
        return frame.local(index);
    }
}
package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.*;
import dev.xdark.blw.simulation.ExecutionEngine;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class BlwAnalysisEngine implements ExecutionEngine, JavaOpcodes {

    private final Deque<ClassType> stack;
    private final ClassType[] locals;

    public BlwAnalysisEngine(List<ClassType> parameters, int numLocals) {
        this.stack = new ArrayDeque<>();
        this.locals = new ClassType[numLocals];
        for (int i = 0; i < parameters.size(); i++) {
            locals[i] = parameters.get(i);
        }
    }

    @Override
    public void label(Label label) {}

    @Override
    public void execute(SimpleInstruction instruction) {
        switch (instruction.opcode()) {
            case DUP -> {
                ClassType type = stack.pop();
                stack.push(type);
                stack.push(type);
            }
            case DUP_X1 -> {
                ClassType type1 = stack.pop();
                ClassType type2 = stack.pop();
                stack.push(type1);
                stack.push(type2);
                stack.push(type1);
            }
            case DUP_X2 -> {
                ClassType type1 = stack.pop();
                ClassType type2 = stack.pop();
                ClassType type3 = stack.pop();
                stack.push(type1);
                stack.push(type3);
                stack.push(type2);
                stack.push(type1);
            }
            case DUP2 -> {
                ClassType type1 = stack.pop();
                ClassType type2 = stack.pop();
                stack.push(type2);
                stack.push(type1);
                stack.push(type2);
                stack.push(type1);
            }
            case DUP2_X1 -> {
                ClassType type1 = stack.pop();
                ClassType type2 = stack.pop();
                ClassType type3 = stack.pop();
                stack.push(type2);
                stack.push(type1);
                stack.push(type3);
                stack.push(type2);
                stack.push(type1);
            }
            case DUP2_X2 -> {
                ClassType type1 = stack.pop();
                ClassType type2 = stack.pop();
                ClassType type3 = stack.pop();
                ClassType type4 = stack.pop();
                stack.push(type2);
                stack.push(type1);
                stack.push(type4);
                stack.push(type3);
                stack.push(type2);
                stack.push(type1);
            }
            case POP, IINC -> stack.pop();
            case POP2 -> {
                ClassType type = stack.pop();
                if(type != Types.LONG || type != Types.DOUBLE) {
                    stack.pop();
                }
            }
            case SWAP -> {
                ClassType type1 = stack.pop();
                ClassType type2 = stack.pop();
                stack.push(type1);
                stack.push(type2);
            }
            case IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR -> {
                stack.pop();
                stack.pop();
                stack.push(Types.INT);
            }
            case LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR -> {
                stack.pop();
                stack.pop();
                stack.push(Types.LONG);
            }
            case FADD, FSUB, FMUL, FDIV, FREM -> {
                stack.pop();
                stack.pop();
                stack.push(Types.FLOAT);
            }
            case DADD, DSUB, DMUL, DDIV, DREM -> {
                stack.pop();
                stack.pop();
                stack.push(Types.DOUBLE);
            }
            case INEG, LNEG, FNEG, DNEG -> {
                stack.pop();
                stack.push(stack.pop());
            }
        }
    }

    @Override
    public void execute(ConstantInstruction<?> instruction) {
        Constant constant = instruction.constant();
        if(constant instanceof OfInt) {
            stack.push(Types.INT);
        } else if(constant instanceof OfLong) {
            stack.push(Types.LONG);
        } else if(constant instanceof OfFloat) {
            stack.push(Types.FLOAT);
        } else if(constant instanceof OfDouble) {
            stack.push(Types.DOUBLE);
        } else if(constant instanceof OfString) {
            stack.push(Types.type(String.class));
        } else if(constant instanceof OfMethodHandle mh) {
            stack.push(Types.methodType(mh.value().type().descriptor()).returnType());
        } else if(constant instanceof OfDynamic dyn) {
            stack.push(dyn.value().type());
        } else if(constant instanceof OfType tp) {
            stack.push((ClassType) tp.value());
        }
    }

    @Override
    public void execute(VarInstruction instruction) {
        switch (instruction.opcode()) {
            case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> stack.push(locals[instruction.variableIndex()]);
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> locals[instruction.variableIndex()] = stack.pop();
        }
    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {}

    @Override
    public void execute(TableSwitchInstruction instruction) {}

    @Override
    public void execute(InstanceofInstruction instruction) {
        stack.pop();
        stack.push(Types.BOOLEAN);
    }

    @Override
    public void execute(CheckCastInstruction instruction) {
        stack.pop();
        stack.push(instruction.type());
    }

    @Override
    public void execute(AllocateInstruction instruction) {
        stack.push(instruction.type());
    }

    @Override
    public void execute(MethodInstruction instruction) {
        List<ClassType> types = instruction.type().parameterTypes();
        for (int i = 0; i < types.size(); i++) {
            stack.pop();
        }
        if(instruction.opcode() != INVOKESTATIC) {
            stack.pop();
        }
        stack.push(instruction.type().returnType());
    }

    @Override
    public void execute(FieldInstruction instruction) {
        if(instruction.opcode() != GETSTATIC)
            stack.pop();

        stack.push(instruction.type());
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {
        stack.pop();
        stack.push((ClassType) instruction.type());
    }

    @Override
    public void execute(ImmediateJumpInstruction instruction) {}

    @Override
    public void execute(ConditionalJumpInstruction instruction) {}

    @Override
    public void execute(VariableIncrementInstruction instruction) {}

    @Override
    public void execute(PrimitiveConversionInstruction instruction) {
        stack.pop();
        stack.push(instruction.to());
    }

    @Override
    public void execute(Instruction instruction) {}

    public ClassType local(int index) {
        return locals[index];
    }
}

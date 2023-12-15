package me.darknet.assembler.cli.repl.impl.jvm;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.generic.GenericCodeBuilder;
import dev.xdark.blw.code.generic.GenericCodeListBuilder;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.*;
import dev.xdark.blw.simulation.ExecutionEngine;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.cli.repl.executor.Executor;
import me.darknet.assembler.cli.repl.executor.Frame;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.ValuedLocal;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.visitor.BlwCodeVisitor;
import me.darknet.assembler.instructions.ParsedInstruction;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodType;
import java.util.Collections;

public class JvmExecutor implements Executor<JvmFrame>, ExecutionEngine, JavaOpcodes {

    private static final Top TOP = new Top();

    private final BlwCodeVisitor codeVisitor;
    private final GenericCodeListBuilder codeListBuilder;
    private final JvmFrame frame;

    private int pc;

    public JvmExecutor() {
        super();
        GenericCodeBuilder codeBuilder = new GenericCodeBuilder();
        this.codeVisitor = new BlwCodeVisitor(new JvmCompilerOptions(), codeBuilder, Collections.emptyList());
        this.codeListBuilder = (GenericCodeListBuilder) codeBuilder.codeList().child();
        this.frame = new JvmFrame();
    }

    @Override
    public JvmFrame execute(ParsedInstruction instruction) {
        instruction.accept(codeVisitor); // insert new element
        this.pc = codeListBuilder.getElements().size() - 1; // jump to end of code
        return execute();
    }

    @Override
    public JvmFrame execute() {
        return execute(codeListBuilder.getElements().get(pc));
    }

    @Override
    public JvmFrame frame() {
        return frame;
    }

    private JvmFrame execute(CodeElement element) {
        if(element instanceof Label label) {
            label(label);
        } else {
            ExecutionEngines.execute(this, (Instruction) element);
        }
        return frame;
    }

    @Override
    public void label(Label label) {
        // do nothing
    }

    @Override
    public void execute(SimpleInstruction instruction) {
        switch (instruction.opcode()) {
            case DUP -> frame.push(frame.peek());
            case DUP_X1 -> {
                Object value1 = frame.pop();
                Object value2 = frame.pop();
                frame.push(value1, value2, value1);
            }
            case DUP_X2 -> {
                Object value1 = frame.pop();
                Object value2 = frame.pop();
                Object value3 = frame.pop();
                frame.push(value1, value3, value2, value1);
            }
            case DUP2 -> {
                Object value1 = frame.pop();
                Object value2 = frame.pop();
                frame.push(value2, value1, value2, value1);
            }
            case DUP2_X1 -> {
                Object value1 = frame.pop();
                Object value2 = frame.pop();
                Object value3 = frame.pop();
                frame.push(value2, value1, value3, value2, value1);
            }
            case DUP2_X2 -> {
                Object value1 = frame.pop();
                Object value2 = frame.pop();
                Object value3 = frame.pop();
                Object value4 = frame.pop();
                frame.push(value2, value1, value4, value3, value2, value1);
            }
            case POP -> frame.pop();
            case POP2 -> frame.pop2();
            case SWAP -> {
                Object value1 = frame.pop();
                Object value2 = frame.pop();
                frame.push(value1, value2);
            }
            case INEG -> {
                Object value = frame.pop();
                if (value instanceof Integer integer) {
                    frame.push(-integer);
                } else {
                    throw new IllegalStateException("Value must be an integer");
                }
            }
            case LNEG -> {
                Object value = frame.pop2();
                if (value instanceof Long longValue) {
                    frame.push(-longValue);
                } else {
                    throw new IllegalStateException("Value must be a long");
                }
            }
            case FNEG -> {
                Object value = frame.pop();
                if (value instanceof Float floatValue) {
                    frame.push(-floatValue);
                } else {
                    throw new IllegalStateException("Value must be a float");
                }
            }
            case DNEG -> {
                Object value = frame.pop2();
                if (value instanceof Double doubleValue) {
                    frame.push(-doubleValue);
                } else {
                    throw new IllegalStateException("Value must be a double");
                }
            }
            case IADD -> ((IntOp) Integer::sum).accept(frame);
            case ISUB -> ((IntOp) (a, b) -> a - b).accept(frame);
            case IMUL -> ((IntOp) (a, b) -> a * b).accept(frame);
            case IDIV -> ((IntOp) (a, b) -> a / b).accept(frame);
            case IREM -> ((IntOp) (a, b) -> a % b).accept(frame);
            case IAND -> ((IntOp) (a, b) -> a & b).accept(frame);
            case IOR -> ((IntOp) (a, b) -> a | b).accept(frame);
            case IXOR -> ((IntOp) (a, b) -> a ^ b).accept(frame);
            case ISHL -> ((IntOp) (a, b) -> a << b).accept(frame);
            case ISHR -> ((IntOp) (a, b) -> a >> b).accept(frame);
            case IUSHR -> ((IntOp) (a, b) -> a >>> b).accept(frame);
            case LADD -> ((LongOp) Long::sum).accept(frame);
            case LSUB -> ((LongOp) (a, b) -> a - b).accept(frame);
            case LMUL -> ((LongOp) (a, b) -> a * b).accept(frame);
            case LDIV -> ((LongOp) (a, b) -> a / b).accept(frame);
            case LREM -> ((LongOp) (a, b) -> a % b).accept(frame);
            case LAND -> ((LongOp) (a, b) -> a & b).accept(frame);
            case LOR -> ((LongOp) (a, b) -> a | b).accept(frame);
            case LXOR -> ((LongOp) (a, b) -> a ^ b).accept(frame);
            case LSHL -> ((LongOp) (a, b) -> a << b).accept(frame);
            case LSHR -> ((LongOp) (a, b) -> a >> b).accept(frame);
            case LUSHR -> ((LongOp) (a, b) -> a >>> b).accept(frame);
            case FADD -> ((FloatOp) Float::sum).accept(frame);
            case FSUB -> ((FloatOp) (a, b) -> a - b).accept(frame);
            case FMUL -> ((FloatOp) (a, b) -> a * b).accept(frame);
            case FDIV -> ((FloatOp) (a, b) -> a / b).accept(frame);
            case FREM -> ((FloatOp) (a, b) -> a % b).accept(frame);
            case DADD -> ((DoubleOp) Double::sum).accept(frame);
            case DSUB -> ((DoubleOp) (a, b) -> a - b).accept(frame);
            case DMUL -> ((DoubleOp) (a, b) -> a * b).accept(frame);
            case DDIV -> ((DoubleOp) (a, b) -> a / b).accept(frame);
            case DREM -> ((DoubleOp) (a, b) -> a % b).accept(frame);
            case FCMPL -> ((FloatCompare) () -> -1).accept(frame);
            case FCMPG -> ((FloatCompare) () -> 1).accept(frame);
            case DCMPL -> ((DoubleCompare) () -> -1).accept(frame);
            case DCMPG -> ((DoubleCompare) () -> 1).accept(frame);
            case LCMP -> {
                Object value1 = frame.pop2();
                Object value2 = frame.pop2();
                if (value1 instanceof Long long1 && value2 instanceof Long long2) {
                    frame.push(Long.compare(long1, long2));
                } else {
                    throw new IllegalStateException("Both values must be longs");
                }
            }
        }
    }

    @Override
    public void execute(ConstantInstruction<?> instruction) {
        Constant constant = instruction.constant();
        if (constant instanceof OfInt cInt) {
            frame.push(cInt.value());
        } else if (constant instanceof OfLong cLong) {
            frame.push(cLong.value());
            frame.push(TOP);
        } else if (constant instanceof OfFloat cFloat) {
            frame.push(cFloat.value());
        } else if (constant instanceof OfDouble cDouble) {
            frame.push(cDouble.value());
            frame.push(TOP);
        } else if (constant instanceof OfString cString) {
            frame.push(cString.value());
        } else if (constant instanceof OfMethodHandle mh) {
            // TODO
        } else if (constant instanceof OfDynamic dyn) {
            // TODO
        } else if (constant instanceof OfType tp) {
            Class<?> type = parseDescriptor(tp.value().descriptor());
            frame.push(type);
        }
    }

    @Override
    public void execute(VarInstruction instruction) {
        final int index = instruction.variableIndex();
        final int opcode = instruction.opcode();
        switch (opcode) {
            case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
                Local local = frame.locals().get(index);
                if (local == null)
                    throw new NullPointerException("Local variable " + index + " is null");
                frame.push(local.value());
            }
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                String name = codeVisitor.getLocalName(index);
                Object value = opcode == LSTORE || opcode == DSTORE ? frame.pop2() : frame.pop();
                frame.setLocal(index, new Local(index, name, value));
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

    }

    @Override
    public void execute(CheckCastInstruction instruction) {

    }

    @Override
    public void execute(AllocateInstruction instruction) {

    }

    @Override
    public void execute(MethodInstruction instruction) {

    }

    @Override
    public void execute(FieldInstruction instruction) {

    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {

    }

    @Override
    public void execute(ImmediateJumpInstruction instruction) {

    }

    @Override
    public void execute(ConditionalJumpInstruction instruction) {

    }

    @Override
    public void execute(VariableIncrementInstruction instruction) {

    }

    @Override
    public void execute(PrimitiveConversionInstruction instruction) {

    }

    @Override
    public void execute(Instruction instruction) {

    }

    private interface DoubleOp {
        double op(double a, double b);

        default void accept(@NotNull JvmFrame frame) {
            Object value1 = frame.pop2();
            Object value2 = frame.pop2();
            if (value1 instanceof Double double1 && value2 instanceof Double double2) {
                frame.push(op(double1, double2));
            } else {
                throw new IllegalStateException("Both values must be doubles");
            }
        }
    }

    private interface LongOp {
        long op(long a, long b);

        default void accept(@NotNull JvmFrame frame) {
            Object value1 = frame.pop2();
            Object value2 = frame.pop2();
            if (value1 instanceof Long long1 && value2 instanceof Long long2) {
                frame.push(op(long1, long2));
            } else {
                throw new IllegalStateException("Both values must be longs");
            }
        }
    }

    private interface IntOp {
        int op(int a, int b);

        default void accept(@NotNull JvmFrame frame) {
            Object value1 = frame.pop();
            Object value2 = frame.pop();
            if (value1 instanceof Integer int1 && value2 instanceof Integer int2) {
                frame.push(op(int1, int2));
            } else {
                throw new IllegalStateException("Both values must be ints");
            }
        }
    }

    private interface FloatOp {
        float op(float a, float b);

        default void accept(@NotNull JvmFrame frame) {
            Object value1 = frame.pop();
            Object value2 = frame.pop();
            if (value1 instanceof Float float1 && value2 instanceof Float float2) {
                frame.push(op(float1, float2));
            } else {
                throw new IllegalStateException("Both values must be floats");
            }
        }
    }

    private interface FloatCompare {
        int nan();

        default void accept(@NotNull JvmFrame frame) {
            Object value1 = frame.pop();
            Object value2 = frame.pop();
            if (value1 instanceof Float float1 && value2 instanceof Float float2) {
                if(Float.isNaN(float1) || Float.isNaN(float2)) {
                    frame.push(nan());
                } else {
                    frame.push(Float.compare(float1, float2));
                }
            } else {
                throw new IllegalStateException("Both values must be floats");
            }
        }
    }

    private interface DoubleCompare {
        int nan();

        default void accept(@NotNull JvmFrame frame) {
            Object value1 = frame.pop2();
            Object value2 = frame.pop2();
            if (value1 instanceof Double double1 && value2 instanceof Double double2) {
                if(Double.isNaN(double1) || Double.isNaN(double2)) {
                    frame.push(nan());
                } else {
                    frame.push(Double.compare(double1, double2));
                }
            } else {
                throw new IllegalStateException("Both values must be doubles");
            }
        }
    }

    private static Class<?> parseDescriptor(String descriptor) {
        return MethodType.fromMethodDescriptorString("()" + descriptor,
                Thread.currentThread().getContextClassLoader()).returnType();
    }

    private record Top() {
        @Override
        public String toString() {
            return "";
        }
    }

}

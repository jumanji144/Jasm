package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.type.*;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.VariableNameLookup;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.assembler.compile.analysis.frame.TypedFrameOps;

import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * JVM engine which tracks types of items in the stack/locals.
 */
public class TypedJvmAnalysisEngine extends JvmAnalysisEngine<TypedFrame> {
    public TypedJvmAnalysisEngine(@NotNull VariableNameLookup variableNameLookup) {
        super(variableNameLookup);
    }

    @Override
    public FrameOps<?> newFrameOps() {
        return new TypedFrameOps();
    }

    @Override
    public void execute(SimpleInstruction instruction) {
        int opcode = instruction.opcode();
        switch (opcode) {
            case DUP -> frame.pushType(frame.peek());
            case DUP_X1 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                frame.pushTypes(type1, type2, type1);
            }
            case DUP_X2 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                ClassType type3 = frame.pop();
                frame.pushTypes(type1, type3, type2, type1);
            }
            case DUP2 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                frame.pushTypes(type2, type1, type2, type1);
            }
            case DUP2_X1 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                ClassType type3 = frame.pop();
                frame.pushTypes(type2, type1, type3, type2, type1);
            }
            case DUP2_X2 -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                ClassType type3 = frame.pop();
                ClassType type4 = frame.pop();
                frame.pushTypes(type2, type1, type4, type3, type2, type1);
            }
            case POP, IRETURN, FRETURN, ARETURN, MONITORENTER, MONITOREXIT -> frame.pop();
            case POP2, LRETURN, DRETURN -> frame.pop2();
            case SWAP -> {
                ClassType type1 = frame.pop();
                ClassType type2 = frame.pop();
                frame.pushTypes(type1, type2);
            }
            case IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR, FCMPG, FCMPL -> {
                ClassType valueType1 = frame.pop();
                ClassType valueType2 = frame.pop();
                if (!(valueType1 instanceof PrimitiveType))
                    warn(instruction, "Top value to operate on is not a primitive");
                if (!(valueType2 instanceof PrimitiveType))
                    warn(instruction, "Bottom value to operate on is not a primitive");
                frame.pushType(Types.INT);
            }
            case LADD, LSUB, LMUL, LDIV, LREM, LAND, LOR, LXOR -> {
                ClassType valueType1 = frame.pop2();
                ClassType valueType2 = frame.pop2();
                if (!(valueType1 instanceof PrimitiveType))
                    warn(instruction, "Top value to operate on is not a primitive");
                if (!(valueType2 instanceof PrimitiveType))
                    warn(instruction, "Bottom value to operate on is not a primitive");
                frame.pushType(Types.LONG);
            }
            case LSHL, LSHR, LUSHR -> {
                ClassType offsetType = frame.pop();
                ClassType valueType = frame.pop2();
                if (!(offsetType instanceof PrimitiveType))
                    warn(instruction, "Shift offset is not a primitive");
                if (!(valueType instanceof PrimitiveType))
                    warn(instruction, "Shift target is not a primitive");
                frame.pushType(Types.LONG);
            }
            case FADD, FSUB, FMUL, FDIV, FREM -> {
                ClassType valueType1 = frame.pop();
                ClassType valueType2 = frame.pop();
                if (!(valueType1 instanceof PrimitiveType))
                    warn(instruction, "Top value to operate on is not a primitive");
                if (!(valueType2 instanceof PrimitiveType))
                    warn(instruction, "Bottom value to operate on is not a primitive");
                frame.pushType(Types.FLOAT);
            }
            case DADD, DSUB, DMUL, DDIV, DREM -> {
                ClassType valueType1 = frame.pop2();
                ClassType valueType2 = frame.pop2();
                if (!(valueType1 instanceof PrimitiveType))
                    warn(instruction, "Top value to operate on is not a primitive");
                if (!(valueType2 instanceof PrimitiveType))
                    warn(instruction, "Bottom value to operate on is not a primitive");
                frame.pushType(Types.DOUBLE);
            }
            case DCMPL, DCMPG, LCMP -> {
                ClassType valueType1 = frame.pop2();
                ClassType valueType2 = frame.pop2();
                if (!(valueType1 instanceof PrimitiveType))
                    warn(instruction, "Top value to compare is not a primitive");
                if (!(valueType2 instanceof PrimitiveType))
                    warn(instruction, "Bottom value to compare is not a primitive");
                frame.pushType(Types.INT);
            }
            case INEG -> {
                ClassType valueType = frame.pop();
                if (!(valueType instanceof PrimitiveType))
                    warn(instruction, "Value to negate is not a primitive");
                frame.pushType(Types.INT);
            }
            case FNEG -> {
                ClassType valueType = frame.pop();
                if (!(valueType instanceof PrimitiveType))
                    warn(instruction, "Value to negate is not a primitive");
                frame.pushType(Types.FLOAT);
            }
            case LNEG -> {
                ClassType valueType = frame.pop2();
                if (!(valueType instanceof PrimitiveType))
                    warn(instruction, "Value to negate is not a primitive");
                frame.pushType(Types.LONG);
            }
            case DNEG -> {
                ClassType valueType = frame.pop2();
                if (!(valueType instanceof PrimitiveType))
                    warn(instruction, "Value to negate is not a primitive");
                frame.pushType(Types.DOUBLE);
            }
            case ATHROW -> frame.getStack().clear();
            case ACONST_NULL -> frame.pushNull();
            case RETURN -> { /* no-op */ }
            case IASTORE, FASTORE, BASTORE, AASTORE, CASTORE, SASTORE -> {
                ClassType valueType = frame.pop();
                ClassType indexType = frame.pop();
                ClassType arrayType = frame.pop();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                if (!(valueType instanceof PrimitiveType))
                    warn(instruction, "Value to store in array is not a primitive");
            }
            case DASTORE, LASTORE -> {
                ClassType valueType = frame.pop2();
                ClassType indexType = frame.pop();
                ClassType arrayType = frame.pop();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                if (!(valueType instanceof PrimitiveType)) {
                    warn(instruction, "Value to store in array is not a primitive");
                } else warn(instruction, "Value to store in array is not a primitive");
            }
            case IALOAD -> {
                ClassType indexType = frame.pop();
                ClassType arrayType = frame.pop();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.INT);
            }
            case FALOAD -> {
                ClassType indexType = frame.pop();
                ClassType arrayType = frame.pop();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.FLOAT);
            }
            case BALOAD -> {
                ClassType indexType = frame.pop();
                ClassType arrayType = frame.pop();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.BYTE);
            }
            case CALOAD -> {
                ClassType indexType = frame.pop();
                ClassType arrayType = frame.pop();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.CHAR);
            }
            case SALOAD -> {
                ClassType indexType = frame.pop();
                ClassType arrayType = frame.pop();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.SHORT);
            }
            case DALOAD -> {
                ClassType indexType = frame.pop();
                ClassType arrayType = frame.pop();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.DOUBLE);
            }
            case LALOAD -> {
                ClassType indexType = frame.pop();
                ClassType arrayType = frame.pop();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.LONG);
            }
            case AALOAD -> {
                ClassType indexType = frame.pop();// index
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                ClassType arrayRef = frame.pop();
                if (arrayRef instanceof ArrayType arrayType) {
                    if (arrayType.dimensions() == 1) {
                        frame.pushType(arrayType.componentType());
                    } else {
                        frame.pushType(Types.arrayTypeFromDescriptor(arrayType.descriptor().substring(1)));
                    }
                } else {
                    warn(instruction, "Array reference on stack is not an array");
                    frame.pushType(Types.OBJECT);
                }
            }
            case ARRAYLENGTH -> {
                ClassType stackType = frame.pop();
                if (stackType == null)
                    warn(instruction, "Cannot get array length of 'null'");
                else if (stackType instanceof PrimitiveType)
                    warn(instruction, "Cannot get array length of primitive");
                else if (stackType instanceof InstanceType)
                    warn(instruction, "Cannot get array length of non-array reference");
                frame.pushType(Types.INT);
            }
            case NOP -> {}
            default -> throw new IllegalStateException("Unhandled simple insn: " + opcode);
        }
    }

    @Override
    public void execute(ConstantInstruction<?> instruction) {
        Constant constant = instruction.constant();
        if (constant instanceof OfInt) {
            frame.pushType(Types.INT);
        } else if (constant instanceof OfLong) {
            frame.pushType(Types.LONG);
        } else if (constant instanceof OfFloat) {
            frame.pushType(Types.FLOAT);
        } else if (constant instanceof OfDouble) {
            frame.pushType(Types.DOUBLE);
        } else if (constant instanceof OfString) {
            frame.pushType(Types.type(String.class));
        } else if (constant instanceof OfMethodHandle) {
            // push java/lang/invoke/MethodHandle
            frame.pushType(METHOD_HANDLE);
        } else if (constant instanceof OfDynamic dyn) {
            frame.pushType(dyn.value().type());
        } else if (constant instanceof OfType tp) {
            Type type = tp.value();
            if (type instanceof ClassType ct)
                frame.pushType(CLASS);
            else if (type instanceof MethodType mt)
                // push java/lang/invoke/MethodType
                frame.pushType(METHOD_TYPE);
        }
    }

    @Override
    public void execute(VarInstruction instruction) {
        final int index = instruction.variableIndex();
        final int opcode = instruction.opcode();
        switch (opcode) {
            case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
                // Check if the index is a known null value, or actually a non-existent local.
                ClassType type = frame.getLocalType(index);
                if (type == null && frame.getLocal(index) == null) {
                    // We only adapt these loads if the value is a truly unknown local.
                    // If the local is known to be null, we keep it as-is.
                    type = switch (opcode) {
                        case ILOAD -> Types.INT;
                        case LLOAD -> Types.LONG;
                        case FLOAD -> Types.FLOAT;
                        case DLOAD -> Types.DOUBLE;
                        case ALOAD -> Types.OBJECT;
                        default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
                    };
                }
                frame.pushType(type);
            }
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                String name = variableNameLookup.getVarName(index);
                ClassType stackType = opcode == LSTORE || opcode == DSTORE ? frame.pop2() : frame.pop();
                ClassType assumedType = switch (opcode) {
                    case ISTORE -> Types.INT;
                    case LSTORE -> Types.LONG;
                    case FSTORE -> Types.FLOAT;
                    case DSTORE -> Types.DOUBLE;
                    case ASTORE -> Types.OBJECT;
                    default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
                };
                if (assumedType == Types.OBJECT) {
                    if (stackType instanceof PrimitiveType)
                        warn(instruction, "Incorrect var assignment, " + stackType.descriptor() + " into reference");
                } else if (stackType == null || assumedType.getClass() != stackType.getClass()) {
                    String stackName = stackType == null ? "'null'" : stackType.descriptor();
                    warn(instruction, "Incorrect var assignment, " + stackName + " into " + assumedType.descriptor());
                }
                frame.setLocal(index, new Local(index, name, stackType));
            }
        }
    }

    @Override
    public void execute(InstanceofInstruction instruction) {
        ClassType origin = frame.pop();
        if (origin instanceof PrimitiveType)
            warn(instruction, "Cannot instanceof primitive to reference");
        frame.pushType(Types.INT);
    }

    @Override
    public void execute(CheckCastInstruction instruction) {
        ClassType origin = frame.pop();
        if (origin instanceof PrimitiveType)
            warn(instruction, "Cannot cast primitive to reference");
        frame.pushType(instruction.type());
    }

    @Override
    public void execute(MethodInstruction instruction) {
        MethodType methodType = instruction.type();
        List<ClassType> types = methodType.parameterTypes();
        for (int i = types.size(); i > 0; i--) {
            frame.pop(types.get(i - 1));
        }
        if (instruction.opcode() != INVOKESTATIC) {
            ClassType contextType = frame.pop();
            if (contextType == null)
                warn(instruction, "Cannot invoke method of 'null' reference");
            else if (contextType instanceof PrimitiveType)
                warn(instruction, "Cannot invoke method on primitive");
            else if (contextType instanceof ArrayType)
                warn(instruction, "Cannot invoke method on array");
        }
        if (methodType.returnType() != Types.VOID)
            frame.pushType(methodType.returnType());
    }

    @Override
    public void execute(FieldInstruction instruction) {
        int opcode = instruction.opcode();
        switch (opcode) {
            case GETFIELD -> {
                ClassType contextType = frame.pop();
                if (contextType == null)
                    warn(instruction, "Cannot get field of 'null' reference");
                else if (contextType instanceof PrimitiveType)
                    warn(instruction, "Cannot get field of primitive");
                else if (contextType instanceof ArrayType)
                    warn(instruction, "Cannot get field of array");
                frame.pushType(instruction.type());
            }
            case GETSTATIC -> frame.pushType(instruction.type());
            case PUTFIELD -> {
                frame.pop(instruction.type());
                ClassType contextType = frame.pop();
                if (contextType == null)
                    warn(instruction, "Cannot put field on 'null' reference");
                else if (contextType instanceof PrimitiveType)
                    warn(instruction, "Cannot put field on primitive");
                else if (contextType instanceof ArrayType)
                    warn(instruction, "Cannot put field on array");
            }
            case PUTSTATIC -> frame.pop(instruction.type());
            default -> throw new IllegalStateException("Unknown field insn: " + opcode);
        }
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {
        MethodType type = (MethodType) instruction.type();
        List<ClassType> types = type.parameterTypes();
        for (ClassType classType : types)
            frame.pop(classType);
        if (type.returnType() != Types.VOID)
            frame.pushType(type.returnType());
    }

    @Override
    public void execute(PrimitiveConversionInstruction instruction) {
        ClassType type = frame.pop(instruction.from());
        if (type == null)
            warn(instruction, "Cannot convert 'null' on stack to primitive");
        else if (type instanceof ObjectType)
            warn(instruction, "Cannot convert 'null' on stack to reference type");
        frame.pushType(instruction.to());
    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {
        ClassType type = frame.pop();
        if (type == null)
            warn(instruction, "Cannot switch off 'null' on stack");
        else if (type instanceof ObjectType)
            warn(instruction, "Cannot switch off reference type on stack");
    }

    @Override
    public void execute(TableSwitchInstruction instruction) {
        ClassType type = frame.pop();
        if (type == null)
            warn(instruction, "Cannot switch off 'null' on stack");
        else if (type instanceof ObjectType)
            warn(instruction, "Cannot switch off reference type on stack");
    }

    @Override
    public void execute(VariableIncrementInstruction instruction) {
        Local local = frame.getLocal(instruction.variableIndex());
        if (local == null) {
            // Invalid iinc target
            String name = variableNameLookup.getVarName(instruction.variableIndex());
            if (name == null) {
                error(instruction, "Invalid iinc target, not a recognized variable");
                return;
            }

            frame.setLocal(instruction.variableIndex(), new Local(instruction.variableIndex(), name, Types.INT));
        }
    }
}

package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.*;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrameOps;

import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.*;
import dev.xdark.blw.type.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * JVM engine which tracks types and values of items in the stack/locals.
 */
public class ValuedJvmAnalysisEngine extends JvmAnalysisEngine<ValuedFrame> {
    private MethodValueLookup methodValueLookup;
    private FieldValueLookup fieldValueLookup;

    public ValuedJvmAnalysisEngine(@NotNull VariableNameLookup variableNameLookup) {
        super(variableNameLookup);
    }

    @Override
    public FrameOps<?> newFrameOps() {
        return new ValuedFrameOps();
    }

    /**
     * @param methodValueLookup
     *                          Lookup for method return values. Can be {@code null}
     *                          to assume unknown values of the return type.
     */
    public void setMethodValueLookup(MethodValueLookup methodValueLookup) {
        this.methodValueLookup = methodValueLookup;
    }

    /**
     * @param fieldValueLookup
     *                         Lookup for field values. Can be {@code null} to
     *                         assume unknown values of the field type.
     */
    public void setFieldValueLookup(FieldValueLookup fieldValueLookup) {
        this.fieldValueLookup = fieldValueLookup;
    }

    @Override
    public void execute(SimpleInstruction instruction) {
        int opcode = instruction.opcode();
        switch (opcode) {
            case DUP -> frame.push(frame.peek());
            case DUP_X1 -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                frame.pushRaw(value1, value2, value1);
            }
            case DUP_X2 -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                Value value3 = frame.pop();
                frame.pushRaw(value1, value3, value2, value1);
            }
            case DUP2 -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                frame.pushRaw(value2, value1, value2, value1);
            }
            case DUP2_X1 -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                Value value3 = frame.pop();
                frame.pushRaw(value2, value1, value3, value2, value1);
            }
            case DUP2_X2 -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                Value value3 = frame.pop();
                Value value4 = frame.pop();
                frame.pushRaw(value2, value1, value4, value3, value2, value1);
            }
            case POP, IRETURN, FRETURN, ARETURN, MONITORENTER, MONITOREXIT -> frame.pop();
            case POP2, LRETURN, DRETURN -> frame.pop2();
            case SWAP -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                frame.pushRaw(value1, value2);
            }
            case INEG, FNEG -> {
                Value value = frame.pop();
                if (value instanceof Value.PrimitiveValue primitiveValue)
                    frame.push(primitiveValue.negate());
                else {
                    frame.pushType(value.type());
                    warn(instruction, "Value to negate is not a primitive");
                }
            }
            case LNEG, DNEG -> {
                Value value = frame.pop2();
                if (value instanceof Value.PrimitiveValue primitiveValue)
                    frame.push(primitiveValue.negate());
                else {
                    frame.pushType(value.type());
                    warn(instruction, "Value to negate is not a primitive");
                }
            }
            case IADD -> ((IntOp) Integer::sum).accept(frame, m -> warn(instruction, m));
            case ISUB -> ((IntOp) (a, b) -> b - a).accept(frame, m -> warn(instruction, m));
            case IMUL -> ((IntOp) (a, b) -> b * a).accept(frame, m -> warn(instruction, m));
            case IREM -> ((IntOp) (a, b) -> b % a).accept(frame, m -> warn(instruction, m));
            case ISHL -> ((IntOp) (a, b) -> b << a).accept(frame, m -> warn(instruction, m));
            case ISHR -> ((IntOp) (a, b) -> b >> a).accept(frame, m -> warn(instruction, m));
            case IUSHR -> ((IntOp) (a, b) -> b >>> a).accept(frame, m -> warn(instruction, m));
            case IAND -> ((IntOp) (a, b) -> b & a).accept(frame, m -> warn(instruction, m));
            case IOR -> ((IntOp) (a, b) -> b | a).accept(frame, m -> warn(instruction, m));
            case IXOR -> ((IntOp) (a, b) -> b ^ a).accept(frame, m -> warn(instruction, m));
            case IDIV -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                if (value1 instanceof Value.KnownIntValue int1 && value2 instanceof Value.KnownIntValue int2) {
                    int a = int1.value();
                    int b = int2.value();
                    if (a == 0)
                        frame.pushType(Types.INT);
                    else
                        frame.push(Values.valueOf(b / a));
                } else {
                    if (!(value1.type() instanceof PrimitiveType))
                        warn(instruction, "Top value to compare is not a primitive");
                    if (!(value2.type() instanceof PrimitiveType))
                        warn(instruction, "Bottom value to compare is not a primitive");
                    frame.pushType(Types.INT);
                }
            }
            case LADD -> ((LongOp) Long::sum).accept(frame, m -> warn(instruction, m));
            case LSUB -> ((LongOp) (a, b) -> b - a).accept(frame, m -> warn(instruction, m));
            case LMUL -> ((LongOp) (a, b) -> b * a).accept(frame, m -> warn(instruction, m));
            case LREM -> ((LongOp) (a, b) -> b % a).accept(frame, m -> warn(instruction, m));
            case LSHL -> ((LongIntOp) (a, b) -> a << b).accept(frame, m -> warn(instruction, m));
            case LSHR -> ((LongIntOp) (a, b) -> a >> b).accept(frame, m -> warn(instruction, m));
            case LUSHR -> ((LongIntOp) (a, b) -> b >>> a).accept(frame, m -> warn(instruction, m));
            case LAND -> ((LongOp) (a, b) -> b & a).accept(frame, m -> warn(instruction, m));
            case LOR -> ((LongOp) (a, b) -> b | a).accept(frame, m -> warn(instruction, m));
            case LXOR -> ((LongOp) (a, b) -> b ^ a).accept(frame, m -> warn(instruction, m));
            case LDIV -> {
                Value value1 = frame.pop2();
                Value value2 = frame.pop2();
                if (value1 instanceof Value.KnownLongValue long1 && value2 instanceof Value.KnownLongValue long2) {
                    long a = long1.value();
                    long b = long2.value();
                    if (a == 0)
                        frame.pushType(Types.LONG);
                    else
                        frame.push(Values.valueOf(b / a));
                } else {
                    if (!(value1.type() instanceof PrimitiveType))
                        warn(instruction, "Top value to compare is not a primitive");
                    if (!(value2.type() instanceof PrimitiveType))
                        warn(instruction, "Bottom value to compare is not a primitive");
                    frame.pushType(Types.LONG);
                }
            }
            case FADD -> ((FloatOp) Float::sum).accept(frame, m -> warn(instruction, m));
            case FSUB -> ((FloatOp) (a, b) -> b - a).accept(frame, m -> warn(instruction, m));
            case FMUL -> ((FloatOp) (a, b) -> b * a).accept(frame, m -> warn(instruction, m));
            case FREM -> ((FloatOp) (a, b) -> b % a).accept(frame, m -> warn(instruction, m));
            case FDIV -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                if (value1 instanceof Value.KnownFloatValue float1 && value2 instanceof Value.KnownFloatValue float2) {
                    float a = float1.value();
                    float b = float2.value();
                    if (a == 0)
                        frame.pushType(Types.FLOAT);
                    else
                        frame.push(Values.valueOf(b / a));
                } else {
                    if (!(value1.type() instanceof PrimitiveType))
                        warn(instruction, "Top value to compare is not a primitive");
                    if (!(value2.type() instanceof PrimitiveType))
                        warn(instruction, "Bottom value to compare is not a primitive");
                    frame.pushType(Types.FLOAT);
                }
            }
            case DADD -> ((DoubleOp) Double::sum).accept(frame, m -> warn(instruction, m));
            case DSUB -> ((DoubleOp) (a, b) -> b - a).accept(frame, m -> warn(instruction, m));
            case DMUL -> ((DoubleOp) (a, b) -> b * a).accept(frame, m -> warn(instruction, m));
            case DREM -> ((DoubleOp) (a, b) -> b % a).accept(frame, m -> warn(instruction, m));
            case DDIV -> {
                Value value1 = frame.pop2();
                Value value2 = frame.pop2();
                if (value1 instanceof Value.KnownDoubleValue double1 && value2 instanceof Value.KnownDoubleValue double2) {
                    double a = double1.value();
                    double b = double2.value();
                    if (a == 0)
                        frame.pushType(Types.DOUBLE);
                    else
                        frame.push(Values.valueOf(b / a));
                } else {
                    if (!(value1.type() instanceof PrimitiveType))
                        warn(instruction, "Top value to compare is not a primitive");
                    if (!(value2.type() instanceof PrimitiveType))
                        warn(instruction, "Bottom value to compare is not a primitive");
                    frame.pushType(Types.DOUBLE);
                }
            }
            case LCMP -> {
                Value value1 = frame.pop2();
                Value value2 = frame.pop2();
                if (value1 instanceof Value.KnownLongValue long1 && value2 instanceof Value.KnownLongValue long2) {
                    long a = long1.value();
                    long b = long2.value();
                    frame.push(Values.valueOf(Long.compare(a, b)));
                } else {
                    if (!(value1.type() instanceof PrimitiveType))
                        warn(instruction, "Top value to compare is not a primitive");
                    if (!(value2.type() instanceof PrimitiveType))
                        warn(instruction, "Bottom value to compare is not a primitive");
                    frame.pushType(Types.INT);
                }
            }
            case FCMPL -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                if (value1 instanceof Value.KnownFloatValue float1 && value2 instanceof Value.KnownFloatValue float2) {
                    float a = float1.value();
                    float b = float2.value();
                    if (Float.isNaN(a) || Float.isNaN(b))
                        frame.push(Values.valueOf(-1));
                    else
                        frame.push(Values.valueOf(Double.compare(a, b)));
                } else {
                    if (!(value1.type() instanceof PrimitiveType))
                        warn(instruction, "Top value to compare is not a primitive");
                    if (!(value2.type() instanceof PrimitiveType))
                        warn(instruction, "Bottom value to compare is not a primitive");
                    frame.pushType(Types.INT);
                }
            }
            case FCMPG -> {
                Value value1 = frame.pop();
                Value value2 = frame.pop();
                if (value1 instanceof Value.KnownFloatValue float1 && value2 instanceof Value.KnownFloatValue float2) {
                    float a = float1.value();
                    float b = float2.value();
                    if (Float.isNaN(a) || Float.isNaN(b))
                        frame.push(Values.valueOf(1));
                    else
                        frame.push(Values.valueOf(Double.compare(a, b)));
                } else {
                    if (!(value1.type() instanceof PrimitiveType))
                        warn(instruction, "Top value to compare is not a primitive");
                    if (!(value2.type() instanceof PrimitiveType))
                        warn(instruction, "Bottom value to compare is not a primitive");
                    frame.pushType(Types.INT);
                }
            }
            case DCMPL -> {
                Value value1 = frame.pop2();
                Value value2 = frame.pop2();
                if (value1 instanceof Value.KnownDoubleValue double1 && value2 instanceof Value.KnownDoubleValue double2) {
                    double a = double1.value();
                    double b = double2.value();
                    if (Double.isNaN(a) || Double.isNaN(b))
                        frame.push(Values.valueOf(-1));
                    else
                        frame.push(Values.valueOf(Double.compare(a, b)));
                } else {
                    if (!(value1.type() instanceof PrimitiveType))
                        warn(instruction, "Top value to compare is not a primitive");
                    if (!(value2.type() instanceof PrimitiveType))
                        warn(instruction, "Bottom value to compare is not a primitive");
                    frame.pushType(Types.INT);
                }
            }
            case DCMPG -> {
                Value value1 = frame.pop2();
                Value value2 = frame.pop2();
                if (value1 instanceof Value.KnownDoubleValue double1 && value2 instanceof Value.KnownDoubleValue double2) {
                    double a = double1.value();
                    double b = double2.value();
                    if (Double.isNaN(a) || Double.isNaN(b))
                        frame.push(Values.valueOf(1));
                    else
                        frame.push(Values.valueOf(Double.compare(a, b)));
                } else {
                    if (!(value1.type() instanceof PrimitiveType))
                        warn(instruction, "Top value to compare is not a primitive");
                    if (!(value2.type() instanceof PrimitiveType))
                        warn(instruction, "Bottom value to compare is not a primitive");
                    frame.pushType(Types.INT);
                }
            }
            case ATHROW -> frame.getStack().clear();
            case ACONST_NULL -> frame.pushNull();
            case RETURN -> {
                /* no-op */ }
            case AASTORE -> {
                ClassType valueType = doArrayStore(instruction);
                if (!(valueType instanceof ObjectType) && valueType != null)
                    warn(instruction, "Value to store in array is not a reference");
            }
            case IASTORE, FASTORE , BASTORE, CASTORE, SASTORE -> {
                ClassType valueType = doArrayStore(instruction);
                if (!(valueType instanceof PrimitiveType))
                    warn(instruction, "Value to store in array is not a primitive");
            }
            case DASTORE, LASTORE -> {
                ClassType valueType = frame.pop2().type();
                ClassType indexType = frame.pop().type();
                ClassType arrayType = frame.pop().type();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                if (!(valueType instanceof PrimitiveType)) {
                    warn(instruction, "Value to store in array is not a primitive");
                } else warn(instruction, "Value to store in array is not a primitive");
            }
            case IALOAD -> {
                ClassType indexType = frame.pop().type();
                ClassType arrayType = frame.pop().type();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.INT);
            }
            case FALOAD -> {
                ClassType indexType = frame.pop().type();
                ClassType arrayType = frame.pop().type();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.FLOAT);
            }
            case BALOAD -> {
                ClassType indexType = frame.pop().type();
                ClassType arrayType = frame.pop().type();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.BYTE);
            }
            case CALOAD -> {
                ClassType indexType = frame.pop().type();
                ClassType arrayType = frame.pop().type();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.CHAR);
            }
            case SALOAD -> {
                ClassType indexType = frame.pop().type();
                ClassType arrayType = frame.pop().type();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.SHORT);
            }
            case DALOAD -> {
                ClassType indexType = frame.pop().type();
                ClassType arrayType = frame.pop().type();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.DOUBLE);
            }
            case LALOAD -> {
                ClassType indexType = frame.pop().type();
                ClassType arrayType = frame.pop().type();
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (!(arrayType instanceof ArrayType))
                    warn(instruction, "Array reference on stack is not an array");
                frame.pushType(Types.LONG);
            }
            case AALOAD -> {
                ClassType indexType = frame.pop().type();// index
                if (!(indexType instanceof PrimitiveType))
                    warn(instruction, "Array index on stack is not a primitive");
                Value arrayRef = frame.pop();
                if (arrayRef instanceof Value.ArrayValue arrayValue) {
                    ArrayType arrayType = arrayValue.arrayType();
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
                ClassType stackType = frame.pop().type();
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

    private ClassType doArrayStore(SimpleInstruction instruction) {
        ClassType valueType = frame.pop().type();
        ClassType indexType = frame.pop().type();
        ClassType arrayType = frame.pop().type();
        if (!(indexType instanceof PrimitiveType))
            warn(instruction, "Array index on stack is not a primitive");
        if (!(arrayType instanceof ArrayType))
            warn(instruction, "Array reference on stack is not an array");
        return valueType;
    }

    @Override
    public void execute(ConstantInstruction<?> instruction) {
        Constant constant = instruction.constant();
        if (constant instanceof OfInt cInt) {
            frame.push(Values.valueOf(cInt.value()));
        } else if (constant instanceof OfLong cLong) {
            frame.push(Values.valueOf(cLong.value()));
        } else if (constant instanceof OfFloat cFloat) {
            frame.push(Values.valueOf(cFloat.value()));
        } else if (constant instanceof OfDouble cDouble) {
            frame.push(Values.valueOf(cDouble.value()));
        } else if (constant instanceof OfString cString) {
            frame.push(Values.valueOfString(cString.value()));
        } else if (constant instanceof OfMethodHandle mh) {
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
                ValuedLocal valuedLocal = frame.getLocals().get(index);
                Value value;
                if (valuedLocal == null) {
                    value = switch (opcode) {
                        case ILOAD -> Values.INT_VALUE;
                        case LLOAD -> Values.LONG_VALUE;
                        case FLOAD -> Values.FLOAT_VALUE;
                        case DLOAD -> Values.DOUBLE_VALUE;
                        case ALOAD -> Values.OBJECT_VALUE;
                        default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
                    };
                } else {
                    value = valuedLocal.value();
                }
                frame.push(value);
            }
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                String name = variableNameLookup.getVarName(index);
                Value value = opcode == LSTORE || opcode == DSTORE ? frame.pop2() : frame.pop();

                ClassType stackType = value.type();
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

                frame.setLocal(index, new ValuedLocal(index, name, value));
            }
        }
    }

    @Override
    public void execute(VariableIncrementInstruction instruction) {
        ValuedLocal local = frame.getLocal(instruction.variableIndex());
        if (local == null) {
            // create a new local with the increment value
            String name = variableNameLookup.getVarName(instruction.variableIndex());
            if (name == null) {
                error(instruction, "Invalid iinc target, not a recognized variable");
                return;
            }

            local = new ValuedLocal(instruction.variableIndex(), name, Values.valueOf(instruction.incrementBy()));
            frame.setLocal(instruction.variableIndex(), local);
        }

        // If the value is known, we can update it
        if (local.value() instanceof Value.KnownIntValue intValue) {
            ValuedLocal updatedLocal = new ValuedLocal(local, Values.valueOf(intValue.value() + instruction.incrementBy()));
            frame.setLocal(local.index(), updatedLocal);
        }
    }

    @Override
    public void execute(InstanceofInstruction instruction) {
        ClassType originType = frame.pop().type();
        ObjectType targetType = instruction.type();

        if (originType instanceof PrimitiveType) {
            warn(instruction, "Cannot instanceof primitive to reference");
            frame.pushType(Types.INT);
        } else if (checker != null && originType instanceof ObjectType objType) {
            String child = objType.internalName();
            String parent = targetType.internalName();
            frame.push(Values.valueOf(checker.isSubclassOf(child, parent)));
        } else {
            if (Objects.equals(originType, targetType))
                frame.push(Values.INT_1);
            else
                frame.pushType(Types.INT);
        }
    }

    @Override
    public void execute(CheckCastInstruction instruction) {
        Value originValue = frame.pop();
        ClassType originType = originValue.type();
        if (originType instanceof PrimitiveType)
            warn(instruction, "Cannot cast primitive to reference");

        ObjectType insnType = instruction.type();
        if (Objects.equals(originType, insnType)) {
            // re-use instance if possible
            frame.push(originValue);
        } else {
            Value valueOfType = Values.valueOf(insnType);
            frame.push(valueOfType);
        }
    }

    @Override
    public void execute(MethodInstruction instruction) {
        MethodType methodType = instruction.type();
        List<ClassType> types = methodType.parameterTypes();
        int size = types.size();

        boolean canLookup = true;
        List<Value> parameters = new ArrayList<>(size);

        for (int i = types.size(); i > 0; i--) {
            ClassType type = types.get(i - 1);
            Value value = frame.pop(type);
            parameters.add(0, value);
            canLookup &= value.isKnown();
            if (value instanceof Value.VoidValue)
                warn(instruction, "Cannot pass 'void' as method argument");
        }

        Value.ObjectValue contextObject = null;
        if (instruction.opcode() != INVOKESTATIC) {
            Value contextValue = frame.pop();

            if (contextValue instanceof Value.ObjectValue poppedContext) {
                contextObject = poppedContext;
                canLookup &= poppedContext.isKnown();
            }

            ClassType contextType = contextValue.type();
            if (contextType == null)
                warn(instruction, "Cannot invoke method of 'null' reference");
            else if (contextType instanceof PrimitiveType)
                warn(instruction, "Cannot invoke method on primitive");
            else if (contextType instanceof ArrayType)
                warn(instruction, "Cannot invoke method on array");
        }

        if (methodType.returnType() != Types.VOID) {
            if (canLookup && methodValueLookup != null) {
                // 3rd parties can register return values for known methods
                Value value = methodValueLookup.accept(instruction, contextObject, parameters);
                if (value != null) {
                    frame.push(value);
                } else {
                    // No value from lookup, use generic value of return type
                    frame.pushType(methodType.returnType());
                }
            } else {
                frame.pushType(methodType.returnType());
            }
        }
    }

    @Override
    public void execute(FieldInstruction instruction) {
        final int opcode = instruction.opcode();
        final ClassType type = instruction.type();
        switch (opcode) {
            case GETFIELD -> {
                Value contextValue = frame.pop();
                ClassType contextType = contextValue.type();
                if (contextType == null)
                    warn(instruction, "Cannot get field of 'null' reference");
                else if (contextType instanceof PrimitiveType)
                    warn(instruction, "Cannot get field of primitive");
                else if (contextType instanceof ArrayType)
                    warn(instruction, "Cannot get field of array");
                if (fieldValueLookup != null && contextValue.isKnown()
                        && contextValue instanceof Value.ObjectValue ov) {
                    // 3rd parties can register values for known fields
                    Value value = fieldValueLookup.accept(instruction, ov);
                    if (value != null) {
                        frame.push(value);
                    } else {
                        // No value from lookup, use generic value of field type
                        frame.pushType(type);
                    }
                } else {
                    frame.pushType(type);
                }
            }
            case GETSTATIC -> {
	            if (fieldValueLookup != null) {
                    // 3rd parties can register values for known fields
                    Value value = fieldValueLookup.accept(instruction, null);
                    if (value != null) {
                        frame.push(value);
                    } else {
                        // No value from lookup, use generic value of field type
                        frame.pushType(type);
                    }
                } else {
                    frame.pushType(type);
                }
            }
            case PUTFIELD -> {
                frame.pop(type);
                Value contextValue = frame.pop();
                ClassType contextType = contextValue.type();
                if (contextType == null)
                    warn(instruction, "Cannot put field on 'null' reference");
                else if (contextType instanceof PrimitiveType)
                    warn(instruction, "Cannot put field on primitive");
                else if (contextType instanceof ArrayType)
                    warn(instruction, "Cannot put field on array");
            }
            case PUTSTATIC -> frame.pop(type);
            default -> throw new IllegalStateException("Unknown field insn: " + opcode);
        }
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {
        MethodType type = (MethodType) instruction.type();
        List<ClassType> types = type.parameterTypes();
        for (int i = types.size(); i > 0; i--) {
            ClassType paramType = types.get(i - 1);
            frame.pop(paramType);
        }
        if (type.returnType() != Types.VOID)
            frame.pushType(type.returnType());
    }

    @Override
    public void execute(PrimitiveConversionInstruction instruction) {
        PrimitiveType targetType = instruction.to();
        Value fromValue = frame.pop(instruction.from());
        if (fromValue instanceof Value.PrimitiveValue primitiveValue) {
            frame.push(primitiveValue.cast(targetType));
        } else {
            ClassType type = fromValue.type();
            if (type == null)
                warn(instruction, "Cannot convert 'null' on stack to primitive");
            else if (type instanceof ObjectType)
                warn(instruction, "Cannot convert 'null' on stack to reference type");
            frame.pushType(targetType);
        }
    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {
        ClassType type = frame.pop().type();
        if (type == null)
            warn(instruction, "Cannot switch off 'null' on stack");
        else if (type instanceof ObjectType)
            warn(instruction, "Cannot switch off reference type on stack");
    }

    @Override
    public void execute(TableSwitchInstruction instruction) {
        ClassType type = frame.pop().type();
        if (type == null)
            warn(instruction, "Cannot switch off 'null' on stack");
        else if (type instanceof ObjectType)
            warn(instruction, "Cannot switch off reference type on stack");
    }

    private interface IntOp {
        int op(int a, int b);

        default void accept(@NotNull ValuedFrame frame, @NotNull Consumer<String> warningConsumer) {
            Value value1 = frame.pop();
            Value value2 = frame.pop();
            if (value1 instanceof Value.KnownIntValue int1 && value2 instanceof Value.KnownIntValue int2) {
                frame.push(Values.valueOf(op(int1.value(), int2.value())));
            } else {
                if (!(value1.type() instanceof PrimitiveType))
                    warningConsumer.accept("Top value is not a primitive");
                if (!(value2.type() instanceof PrimitiveType))
                    warningConsumer.accept("Bottom value is not a primitive");
                frame.pushType(Types.INT);
            }
        }
    }

    private interface FloatOp {
        float op(float a, float b);

        default void accept(@NotNull ValuedFrame frame, @NotNull Consumer<String> warningConsumer) {
            Value value1 = frame.pop();
            Value value2 = frame.pop();
            if (value1 instanceof Value.KnownFloatValue float1 && value2 instanceof Value.KnownFloatValue float2) {
                frame.push(Values.valueOf(op(float1.value(), float2.value())));
            } else {
                if (!(value1.type() instanceof PrimitiveType))
                    warningConsumer.accept("Top value is not a primitive");
                if (!(value2.type() instanceof PrimitiveType))
                    warningConsumer.accept("Bottom value is not a primitive");
                frame.pushType(Types.FLOAT);
            }
        }
    }

    private interface LongOp {
        long op(long a, long b);

        default void accept(@NotNull ValuedFrame frame, @NotNull Consumer<String> warningConsumer) {
            Value value1 = frame.pop2();
            Value value2 = frame.pop2();
            if (value1 instanceof Value.KnownLongValue long1 && value2 instanceof Value.KnownLongValue long2) {
                frame.push(Values.valueOf(op(long1.value(), long2.value())));
            } else {
                if (!(value1.type() instanceof PrimitiveType))
                    warningConsumer.accept("Top value is not a primitive");
                if (!(value2.type() instanceof PrimitiveType))
                    warningConsumer.accept("Bottom value is not a primitive");
                frame.pushType(Types.LONG);
            }
        }
    }

    private interface LongIntOp {
        long op(long a, int b);

        default void accept(@NotNull ValuedFrame frame, @NotNull Consumer<String> warningConsumer) {
            Value value1 = frame.pop();
            Value value2 = frame.pop2();
            if (value1 instanceof Value.KnownIntValue int1 && value2 instanceof Value.KnownLongValue long2) {
                frame.push(Values.valueOf(op(long2.value(), int1.value())));
            } else {
                if (!(value1.type() instanceof PrimitiveType))
                    warningConsumer.accept("Top value is not a primitive");
                if (!(value2.type() instanceof PrimitiveType))
                    warningConsumer.accept("Bottom value is not a primitive");
                frame.pushType(Types.LONG);
            }
        }
    }

    private interface DoubleOp {
        double op(double a, double b);

        default void accept(@NotNull ValuedFrame frame, @NotNull Consumer<String> warningConsumer) {
            Value value1 = frame.pop2();
            Value value2 = frame.pop2();
            if (value1 instanceof Value.KnownDoubleValue double1 && value2 instanceof Value.KnownDoubleValue double2) {
                frame.push(Values.valueOf(op(double1.value(), double2.value())));
            } else {
                if (!(value1.type() instanceof PrimitiveType))
                    warningConsumer.accept("Top value is not a primitive");
                if (!(value2.type() instanceof PrimitiveType))
                    warningConsumer.accept("Bottom value is not a primitive");
                frame.pushType(Types.DOUBLE);
            }
        }
    }
}

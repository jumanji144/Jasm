package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.assembler.compile.analysis.frame.TypedFrameOps;
import me.darknet.assembler.util.JvmTypeUtils;
import me.darknet.assembler.util.VarNaming;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * JVM engine which tracks types of items in the stack/locals.
 */
public class TypedJvmAnalysisEngine extends JvmAnalysisEngine<TypedFrame> {
    public TypedJvmAnalysisEngine(@NotNull VarCache varCache) {
        super(varCache);
    }

    @Override
    public FrameOps<?> newFrameOps() {
        return new TypedFrameOps();
    }

    @Override
    public void execute(@NotNull AbstractInsnNode instruction) {
	    switch (instruction) {
		    case InsnNode insnNode -> executeSimple(insnNode);
		    case IntInsnNode intInsnNode -> executeIntInsn(intInsnNode);
		    case LdcInsnNode ldcInsnNode -> executeLdcInsn(ldcInsnNode);
		    case VarInsnNode varInsnNode -> executeVarInsn(varInsnNode);
		    case IincInsnNode iincInsnNode -> executeIincInsn(iincInsnNode);
		    case TypeInsnNode typeInsnNode -> executeTypeInsn(typeInsnNode);
		    case MethodInsnNode methodInsnNode -> executeMethodInsn(methodInsnNode);
		    case FieldInsnNode fieldInsnNode -> executeFieldInsn(fieldInsnNode);
		    case InvokeDynamicInsnNode invokeDynamicInsnNode -> executeInvokeDynamicInsn(invokeDynamicInsnNode);
		    case JumpInsnNode jumpInsnNode -> executeJumpInsn(jumpInsnNode);
		    case LookupSwitchInsnNode lookupSwitchInsnNode -> executeSwitchInsn(lookupSwitchInsnNode);
		    case TableSwitchInsnNode tableSwitchInsnNode -> executeSwitchInsn(tableSwitchInsnNode);
		    case MultiANewArrayInsnNode multiANewArrayInsnNode -> executeMultiANewArrayInsn(multiANewArrayInsnNode);
		    default -> {}
	    }
    }

    private void executeSimple(@NotNull InsnNode instruction) {
        TypedFrame frame = frame();
        int opcode = instruction.getOpcode();
        switch (opcode) {
            case ICONST_M1 -> frame.pushType(JvmTypeUtils.INT);
            case ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> frame.pushType(JvmTypeUtils.INT);
            case LCONST_0, LCONST_1 -> frame.pushType(JvmTypeUtils.LONG);
            case FCONST_0, FCONST_1, FCONST_2 -> frame.pushType(JvmTypeUtils.FLOAT);
            case DCONST_0, DCONST_1 -> frame.pushType(JvmTypeUtils.DOUBLE);
            case DUP -> frame.pushType(frame.peek());
            case DUP_X1 -> {
                Type type1 = frame.pop();
                Type type2 = frame.pop();
                frame.pushTypes(type1, type2, type1);
            }
            case DUP_X2 -> {
                Type type1 = frame.pop();
                Type type2 = frame.pop();
                Type type3 = frame.pop();
                frame.pushTypes(type1, type3, type2, type1);
            }
            case DUP2 -> {
                Type type1 = frame.pop();
                Type type2 = frame.pop();
                frame.pushTypes(type2, type1, type2, type1);
            }
            case DUP2_X1 -> {
                Type type1 = frame.pop();
                Type type2 = frame.pop();
                Type type3 = frame.pop();
                frame.pushTypes(type2, type1, type3, type2, type1);
            }
            case DUP2_X2 -> {
                Type type1 = frame.pop();
                Type type2 = frame.pop();
                Type type3 = frame.pop();
                Type type4 = frame.pop();
                frame.pushTypes(type2, type1, type4, type3, type2, type1);
            }
            case POP, IRETURN, FRETURN, ARETURN, MONITORENTER, MONITOREXIT -> frame.pop();
            case POP2, LRETURN, DRETURN -> frame.pop2();
            case SWAP -> {
                Type type1 = frame.pop();
                Type type2 = frame.pop();
                frame.pushTypes(type1, type2);
            }
            case IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR, FCMPG, FCMPL -> {
                Type valueType1 = frame.pop();
                Type valueType2 = frame.pop();
                if (!JvmTypeUtils.isPrimitive(valueType1))
                    warn(instruction, "Top value to operate on is not a primitive");
                if (!JvmTypeUtils.isPrimitive(valueType2))
                    warn(instruction, "Bottom value to operate on is not a primitive");
                frame.pushType(JvmTypeUtils.INT);
            }
            case LADD, LSUB, LMUL, LDIV, LREM, LAND, LOR, LXOR -> {
                Type valueType1 = frame.pop2();
                Type valueType2 = frame.pop2();
                if (!JvmTypeUtils.isPrimitive(valueType1))
                    warn(instruction, "Top value to operate on is not a primitive");
                if (!JvmTypeUtils.isPrimitive(valueType2))
                    warn(instruction, "Bottom value to operate on is not a primitive");
                frame.pushType(JvmTypeUtils.LONG);
            }
            case LSHL, LSHR, LUSHR -> {
                Type offsetType = frame.pop();
                Type valueType = frame.pop2();
                if (!JvmTypeUtils.isPrimitive(offsetType))
                    warn(instruction, "Shift offset is not a primitive");
                if (!JvmTypeUtils.isPrimitive(valueType))
                    warn(instruction, "Shift target is not a primitive");
                frame.pushType(JvmTypeUtils.LONG);
            }
            case FADD, FSUB, FMUL, FDIV, FREM -> {
                Type valueType1 = frame.pop();
                Type valueType2 = frame.pop();
                if (!JvmTypeUtils.isPrimitive(valueType1))
                    warn(instruction, "Top value to operate on is not a primitive");
                if (!JvmTypeUtils.isPrimitive(valueType2))
                    warn(instruction, "Bottom value to operate on is not a primitive");
                frame.pushType(JvmTypeUtils.FLOAT);
            }
            case DADD, DSUB, DMUL, DDIV, DREM -> {
                Type valueType1 = frame.pop2();
                Type valueType2 = frame.pop2();
                if (!JvmTypeUtils.isPrimitive(valueType1))
                    warn(instruction, "Top value to operate on is not a primitive");
                if (!JvmTypeUtils.isPrimitive(valueType2))
                    warn(instruction, "Bottom value to operate on is not a primitive");
                frame.pushType(JvmTypeUtils.DOUBLE);
            }
            case DCMPL, DCMPG, LCMP -> {
                Type valueType1 = frame.pop2();
                Type valueType2 = frame.pop2();
                if (!JvmTypeUtils.isPrimitive(valueType1))
                    warn(instruction, "Top value to compare is not a primitive");
                if (!JvmTypeUtils.isPrimitive(valueType2))
                    warn(instruction, "Bottom value to compare is not a primitive");
                frame.pushType(JvmTypeUtils.INT);
            }
            case INEG -> unaryPrimitive(instruction, JvmTypeUtils.INT);
            case FNEG -> unaryPrimitive(instruction, JvmTypeUtils.FLOAT);
            case LNEG -> unaryWidePrimitive(instruction, JvmTypeUtils.LONG);
            case DNEG -> unaryWidePrimitive(instruction, JvmTypeUtils.DOUBLE);
            case ATHROW -> frame.getStack().clear();
            case ACONST_NULL -> frame.pushNull();
            case RETURN, NOP -> { }
            case I2L, I2F, I2D, I2B, I2C, I2S, F2I, F2L, F2D, L2I, L2F, L2D, D2I, D2L, D2F ->
                    executePrimitiveConversion(instruction);
            case AASTORE -> {
                Type valueType = doArrayStore(instruction);
                if (!JvmTypeUtils.isReference(valueType) && valueType != null)
                    warn(instruction, "Value to store in array is not a reference");
            }
            case IASTORE, FASTORE, BASTORE, CASTORE, SASTORE -> {
                Type valueType = doArrayStore(instruction);
                if (!JvmTypeUtils.isPrimitive(valueType))
                    warn(instruction, "Value to store in array is not a primitive");
            }
            case DASTORE, LASTORE -> {
                Type valueType = frame.pop2();
                Type indexType = frame.pop();
                Type arrayType = frame.pop();
                if (!JvmTypeUtils.isPrimitive(indexType))
                    warn(instruction, "Array index on stack is not a primitive");
                if (arrayType == null || arrayType.getSort() != Type.ARRAY)
                    warn(instruction, "Array reference on stack is not an array");
                if (!JvmTypeUtils.isPrimitive(valueType))
                    warn(instruction, "Value to store in array is not a primitive");
            }
            case IALOAD -> arrayLoad(instruction, JvmTypeUtils.INT);
            case FALOAD -> arrayLoad(instruction, JvmTypeUtils.FLOAT);
            case BALOAD -> arrayLoad(instruction, JvmTypeUtils.BYTE);
            case CALOAD -> arrayLoad(instruction, JvmTypeUtils.CHAR);
            case SALOAD -> arrayLoad(instruction, JvmTypeUtils.SHORT);
            case DALOAD -> arrayLoad(instruction, JvmTypeUtils.DOUBLE);
            case LALOAD -> arrayLoad(instruction, JvmTypeUtils.LONG);
            case AALOAD -> {
                Type indexType = frame.pop();
                if (!JvmTypeUtils.isPrimitive(indexType))
                    warn(instruction, "Array index on stack is not a primitive");
                Type arrayRef = frame.pop();
                if (arrayRef != null && arrayRef.getSort() == Type.ARRAY) {
                    if (arrayRef.getDimensions() == 1) {
                        frame.pushType(arrayRef.getElementType());
                    } else {
                        frame.pushType(Type.getType(arrayRef.getDescriptor().substring(1)));
                    }
                } else {
                    warn(instruction, "Array reference on stack is not an array");
                    frame.pushType(JvmTypeUtils.OBJECT);
                }
            }
            case ARRAYLENGTH -> {
                Type stackType = frame.pop();
                if (stackType == null)
                    warn(instruction, "Cannot get array length of 'null'");
                else if (JvmTypeUtils.isPrimitive(stackType))
                    warn(instruction, "Cannot get array length of primitive");
                else if (stackType.getSort() == Type.OBJECT)
                    warn(instruction, "Cannot get array length of non-array reference");
                frame.pushType(JvmTypeUtils.INT);
            }
            default -> throw new IllegalStateException("Unhandled simple insn: " + opcode);
        }
    }

    private void executeIntInsn(@NotNull IntInsnNode instruction) {
        if (instruction.getOpcode() == NEWARRAY) {
            frame().pop();
            frame().pushType(JvmTypeUtils.arrayType(switch (instruction.operand) {
                case T_BOOLEAN -> JvmTypeUtils.BOOLEAN;
                case T_CHAR -> JvmTypeUtils.CHAR;
                case T_FLOAT -> JvmTypeUtils.FLOAT;
                case T_DOUBLE -> JvmTypeUtils.DOUBLE;
                case T_BYTE -> JvmTypeUtils.BYTE;
                case T_SHORT -> JvmTypeUtils.SHORT;
                case T_INT -> JvmTypeUtils.INT;
                case T_LONG -> JvmTypeUtils.LONG;
                default -> throw new IllegalStateException("Unexpected newarray operand: " + instruction.operand);
            }));
            return;
        }
        frame().pushType(JvmTypeUtils.INT);
    }

    private void executeLdcInsn(@NotNull LdcInsnNode instruction) {
        Object value = instruction.cst;
        if (value instanceof Integer) {
            frame().pushType(JvmTypeUtils.INT);
        } else if (value instanceof Long) {
            frame().pushType(JvmTypeUtils.LONG);
        } else if (value instanceof Float) {
            frame().pushType(JvmTypeUtils.FLOAT);
        } else if (value instanceof Double) {
            frame().pushType(JvmTypeUtils.DOUBLE);
        } else if (value instanceof String) {
            frame().pushType(JvmTypeUtils.STRING);
        } else if (value instanceof org.objectweb.asm.Handle) {
	        // push java/lang/invoke/MethodHandle
	        frame().pushType(METHOD_HANDLE);
        } else if (value instanceof org.objectweb.asm.ConstantDynamic dynamic) {
            frame().pushType(Type.getType(dynamic.getDescriptor()));
        } else if (value instanceof Type type) {
	        // push java/lang/Class or java/lang/invoke/MethodType
	        frame().pushType(type.getSort() == Type.METHOD ? METHOD_TYPE : CLASS);
        }
    }

    private void executeVarInsn(@NotNull VarInsnNode instruction) {
        TypedFrame frame = frame();
        int index = instruction.var;
        int opcode = instruction.getOpcode();
        switch (opcode) {
            case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
                Type type = frame.getLocalType(index);
                if (type == null) {
                    type = switch (opcode) {
                        case ILOAD -> JvmTypeUtils.INT;
                        case LLOAD -> JvmTypeUtils.LONG;
                        case FLOAD -> JvmTypeUtils.FLOAT;
                        case DLOAD -> JvmTypeUtils.DOUBLE;
                        case ALOAD -> JvmTypeUtils.OBJECT;
                        default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
                    };
                }
                frame.pushType(type);
            }
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                String name = varCache.getVarName(index);
                Type stackType = JvmTypeUtils.isWide(JvmTypeUtils.primitiveFromStoreOpcode(opcode)) ? frame.pop2() : frame.pop();
                Type assumedType = switch (opcode) {
                    case ISTORE -> JvmTypeUtils.INT;
                    case LSTORE -> JvmTypeUtils.LONG;
                    case FSTORE -> JvmTypeUtils.FLOAT;
                    case DSTORE -> JvmTypeUtils.DOUBLE;
                    case ASTORE -> JvmTypeUtils.OBJECT;
                    default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
                };
                if (name == null) {
                    Type fallbackType = stackType != null ? stackType : assumedType;
                    name = VarNaming.name(index, fallbackType);
                    varCache.getOrCreate(name, index, JvmTypeUtils.isWide(assumedType)).updateTypeHint(fallbackType);
                }
                if (assumedType.equals(JvmTypeUtils.OBJECT)) {
                    if (JvmTypeUtils.isPrimitive(stackType))
                        warn(instruction, "Incorrect var assignment, " + JvmTypeUtils.displayName(stackType) + " into reference");
                } else if (stackType == null || !assumedType.equals(stackType)) {
                    warn(instruction, "Incorrect var assignment, " + JvmTypeUtils.displayName(stackType) + " into " + assumedType.getDescriptor());
                }
                frame.setLocal(index, new Local(index, name, stackType));
            }
        }
    }

    private void executeTypeInsn(@NotNull TypeInsnNode instruction) {
        switch (instruction.getOpcode()) {
            case NEW -> frame().pushType(Type.getObjectType(instruction.desc));
            case CHECKCAST -> {
                Type origin = frame().pop();
                if (JvmTypeUtils.isPrimitive(origin))
                    warn(instruction, "Cannot cast primitive to reference");
                frame().pushType(Type.getObjectType(instruction.desc));
            }
            case INSTANCEOF -> {
                Type origin = frame().pop();
                if (JvmTypeUtils.isPrimitive(origin))
                    warn(instruction, "Cannot instanceof primitive to reference");
                frame().pushType(JvmTypeUtils.INT);
            }
            case ANEWARRAY -> {
                frame().pop();
                frame().pushType(JvmTypeUtils.arrayType(Type.getObjectType(instruction.desc)));
            }
        }
    }

    private void executeMethodInsn(@NotNull MethodInsnNode instruction) {
        TypedFrame frame = frame();
        Type methodType = Type.getMethodType(instruction.desc);
        Type[] types = methodType.getArgumentTypes();
        for (int i = types.length; i > 0; i--) {
            frame.pop(types[i - 1]);
        }
        if (instruction.getOpcode() != INVOKESTATIC) {
            Type contextType = frame.pop();
            if (contextType == null)
                warn(instruction, "Cannot invoke method of 'null' reference");
            else if (JvmTypeUtils.isPrimitive(contextType))
                warn(instruction, "Cannot invoke method on primitive");
        }
        if (!methodType.getReturnType().equals(JvmTypeUtils.VOID))
            frame.pushType(methodType.getReturnType());
    }

    private void executeFieldInsn(@NotNull FieldInsnNode instruction) {
        TypedFrame frame = frame();
        int opcode = instruction.getOpcode();
        Type fieldType = Type.getType(instruction.desc);
        switch (opcode) {
            case GETFIELD -> {
                Type contextType = frame.pop();
                if (contextType == null)
                    warn(instruction, "Cannot get field of 'null' reference");
                else if (JvmTypeUtils.isPrimitive(contextType))
                    warn(instruction, "Cannot get field of primitive");
                else if (contextType.getSort() == Type.ARRAY)
                    warn(instruction, "Cannot get field of array");
                frame.pushType(fieldType);
            }
            case GETSTATIC -> frame.pushType(fieldType);
            case PUTFIELD, PUTSTATIC -> {
                Type valueType = frame.pop(fieldType);
                if (opcode == PUTFIELD) {
                    Type contextType = frame.pop();
                    if (contextType == null)
                        warn(instruction, "Cannot put field on 'null' reference");
                    else if (JvmTypeUtils.isPrimitive(contextType))
                        warn(instruction, "Cannot put field on primitive");
                    else if (contextType.getSort() == Type.ARRAY)
                        warn(instruction, "Cannot put field on array");
                }
                validateTypeUse(instruction, valueType, fieldType, "store", "field");
            }
            default -> throw new IllegalStateException("Unknown field insn: " + opcode);
        }
    }

    private void executeInvokeDynamicInsn(@NotNull InvokeDynamicInsnNode instruction) {
        TypedFrame frame = frame();
        Type methodType = Type.getMethodType(instruction.desc);
        Type[] types = methodType.getArgumentTypes();
        for (int i = types.length; i > 0; i--) {
            Type paramType = types[i - 1];
            Type valueType = frame.pop(paramType);
            validateTypeUse(instruction, valueType, paramType, "use", "parameter");
        }
        if (!methodType.getReturnType().equals(JvmTypeUtils.VOID))
            frame.pushType(methodType.getReturnType());
    }

    private void executeJumpInsn(@NotNull JumpInsnNode instruction) {
        switch (instruction.getOpcode()) {
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> frame().pop(1);
            case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> frame().pop(2);
        }
    }

    private void executeSwitchInsn(@NotNull AbstractInsnNode instruction) {
        Type type = frame().pop();
        if (type == null)
            warn(instruction, "Cannot switch off 'null' on stack");
        else if (JvmTypeUtils.isReference(type))
            warn(instruction, "Cannot switch off reference type on stack");
    }

    private void executeMultiANewArrayInsn(@NotNull MultiANewArrayInsnNode instruction) {
        int dimensions = instruction.dims;
        if (dimensions <= 0)
            warn(instruction, "multianewarray must have > 0 dimensions");
        frame().pop(dimensions);
        frame().pushType(Type.getType(instruction.desc));
    }

    private void executeIincInsn(@NotNull IincInsnNode instruction) {
        TypedFrame frame = frame();
        Local local = frame.getLocal(instruction.var);
        if (local == null) {
            String name = varCache.getVarName(instruction.var);
            if (name == null) {
                error(instruction, "Invalid iinc target, not a recognized variable");
                return;
            }
            frame.setLocal(instruction.var, new Local(instruction.var, name, JvmTypeUtils.INT));
        }
    }

    private void executePrimitiveConversion(@NotNull AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        Type fromType = switch (opcode) {
            case I2L, I2F, I2D, I2B, I2C, I2S -> JvmTypeUtils.INT;
            case F2I, F2L, F2D -> JvmTypeUtils.FLOAT;
            case L2I, L2F, L2D -> JvmTypeUtils.LONG;
            case D2I, D2L, D2F -> JvmTypeUtils.DOUBLE;
            default -> throw new IllegalStateException("Unexpected conversion opcode: " + opcode);
        };
        Type targetType = switch (opcode) {
            case I2L, F2L, D2L -> JvmTypeUtils.LONG;
            case I2F, L2F, D2F -> JvmTypeUtils.FLOAT;
            case I2D, L2D, F2D -> JvmTypeUtils.DOUBLE;
            case I2B -> JvmTypeUtils.BYTE;
            case I2C -> JvmTypeUtils.CHAR;
            case I2S -> JvmTypeUtils.SHORT;
            case F2I, L2I, D2I -> JvmTypeUtils.INT;
            default -> throw new IllegalStateException("Unexpected conversion opcode: " + opcode);
        };
        Type valueType = JvmTypeUtils.isWide(fromType) ? frame().pop2() : frame().pop();
        if (valueType == null) {
            warn(instruction, "Cannot convert 'null' on stack to primitive");
        } else if (JvmTypeUtils.isReference(valueType)) {
            warn(instruction, "Cannot convert reference on stack to primitive");
        } else if (!fromType.equals(valueType)) {
            warn(instruction, "Cannot convert " + valueType.getDescriptor() + " using " + fromType.getDescriptor() + " conversion");
        }
        frame().pushType(targetType);
    }

    private void unaryPrimitive(@NotNull AbstractInsnNode instruction, @NotNull Type pushedType) {
        Type valueType = frame().pop();
        if (!JvmTypeUtils.isPrimitive(valueType))
            warn(instruction, "Value to negate is not a primitive");
        frame().pushType(pushedType);
    }

    private void unaryWidePrimitive(@NotNull AbstractInsnNode instruction, @NotNull Type pushedType) {
        Type valueType = frame().pop2();
        if (!JvmTypeUtils.isPrimitive(valueType))
            warn(instruction, "Value to negate is not a primitive");
        frame().pushType(pushedType);
    }

    private void arrayLoad(@NotNull AbstractInsnNode instruction, @NotNull Type elementType) {
        Type indexType = frame().pop();
        Type arrayType = frame().pop();
        if (!JvmTypeUtils.isPrimitive(indexType))
            warn(instruction, "Array index on stack is not a primitive");
        if (arrayType == null || arrayType.getSort() != Type.ARRAY)
            warn(instruction, "Array reference on stack is not an array");
        frame().pushType(elementType);
    }

    private Type doArrayStore(@NotNull AbstractInsnNode instruction) {
        TypedFrame frame = frame();
        Type valueType = frame.pop();
        Type indexType = frame.pop();
        Type arrayType = frame.pop();
        if (!JvmTypeUtils.isPrimitive(indexType))
            warn(instruction, "Array index on stack is not a primitive");
        if (arrayType == null || arrayType.getSort() != Type.ARRAY)
            warn(instruction, "Array reference on stack is not an array");
        return valueType;
    }
}

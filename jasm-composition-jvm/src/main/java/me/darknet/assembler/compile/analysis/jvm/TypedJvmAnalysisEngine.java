package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.assembler.compile.analysis.frame.TypedFrameOps;
import me.darknet.assembler.util.JvmTypeUtils;
import me.darknet.assembler.util.VarNaming;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;

/**
 * JVM engine which tracks types of items in the stack/locals.
 */
public class TypedJvmAnalysisEngine extends JvmAnalysisEngine<TypedFrame> {
	public TypedJvmAnalysisEngine(@NotNull VarCache varCache) {
		super(varCache);
	}

	@Override
	public @NotNull FrameOps<?> newFrameOps() {
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
		TypedFrame frame = getCurrentFrame();
		int opcode = instruction.getOpcode();
		switch (opcode) {
			case ICONST_M1 -> frame.pushType(JvmTypeUtils.INT);
			case ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> frame.pushType(JvmTypeUtils.INT);
			case LCONST_0, LCONST_1 -> frame.pushType(JvmTypeUtils.LONG);
			case FCONST_0, FCONST_1, FCONST_2 -> frame.pushType(JvmTypeUtils.FLOAT);
			case DCONST_0, DCONST_1 -> frame.pushType(JvmTypeUtils.DOUBLE);
			case DUP -> {
				if (JvmTypeUtils.VOID.equals(frame.peek())) {
					warn(instruction, "dup cannot duplicate a category-2 value");
					ArrayList<Type> raw = new ArrayList<>(frame.getStack());
					if (raw.size() >= 2) {
						raw.add(2, raw.get(0));
						raw.add(3, raw.get(1));
						frame.getStack().clear();
						frame.getStack().addAll(raw);
					}
				} else {
					frame.pushType(frame.peek());
				}
			}
			case DUP_X1 -> {
				Type type1 = frame.pop();
				Type type2 = frame.pop();
				if (JvmTypeUtils.VOID.equals(type1) || JvmTypeUtils.VOID.equals(type2))
					warn(instruction, "dup_x1 requires two category-1 values");
				frame.pushTypes(type1, type2, type1);
			}
			case DUP_X2 -> {
				Type type1 = frame.pop();
				Type type2 = frame.pop();
				Type type3 = frame.pop();
				if (JvmTypeUtils.VOID.equals(type1))
					warn(instruction, "dup_x2 cannot duplicate a category-2 top value");
				frame.pushTypes(type1, type3, type2, type1);
			}
			case DUP2 -> {
				Type type1 = frame.pop();
				Type type2 = frame.pop();
				if (!JvmTypeUtils.VOID.equals(type1) && JvmTypeUtils.VOID.equals(type2))
					warn(instruction, "dup2 requires a category-2 value or two category-1 values");
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
			case POP -> {
				if (JvmTypeUtils.VOID.equals(frame.peek()))
					warn(instruction, "pop cannot remove a category-2 value");
				frame.pop();
			}
			case IRETURN, FRETURN, ARETURN -> {
				validateReturnValue(instruction, frame.pop(), analyzedReturnType());
				validateEmptyStack(instruction, frame);
			}
			case LRETURN, DRETURN -> {
				validateReturnValue(instruction, frame.pop2(), analyzedReturnType());
				validateEmptyStack(instruction, frame);
			}
			case MONITORENTER, MONITOREXIT -> {
				Type monitor = frame.pop();
				if (JvmTypeUtils.isUninitialized(monitor))
					warn(instruction, "Monitor value is uninitialized");
				else if (monitor != null && !JvmTypeUtils.isReference(monitor))
					warn(instruction, "Monitor value is not a reference");
			}
			case POP2 -> {
				Type top = frame.peek();
				frame.pop2();
				if (!JvmTypeUtils.VOID.equals(top) && !frame.getStack().isEmpty()
						&& JvmTypeUtils.VOID.equals(frame.peek()))
					warn(instruction, "pop2 requires a category-2 value or two category-1 values");
			}
			case SWAP -> {
				Type type1 = frame.pop();
				Type type2 = frame.pop();
				if (JvmTypeUtils.VOID.equals(type1) || JvmTypeUtils.VOID.equals(type2))
					warn(instruction, "swap requires two category-1 values");
				frame.pushTypes(type1, type2);
			}
			case IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR -> {
				Type valueType1 = frame.pop();
				Type valueType2 = frame.pop();
				requireArithmeticType(instruction, valueType1, JvmTypeUtils.INT, "Top");
				requireArithmeticType(instruction, valueType2, JvmTypeUtils.INT, "Bottom");
				frame.pushType(JvmTypeUtils.INT);
			}
			case LADD, LSUB, LMUL, LDIV, LREM, LAND, LOR, LXOR -> {
				Type valueType1 = frame.pop2();
				Type valueType2 = frame.pop2();
				requireArithmeticType(instruction, valueType1, JvmTypeUtils.LONG, "Top");
				requireArithmeticType(instruction, valueType2, JvmTypeUtils.LONG, "Bottom");
				frame.pushType(JvmTypeUtils.LONG);
			}
			case LSHL, LSHR, LUSHR -> {
				Type offsetType = frame.pop();
				Type valueType = frame.pop2();
				requireArithmeticType(instruction, offsetType, JvmTypeUtils.INT, "Shift offset");
				requireArithmeticType(instruction, valueType, JvmTypeUtils.LONG, "Shift target");
				frame.pushType(JvmTypeUtils.LONG);
			}
			case FADD, FSUB, FMUL, FDIV, FREM -> {
				Type valueType1 = frame.pop();
				Type valueType2 = frame.pop();
				requireArithmeticType(instruction, valueType1, JvmTypeUtils.FLOAT, "Top");
				requireArithmeticType(instruction, valueType2, JvmTypeUtils.FLOAT, "Bottom");
				frame.pushType(JvmTypeUtils.FLOAT);
			}
			case DADD, DSUB, DMUL, DDIV, DREM -> {
				Type valueType1 = frame.pop2();
				Type valueType2 = frame.pop2();
				requireArithmeticType(instruction, valueType1, JvmTypeUtils.DOUBLE, "Top");
				requireArithmeticType(instruction, valueType2, JvmTypeUtils.DOUBLE, "Bottom");
				frame.pushType(JvmTypeUtils.DOUBLE);
			}
			case DCMPL, DCMPG, LCMP -> {
				Type valueType1 = frame.pop2();
				Type valueType2 = frame.pop2();
				Type expected = opcode == LCMP ? JvmTypeUtils.LONG : JvmTypeUtils.DOUBLE;
				requireArithmeticType(instruction, valueType1, expected, "Top");
				requireArithmeticType(instruction, valueType2, expected, "Bottom");
				frame.pushType(JvmTypeUtils.INT);
			}
			case FCMPG, FCMPL -> {
				Type valueType1 = frame.pop();
				Type valueType2 = frame.pop();
				requireArithmeticType(instruction, valueType1, JvmTypeUtils.FLOAT, "Top");
				requireArithmeticType(instruction, valueType2, JvmTypeUtils.FLOAT, "Bottom");
				frame.pushType(JvmTypeUtils.INT);
			}
			case INEG -> unaryPrimitive(instruction, JvmTypeUtils.INT);
			case FNEG -> unaryPrimitive(instruction, JvmTypeUtils.FLOAT);
			case LNEG -> unaryWidePrimitive(instruction, JvmTypeUtils.LONG);
			case DNEG -> unaryWidePrimitive(instruction, JvmTypeUtils.DOUBLE);
			case ATHROW -> {
				Type thrown = frame.pop();
				if (thrown == null)
					warn(instruction, "Cannot throw 'null'");
				else if (JvmTypeUtils.isUninitialized(thrown) || !JvmTypeUtils.isReference(thrown))
					warn(instruction, "Thrown value is not a reference");
				frame.getStack().clear();
			}
			case ACONST_NULL -> frame.pushNull();
			case RETURN -> {
				validateReturnValue(instruction, null, analyzedReturnType());
				validateEmptyStack(instruction, frame);
			}
			case NOP -> {}
			case I2L, I2F, I2D, I2B, I2C, I2S, F2I, F2L, F2D, L2I, L2F, L2D, D2I, D2L, D2F ->
					executePrimitiveConversion(instruction);
			case AASTORE -> {
				doArrayStore(instruction, null);
			}
			case IASTORE, FASTORE, BASTORE, CASTORE, SASTORE -> {
				Type expected = switch (opcode) {
					case IASTORE -> JvmTypeUtils.INT;
					case FASTORE -> JvmTypeUtils.FLOAT;
					case BASTORE -> JvmTypeUtils.BYTE;
					case CASTORE -> JvmTypeUtils.CHAR;
					case SASTORE -> JvmTypeUtils.SHORT;
					default -> throw new IllegalStateException();
				};
				doArrayStore(instruction, expected);
			}
			case DASTORE, LASTORE -> {
				Type valueType = frame.pop2();
				Type indexType = frame.pop();
				Type arrayType = frame.pop();
				if (!JvmTypeUtils.isPrimitive(indexType))
					warn(instruction, "Array index on stack is not a primitive");
				if (arrayType == null || arrayType.getSort() != Type.ARRAY)
					warn(instruction, "Array reference on stack is not an array");
				else if (!JvmTypeUtils.verificationType(arrayType.getElementType()).equals(JvmTypeUtils.verificationType(valueType)))
					warn(instruction, "Array component is incompatible with stored value");
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
					if (JvmTypeUtils.isPrimitive(arrayRef.getElementType()))
						warn(instruction, "aaload requires an array of references");
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
			Type size = getCurrentFrame().pop();
			if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(size)))
				warn(instruction, "Array size is not an int");
			getCurrentFrame().pushType(JvmTypeUtils.arrayType(switch (instruction.operand) {
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
		getCurrentFrame().pushType(JvmTypeUtils.INT);
	}

	private void executeLdcInsn(@NotNull LdcInsnNode instruction) {
		Object value = instruction.cst;
		if (value instanceof Integer) {
			getCurrentFrame().pushType(JvmTypeUtils.INT);
		} else if (value instanceof Long) {
			getCurrentFrame().pushType(JvmTypeUtils.LONG);
		} else if (value instanceof Float) {
			getCurrentFrame().pushType(JvmTypeUtils.FLOAT);
		} else if (value instanceof Double) {
			getCurrentFrame().pushType(JvmTypeUtils.DOUBLE);
		} else if (value instanceof String) {
			getCurrentFrame().pushType(JvmTypeUtils.STRING);
		} else if (value instanceof org.objectweb.asm.Handle) {
			// push java/lang/invoke/MethodHandle
			getCurrentFrame().pushType(METHOD_HANDLE);
		} else if (value instanceof org.objectweb.asm.ConstantDynamic dynamic) {
			getCurrentFrame().pushType(Type.getType(dynamic.getDescriptor()));
		} else if (value instanceof Type type) {
			// push java/lang/Class or java/lang/invoke/MethodType
			getCurrentFrame().pushType(type.getSort() == Type.METHOD ? METHOD_TYPE : CLASS);
		}
	}

	private void executeVarInsn(@NotNull VarInsnNode instruction) {
		TypedFrame frame = getCurrentFrame();
		int index = instruction.var;
		int opcode = instruction.getOpcode();
		switch (opcode) {
			case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
				Type expected = switch (opcode) {
					case ILOAD -> JvmTypeUtils.INT;
					case LLOAD -> JvmTypeUtils.LONG;
					case FLOAD -> JvmTypeUtils.FLOAT;
					case DLOAD -> JvmTypeUtils.DOUBLE;
					case ALOAD -> JvmTypeUtils.OBJECT;
					default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
				};
				Local local = frame.getLocal(index);
				boolean undefined = local == null || JvmTypeUtils.isTop(local.type());
				if (undefined)
					warn(instruction, "Loading uninitialized local " + index);
				Type type = undefined ? expected : local.type();
				Type actual = JvmTypeUtils.verificationType(type);
				if (JvmTypeUtils.isPrimitive(expected) && !expected.equals(actual))
					warn(instruction, "Loading " + JvmTypeUtils.displayName(actual) + " as " + expected.getDescriptor());
				else if (expected.equals(JvmTypeUtils.OBJECT) && !JvmTypeUtils.isReference(actual) && actual != null)
					warn(instruction, "Loading non-object as object");
				frame.pushType(expected.equals(JvmTypeUtils.OBJECT) ? actual : expected);
			}
			case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
				String name = varCache.getVarName(index);
				int expectedStackSize = JvmTypeUtils.isWide(JvmTypeUtils.primitiveFromStoreOpcode(opcode)) ? 2 : 1;
				if (frame.getStack().size() < expectedStackSize)
					warnInvalidStoreStack(instruction, index, expectedStackSize, frame.getStack().size());
				Type stackType = expectedStackSize == 2 ? frame.pop2() : frame.pop();
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
				Type normalizedStackType = JvmTypeUtils.verificationType(stackType);
				if (assumedType.equals(JvmTypeUtils.OBJECT)) {
					if (JvmTypeUtils.isPrimitive(stackType))
						warn(instruction, "Incorrect var assignment, " + JvmTypeUtils.displayName(stackType) + " into reference");
				} else if (normalizedStackType == null || !assumedType.equals(normalizedStackType)) {
					warn(instruction, "Incorrect var assignment, " + JvmTypeUtils.displayName(stackType) + " into " + assumedType.getDescriptor());
				}
				boolean valid = assumedType.equals(JvmTypeUtils.OBJECT)
						? (stackType == null || JvmTypeUtils.isReference(stackType))
						: assumedType.equals(normalizedStackType);
				frame.setLocal(index, new Local(index, name, valid ? normalizedStackType : JvmTypeUtils.TOP));
			}
		}
	}

	private void executeTypeInsn(@NotNull TypeInsnNode instruction) {
		switch (instruction.getOpcode()) {
			case NEW -> getCurrentFrame().pushType(newUninitializedType(Type.getObjectType(instruction.desc), instruction));
			case CHECKCAST -> {
				Type origin = getCurrentFrame().pop();
				if (JvmTypeUtils.isUninitialized(origin))
					warn(instruction, "Cannot cast uninitialized object");
				else if (JvmTypeUtils.isPrimitive(origin))
					warn(instruction, "Cannot cast primitive to reference");
				getCurrentFrame().pushType(Type.getObjectType(instruction.desc));
			}
			case INSTANCEOF -> {
				Type origin = getCurrentFrame().pop();
				if (JvmTypeUtils.isUninitialized(origin))
					warn(instruction, "Cannot instanceof uninitialized object");
				else if (JvmTypeUtils.isPrimitive(origin))
					warn(instruction, "Cannot instanceof primitive to reference");
				getCurrentFrame().pushType(JvmTypeUtils.INT);
			}
			case ANEWARRAY -> {
				Type size = getCurrentFrame().pop();
				if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(size)))
					warn(instruction, "Array size is not an int");
				getCurrentFrame().pushType(JvmTypeUtils.arrayType(Type.getObjectType(instruction.desc)));
			}
		}
	}

	private void executeMethodInsn(@NotNull MethodInsnNode instruction) {
		TypedFrame frame = getCurrentFrame();
		Type methodType = Type.getMethodType(instruction.desc);
		Type[] types = methodType.getArgumentTypes();
		for (int i = types.length; i > 0; i--) {
			Type expected = types[i - 1];
			Type actual = frame.pop(expected);
			validateTypeUse(instruction, actual, expected, "use", "parameter");
		}
		if (instruction.getOpcode() != INVOKESTATIC) {
			Type contextType = frame.pop();
			Type owner = Type.getObjectType(instruction.owner);
			if ("<init>".equals(instruction.name) && JvmTypeUtils.isUninitialized(contextType)) {
				Type allocationOwner = JvmTypeUtils.uninitializedOwner(contextType);
				String currentOwner = analyzedOwner();
				boolean matching = allocationOwner.equals(owner)
						|| (isConstructor()
						&& currentOwner != null && currentOwner.equals(allocationOwner.getInternalName())
						&& checker != null
						&& checker.isSubclassOf(currentOwner, instruction.owner));
				if (!matching)
					warn(instruction, "Constructor does not match uninitialized object");
				else
					initializeAliases(contextType, allocationOwner);
			} else if ("<init>".equals(instruction.name)) {
				warn(instruction, "Constructor invoked on initialized object");
			} else {
				validateReceiver(instruction, contextType, owner, "invoke method");
			}
		}
		if (!methodType.getReturnType().equals(JvmTypeUtils.VOID))
			frame.pushType(methodType.getReturnType());
	}

	private void executeFieldInsn(@NotNull FieldInsnNode instruction) {
		TypedFrame frame = getCurrentFrame();
		int opcode = instruction.getOpcode();
		Type fieldType = Type.getType(instruction.desc);
		switch (opcode) {
			case GETFIELD -> {
				Type contextType = frame.pop();
				validateReceiver(instruction, contextType, Type.getObjectType(instruction.owner), "get field");
				frame.pushType(fieldType);
			}
			case GETSTATIC -> frame.pushType(fieldType);
			case PUTFIELD, PUTSTATIC -> {
				Type valueType = frame.pop(fieldType);
				if (opcode == PUTFIELD) {
					Type contextType = frame.pop();
					validateReceiver(instruction, contextType, Type.getObjectType(instruction.owner), "put field");
				}
				validateTypeUse(instruction, valueType, fieldType, "store", "field");
			}
			default -> throw new IllegalStateException("Unknown field insn: " + opcode);
		}
	}

	private void executeInvokeDynamicInsn(@NotNull InvokeDynamicInsnNode instruction) {
		TypedFrame frame = getCurrentFrame();
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
			case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE -> {
				Type value = getCurrentFrame().pop();
				if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(value)))
					warn(instruction, "Branch value is not an int");
			}
			case IFNULL, IFNONNULL -> {
				Type value = getCurrentFrame().pop();
				if (value != null && !JvmTypeUtils.isReference(value))
					warn(instruction, "Null branch value is not a reference");
			}
			case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE -> {
				Type first = getCurrentFrame().pop();
				Type second = getCurrentFrame().pop();
				if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(first)))
					warn(instruction, "Top branch value is not an int");
				if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(second)))
					warn(instruction, "Bottom branch value is not an int");
			}
			case IF_ACMPEQ, IF_ACMPNE -> {
				Type first = getCurrentFrame().pop();
				Type second = getCurrentFrame().pop();
				if (first != null && !JvmTypeUtils.isReference(first))
					warn(instruction, "Top branch value is not a reference");
				if (second != null && !JvmTypeUtils.isReference(second))
					warn(instruction, "Bottom branch value is not a reference");
			}
		}
	}

	private void executeSwitchInsn(@NotNull AbstractInsnNode instruction) {
		Type type = getCurrentFrame().pop();
		if (type == null)
			warn(instruction, "Cannot switch off 'null' on stack");
		else if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(type)))
			warn(instruction, "Cannot switch off non-int type on stack");
	}

	private void executeMultiANewArrayInsn(@NotNull MultiANewArrayInsnNode instruction) {
		int dimensions = instruction.dims;
		if (dimensions <= 0)
			warn(instruction, "multianewarray must have > 0 dimensions");
		for (int i = 0; i < dimensions; i++) {
			Type size = getCurrentFrame().pop();
			if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(size)))
				warn(instruction, "Array dimension is not an int");
		}
		Type arrayType = Type.getType(instruction.desc);
		if (arrayType.getDimensions() < dimensions)
			warn(instruction, "multianewarray dimensions exceed array dimensions");
		getCurrentFrame().pushType(arrayType);
	}

	private void executeIincInsn(@NotNull IincInsnNode instruction) {
		TypedFrame frame = getCurrentFrame();
		Local local = frame.getLocal(instruction.var);
		if (local == null) {
			String name = varCache.getVarName(instruction.var);
			if (name == null) {
				error(instruction, "Invalid iinc target, not a recognized variable");
				return;
			}
			warn(instruction, "Incrementing uninitialized local " + instruction.var);
			frame.setLocal(instruction.var, new Local(instruction.var, name, JvmTypeUtils.INT));
			return;
		}
		if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(local.type())))
			warn(instruction, "Invalid iinc target, local is not an int");
	}

	private void initializeAliases(@NotNull Type marker, @NotNull Type initializedType) {
		TypedFrame frame = getCurrentFrame();
		for (Local local : new ArrayList<>(frame.getLocals().values())) {
			if (marker.equals(local.type()))
				frame.setLocal(local.index(), new Local(local.index(), local.name(), initializedType));
		}
		ArrayList<Type> stack = new ArrayList<>(frame.getStack());
		for (int i = 0; i < stack.size(); i++)
			if (marker.equals(stack.get(i)))
				stack.set(i, initializedType);
		frame.getStack().clear();
		frame.getStack().addAll(stack);
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
		Type valueType = JvmTypeUtils.isWide(fromType) ? getCurrentFrame().pop2() : getCurrentFrame().pop();
		if (valueType == null) {
			warn(instruction, "Cannot convert 'null' on stack to primitive");
		} else if (JvmTypeUtils.isReference(valueType)) {
			warn(instruction, "Cannot convert reference on stack to primitive");
		} else if (!fromType.equals(valueType)) {
			warn(instruction, "Cannot convert " + valueType.getDescriptor() + " using " + fromType.getDescriptor() + " conversion");
		}
		getCurrentFrame().pushType(targetType);
	}

	private void unaryPrimitive(@NotNull AbstractInsnNode instruction, @NotNull Type pushedType) {
		Type valueType = getCurrentFrame().pop();
		if (!pushedType.equals(JvmTypeUtils.verificationType(valueType)))
			warn(instruction, "Value to negate is not a " + pushedType.getDescriptor());
		getCurrentFrame().pushType(pushedType);
	}

	private void requireArithmeticType(@NotNull AbstractInsnNode instruction, @Nullable Type actual,
	                                   @NotNull Type expected, @NotNull String position) {
		if (!expected.equals(JvmTypeUtils.verificationType(actual)))
			warn(instruction, position + " value is not a " + expected.getDescriptor());
	}

	private void unaryWidePrimitive(@NotNull AbstractInsnNode instruction, @NotNull Type pushedType) {
		Type valueType = getCurrentFrame().pop2();
		if (!pushedType.equals(JvmTypeUtils.verificationType(valueType)))
			warn(instruction, "Value to negate is not a " + pushedType.getDescriptor());
		getCurrentFrame().pushType(pushedType);
	}

	private void arrayLoad(@NotNull AbstractInsnNode instruction, @NotNull Type elementType) {
		Type indexType = getCurrentFrame().pop();
		Type arrayType = getCurrentFrame().pop();
		if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(indexType)))
			warn(instruction, "Array index on stack is not an int");
		if (arrayType == null || arrayType.getSort() != Type.ARRAY) {
			warn(instruction, "Array reference on stack is not an array");
		} else {
			Type actual = arrayType.getElementType();
			Type expected = JvmTypeUtils.verificationType(elementType);
			if (!expected.equals(JvmTypeUtils.verificationType(actual)))
				warn(instruction, "Array component is " + actual.getDescriptor()
						+ ", not " + elementType.getDescriptor());
		}
		getCurrentFrame().pushType(elementType);
	}

	private Type doArrayStore(@NotNull AbstractInsnNode instruction, @Nullable Type expectedValueType) {
		TypedFrame frame = getCurrentFrame();
		Type valueType = frame.pop();
		Type indexType = frame.pop();
		Type arrayType = frame.pop();
		if (!JvmTypeUtils.INT.equals(JvmTypeUtils.verificationType(indexType)))
			warn(instruction, "Array index on stack is not an int");
		if (arrayType == null || arrayType.getSort() != Type.ARRAY) {
			warn(instruction, "Array reference on stack is not an array");
		} else if (expectedValueType == null) {
			if (!JvmTypeUtils.isReference(valueType) && valueType != null)
				warn(instruction, "Value to store in array is not a reference");
		} else {
			validateTypeUse(instruction, valueType, arrayType.getElementType(), "store", "array component");
		}
		return valueType;
	}
}

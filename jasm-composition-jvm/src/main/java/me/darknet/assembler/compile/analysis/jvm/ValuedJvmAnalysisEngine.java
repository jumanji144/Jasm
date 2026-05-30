package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.ValuedLocal;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrameOps;
import me.darknet.assembler.util.JvmTypeUtils;
import me.darknet.assembler.util.VarNaming;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
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
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * JVM engine which tracks types and values of items in the stack/locals.
 */
public class ValuedJvmAnalysisEngine extends JvmAnalysisEngine<ValuedFrame> {
	private MethodValueLookup methodValueLookup;
	private FieldValueLookup fieldValueLookup;

	public ValuedJvmAnalysisEngine(@NotNull VarCache varCache) {
		super(varCache);
	}

	@Override
	public FrameOps<?> newFrameOps() {
		return new ValuedFrameOps();
	}

	public void setMethodValueLookup(MethodValueLookup methodValueLookup) {
		this.methodValueLookup = methodValueLookup;
	}

	public void setFieldValueLookup(FieldValueLookup fieldValueLookup) {
		this.fieldValueLookup = fieldValueLookup;
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
		ValuedFrame frame = frame();
		int opcode = instruction.getOpcode();
		switch (opcode) {
			case ICONST_M1 -> frame.push(Values.INT_M1);
			case ICONST_0 -> frame.push(Values.INT_0);
			case ICONST_1 -> frame.push(Values.INT_1);
			case ICONST_2 -> frame.push(Values.valueOf(2));
			case ICONST_3 -> frame.push(Values.valueOf(3));
			case ICONST_4 -> frame.push(Values.valueOf(4));
			case ICONST_5 -> frame.push(Values.valueOf(5));
			case LCONST_0 -> frame.push(Values.valueOf(0L));
			case LCONST_1 -> frame.push(Values.valueOf(1L));
			case FCONST_0 -> frame.push(Values.FLOAT_0);
			case FCONST_1 -> frame.push(Values.FLOAT_1);
			case FCONST_2 -> frame.push(Values.valueOf(2.0F));
			case DCONST_0 -> frame.push(Values.DOUBLE_0);
			case DCONST_1 -> frame.push(Values.DOUBLE_1);
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
				Value value = frame().pop();
				if (value instanceof Value.PrimitiveValue primitiveValue) {
					if (primitiveValue.isWide())
						warn(instruction, "Value negated is wide");
					else if (primitiveValue.isReserved())
						warn(instruction, "Value negated is top");
					frame().push(primitiveValue.negate());
				} else {
					frame().pushType(value.type());
					warn(instruction, "Value to negate is not a primitive");
				}
			}
			case LNEG, DNEG -> {
				Value value = frame().pop2();
				if (value instanceof Value.PrimitiveValue primitiveValue) {
					if (!primitiveValue.isWide())
						warn(instruction, "Value negated is not wide");
					else if (primitiveValue.isReserved())
						warn(instruction, "Value negated is top");
					frame().push(primitiveValue.negate());
				} else {
					frame().pushType(value.type());
					warn(instruction, "Value to negate is not a primitive");
				}
			}
			case IADD -> ((IntOp) Integer::sum).accept(frame, m -> warn(instruction, m));
			case ISUB -> ((IntOp) (a, b) -> b - a).accept(frame, m -> warn(instruction, m));
			case IMUL -> ((IntOp) (a, b) -> b * a).accept(frame, m -> warn(instruction, m));
			case IREM -> {
				Value value1 = frame.pop();
				Value value2 = frame.pop();
				if (value1 instanceof Value.KnownIntValue int1 && value2 instanceof Value.KnownIntValue int2) {
					int a = int1.value();
					int b = int2.value();
					frame.push(a == 0 ? Values.INT_VALUE : Values.valueOf(b % a));
				} else {
					primitiveBinaryFallback(instruction, value1, value2, JvmTypeUtils.INT);
				}
			}
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
					frame.push(a == 0 ? Values.INT_VALUE : Values.valueOf(b / a));
				} else {
					primitiveBinaryFallback(instruction, value1, value2, JvmTypeUtils.INT);
				}
			}
			case LADD -> ((LongOp) Long::sum).accept(frame, m -> warn(instruction, m));
			case LSUB -> ((LongOp) (a, b) -> b - a).accept(frame, m -> warn(instruction, m));
			case LMUL -> ((LongOp) (a, b) -> b * a).accept(frame, m -> warn(instruction, m));
			case LREM -> {
				Value value1 = frame.pop2();
				Value value2 = frame.pop2();
				if (value1 instanceof Value.KnownLongValue long1 && value2 instanceof Value.KnownLongValue long2) {
					long a = long1.value();
					long b = long2.value();
					frame.push(a == 0 ? Values.LONG_VALUE : Values.valueOf(b % a));
				} else {
					primitiveBinaryFallback(instruction, value1, value2, JvmTypeUtils.LONG);
				}
			}
			case LSHL -> ((LongIntOp) (a, b) -> a << b).accept(frame, m -> warn(instruction, m));
			case LSHR -> ((LongIntOp) (a, b) -> a >> b).accept(frame, m -> warn(instruction, m));
			case LUSHR -> ((LongIntOp) (a, b) -> a >>> b).accept(frame, m -> warn(instruction, m));
			case LAND -> ((LongOp) (a, b) -> b & a).accept(frame, m -> warn(instruction, m));
			case LOR -> ((LongOp) (a, b) -> b | a).accept(frame, m -> warn(instruction, m));
			case LXOR -> ((LongOp) (a, b) -> b ^ a).accept(frame, m -> warn(instruction, m));
			case LDIV -> {
				Value value1 = frame.pop2();
				Value value2 = frame.pop2();
				if (value1 instanceof Value.KnownLongValue long1 && value2 instanceof Value.KnownLongValue long2) {
					long a = long1.value();
					long b = long2.value();
					frame.push(a == 0 ? Values.LONG_VALUE : Values.valueOf(b / a));
				} else {
					primitiveBinaryFallback(instruction, value1, value2, JvmTypeUtils.LONG);
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
					frame.push(a == 0 ? Values.FLOAT_VALUE : Values.valueOf(b / a));
				} else {
					primitiveBinaryFallback(instruction, value1, value2, JvmTypeUtils.FLOAT);
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
					frame.push(a == 0 ? Values.DOUBLE_VALUE : Values.valueOf(b / a));
				} else {
					primitiveBinaryFallback(instruction, value1, value2, JvmTypeUtils.DOUBLE);
				}
			}
			case LCMP -> {
				Value value1 = frame.pop2();
				Value value2 = frame.pop2();
				if (value1 instanceof Value.KnownLongValue long1 && value2 instanceof Value.KnownLongValue long2) {
					frame.push(Values.valueOf(Long.compare(long2.value(), long1.value())));
				} else {
					primitiveBinaryFallback(instruction, value1, value2, JvmTypeUtils.INT);
				}
			}
			case FCMPL -> compareFloat(instruction, true);
			case FCMPG -> compareFloat(instruction, false);
			case DCMPL -> compareDouble(instruction, true);
			case DCMPG -> compareDouble(instruction, false);
			case ATHROW -> frame.getStack().clear();
			case ACONST_NULL -> frame.pushNull();
			case RETURN, NOP -> {}
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
				Type valueType = frame.pop2().type();
				Type indexType = frame.pop().type();
				Type arrayType = frame.pop().type();
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
				Type indexType = frame.pop().type();
				if (!JvmTypeUtils.isPrimitive(indexType))
					warn(instruction, "Array index on stack is not a primitive");
				Value arrayRef = frame.pop();
				if (arrayRef instanceof Value.ArrayValue arrayValue) {
					Type arrayType = arrayValue.arrayType();
					frame.pushType(arrayType.getDimensions() == 1
							? arrayType.getElementType()
							: Type.getType(arrayType.getDescriptor().substring(1)));
				} else {
					warn(instruction, "Array reference on stack is not an array");
					frame.pushType(JvmTypeUtils.OBJECT);
				}
			}
			case ARRAYLENGTH -> {
				Value arrayRef = frame.pop();
				Type stackType = arrayRef.type();
				if (stackType == null)
					warn(instruction, "Cannot get array length of 'null'");
				else if (JvmTypeUtils.isPrimitive(stackType))
					warn(instruction, "Cannot get array length of primitive");
				else if (stackType.getSort() == Type.OBJECT)
					warn(instruction, "Cannot get array length of non-array reference");
				if (arrayRef instanceof Value.KnownLengthArrayValue arrayValue) {
					frame.push(Values.valueOf(arrayValue.length()));
				} else {
					frame.pushType(JvmTypeUtils.INT);
				}
			}
			default -> throw new IllegalStateException("Unhandled simple insn: " + opcode);
		}
	}

	private void executeIntInsn(@NotNull IntInsnNode instruction) {
		if (instruction.getOpcode() == NEWARRAY) {
			Value size = frame().pop();
			Type componentType = switch (instruction.operand) {
				case T_BOOLEAN -> JvmTypeUtils.BOOLEAN;
				case T_CHAR -> JvmTypeUtils.CHAR;
				case T_FLOAT -> JvmTypeUtils.FLOAT;
				case T_DOUBLE -> JvmTypeUtils.DOUBLE;
				case T_BYTE -> JvmTypeUtils.BYTE;
				case T_SHORT -> JvmTypeUtils.SHORT;
				case T_INT -> JvmTypeUtils.INT;
				case T_LONG -> JvmTypeUtils.LONG;
				default -> throw new IllegalStateException("Unexpected newarray operand: " + instruction.operand);
			};
			Type arrayType = JvmTypeUtils.arrayType(componentType);
			if (size instanceof Value.KnownIntValue intValue) {
				frame().push(Values.valueOfArray(arrayType, intValue.value()));
			} else {
				frame().pushType(arrayType);
			}
			return;
		}
		frame().push(Values.valueOf(instruction.operand));
	}

	private void executeLdcInsn(@NotNull LdcInsnNode instruction) {
		Object constant = instruction.cst;
		if (constant instanceof Integer cInt) {
			frame().push(Values.valueOf(cInt));
		} else if (constant instanceof Long cLong) {
			frame().push(Values.valueOf(cLong));
		} else if (constant instanceof Float cFloat) {
			frame().push(Values.valueOf(cFloat));
		} else if (constant instanceof Double cDouble) {
			frame().push(Values.valueOf(cDouble));
		} else if (constant instanceof String cString) {
			frame().push(Values.valueOfString(cString));
		} else if (constant instanceof Handle) {
			frame().pushType(METHOD_HANDLE);
		} else if (constant instanceof ConstantDynamic dynamic) {
			frame().pushType(Type.getType(dynamic.getDescriptor()));
		} else if (constant instanceof Type type) {
			frame().pushType(type.getSort() == Type.METHOD ? METHOD_TYPE : CLASS);
		}
	}

	private void executeVarInsn(@NotNull VarInsnNode instruction) {
		ValuedFrame frame = frame();
		int index = instruction.var;
		int opcode = instruction.getOpcode();
		Type expectedVarType = switch (opcode) {
			case ILOAD, ISTORE -> JvmTypeUtils.INT;
			case LLOAD, LSTORE -> JvmTypeUtils.LONG;
			case FLOAD, FSTORE -> JvmTypeUtils.FLOAT;
			case DLOAD, DSTORE -> JvmTypeUtils.DOUBLE;
			case ALOAD, ASTORE -> JvmTypeUtils.OBJECT;
			default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
		};
		switch (opcode) {
			case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
				ValuedLocal valuedLocal = frame.getLocal(index);
				Value value = valuedLocal == null
						? switch (opcode) {
					case ILOAD -> Values.INT_VALUE;
					case LLOAD -> Values.LONG_VALUE;
					case FLOAD -> Values.FLOAT_VALUE;
					case DLOAD -> Values.DOUBLE_VALUE;
					case ALOAD -> Values.OBJECT_VALUE;
					default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
				}
						: valuedLocal.value();
				Type actualVarType = value.type();
				if (JvmTypeUtils.isPrimitive(expectedVarType)) {
					if (actualVarType == null) {
						warn(instruction, "Loading 'null' as " + expectedVarType.getDescriptor());
					} else if (!expectedVarType.equals(actualVarType)) {
						warn(instruction, "Loading " + actualVarType.getDescriptor() + " as " + expectedVarType.getDescriptor());
					}
				} else if (!JvmTypeUtils.isReference(actualVarType) && actualVarType != null) {
					warn(instruction, "Loading non-object as object");
				}
				frame.push(value);
			}
			case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
				String name = varCache.getVarName(index);
				Value value = JvmTypeUtils.isWide(expectedVarType) ? frame.pop2() : frame.pop();
				Type actualStackType = value.type();
				if (JvmTypeUtils.isPrimitive(expectedVarType)) {
					if (actualStackType == null) {
						warn(instruction, "Incorrect var assignment, 'null' into " + expectedVarType.getDescriptor());
					} else if (!expectedVarType.equals(actualStackType)) {
						warn(instruction, "Incorrect var assignment, " + actualStackType.getDescriptor() + " into " + expectedVarType.getDescriptor());
					}
				} else if (!JvmTypeUtils.isReference(actualStackType) && actualStackType != null) {
					warn(instruction, "Incorrect var assignment, " + actualStackType.getDescriptor() + " into object");
				}
				if (name == null) {
					Type fallbackType = actualStackType != null ? actualStackType : expectedVarType;
					name = VarNaming.name(index, fallbackType == null ? JvmTypeUtils.OBJECT : fallbackType);
					varCache.getOrCreate(name, index, JvmTypeUtils.isWide(expectedVarType)).updateTypeHint(fallbackType);
				}
				frame.setLocal(index, new ValuedLocal(index, name, value));
			}
		}
	}

	private void executeIincInsn(@NotNull IincInsnNode instruction) {
		ValuedFrame frame = frame();
		ValuedLocal local = frame.getLocal(instruction.var);
		if (local == null) {
			// create a new local with the increment value
			String name = varCache.getVarName(instruction.var);
			if (name == null) {
				error(instruction, "Invalid iinc target, not a recognized variable");
				return;
			}

			local = new ValuedLocal(instruction.var, name, Values.valueOf(instruction.incr));
			frame.setLocal(instruction.var, local);
		}

		// If the value is known, we can update it
		if (local.value() instanceof Value.KnownIntValue intValue) {
			ValuedLocal updatedLocal = new ValuedLocal(local, Values.valueOf(intValue.value() + instruction.incr));
			frame.setLocal(local.index(), updatedLocal);
		}
	}

	private void executeTypeInsn(@NotNull TypeInsnNode instruction) {
		ValuedFrame frame = frame();
		Type instructionType = Type.getObjectType(instruction.desc);
		switch (instruction.getOpcode()) {
			case NEW -> frame.pushType(instructionType);
			case CHECKCAST -> {
				Value originValue = frame.pop();
				Type originType = originValue.type();
				if (JvmTypeUtils.isPrimitive(originType))
					warn(instruction, "Cannot cast primitive to reference");
				if (Objects.equals(originType, instructionType)) {
					frame.push(originValue);
				} else {
					frame.push(Values.valueOf(instructionType));
				}
			}
			case INSTANCEOF -> {
				Type originType = frame.pop().type();
				if (JvmTypeUtils.isPrimitive(originType)) {
					warn(instruction, "Cannot instanceof primitive to reference");
					frame.pushType(JvmTypeUtils.INT);
				} else if (checker != null && originType != null && JvmTypeUtils.isReference(originType)) {
					frame.push(Values.valueOf(checker.isSubclassOf(
							JvmTypeUtils.internalName(originType),
							JvmTypeUtils.internalName(instructionType)
					)));
				} else {
					frame.push(Objects.equals(originType, instructionType) ? Values.INT_1 : Values.INT_VALUE);
				}
			}
			case ANEWARRAY -> {
				Value size = frame.pop();
				Type arrayType = JvmTypeUtils.arrayType(instructionType);
				if (size instanceof Value.KnownIntValue intValue) {
					frame.push(Values.valueOfArray(arrayType, intValue.value()));
				} else {
					frame.pushType(arrayType);
				}
			}
		}
	}

	private void executeMethodInsn(@NotNull MethodInsnNode instruction) {
		ValuedFrame frame = frame();
		Type methodType = Type.getMethodType(instruction.desc);
		Type[] argumentTypes = methodType.getArgumentTypes();

		boolean canLookup = true;
		List<Value> parameters = new ArrayList<>(argumentTypes.length);
		for (int i = argumentTypes.length; i > 0; i--) {
			Type argumentType = argumentTypes[i - 1];
			Value value = frame.pop(argumentType);
			parameters.add(0, value);
			canLookup &= value.isKnown();
			if (value instanceof Value.VoidValue)
				warn(instruction, "Cannot pass 'void' as method argument");
			else
				validateTypeUse(instruction, value.type(), argumentType, "use", "parameter");
		}

		Value.ObjectValue contextObject = null;
		if (instruction.getOpcode() != INVOKESTATIC) {
			Value contextValue = frame.pop();
			if (contextValue instanceof Value.ObjectValue poppedContext) {
				contextObject = poppedContext;
				canLookup &= poppedContext.isKnown();
			}

			Type contextType = contextValue.type();
			if (contextType == null)
				warn(instruction, "Cannot invoke method of 'null' reference");
			else if (JvmTypeUtils.isPrimitive(contextType))
				warn(instruction, "Cannot invoke method on primitive");
		}

		Type returnType = methodType.getReturnType();
		if (!returnType.equals(JvmTypeUtils.VOID)) {
			if (canLookup && methodValueLookup != null) {
				// 3rd parties can register return values for known methods
				Value value = methodValueLookup.accept(instruction, contextObject, parameters);
				if (value != null) {
					frame.push(value);
				} else {
					// No value from lookup, use generic value of return type
					frame.pushType(returnType);
				}
			} else {
				frame.pushType(returnType);
			}
		}
	}

	private void executeFieldInsn(@NotNull FieldInsnNode instruction) {
		ValuedFrame frame = frame();
		int opcode = instruction.getOpcode();
		Type fieldType = Type.getType(instruction.desc);
		switch (opcode) {
			case GETFIELD -> {
				Value contextValue = frame.pop();
				Type contextType = contextValue.type();
				if (contextType == null)
					warn(instruction, "Cannot get field of 'null' reference");
				else if (JvmTypeUtils.isPrimitive(contextType))
					warn(instruction, "Cannot get field of primitive");
				else if (contextType.getSort() == Type.ARRAY)
					warn(instruction, "Cannot get field of array");
				if (fieldValueLookup != null && contextValue.isKnown() && contextValue instanceof Value.ObjectValue ov) {
					// 3rd parties can register values for known fields
					Value value = fieldValueLookup.accept(instruction, ov);
					if (value != null) {
						frame.push(value);
					} else {
						// No value from lookup, use generic value of field type
						frame.pushType(fieldType);
					}
				} else {
					frame.pushType(fieldType);
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
						frame.pushType(fieldType);
					}
				} else {
					frame.pushType(fieldType);
				}
			}
			case PUTFIELD, PUTSTATIC -> {
				Value value = frame.pop(fieldType);
				Type valueType = value.type();

				// Validate field context value
				if (opcode == PUTFIELD) {
					Value contextValue = frame.pop();
					Type contextType = contextValue.type();
					if (contextType == null)
						warn(instruction, "Cannot put field on 'null' reference");
					else if (JvmTypeUtils.isPrimitive(contextType))
						warn(instruction, "Cannot put field on primitive");
					else if (contextType.getSort() == Type.ARRAY)
						warn(instruction, "Cannot put field on array");
				}

				// Value --> Field type checks
				validateTypeUse(instruction, valueType, fieldType, "store", "field");
			}
			default -> throw new IllegalStateException("Unknown field insn: " + opcode);
		}
	}

	private void executeInvokeDynamicInsn(@NotNull InvokeDynamicInsnNode instruction) {
		ValuedFrame frame = frame();
		Type methodType = Type.getMethodType(instruction.desc);
		Type[] argumentTypes = methodType.getArgumentTypes();
		for (int i = argumentTypes.length; i > 0; i--) {
			Type parameterType = argumentTypes[i - 1];
			Value value = frame.pop(parameterType);
			validateTypeUse(instruction, value.type(), parameterType, "use", "parameter");
		}
		if (!methodType.getReturnType().equals(JvmTypeUtils.VOID))
			frame.pushType(methodType.getReturnType());
	}

	private void executeJumpInsn(@NotNull JumpInsnNode instruction) {
		ValuedFrame frame = frame();
		switch (instruction.getOpcode()) {
			case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE -> {
				Value value = frame.pop();
				if (!(value instanceof Value.IntValue))
					warn(instruction, "Top value is not an int");
			}
			case IFNULL, IFNONNULL -> {
				Value value = frame.pop();
				if (!(value instanceof Value.ObjectValue))
					warn(instruction, "Top value is not an object");
			}
			case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE -> {
				Value value1 = frame.pop();
				Value value2 = frame.pop();
				if (!(value1 instanceof Value.IntValue))
					warn(instruction, "Top value is not an int");
				if (!(value2 instanceof Value.IntValue))
					warn(instruction, "Top-1 value is not an int");
			}
			case IF_ACMPEQ, IF_ACMPNE -> {
				Value value1 = frame.pop();
				Value value2 = frame.pop();
				if (!(value1 instanceof Value.ObjectValue))
					warn(instruction, "Top value is not an object");
				if (!(value2 instanceof Value.ObjectValue))
					warn(instruction, "Top-1 value is not an object");
			}
		}
	}

	private void executeSwitchInsn(@NotNull AbstractInsnNode instruction) {
		Type type = frame().pop().type();
		if (type == null)
			warn(instruction, "Cannot switch off 'null' on stack");
		else if (JvmTypeUtils.isReference(type))
			warn(instruction, "Cannot switch off reference type on stack");
	}

	private void executeMultiANewArrayInsn(@NotNull MultiANewArrayInsnNode instruction) {
		ValuedFrame frame = frame();
		int dimensions = instruction.dims;
		if (dimensions <= 0)
			warn(instruction, "multianewarray must have > 0 dimensions");
		for (int i = 0; i < dimensions; i++) {
			Value size = frame.pop();
			if (!(size instanceof Value.IntValue))
				warn(instruction, "Array size on stack is not an int");
		}
		frame.pushType(Type.getType(instruction.desc));
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
		Value fromValue = frame().pop(fromType);
		if (fromValue instanceof Value.PrimitiveValue primitiveValue) {
			frame().push(primitiveValue.cast(targetType));
		} else {
			Type type = fromValue.type();
			if (type == null)
				warn(instruction, "Cannot convert 'null' on stack to primitive");
			else if (JvmTypeUtils.isReference(type))
				warn(instruction, "Cannot convert reference on stack to primitive");
			frame().pushType(targetType);
		}
	}

	private void arrayLoad(@NotNull AbstractInsnNode instruction, @NotNull Type elementType) {
		Type indexType = frame().pop().type();
		Type arrayType = frame().pop().type();
		if (!JvmTypeUtils.isPrimitive(indexType))
			warn(instruction, "Array index on stack is not a primitive");
		if (arrayType == null || arrayType.getSort() != Type.ARRAY)
			warn(instruction, "Array reference on stack is not an array");
		frame().pushType(elementType);
	}

	private Type doArrayStore(@NotNull AbstractInsnNode instruction) {
		ValuedFrame frame = frame();
		Type valueType = frame.pop().type();
		Type indexType = frame.pop().type();
		Type arrayType = frame.pop().type();
		if (!JvmTypeUtils.isPrimitive(indexType))
			warn(instruction, "Array index on stack is not a primitive");
		if (arrayType == null || arrayType.getSort() != Type.ARRAY)
			warn(instruction, "Array reference on stack is not an array");
		return valueType;
	}

	private void primitiveBinaryFallback(@NotNull AbstractInsnNode instruction, @NotNull Value value1,
	                                     @NotNull Value value2, @NotNull Type resultType) {
		if (!JvmTypeUtils.isPrimitive(value1.type()))
			warn(instruction, "Top value to compare is not a primitive");
		if (!JvmTypeUtils.isPrimitive(value2.type()))
			warn(instruction, "Bottom value to compare is not a primitive");
		frame().pushType(resultType);
	}

	private void compareFloat(@NotNull AbstractInsnNode instruction, boolean nanIsMinusOne) {
		ValuedFrame frame = frame();
		Value value1 = frame.pop();
		Value value2 = frame.pop();
		if (value1 instanceof Value.KnownFloatValue float1 && value2 instanceof Value.KnownFloatValue float2) {
			float a = float1.value();
			float b = float2.value();
			if (Float.isNaN(a) || Float.isNaN(b)) {
				frame.push(Values.valueOf(nanIsMinusOne ? -1 : 1));
			} else {
				frame.push(Values.valueOf(Float.compare(b, a)));
			}
		} else {
			primitiveBinaryFallback(instruction, value1, value2, JvmTypeUtils.INT);
		}
	}

	private void compareDouble(@NotNull AbstractInsnNode instruction, boolean nanIsMinusOne) {
		ValuedFrame frame = frame();
		Value value1 = frame.pop2();
		Value value2 = frame.pop2();
		if (value1 instanceof Value.KnownDoubleValue double1 && value2 instanceof Value.KnownDoubleValue double2) {
			double a = double1.value();
			double b = double2.value();
			if (Double.isNaN(a) || Double.isNaN(b)) {
				frame.push(Values.valueOf(nanIsMinusOne ? -1 : 1));
			} else {
				frame.push(Values.valueOf(Double.compare(b, a)));
			}
		} else {
			primitiveBinaryFallback(instruction, value1, value2, JvmTypeUtils.INT);
		}
	}

	private interface IntOp {
		int op(int a, int b);

		default void accept(@NotNull ValuedFrame frame, @NotNull Consumer<String> warningConsumer) {
			Value value1 = frame.pop();
			Value value2 = frame.pop();
			if (value1 instanceof Value.KnownIntValue int1 && value2 instanceof Value.KnownIntValue int2) {
				frame.push(Values.valueOf(op(int1.value(), int2.value())));
			} else {
				if (!JvmTypeUtils.isPrimitive(value1.type()))
					warningConsumer.accept("Top value is not a primitive");
				if (!JvmTypeUtils.isPrimitive(value2.type()))
					warningConsumer.accept("Bottom value is not a primitive");
				frame.pushType(JvmTypeUtils.INT);
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
				if (!JvmTypeUtils.isPrimitive(value1.type()))
					warningConsumer.accept("Top value is not a primitive");
				if (!JvmTypeUtils.isPrimitive(value2.type()))
					warningConsumer.accept("Bottom value is not a primitive");
				frame.pushType(JvmTypeUtils.FLOAT);
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
				if (!JvmTypeUtils.isPrimitive(value1.type()))
					warningConsumer.accept("Top value is not a primitive");
				if (!JvmTypeUtils.isPrimitive(value2.type()))
					warningConsumer.accept("Bottom value is not a primitive");
				frame.pushType(JvmTypeUtils.LONG);
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
				if (!JvmTypeUtils.isPrimitive(value1.type()))
					warningConsumer.accept("Top value is not a primitive");
				if (!JvmTypeUtils.isPrimitive(value2.type()))
					warningConsumer.accept("Bottom value is not a primitive");
				frame.pushType(JvmTypeUtils.LONG);
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
				if (!JvmTypeUtils.isPrimitive(value1.type()))
					warningConsumer.accept("Top value is not a primitive");
				if (!JvmTypeUtils.isPrimitive(value2.type()))
					warningConsumer.accept("Bottom value is not a primitive");
				frame.pushType(JvmTypeUtils.DOUBLE);
			}
		}
	}
}

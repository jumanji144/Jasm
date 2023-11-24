package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.*;
import dev.xdark.blw.type.*;
import me.darknet.assembler.compile.analysis.*;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrameOps;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * JVM engine which tracks types and values of items in the stack/locals.
 */
public class ValuedJvmAnalysisEngine extends JvmAnalysisEngine<ValuedFrame> {
	private InheritanceChecker checker;
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
	 * @param checker
	 * 		Inheritance checker to use.
	 * 		Can be {@code null} to disable capabilities surrounding {@code instanceof} and casting.
	 */
	public void setChecker(InheritanceChecker checker) {
		this.checker = checker;
	}

	/**
	 * @param methodValueLookup
	 * 		Lookup for method return values.
	 * 		Can be {@code null} to assume unknown values of the return type.
	 */
	public void setMethodValueLookup(MethodValueLookup methodValueLookup) {
		this.methodValueLookup = methodValueLookup;
	}

	/**
	 * @param fieldValueLookup
	 * 		Lookup for field values.
	 * 		Can be {@code null} to assume unknown values of the field type.
	 */
	public void setFieldValueLookup(FieldValueLookup fieldValueLookup) {
		this.fieldValueLookup = fieldValueLookup;
	}

	@Override
	public void execute(SimpleInstruction instruction) {
		switch (instruction.opcode()) {
			case DUP -> frame.push(frame.peek());
			case DUP_X1 -> {
				Value value1 = frame.pop();
				Value value2 = frame.pop();
				frame.push(value1, value2, value1);
			}
			case DUP_X2 -> {
				Value value1 = frame.pop();
				Value value2 = frame.pop();
				Value value3 = frame.pop();
				frame.push(value1, value3, value2, value1);
			}
			case DUP2 -> {
				Value value1 = frame.pop();
				Value value2 = frame.pop();
				frame.push(value2, value1, value2, value1);
			}
			case DUP2_X1 -> {
				Value value1 = frame.pop();
				Value value2 = frame.pop();
				Value value3 = frame.pop();
				frame.push(value2, value1, value3, value2, value1);
			}
			case DUP2_X2 -> {
				Value value1 = frame.pop();
				Value value2 = frame.pop();
				Value value3 = frame.pop();
				Value value4 = frame.pop();
				frame.push(value2, value1, value4, value3, value2, value1);
			}
			case POP, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> frame.pop();
			case POP2 -> frame.pop2();
			case SWAP -> {
				Value value1 = frame.pop();
				Value value2 = frame.pop();
				frame.push(value1, value2);
			}
			case INEG, LNEG, FNEG, DNEG -> {
				Value value = frame.pop();
				if (value instanceof Value.PrimitiveValue primitiveValue)
					frame.push(primitiveValue.negate());
				else {
					frame.pushType(value.type());
					setAnalysisFailure(new AnalysisException(instruction, "Negate on non-primitive stack value"));
				}
			}
			case IADD -> ((IntOp) Integer::sum).accept(frame);
			case ISUB -> ((IntOp) (a, b) -> b - a).accept(frame);
			case IMUL -> ((IntOp) (a, b) -> b * a).accept(frame);
			case IREM -> ((IntOp) (a, b) -> b % a).accept(frame);
			case ISHL -> ((IntOp) (a, b) -> b << a).accept(frame);
			case ISHR -> ((IntOp) (a, b) -> b >> a).accept(frame);
			case IUSHR -> ((IntOp) (a, b) -> b >>> a).accept(frame);
			case IAND -> ((IntOp) (a, b) -> b & a).accept(frame);
			case IOR -> ((IntOp) (a, b) -> b | a).accept(frame);
			case IXOR -> ((IntOp) (a, b) -> b ^ a).accept(frame);
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
					frame.pushType(Types.INT);
				}
			}
			case LADD -> ((LongOp) Long::sum).accept(frame);
			case LSUB -> ((LongOp) (a, b) -> b - a).accept(frame);
			case LMUL -> ((LongOp) (a, b) -> b * a).accept(frame);
			case LREM -> ((LongOp) (a, b) -> b % a).accept(frame);
			case LSHL -> ((LongOp) (a, b) -> b << a).accept(frame);
			case LSHR -> ((LongOp) (a, b) -> b >> a).accept(frame);
			case LUSHR -> ((LongOp) (a, b) -> b >>> a).accept(frame);
			case LAND -> ((LongOp) (a, b) -> b & a).accept(frame);
			case LOR -> ((LongOp) (a, b) -> b | a).accept(frame);
			case LXOR -> ((LongOp) (a, b) -> b ^ a).accept(frame);
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
					frame.pushType(Types.LONG);
				}
			}
			case FADD -> ((FloatOp) Float::sum).accept(frame);
			case FSUB -> ((FloatOp) (a, b) -> b - a).accept(frame);
			case FMUL -> ((FloatOp) (a, b) -> b * a).accept(frame);
			case FREM -> ((FloatOp) (a, b) -> b % a).accept(frame);
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
					frame.pushType(Types.FLOAT);
				}
			}
			case DADD -> ((DoubleOp) Double::sum).accept(frame);
			case DSUB -> ((DoubleOp) (a, b) -> b - a).accept(frame);
			case DMUL -> ((DoubleOp) (a, b) -> b * a).accept(frame);
			case DREM -> ((DoubleOp) (a, b) -> b % a).accept(frame);
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
					frame.pushType(Types.DOUBLE);
				}
			}
			case ATHROW -> {
				Value value = frame.pop();
				if (value instanceof Value.NullValue) {
					frame.pushType(Types.type(NullPointerException.class));
				} else {
					frame.push(value);
				}
			}
			case ACONST_NULL -> frame.pushNull();
		}
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
			frame.pushType(Types.methodType(mh.value().type().descriptor()).returnType());
		} else if (constant instanceof OfDynamic dyn) {
			frame.pushType(dyn.value().type());
		} else if (constant instanceof OfType tp) {
			frame.pushType((ClassType) tp.value());
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
				Value value = opcode == LSTORE || opcode == DSTORE ?
						frame.pop2() :
						frame.pop();
				frame.setLocal(index, new ValuedLocal(index, name, value));
			}
		}
	}

	@Override
	public void execute(VariableIncrementInstruction instruction) {
		ValuedLocal local = frame.getLocal(instruction.variableIndex());
		if (local == null) {
			// Invalid iinc target
			setAnalysisFailure(new AnalysisException(instruction, "Invalid iinc target"));
			return;
		}

		// If the value is known, we can update it
		if (local.value() instanceof Value.KnownIntValue intValue) {
			frame.setLocal(0, new ValuedLocal(local, Values.valueOf(intValue.value() + instruction.incrementBy())));
		}
	}

	@Override
	public void execute(InstanceofInstruction instruction) {
		ClassType type = frame.pop().type();
		ObjectType targetType = instruction.type();

		if (checker != null && type instanceof ObjectType objType) {
			String child = objType.internalName();
			String parent = targetType.internalName();
			frame.push(Values.valueOf(checker.isSubclassOf(child, parent)));
		} else {
			if (type.equals(targetType))
				frame.push(Values.INT_1);
			else
				frame.pushType(Types.INT);
		}
	}

	@Override
	public void execute(CheckCastInstruction instruction) {
		// Doing this instead of no-op so if we have an empty stack we trigger an error
		Value value = frame.pop();
		frame.push(value);
	}

	@Override
	public void execute(MethodInstruction instruction) {
		MethodType methodType = instruction.type();
		List<ClassType> types = methodType.parameterTypes();
		int size = types.size();

		List<Value> parameters = new ArrayList<>(size);
		for (ClassType type : types)
			parameters.add(0, frame.pop(type));

		Value.ObjectValue context = null;
		if (instruction.opcode() != INVOKESTATIC && frame.pop() instanceof Value.ObjectValue poppedContext)
			context = poppedContext;

		if (methodType.returnType() != Types.VOID) {
			var lookup = methodValueLookup;
			if (lookup != null) {
				// 3rd parties can register return values for known methods
				Value value = lookup.accept(instruction, context, parameters);
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
			case GETFIELD, GETSTATIC -> {
				Value.ObjectValue context = null;
				if (opcode == GETFIELD && frame.pop() instanceof Value.ObjectValue poppedContext)
					context = poppedContext;
				var lookup = fieldValueLookup;
				if (lookup != null) {
					// 3rd parties can register values for known fields
					Value value = lookup.accept(instruction, context);
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
				frame.pop();
			}
			case PUTSTATIC -> frame.pop(type);
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
		PrimitiveType targetType = instruction.to();
		Value fromValue = frame.pop(instruction.from());
		if (fromValue instanceof Value.PrimitiveValue primitiveValue) {
			frame.push(primitiveValue.cast(targetType));
		} else {
			setAnalysisFailure(new AnalysisException(instruction, "Primitive conversion on non-primitive stack value"));
			frame.pushType(targetType);
		}
	}

	private interface IntOp {
		int op(int a, int b);

		default void accept(@NotNull ValuedFrame frame) {
			Value value1 = frame.pop();
			Value value2 = frame.pop();
			if (value1 instanceof Value.KnownIntValue int1 && value2 instanceof Value.KnownIntValue int2) {
				frame.push(Values.valueOf(op(int1.value(), int2.value())));
			} else {
				frame.pushType(Types.INT);
			}
		}
	}

	private interface FloatOp {
		float op(float a, float b);

		default void accept(@NotNull ValuedFrame frame) {
			Value value1 = frame.pop();
			Value value2 = frame.pop();
			if (value1 instanceof Value.KnownFloatValue float1 && value2 instanceof Value.KnownFloatValue float2) {
				frame.push(Values.valueOf(op(float1.value(), float2.value())));
			} else {
				frame.pushType(Types.FLOAT);
			}
		}
	}

	private interface LongOp {
		long op(long a, long b);

		default void accept(@NotNull ValuedFrame frame) {
			Value value1 = frame.pop2();
			Value value2 = frame.pop2();
			if (value1 instanceof Value.KnownLongValue long1 && value2 instanceof Value.KnownLongValue long2) {
				frame.push(Values.valueOf(op(long1.value(), long2.value())));
			} else {
				frame.pushType(Types.LONG);
			}
		}
	}

	private interface DoubleOp {
		double op(double a, double b);

		default void accept(@NotNull ValuedFrame frame) {
			Value value1 = frame.pop2();
			Value value2 = frame.pop2();
			if (value1 instanceof Value.KnownDoubleValue double1 && value2 instanceof Value.KnownDoubleValue double2) {
				frame.push(Values.valueOf(op(double1.value(), double2.value())));
			} else {
				frame.pushType(Types.DOUBLE);
			}
		}
	}
}

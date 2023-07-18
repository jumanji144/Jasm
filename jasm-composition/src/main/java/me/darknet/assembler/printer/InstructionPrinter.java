package me.darknet.assembler.printer;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.*;
import dev.xdark.blw.simulation.ExecutionEngine;
import dev.xdark.blw.type.*;
import me.darknet.assembler.printer.util.LabelUtil;

import java.util.HashMap;
import java.util.Map;

public class InstructionPrinter implements ExecutionEngine {

	private final static String[] OPCODES = new String[256];
	private static final Map<Integer, String> HANDLE_TYPES = Map.of(
			1, "getfield",
			2, "getstatic",
			3, "putfield",
			4, "putstatic",
			5, "invokevirtual",
			6, "invokestatic",
			7, "invokespecial",
			8, "newinvokespecial",
			9, "invokeinterface"
	);

	static {
		for (var field : Opcodes.class.getFields()) {
			try {
				OPCODES[field.getInt(null)] = field.getName().toLowerCase();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	protected PrintContext.CodePrint ctx;
	protected Code code;
	protected Map<Integer, String> labelNames = new HashMap<>();
	protected Names names;
	protected int count;

	public InstructionPrinter(PrintContext.CodePrint ctx, Code code, Names names) {
		this.ctx = ctx;
		this.code = code;
		this.names = names;

		// map labels
		int index = 0;
		for (CodeElement codeElement : code.codeList()) {
			if(codeElement instanceof Label label) {
				labelNames.put(label.index(), LabelUtil.getLabelName(index++));
			}
		}
	}

	void printMethodHandle(MethodHandle handle) {
		var array = ctx.array(3);
		array.print(HANDLE_TYPES.get(handle.kind()))
				.arg()
					.literal(handle.owner().internalName())
					.literal(".")
					.literal(handle.name())
				.arg()
					.literal(handle.type().descriptor())
				.end();
	}

	class ConstantPrinter implements ConstantSink {

		protected final PrintContext<?> ctx;

		public ConstantPrinter(PrintContext<?> ctx) {
			this.ctx = ctx;
		}

		@Override
		public void acceptString(OfString value) {
			ctx.string(value.value());
		}

		@Override
		public void acceptMethodHandle(OfMethodHandle value) {
			printMethodHandle(value.value());
		}

		@Override
		public void acceptType(OfType value) {
			Type type = value.value();
			if(type instanceof ObjectType ct) {
				ctx.print("L").literal(ct.internalName()).literal(";");
			} else {
				ctx.literal(type.descriptor());
			}
		}

		@Override
		public void acceptDynamic(OfDynamic value) {
			ConstantDynamic dynamic = value.value();
			var array = ctx.array(4);
			array.literal(dynamic.name())
					.arg()
						.literal(dynamic.type().descriptor())
					.arg();
			printMethodHandle(dynamic.methodHandle());
			var bsmArray = array.arg().array(dynamic.args().size());
			ConstantPrinter printer = new ConstantPrinter(bsmArray);
			for (var arg : dynamic.args()) {
				arg.accept(printer);
			}
			bsmArray.end();
			array.end();
		}

		@Override
		public void acceptLong(OfLong value) {
			ctx.print(String.valueOf(value.value())).print("L");
		}

		@Override
		public void acceptDouble(OfDouble value) {
			ctx.print(String.valueOf(value.value())).print("D");
		}

		@Override
		public void acceptInt(OfInt value) {
			ctx.print(String.valueOf(value.value()));
		}

		@Override
		public void acceptFloat(OfFloat value) {
			ctx.print(String.valueOf(value.value())).print("F");
		}

	}

	@Override
	public void label(Label label) {
		ctx.begin()
				.print(labelNames.get(label.index()))
				.print(":")
				.indent() // simulate indentation, to offset new line indentation
				.next();
	}

	@Override
	public void execute(SimpleInstruction instruction) {
		ctx.instruction(OPCODES[instruction.opcode()]).next();
	}

	@Override
	public void execute(ConstantInstruction<?> instruction) {
		ctx.instruction("ldc");
		instruction.constant().accept(new ConstantPrinter(ctx));
		ctx.next();
	}

	@Override
	public void execute(VarInstruction instruction) {
		ctx.instruction(OPCODES[instruction.opcode()])
				.literal(names.getName(instruction.variableIndex(), code.codeList().indexOf(instruction)))
				.next();
	}

	@Override
	public void execute(LookupSwitchInstruction instruction) {
		var obj = ctx.instruction("lookupswitch").object(1 + instruction.targets().size());
		obj.value("default").print(labelNames.get(instruction.defaultTarget().index())).next();
		for (int i = 0; i < instruction.keys().length; i++) {
			Label target = instruction.targets().get(i);
			int key = instruction.keys()[i];
			obj.value(String.valueOf(key)).print(labelNames.get(target.index())).next();
		}
		obj.indent().end();
		ctx.unindent().newline();
	}

	@Override
	public void execute(TableSwitchInstruction instruction) {
		var obj = ctx.instruction("tableswitch").object(4);
		obj.value("min").print(String.valueOf(instruction.min())).next();
		obj.value("max").print(String.valueOf(instruction.min() + instruction.targets().size())).next();
		var arr = obj.value("targets").array(instruction.targets().size());
		for (Label target : instruction.targets()) {
			arr.print(labelNames.get(target.index())).arg();
		}
		arr.end();
		obj.value("default").print(labelNames.get(instruction.defaultTarget().index()));
	}

	@Override
	public void execute(InstanceofInstruction instruction) {
		ctx.instruction("instanceof").literal(instruction.type().descriptor()).next();
	}

	@Override
	public void execute(CheckCastInstruction instruction) {
		ctx.instruction("checkcast").literal(instruction.type().descriptor()).next();
	}

	@Override
	public void execute(AllocateInstruction instruction) {
		Type type = instruction.type();
		if (type instanceof InstanceType instance) {
			ctx.instruction("new").literal(instance.internalName()).next();
		} else {
			ArrayType arrayType = (ArrayType) type;
			String descriptor = arrayType.descriptor();
			int dimensions = arrayType.dimensions();
			if (dimensions == 1) {
				ClassType component = arrayType.componentType();
				if (!(component instanceof PrimitiveType primitiveType)) {
					ctx.instruction("anewarray").literal(component.descriptor()).next();
				} else {
					ctx.instruction("newarray").print(primitiveType.name()).next();
				}
			} else {
				ctx.instruction("multianewarray").literal(descriptor).arg()
						.print(Integer.toString(dimensions)).next();
			}
		}
	}

	@Override
	public void execute(MethodInstruction instruction) {
		String opcode = OPCODES[instruction.opcode()];
		if(instruction.isInterface() && instruction.opcode() != Opcodes.INVOKEINTERFACE) {
			opcode += "interface";
		}
		ctx.instruction(opcode)
				.literal(instruction.owner().internalName())
				.print(".")
				.literal(instruction.name())
				.print(" ")
				.literal(instruction.type().descriptor())
				.next();
	}

	@Override
	public void execute(FieldInstruction instruction) {
		ctx.instruction(OPCODES[instruction.opcode()])
				.literal(instruction.owner().internalName())
				.print(".")
				.literal(instruction.name())
				.print(" ")
				.literal(instruction.type().descriptor())
				.next();
	}

	@Override
	public void execute(InvokeDynamicInstruction instruction) {
		ctx.instruction("invokedynamic").literal(instruction.name())
				.arg()
				.literal(instruction.type().descriptor())
				.arg();
		printMethodHandle(instruction.bootstrapHandle());
		var bsmArray = ctx.arg().array(instruction.args().size());
		ConstantPrinter printer = new ConstantPrinter(bsmArray);
		for (var arg : instruction.args()) {
			arg.accept(printer);
		}
		bsmArray.end();
	}

	@Override
	public void execute(ImmediateJumpInstruction instruction) {
		ctx.instruction(OPCODES[instruction.opcode()])
				.print(labelNames.get(instruction.target().index()))
				.next();
	}

	@Override
	public void execute(ConditionalJumpInstruction instruction) {
		ctx.instruction(OPCODES[instruction.opcode()])
				.print(labelNames.get(instruction.target().index()))
				.next();
	}

	@Override
	public void execute(VariableIncrementInstruction instruction) {
		ctx.instruction(OPCODES[instruction.opcode()])
				.arg()
				.literal(names.getName(instruction.variableIndex(), code.codeList().indexOf(instruction)))
				.arg()
				.literal(Integer.toString(instruction.incrementBy()))
				.next();
	}

	@Override
	public void execute(Instruction instruction) {

	}
}

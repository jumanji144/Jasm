package me.darknet.assembler.printer.impl.asm;

import me.darknet.assembler.printer.Names;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.util.LabelUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.tree.AbstractInsnNode.*;

public class ASMInstructionPrinter implements Opcodes {

	private static final String[] OPCODES = new String[IFNONNULL + 1];
	private static final Map<Integer, String> NEWARRAY_TYPES = Map.of(
			T_BOOLEAN, "boolean",
			T_CHAR, "char",
			T_FLOAT, "float",
			T_DOUBLE, "double",
			T_BYTE, "byte",
			T_SHORT, "short",
			T_INT, "int",
			T_LONG, "long"
	);
	private static final Map<Integer, String> HANDLE_TYPES = Map.of(
			H_GETSTATIC, "getstatic",
			H_PUTSTATIC, "putstatic",
			H_GETFIELD, "getfield",
			H_PUTFIELD, "putfield",
			H_INVOKEVIRTUAL, "invokevirtual",
			H_INVOKESTATIC, "invokestatic",
			H_INVOKESPECIAL, "invokespecial",
			H_NEWINVOKESPECIAL, "newinvokespecial",
			H_INVOKEINTERFACE, "invokeinterface"
	);

	static {
		try {
			Field[] fields = Opcodes.class.getDeclaredFields();
			boolean foundBase = false;
			for (Field field : fields) {
				if (field.getName().equals("NOP")) {
					foundBase = true;
				}
				if (foundBase) {
					OPCODES[(Integer) field.get(null)] = field.getName().toLowerCase();
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private final Names localNames;
	private final InsnList instructions;

	public ASMInstructionPrinter(InsnList instructions, Names localNames) {
		this.instructions = instructions;
		this.localNames = localNames;
	}

	void printHandle(Handle handle, PrintContext<?> insn) {
		var arr = insn.array(3);
		arr.print(HANDLE_TYPES.get(handle.getTag()));
		arr.arg().literal(handle.getOwner()).print(".").literal(handle.getName());
		arr.arg().literal(handle.getDesc());
		arr.end();
	}

	void printConstant(Object cst, PrintContext<?> insn) {
		if (cst instanceof String) {
			insn.string((String) cst);
		} else if (cst instanceof Number) {
			insn.print(cst.toString());
		} else if (cst instanceof Type) {
			Type type = (Type) cst;
			switch (type.getSort()) {
				case Type.OBJECT: {
					insn.print("L").literal(type.getInternalName()).print(";");
					break;
				}
				case Type.METHOD: {
					insn.literal(type.getDescriptor());
					break;
				}
				default: {
					throw new IllegalArgumentException("Unknown type: " + type);
				}
			}
		} else if (cst instanceof Handle) {
			printHandle((Handle) cst, insn);
		} else {
			throw new IllegalArgumentException("Unknown constant: " + cst);
		}
	}

	public void print(PrintContext.CodePrint ctx) {
		Map<Integer, String> labels = new HashMap<>();
		int currentLabel = 0;
		for (AbstractInsnNode instruction : instructions) {
			if (instruction.getType() == LABEL) {
				LabelNode node = (LabelNode) instruction;
				labels.put(instructions.indexOf(node), LabelUtil.getLabelName(currentLabel++));
			}
		}
		int count = 0;
		for (AbstractInsnNode instruction : instructions) {
			int op = instruction.getOpcode();
			if (instruction.getType() == LABEL) {
				LabelNode node = (LabelNode) instruction;
				ctx.print(ctx.getIndent()).print(labels.get(instructions.indexOf(node))).print(":");
				ctx.indent().next(count++);
				continue;
			}
			if(instruction.getType() == LINE) {
				LineNumberNode node = (LineNumberNode) instruction;
				ctx.instruction("line")
						.arg().print(labels.get(instructions.indexOf(node.start)))
						.arg().print(Integer.toString(node.line))
						.next(count++);
				continue;
			}
			if (instruction.getOpcode() >= 0) {
				String opcode = OPCODES[instruction.getOpcode()];
				var insn = ctx.instruction(opcode);
				switch (instruction.getType()) {
					case INSN: {
						break;
					}
					case INT_INSN: {
						IntInsnNode node = (IntInsnNode) instruction;
						String elem = Integer.toString(node.operand);
						if (op == NEWARRAY) {
							elem = NEWARRAY_TYPES.get(node.operand);
						}
						insn.arg().element(elem);
						break;
					}
					case VAR_INSN: {
						VarInsnNode node = (VarInsnNode) instruction;
						String local = localNames.getName(node.var, instructions.indexOf(instruction));
						insn.arg().literal(local);
						break;
					}
					case TYPE_INSN: {
						TypeInsnNode node = (TypeInsnNode) instruction;
						insn.arg().literal(node.desc);
						break;
					}
					case FIELD_INSN: {
						FieldInsnNode node = (FieldInsnNode) instruction;
						insn.arg().literal(node.owner).print(".").literal(node.name);
						insn.arg().literal(node.desc);
						break;
					}
					case METHOD_INSN: {
						MethodInsnNode node = (MethodInsnNode) instruction;
						insn.arg().literal(node.owner).print(".").literal(node.name);
						insn.arg().literal(node.desc);
						break;
					}
					case INVOKE_DYNAMIC_INSN: {
						InvokeDynamicInsnNode node = (InvokeDynamicInsnNode) instruction;
						insn.arg().literal(node.name).print(" ").literal(node.desc);
						printHandle(node.bsm, insn);
						var arr = insn.arg().array(node.bsmArgs.length);
						for (Object arg : node.bsmArgs) {
							printConstant(arg, arr);
							arr.arg();
						}
						arr.end();
						break;
					}
					case JUMP_INSN: {
						JumpInsnNode node = (JumpInsnNode) instruction;
						insn.arg().print(labels.get(instructions.indexOf(node.label)));
						break;
					}
					case LDC_INSN: {
						LdcInsnNode node = (LdcInsnNode) instruction;
						printConstant(node.cst, insn.arg());
						break;
					}
					case IINC_INSN: {
						IincInsnNode node = (IincInsnNode) instruction;
						String local = localNames.getName(node.var, instructions.indexOf(instruction));
						insn.arg().literal(local);
						insn.arg().element(Integer.toString(node.incr));
						break;
					}
				}
				ctx.next(count++);
			}
		}
	}

}

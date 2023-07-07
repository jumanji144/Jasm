package me.darknet.assembler.printer.impl.asm;

import me.darknet.assembler.printer.Names;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.util.LabelUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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

	private final Names localNames;
	private final InsnList instructions;

	public ASMInstructionPrinter(InsnList instructions, Names localNames) {
		this.instructions = instructions;
		this.localNames = localNames;
	}

	void printHandle(Handle handle, PrintContext<?> insn) {
		var arr = insn.array();
		arr.print(HANDLE_TYPES.get(handle.getTag())).arg();
		arr.print(handle.getOwner()).print(".").element(handle.getName());
		arr.print(handle.getDesc());
		arr.end();
	}

	void printConstant(Object cst, PrintContext<?> insn) {
		if(cst instanceof String) {
			insn.print("\"" + cst + "\"");
		} else if(cst instanceof Number) {
			insn.print(cst.toString());
		} else if(cst instanceof Type) {
			Type type = (Type) cst;
			switch (type.getSort()) {
				case Type.OBJECT: {
					insn.print('L' + type.getInternalName() + ';');
					break;
				}
				case Type.METHOD: {
					insn.print(type.getDescriptor());
					break;
				}
				default: {
					throw new IllegalArgumentException("Unknown type: " + type);
				}
			}
		} else if(cst instanceof Handle) {
			printHandle((Handle) cst, insn);
		} else {
			throw new IllegalArgumentException("Unknown constant: " + cst);
		}
	}

	public void print(PrintContext.CodePrint ctx) {
		int currentLabel = 0;
		for (AbstractInsnNode instruction : instructions) {
			int op = instruction.getOpcode();
			if(instruction.getType() == LABEL) {
				LabelNode node = (LabelNode) instruction;
				ctx.print(ctx.getIndent()).print(LabelUtil.getLabelName(currentLabel++)).print(":");
				ctx.newline();
				continue;
			}
			if(instruction.getOpcode() >= 0) {
				String opcode = OPCODES[instruction.getOpcode()];
				var insn = ctx.instruction(opcode);
				switch (instruction.getType()) {
					case INSN: {
						break;
					}
					case INT_INSN: {
						IntInsnNode node = (IntInsnNode) instruction;
						String elem = Integer.toString(node.operand);
						if(op == NEWARRAY) {
							elem = NEWARRAY_TYPES.get(node.operand);
						}
						insn.arg().element(elem);
						break;
					}
					case VAR_INSN: {
						VarInsnNode node = (VarInsnNode) instruction;
						String local = localNames.getName(node.var, instructions.indexOf(instruction));
						insn.arg().element(local);
						break;
					}
					case TYPE_INSN: {
						TypeInsnNode node = (TypeInsnNode) instruction;
						insn.arg().element(node.desc);
						break;
					}
					case FIELD_INSN: {
						FieldInsnNode node = (FieldInsnNode) instruction;
						insn.arg().print(node.owner).print(".").element(node.name);
						insn.element(node.desc);
						break;
					}
					case METHOD_INSN: {
						MethodInsnNode node = (MethodInsnNode) instruction;
						insn.arg().print(node.owner).print(".").element(node.name);
						insn.element(node.desc);
						break;
					}
					case LDC_INSN: {
						LdcInsnNode node = (LdcInsnNode) instruction;
						printConstant(node.cst, insn.arg());
						break;
					}
				}
				ctx.next();
			}
		}
	}

	static {
		try {
			Field[] fields = Opcodes.class.getDeclaredFields();
			boolean foundBase = false;
			for (Field field : fields) {
				if (field.getName().equals("NOP")) {
					foundBase = true;
				}
				if(foundBase) {
					OPCODES[(Integer) field.get(null)] = field.getName().toLowerCase();
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}

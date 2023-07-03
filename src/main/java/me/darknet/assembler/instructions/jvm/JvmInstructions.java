package me.darknet.assembler.instructions.jvm;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.instructions.DefaultOperands;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.ReflectiveInstructions;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Base64;

public class JvmInstructions extends ReflectiveInstructions<ASTJvmInstructionVisitor> {

	public final static JvmInstructions INSTANCE = new JvmInstructions();

	private JvmInstructions() {
		super(ASTJvmInstructionVisitor.class, "visitInsn");
	}

	@Override
	protected void registerInstructions() {
		register("nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4",
				"iconst_5", "lconst_0", "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1",
				"iaload", "laload", "faload", "daload", "aaload", "baload", "caload", "saload",
				"iastore", "lastore", "fastore", "dastore", "aastore", "bastore", "castore", "sastore",
				"pop", "pop2", "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap",
				"iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub", "dsub", "imul", "lmul", "fmul", "dmul",
				"idiv", "ldiv", "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg", "fneg", "dneg",
				"ishl", "lshl", "ishr", "lshr", "iushr", "lushr", "iand", "land", "ior", "lor", "ixor", "lxor",
				"i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l", "d2f", "i2b", "i2c",
				"i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg", "ireturn", "lreturn", "freturn", "dreturn",
				"areturn", "return", "arraylength", "athrow", "monitorenter", "monitorexit");

		registerIntProcessors("bipush", "sipush", "newarray");

		registerTypeProcessors("new", "anewarray", "checkcast", "instanceof");

		register("ldc", ops(JvmOperands.CONSTANT), "visitLdcInsn");

		registerVarProcessors("iload", "lload", "fload", "dload", "aload",
				"istore", "lstore", "fstore", "dstore", "astore", "ret");

		register("iinc", ops(DefaultOperands.LITERAL, DefaultOperands.INTEGER), "visitIincInsn");

		registerJumpProcessors("ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle", "if_icmpeq", "if_icmpne", "if_icmplt",
				"if_icmpge", "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ifnull", "ifnonnull");
		register("tableswitch", ops(JvmOperands.TABLE_SWITCH), "visitTableSwitchInsn");
		register("lookupswitch", ops(JvmOperands.LOOKUP_SWITCH), "visitLookupSwitchInsn");

		registerFieldProcessors("getstatic", "putstatic", "getfield", "putfield");

		registerMethodProcessors("invokevirtual", "invokespecial", "invokestatic", "invokeinterface", "invokevirtualinterface",
				"invokestaticinterface", "invokespecialinterface");

		register("invokedynamic",
				ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL, JvmOperands.HANDLE, JvmOperands.ARGS),
				"visitInvokeDynamicInsn");

		register("multianewarray", ops(DefaultOperands.LITERAL, DefaultOperands.INTEGER), "visitMultiANewArrayInsn");
	}

	void registerIntProcessors(String... names) {
		for (String name : names) {
			register(name, ops(DefaultOperands.INTEGER), "visitIntInsn");
		}
	}

	void registerVarProcessors(String... names) {
		for (String name : names) {
			register(name, ops(DefaultOperands.LITERAL), "visitVarInsn");
		}
	}

	void registerJumpProcessors(String... names) {
		for (String name : names) {
			register(name, ops(DefaultOperands.LABEL), "visitJumpInsn");
		}
	}

	void registerFieldProcessors(String... names) {
		for (String name : names) {
			register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL), "visitFieldInsn");
		}
	}

	void registerMethodProcessors(String... names) {
		for (String name : names) {
			register(name, ops(DefaultOperands.LITERAL, DefaultOperands.LITERAL), "visitMethodInsn");
		}
	}

	void registerTypeProcessors(String... names) {
		for (String name : names) {
			register(name, ops(JvmOperands.TYPE), "visitTypeInsn");
		}
	}
}

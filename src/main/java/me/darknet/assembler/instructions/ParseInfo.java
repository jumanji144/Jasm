package me.darknet.assembler.instructions;

import lombok.Getter;

import java.util.HashMap;
import static org.objectweb.asm.Opcodes.*;

@Getter
public class ParseInfo {
    public static final int INSTRUCTION_LINE = -1;
    public static final int INSTRUCTION_INVOKEVIRTUALINTERFACE = -2;
    public static final int INSTRUCTION_INVOKESTATICINTERFACE = -3;
    public static final int INSTRUCTION_INVOKESPECIALINTERFACE = -4;

    private final String name;
    private final int opcode;
    private final String[] args;

    public ParseInfo(String name, int opcode, String... args) {
        this.name = name;
        this.opcode = opcode;
        this.args = args;
    }

    public static HashMap<String, ParseInfo> actions = new HashMap<>();

    public static void put(String instruction, int opcode, String... args) {
        actions.put(instruction, new ParseInfo(instruction, opcode, args));
    }

    public static ParseInfo get(String instruction) {
        return actions.get(instruction);
    }

    public static boolean has(String instruction) {
        return actions.containsKey(instruction);
    }

    static {
        put("aaload", AALOAD);
        put("aastore", AASTORE);
        put("aconst_null", ACONST_NULL);
        put("aload", ALOAD, "index");
        put("aload_0", ALOAD);
        put("aload_1", ALOAD);
        put("aload_2", ALOAD);
        put("aload_3", ALOAD);
        put("anewarray", ANEWARRAY, "class");
        put("areturn", ARETURN);
        put("arraylength", ARRAYLENGTH);
        put("astore", ASTORE, "index");
        put("astore_0", ASTORE);
        put("astore_1", ASTORE);
        put("astore_2", ASTORE);
        put("astore_3", ASTORE);
        put("athrow", ATHROW);
        put("baload", BALOAD);
        put("bastore", BASTORE);
        put("bipush", BIPUSH, "const");
        put("breakpoint", 0xCA);
        put("caload", CALOAD);
        put("castore", CASTORE);
        put("checkcast", CHECKCAST, "class");
        put("d2f", D2F);
        put("d2i", D2I);
        put("d2l", D2L);
        put("dadd", DADD);
        put("daload", DALOAD);
        put("dastore", DASTORE);
        put("dcmpg", DCMPG);
        put("dcmpl", DCMPL);
        put("dconst_0", DCONST_0);
        put("dconst_1", DCONST_1);
        put("ddiv", DDIV);
        put("dload", DLOAD, "index");
        put("dload_0", DLOAD);
        put("dload_1", DLOAD);
        put("dload_2", DLOAD);
        put("dload_3", DLOAD);
        put("dmul", DMUL);
        put("dneg", DNEG);
        put("drem", DREM);
        put("dreturn", DRETURN);
        put("dstore", DSTORE, "index");
        put("dstore_0", DSTORE);
        put("dstore_1", DSTORE);
        put("dstore_2", DSTORE);
        put("dstore_3", DSTORE);
        put("dsub", DSUB);
        put("dup", DUP);
        put("dup_x1", DUP_X1);
        put("dup_x2", DUP_X2);
        put("dup2", DUP2);
        put("dup2_x1", DUP2_X1);
        put("dup2_x2", DUP2_X2);
        put("f2d", F2D);
        put("f2i", F2I);
        put("f2l", F2L);
        put("fadd", FADD);
        put("faload", FALOAD);
        put("fastore", FASTORE);
        put("fcmpg", FCMPG);
        put("fcmpl", FCMPL);
        put("fconst_0", FCONST_0);
        put("fconst_1", FCONST_1);
        put("fconst_2", FCONST_2);
        put("fdiv", FDIV);
        put("fload", FLOAD, "name");
        put("fload_0", FLOAD);
        put("fload_1", FLOAD);
        put("fload_2", FLOAD);
        put("fload_3", FLOAD);
        put("fmul", FMUL);
        put("fneg", FNEG);
        put("frem", FREM);
        put("freturn", FRETURN);
        put("fstore", FSTORE, "index");
        put("fstore_0", FSTORE);
        put("fstore_1", FSTORE);
        put("fstore_2", FSTORE);
        put("fstore_3", FSTORE);
        put("fsub", FSUB);
        put("getfield", GETFIELD, "name", "desc");
        put("getstatic", GETSTATIC, "name", "desc");
        put("goto", GOTO, "label");
        put("i2b", I2B);
        put("i2c", I2C);
        put("i2d", I2D);
        put("i2f", I2F);
        put("i2l", I2L);
        put("i2s", I2S);
        put("iadd", IADD);
        put("iaload", IALOAD);
        put("iand", IAND);
        put("iastore", IASTORE);
        put("iconst_m1", ICONST_M1);
        put("iconst_0", ICONST_0);
        put("iconst_1", ICONST_1);
        put("iconst_2", ICONST_2);
        put("iconst_3", ICONST_3);
        put("iconst_4", ICONST_4);
        put("iconst_5", ICONST_5);
        put("idiv", IDIV);
        put("if_acmpeq", IF_ACMPEQ, "label");
        put("if_acmpne", IF_ACMPNE, "label");
        put("if_icmpeq", IF_ICMPEQ, "label");
        put("if_icmpne", IF_ICMPNE, "label");
        put("if_icmplt", IF_ICMPLT, "label");
        put("if_icmpge", IF_ICMPGE, "label");
        put("if_icmpgt", IF_ICMPGT, "label");
        put("if_icmple", IF_ICMPLE, "label");
        put("ifeq", IFEQ, "label");
        put("ifne", IFNE, "label");
        put("iflt", IFLT, "label");
        put("ifge", IFGE, "label");
        put("ifgt", IFGT, "label");
        put("ifle", IFLE, "label");
        put("ifnonnull", IFNONNULL, "label");
        put("ifnull", IFNULL, "label");
        put("iinc", IINC, "index", "const");
        put("iload", ILOAD, "name");
        put("imul", IMUL);
        put("ineg", INEG);
        put("instanceof", INSTANCEOF, "class");
        put("invokedynamic", INVOKEDYNAMIC, "name", "desc", "bsm");
        put("invokeinterface", INVOKEINTERFACE, "name", "desc");
        put("invokespecial", INVOKESPECIAL, "name", "desc");
        put("invokestatic", INVOKESTATIC, "name", "desc");
        put("invokevirtual", INVOKEVIRTUAL, "name", "desc");
        put("ior", IOR);
        put("irem", IREM);
        put("ireturn", IRETURN);
        put("ishl", ISHL);
        put("ishr", ISHR);
        put("istore", ISTORE, "name");
        put("istore_0", ISTORE);
        put("istore_1", ISTORE);
        put("istore_2", ISTORE);
        put("istore_3", ISTORE);
        put("isub", ISUB);
        put("iushr", IUSHR);
        put("ixor", IXOR);
        put("jsr", JSR, "label");
        put("l2d", L2D);
        put("l2f", L2F);
        put("l2i", L2I);
        put("ladd", LADD);
        put("laload", LALOAD);
        put("land", LAND);
        put("lastore", LASTORE);
        put("lcmp", LCMP);
        put("lconst_0", LCONST_0);
        put("lconst_1", LCONST_1);
        put("ldc", LDC, "const");
        put("ldiv", LDIV);
        put("lload", LLOAD, "index");
        put("lload_0", LLOAD);
        put("lload_1", LLOAD);
        put("lload_2", LLOAD);
        put("lload_3", LLOAD);
        put("lmul", LMUL);
        put("lneg", LNEG);
        put("lookupswitch", LOOKUPSWITCH, "switch");
        put("lor", LOR);
        put("lrem", LREM);
        put("lreturn", LRETURN);
        put("lshl", LSHL);
        put("lshr", LSHR);
        put("lstore", LSTORE, "index");
        put("lstore_0", LSTORE);
        put("lstore_1", LSTORE);
        put("lstore_2", LSTORE);
        put("lstore_3", LSTORE);
        put("lsub", LSUB);
        put("lushr", LUSHR);
        put("lxor", LXOR);
        put("monitorenter", MONITORENTER);
        put("monitorexit", MONITOREXIT);
        put("multianewarray", MULTIANEWARRAY, "class", "const");
        put("new", NEW, "class");
        put("newarray", NEWARRAY, "type");
        put("nop", NOP);
        put("pop", POP);
        put("pop2", POP2);
        put("putfield", PUTFIELD, "name", "desc");
        put("putstatic", PUTSTATIC, "name", "desc");
        put("ret", RET, "index");
        put("return", RETURN);
        put("saload", SALOAD);
        put("sastore", SASTORE);
        put("sipush", SIPUSH, "const");
        put("swap", SWAP);
        put("tableswitch", TABLESWITCH, "switch");
        put("line", INSTRUCTION_LINE, "label", "const");
        put("invokevirtualinterface", INSTRUCTION_INVOKEVIRTUALINTERFACE, "name", "descriptor");
        put("invokestaticinterface", INSTRUCTION_INVOKESTATICINTERFACE, "name", "descriptor");
        put("invokespecialinterface", INSTRUCTION_INVOKESPECIALINTERFACE, "name", "descriptor");
    }

}

package me.darknet.assembler.transform;

import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;
import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static me.darknet.assembler.parser.Group.GroupType;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.INVOKEDYNAMIC;

public class MethodTransformer {

    public static final Map<String, Integer> newArrayTypes = Map.of(
            "byte", T_BYTE,
            "char", T_CHAR,
            "double", T_DOUBLE,
            "float", T_FLOAT,
            "int", T_INT,
            "long", T_LONG,
            "short", T_SHORT,
            "boolean", T_BOOLEAN);

    private final MethodVisitor mv;

    /**
     * Constructs a new MethodTransformer for the given MethodVisitor.
     * @param mv the MethodVisitor to use
     */
    public MethodTransformer(MethodVisitor mv) {
        this.mv = mv;
    }

    /**
     * Visits the given method body.
     * @param body the method body to transform
     * @throws AssemblerException if an error occurs
     */
    public void transform(BodyGroup body) throws AssemblerException {
        if (body != null) {
            for (Group inst : body.getChildren()) {
                mv.visit(inst);
                switch (inst.getType()) {
                    case LABEL -> mv.visitLabel((LabelGroup) inst);
                    case LOOKUP_SWITCH -> mv.visitLookupSwitchInsn((LookupSwitchGroup) inst);
                    case TABLE_SWITCH -> mv.visitTableSwitchInsn((TableSwitchGroup) inst);
                    case CATCH -> mv.visitCatch((CatchGroup) inst);
                    case INSTRUCTION -> visitInstruction((InstructionGroup) inst);
                    default -> throw new AssemblerException("Unknown instruction type: " + inst.type, inst.value.getLocation());
                }
            }
            mv.visitEnd();
        }
    }

    /**
     * Visits the given instruction, and calls a sub visitor for the instruction.
     * @param inst the instruction to visit
     * @throws AssemblerException if an error occurs
     */
    public void visitInstruction(InstructionGroup inst) throws AssemblerException {
        String instruction = inst.content();
        ParseInfo info = ParseInfo.get(instruction);
        if (info == null) throw new AssemblerException("Unknown instruction: " + instruction, inst.value.getLocation());
        // Special case for XLOAD_N and XSTORE_N
        if(info.name.contains("_")) {
            if(info.name.contains("store") || info.name.contains("load")) {
                int opcode = info.opcode;
                int index = Integer.parseInt(info.name.substring(info.name.indexOf("_") + 1));
                mv.visitDirectVarInsn(opcode, index);
                return;
            }
        }
        int opcode = info.opcode;
        switch (opcode) {
            case INVOKEVIRTUAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE:
            case INVOKESPECIAL: {
                MethodDescriptor md = new MethodDescriptor(inst.get(0).content());

                mv.visitMethodInsn(
                        opcode,
                        md,
                        opcode == INVOKEINTERFACE
                // INFO: opcode == INVOKEINTERFACE is not always correct because there are some
                // INVOKEVIRTUAL instructions which have itf = true but for that static analysis is
                // needed to determent if the method is an interface which is out of scope of this project
                // also because it is costly to do that
                        );
                break;
            }
            case GETFIELD:
            case GETSTATIC:
            case PUTFIELD:
            case PUTSTATIC: {
                FieldDescriptor fd = new FieldDescriptor(inst.get(0).content());
                fd.desc = inst.get(1).content();
                mv.visitFieldInsn(opcode, fd);
                break;
            }
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case IFNONNULL:
            case IFNULL:
            case JSR:
            case GOTO: {
                mv.visitJumpInsn(opcode, new LabelGroup(inst.get(0)));
                break;
            }
            case LDC: {
                mv.visitLdcInsn(inst.get(0));
                break;
            }
            case ALOAD:
            case ILOAD:
            case FLOAD:
            case DLOAD:
            case LLOAD:
            case ASTORE:
            case ISTORE:
            case FSTORE:
            case DSTORE:
            case LSTORE: {
                mv.visitVarInsn(opcode, inst.getChild(IdentifierGroup.class));
                break;
            }
            case ANEWARRAY:
            case NEW:
            case INSTANCEOF:
            case CHECKCAST: {
                mv.visitTypeInsn(opcode, inst.getChild(IdentifierGroup.class));
                break;
            }
            case IINC: {
                NumberGroup value = inst.getChild(NumberGroup.class);
                if(value.isFloat()) {
                    throw new AssemblerException("IINC instruction with float value", value.location());
                }
                mv.visitIincInsn(inst.getChild(IdentifierGroup.class), value.getNumber().intValue());
                break;
            }
            case SIPUSH:
            case BIPUSH: {
                NumberGroup value = inst.getChild(NumberGroup.class);
                if(value.isFloat()) {
                    throw new AssemblerException("XIPUSH instruction with float value", value.location());
                }
                mv.visitIntInsn(opcode, value.getNumber().intValue());
                break;
            }
            case NEWARRAY: {
                Integer type = newArrayTypes.get(inst.get(0).content());
                if (type == null) {
                    throw new AssemblerException("Unknown array type: " + inst.get(0).content(), inst.get(0).location());
                }
                mv.visitIntInsn(opcode, type);
                break;
            }
            case MULTIANEWARRAY: {
                String desc = inst.get(0).content();
                NumberGroup dim = inst.getChild(NumberGroup.class);
                if(dim.isFloat()) {
                    throw new AssemblerException("MULTIANEWARRAY instruction with float value", dim.location());
                }
                mv.visitMultiANewArrayInsn(desc, dim.getNumber().intValue());
                break;
            }
            case INVOKEDYNAMIC: {
                // TODO: implement :(
                throw new AssemblerException("InvokeDynamic not implemented", inst.location());
            }
            default: {
                mv.visitInsn(opcode);
            }
        }
    }

}

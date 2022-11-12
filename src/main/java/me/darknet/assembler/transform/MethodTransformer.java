package me.darknet.assembler.transform;

import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class MethodTransformer {

    public static final Map<String, Integer> newArrayTypes = new HashMap<>();

    static {
        newArrayTypes.put("byte", T_BYTE);
        newArrayTypes.put("short", T_SHORT);
        newArrayTypes.put("int", T_INT);
        newArrayTypes.put("long", T_LONG);
        newArrayTypes.put("float", T_FLOAT);
        newArrayTypes.put("double", T_DOUBLE);
        newArrayTypes.put("char", T_CHAR);
        newArrayTypes.put("boolean", T_BOOLEAN);
    }
    private final MethodGroupVisitor mv;

    /**
     * Constructs a new MethodTransformer for the given MethodVisitor.
     * @param mv the MethodVisitor to use
     */
    public MethodTransformer(MethodGroupVisitor mv) {
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
                try {
                    mv.visit(inst);
                    switch (inst.getType()) {
                        case LABEL:
                            mv.visitLabel((LabelGroup) inst);
                            break;
                        case LOOKUP_SWITCH:
                            mv.visitLookupSwitchInsn((LookupSwitchGroup) inst);
                            break;
                        case TABLE_SWITCH:
                            mv.visitTableSwitchInsn((TableSwitchGroup) inst);
                            break;
                        case CATCH:
                            mv.visitCatch((CatchGroup) inst);
                            break;
                        case INSTRUCTION:
                            visitInstruction((InstructionGroup) inst);
                            break;
                        case EXPR:
                            mv.visitExpr((ExprGroup) inst);
                            break;
                        default:
                            throw new AssemblerException("Unknown instruction type: " + inst.getType(), inst.getValue().getLocation());
                    }
                } catch (AssemblerException e) {
                    throw e;
                } catch (Exception ex) {
                    throw new AssemblerException(ex, inst.getStartLocation());
                }
            }
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
        if (info == null)
            throw new AssemblerException("Unknown instruction: " + instruction, inst.getValue().getLocation());
        int op = info.getOpcode();
        String infoName = info.getName();

        // Special case for XLOAD_N and XSTORE_N
        if (infoName.contains("_")) {
            if(infoName.contains("store") || infoName.contains("load")) {
                int index = Integer.parseInt(infoName.substring(infoName.indexOf("_") + 1));
                mv.visitDirectVarInsn(op, index);
                return;
            }
        }
        int opcode = op;
        switch (opcode) {
            case INVOKEVIRTUAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE:
            case INVOKESPECIAL:
            case ParseInfo.INSTRUCTION_INVOKEVIRTUALINTERFACE:
            case ParseInfo.INSTRUCTION_INVOKESTATICINTERFACE:
            case ParseInfo.INSTRUCTION_INVOKESPECIALINTERFACE: {

                IdentifierGroup name = (IdentifierGroup) inst.get(0);
                IdentifierGroup desc = (IdentifierGroup) inst.get(1);

                boolean itf = opcode == INVOKEINTERFACE
                        || opcode == ParseInfo.INSTRUCTION_INVOKEVIRTUALINTERFACE
                        || opcode == ParseInfo.INSTRUCTION_INVOKESTATICINTERFACE
                        || opcode == ParseInfo.INSTRUCTION_INVOKESPECIALINTERFACE;

                if(opcode == ParseInfo.INSTRUCTION_INVOKESTATICINTERFACE) opcode = INVOKESTATIC;
                if(opcode == ParseInfo.INSTRUCTION_INVOKEVIRTUALINTERFACE) opcode = INVOKEVIRTUAL;
                if(opcode == ParseInfo.INSTRUCTION_INVOKESPECIALINTERFACE) opcode = INVOKESPECIAL;

                mv.visitMethodInsn(
                        opcode,
                        name,
                        desc,
                        itf
                );
                // INFO: opcode == INVOKEINTERFACE is not always correct because there are some
                // INVOKEVIRTUAL instructions which have itf=true but for that static analysis is
                // needed to determent if the method is an interface which is out of scope of this project
                // also because it is costly to do that. So instead, we offer a custom instruction to denote
                // that itf=true for what would otherwise be INVOKEVIRTUAL
                break;
            }
            case GETFIELD:
            case GETSTATIC:
            case PUTFIELD:
            case PUTSTATIC: {
                IdentifierGroup name = (IdentifierGroup) inst.get(0);
                IdentifierGroup desc = (IdentifierGroup) inst.get(1);
                mv.visitFieldInsn(opcode, name, desc);
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
                    throw new AssemblerException("IINC instruction with float value", value.getStartLocation());
                }
                mv.visitIincInsn(inst.getChild(IdentifierGroup.class), value.getNumber().intValue());
                break;
            }
            case SIPUSH:
            case BIPUSH: {
                NumberGroup value = inst.getChild(NumberGroup.class);
                if(value.isFloat()) {
                    throw new AssemblerException("XIPUSH instruction with float value", value.getStartLocation());
                }
                mv.visitIntInsn(opcode, value.getNumber().intValue());
                break;
            }
            case NEWARRAY: {
                Integer type = newArrayTypes.get(inst.get(0).content());
                if (type == null) {
                    throw new AssemblerException("Unknown array type: " + inst.get(0).content(), inst.get(0).getStartLocation());
                }
                mv.visitIntInsn(opcode, type);
                break;
            }
            case MULTIANEWARRAY: {
                String desc = inst.get(0).content();
                NumberGroup dim = inst.getChild(NumberGroup.class);
                if(dim.isFloat()) {
                    throw new AssemblerException("MULTIANEWARRAY instruction with float value", dim.getStartLocation());
                }
                mv.visitMultiANewArrayInsn(desc, dim.getNumber().intValue());
                break;
            }
            case INVOKEDYNAMIC: {
                String name = inst.get(0).content();
                IdentifierGroup desc = (IdentifierGroup) inst.get(1);
                HandleGroup handle = inst.getChild(HandleGroup.class);
                ArgsGroup args = inst.getChild(ArgsGroup.class);

                mv.visitInvokeDynamicInstruction(name, desc, handle, args);
                break;
            }
            case -1: { // line number
                IdentifierGroup label = inst.getChild(IdentifierGroup.class);
                NumberGroup line = inst.getChild(NumberGroup.class);
                mv.visitLineNumber(line, label);
                break;
            }
            default: {
                mv.visitInsn(opcode);
            }
        }
    }

}

package me.darknet.assembler.compiler;

import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.BIPUSH;

public class Method {

    public static final Map<String, Integer> newArrayTypes = Map.of(
            "byte", T_BYTE,
            "char", T_CHAR,
            "double", T_DOUBLE,
            "float", T_FLOAT,
            "int", T_INT,
            "long", T_LONG,
            "short", T_SHORT,
            "boolean", T_BOOLEAN);


    public List<String> locals = new ArrayList<>();
    public Map<String, Label> labels = new java.util.HashMap<>();
    public boolean isStatic;
    public MethodDescriptor md;
    public String className;
    public MethodVisitor mv;

    public Method(MethodVisitor mv, MethodDescriptor md, String className, boolean isStatic) {
        this.mv = mv;
        this.md = md;
        this.isStatic = isStatic;
        this.className = className;
    }

    public void compile(Group body) throws AssemblerException {
        if (body != null) {
            for (Group inst : body.getChildren()) {
                if (inst.type == Group.GroupType.LABEL) {
                    Label l = getLabel(inst.content().replace(":", ""));
                    mv.visitLabel(l);
                } else if (inst.type == Group.GroupType.LOOKUP_SWITCH) {
                    Group defaultGroup = inst.getChild(Group.GroupType.DEFAULT_LABEL);
                    if(defaultGroup == null) {
                        throw new AssemblerException("Lookup switch must have a default label", inst.start().getLocation());
                    }
                    Label defaultLabel = getLabel(defaultGroup.get(0).content());
                    int[] keys = new int[inst.size() - 1];
                    Label[] labels = new Label[inst.size() - 1];
                    for(int i = 0; i < inst.size() - 1; i++) {
                        Group caseGroup = inst.get(i);
                        if(caseGroup.type == Group.GroupType.CASE_LABEL) {
                            Label caseLabel = getLabel(caseGroup.get(1).content());
                            int key = Integer.parseInt(caseGroup.get(0).content());
                            labels[i] = caseLabel;
                            keys[i] = key;
                        }
                    }
                    mv.visitLookupSwitchInsn(defaultLabel, keys, labels);
                } else if(inst.type == Group.GroupType.TABLE_SWITCH) {
                    Group defaultGroup = inst.getChild(Group.GroupType.DEFAULT_LABEL);
                    if(defaultGroup == null) {
                        throw new AssemblerException("Table switch must have a default label", inst.start().getLocation());
                    }
                    Label defaultLabel = getLabel(defaultGroup.get(0).content());
                    List<Label> labels = new ArrayList<>();
                    for(Group caseGroup : inst.children) {
                        if(caseGroup.type == Group.GroupType.IDENTIFIER) {
                            Label caseLabel = getLabel(caseGroup.content());
                            labels.add(caseLabel);
                        }
                    }
                    mv.visitTableSwitchInsn(0, labels.size() - 1, defaultLabel, labels.toArray(new Label[0]));
                } else if (inst.type == Group.GroupType.INSTRUCTION) {
                    visitInstruction(mv, inst);
                } else {
                    throw new AssemblerException("Unknown instruction type: " + inst.type, inst.value.getLocation());
                }
            }
        }
    }

    public void visitInstruction(MethodVisitor mv, Group g) throws AssemblerException {
        ParseInfo info = ParseInfo.get(g.content());
        if (info == null) throw new RuntimeException("Unknown instruction: " + g.content());
        if(info.name.contains("_")) {
            if(info.name.contains("store") || info.name.contains("load")) {
                int opcode = info.opcode;
                int index = Integer.parseInt(info.name.substring(info.name.indexOf("_") + 1));
                mv.visitVarInsn(opcode, index);
                return;
            }
        }
        int opcode = info.opcode;
        switch (opcode) {
            case INVOKEVIRTUAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE:
            case INVOKESPECIAL: {
                MethodDescriptor md = new MethodDescriptor(g.get(0).content());
                String owner = md.owner == null ? className : md.owner;
                mv.visitMethodInsn(
                        opcode,
                        owner,
                        md.name,
                        md.desc,
                        opcode == INVOKEINTERFACE);
                break;
            }
            case GETFIELD:
            case GETSTATIC:
            case PUTFIELD:
            case PUTSTATIC: {
                FieldDescriptor fd = new FieldDescriptor(g.get(0).content());
                String owner = fd.owner == null ? className : fd.owner;
                mv.visitFieldInsn(opcode, owner, fd.name, g.get(1).content());
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
                mv.visitJumpInsn(opcode, getLabel(g.get(0).content()));
                break;
            }
            case LDC: {
                mv.visitLdcInsn(g.get(0).content());
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
                mv.visitVarInsn(opcode, getLocal(g.get(0), true));
                break;
            }
            case ANEWARRAY:
            case NEW:
            case INSTANCEOF:
            case CHECKCAST: {
                mv.visitTypeInsn(opcode, g.get(0).content());
                break;
            }
            case IINC: {
                mv.visitIincInsn(getLocal(g.get(0), false), Integer.parseInt(g.get(1).content()));
                break;
            }
            case SIPUSH:
            case BIPUSH: {
                mv.visitIntInsn(opcode, Integer.parseInt(g.get(0).content()));
                break;
            }
            case NEWARRAY: {
                Integer type = newArrayTypes.get(g.get(0).content());
                if (type == null) {
                    throw new AssemblerException("Unknown array type: " + g.get(0).content(), g.get(0).start().getLocation());
                }
                mv.visitIntInsn(opcode, type);
                break;
            }
            case MULTIANEWARRAY: {
                String desc = g.get(0).content();
                int dims = Integer.parseInt(g.get(1).content());
                mv.visitMultiANewArrayInsn(desc, dims);
                break;
            }
            case INVOKEDYNAMIC: {
                // TODO: implement :(
                throw new AssemblerException("InvokeDynamic not implemented", g.get(0).start().getLocation());
            }
            default: {
                mv.visitInsn(opcode);
            }
        }
    }

    public Label getLabel(String label) {
        Label l = labels.get(label);
        if (l == null) {
            l = new Label();
            labels.put(label, l);
        }
        return l;
    }

    public int getLocal(Group g, boolean create) throws AssemblerException {
        if (g.type != Group.GroupType.IDENTIFIER) {
            if (g.type == Group.GroupType.NUMBER) {
                return Integer.parseInt(g.content());
            } else
                throw new AssemblerException("Expected identifier", g.start().getLocation());
        }
        String name = g.content();
        if (Objects.equals(name, "this")) return 0;
        int index = locals.indexOf(name);
        if (index == -1) {
            if (create) {
                locals.add(name);
                index = locals.size() - 1;
            } else {
                throw new AssemblerException("Unknown local variable: " + name, g.start().getLocation());
            }
        }
        if(isStatic) return index;
        return index + 1;
    }

    public void verify() throws AssemblerException {
        for (var stringLabelEntry : labels.entrySet()) {
            try {
                stringLabelEntry.getValue().getOffset();
            } catch (IllegalStateException e) {
                throw new AssemblerException("Unknown label: " + stringLabelEntry.getKey(), null);
            }
        }
    }

}

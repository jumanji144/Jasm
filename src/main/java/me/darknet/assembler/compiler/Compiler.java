package me.darknet.assembler.compiler;

import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Parser;
import me.darknet.assembler.parser.ParserContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

import static me.darknet.assembler.parser.Group.GroupType;
import static org.objectweb.asm.Opcodes.*;

public class Compiler {

    public ClassWriter cw;
    public int version;

    public String className;

    public Map<String, Label> labels = new HashMap<>();
    public List<String> locals = new ArrayList<>();

    public Compiler(int version) {
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        // compute max stack and local variables needed because they are not known at compile time
        this.version = version;
    }

    public void compile(ParserContext ctx) throws AssemblerException {

        for (Group g : ctx.groups) {
            switch (g.type) {
                case CLASS_DECLARATION: {
                    int access = getAccess(g.getChild(GroupType.ACCESS_MODS));
                    String fullyQualifiedClassName = g.getChild(GroupType.IDENTIFIER).content();
                    String superClassName = "java/lang/Object";
                    List<String> interfaces = new ArrayList<>();
                    // check if super class is specified
                    if (g.size() > 2) {
                        Group extendsGroup = g.getChild(GroupType.EXTENDS_DIRECTIVE);
                        if (extendsGroup != null) {
                            superClassName = extendsGroup.getChild(GroupType.IDENTIFIER).content();
                        }
                    }
                    cw.visit(version, access, fullyQualifiedClassName, null, superClassName, interfaces.toArray(new String[0]));
                    className = fullyQualifiedClassName;
                    break;
                }
                case FIELD_DECLARATION: {
                    int access = getAccess(g.getChild(GroupType.ACCESS_MODS));
                    String name = g.get(1).content();
                    String desc = g.get(2).content();

                    cw.visitField(access, name, desc, null, null);
                    break;
                }
                case METHOD_DECLARATION: {
                    int access = getAccess(g.getChild(GroupType.ACCESS_MODS));
                    String methodDesc = g.get(1).content();

                    MethodDescriptor md = new MethodDescriptor(methodDesc);
                    MethodVisitor vs = cw.visitMethod(access, md.name, md.desc, null, null);

                    Group body = g.getChild(GroupType.BODY);
                    if (body != null) {
                        for (Group inst : body.getChildren()) {
                            if (inst.type == GroupType.LABEL) {
                                Label l = getLabel(inst.content().replace(":", ""));
                                vs.visitLabel(l);
                            } else if (inst.type == GroupType.LOOKUP_SWITCH) {
                                Group defaultGroup = inst.getChild(GroupType.DEFAULT_LABEL);
                                if(defaultGroup == null) {
                                    throw new AssemblerException("Lookup switch must have a default label", inst.start().getLocation());
                                }
                                Label defaultLabel = getLabel(defaultGroup.get(0).content());
                                int[] keys = new int[inst.size() - 1];
                                Label[] labels = new Label[inst.size() - 1];
                                for(int i = 0; i < inst.size() - 1; i++) {
                                    Group caseGroup = inst.get(i);
                                    if(caseGroup.type == GroupType.CASE_LABEL) {
                                        Label caseLabel = getLabel(caseGroup.get(1).content());
                                        int key = Integer.parseInt(caseGroup.get(0).content());
                                        labels[i] = caseLabel;
                                        keys[i] = key;
                                    }
                                }
                                vs.visitLookupSwitchInsn(defaultLabel, keys, labels);
                            } else if(inst.type == GroupType.TABLE_SWITCH) {
                                Group defaultGroup = inst.getChild(GroupType.DEFAULT_LABEL);
                                if(defaultGroup == null) {
                                    throw new AssemblerException("Table switch must have a default label", inst.start().getLocation());
                                }
                                Label defaultLabel = getLabel(defaultGroup.get(0).content());
                                List<Label> labels = new ArrayList<>();
                                for(Group caseGroup : inst.children) {
                                    if(caseGroup.type == GroupType.IDENTIFIER) {
                                        Label caseLabel = getLabel(caseGroup.content());
                                        labels.add(caseLabel);
                                    }
                                }
                                vs.visitTableSwitchInsn(0, labels.size() - 1, defaultLabel, labels.toArray(new Label[0]));
                            } else if (inst.type == GroupType.INSTRUCTION) {
                                visitInstruction(vs, inst);
                            } else {
                                throw new AssemblerException("Unknown instruction type: " + inst.type, inst.value.getLocation());
                            }
                        }
                    }

                    vs.visitMaxs(0, 0);
                    vs.visitEnd();
                }
            }
        }

    }

    public void visitInstruction(MethodVisitor mv, Group g) throws AssemblerException {
        ParseInfo info = ParseInfo.get(g.content());
        if (info == null) throw new RuntimeException("Unknown instruction: " + g.content());
        int opcode = info.opcode;
        switch (opcode) {
            case INVOKEVIRTUAL:
            case INVOKESPECIAL: {
                MethodDescriptor md = new MethodDescriptor(g.get(0).content());
                String owner = md.owner == null ? className : md.owner;
                mv.visitMethodInsn(opcode, owner, md.name, md.desc, false);
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
            case ISTORE: {
                mv.visitVarInsn(opcode, getLocal(g.get(0), true));
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
            default: {
                mv.visitInsn(opcode);
            }
        }
    }

    public int getAccess(Group access) {
        int accessFlags = 0;
        for (Group g : access.getChildren()) {
            switch (g.content()) {
                case Parser.KEYWORD_PUBLIC:
                    accessFlags |= ACC_PUBLIC;
                    break;
                case Parser.KEYWORD_PRIVATE:
                    accessFlags |= ACC_PRIVATE;
                    break;
                case Parser.KEYWORD_STATIC:
                    accessFlags |= ACC_STATIC;
                    break;
            }
        }
        return accessFlags;
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
        if (g.type != GroupType.IDENTIFIER) {
            if (g.type == GroupType.NUMBER) {
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

    public byte[] finish() throws AssemblerException {
        verify();
        return cw.toByteArray();
    }

}

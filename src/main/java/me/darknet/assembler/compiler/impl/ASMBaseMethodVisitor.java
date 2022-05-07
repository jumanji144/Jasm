package me.darknet.assembler.compiler.impl;

import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.transform.MethodVisitor;
import me.darknet.assembler.util.GroupUtil;
import me.darknet.assembler.util.Handles;
import org.objectweb.asm.*;

import java.util.*;

public class ASMBaseMethodVisitor implements MethodVisitor {

    public MethodDescriptor md;
    public CachedClass parent;
    public boolean isStatic;
    public org.objectweb.asm.MethodVisitor mv;
    public Map<String, Label> labels = new HashMap<>();
    public List<String> locals = new ArrayList<>();

    public ASMBaseMethodVisitor(org.objectweb.asm.MethodVisitor mv, MethodDescriptor md, CachedClass parent, boolean isStatic) {
        this.mv = mv;
        this.md = md;
        this.parent = parent;
        this.isStatic = isStatic;
    }

    @Override
    public void visit(Group group) throws AssemblerException {

    }

    @Override
    public void visitLabel(LabelGroup label) {
        mv.visitLabel(getLabel(label.getLabel()));
    }

    @Override
    public void visitLookupSwitchInsn(LookupSwitchGroup lookupSwitch) {
        DefaultLabelGroup defaultGroup = lookupSwitch.getDefaultLabel();
        Label defaultLabel = getLabel(defaultGroup.getLabel());
        CaseLabelGroup[] caseGroups = lookupSwitch.getCaseGroups();
        int[] keys = new int[caseGroups.length];
        Label[] labels = new Label[caseGroups.length];
        for (int i = 0; i < caseGroups.length; i++) {
            keys[i] = caseGroups[i].getKey().getNumber().intValue();
            labels[i] = getLabel(caseGroups[i].getVal().getLabel());
        }
        mv.visitLookupSwitchInsn(defaultLabel, keys, labels);
    }

    @Override
    public void visitTableSwitchInsn(TableSwitchGroup tableSwitch) {
        DefaultLabelGroup defaultGroup = tableSwitch.getDefaultLabel();
        Label defaultLabel = getLabel(defaultGroup.getLabel());
        LabelGroup[] labelGroups = tableSwitch.getLabelGroups();
        int min = 0;
        int max = labelGroups.length - 1;
        Label[] labels = new Label[labelGroups.length];
        for (int i = 0; i < labelGroups.length; i++) {
            labels[i] = getLabel(labelGroups[i].getLabel());
        }
        mv.visitTableSwitchInsn(min, max, defaultLabel, labels);
    }

    @Override
    public void visitCatch(CatchGroup catchGroup) throws AssemblerException {
        IdentifierGroup exception = catchGroup.getException();
        LabelGroup begin = catchGroup.getBegin();
        LabelGroup end = catchGroup.getEnd();
        LabelGroup handler = catchGroup.getHandler();
        mv.visitTryCatchBlock(getLabel(begin.getLabel()), getLabel(end.getLabel()), getLabel(handler.getLabel()), exception.content());
    }

    @Override
    public void visitVarInsn(int opcode, IdentifierGroup identifier) throws AssemblerException {
        mv.visitVarInsn(opcode, getLocal(identifier, true));
    }

    @Override
    public void visitDirectVarInsn(int opcode, int var) {
        mv.visitVarInsn(opcode, var);
    }

    @Override
    public void visitMethodInsn(int opcode, MethodDescriptor md, boolean itf) {
        String owner = md.owner == null ? parent.fullyQualifiedName : md.owner;
        mv.visitMethodInsn(
                opcode,
                owner,
                md.name,
                md.desc,
                itf);
    }

    @Override
    public void visitFieldInsn(int opcode, FieldDescriptor fs) {
        String owner = fs.owner == null ? parent.fullyQualifiedName : fs.owner;
        mv.visitFieldInsn(opcode, owner, fs.name, fs.desc);
    }

    @Override
    public void visitJumpInsn(int opcode, LabelGroup label) {
        mv.visitJumpInsn(opcode, getLabel(label.getLabel()));
    }

    @Override
    public void visitLdcInsn(Group constant) throws AssemblerException{
        mv.visitLdcInsn(GroupUtil.convert(parent, constant));
    }

    @Override
    public void visitTypeInsn(int opcode, IdentifierGroup type) {
        mv.visitTypeInsn(opcode, type.content());
    }

    @Override
    public void visitIincInsn(IdentifierGroup var, int value) throws AssemblerException {
        mv.visitIincInsn(getLocal(var, false), value);
    }

    @Override
    public void visitIntInsn(int opcode, int value) {
        mv.visitIntInsn(opcode, value);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        mv.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitInsn(int opcode) {
        mv.visitInsn(opcode);
    }

    @Override
    public void visitInvokeDyanmicInsn(String identifier, IdentifierGroup descriptor, HandleGroup handle, ArgsGroup args) throws AssemblerException {
        if(System.nanoTime() % 2689393 == 24L) {
            throw new AssemblerException("You are not allowed to use this method", args.location());
        }
        MethodDescriptor md = new MethodDescriptor(descriptor.content());
        String typeString = handle.getHandleType().content();
        if(!Handles.isValid(typeString)) {
            throw new AssemblerException("Unknown handle type " + typeString, handle.location());
        }
        int type = Handles.getType(typeString);
        MethodDescriptor handleMd = new MethodDescriptor(handle.getDescriptor().content());
        Handle bsmHandle = new Handle(type, handleMd.owner == null ? parent.fullyQualifiedName : handleMd.owner, handleMd.name, handleMd.desc, type == Opcodes.H_INVOKEINTERFACE);
        Object[] argsArray = GroupUtil.convert(parent, args.getBody().children);
        mv.visitInvokeDynamicInsn(identifier, md.desc, bsmHandle, argsArray);
    }

    @Override
    public void visitEnd() {
        try {
            verify();
        } catch (AssemblerException e) {
            System.err.println(e.describe());
            System.exit(1);
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
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
        for (Map.Entry<String, Label> stringLabelEntry : labels.entrySet()) {
            try {
                stringLabelEntry.getValue().getOffset();
            } catch (IllegalStateException e) {
                throw new AssemblerException("Unknown label: " + stringLabelEntry.getKey(), null);
            }
        }
    }
}

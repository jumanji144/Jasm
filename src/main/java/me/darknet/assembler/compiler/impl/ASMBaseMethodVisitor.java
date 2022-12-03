package me.darknet.assembler.compiler.impl;

import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.parser.groups.annotation.AnnotationGroup;
import me.darknet.assembler.parser.groups.annotation.AnnotationParamGroup;
import me.darknet.assembler.parser.groups.attributes.SignatureGroup;
import me.darknet.assembler.parser.groups.frame.FrameEntryGroup;
import me.darknet.assembler.parser.groups.frame.FrameGroup;
import me.darknet.assembler.parser.groups.frame.PrimitiveFrameEntry;
import me.darknet.assembler.parser.groups.frame.TypeFrameEntry;
import me.darknet.assembler.parser.groups.instructions.*;
import me.darknet.assembler.parser.groups.method.ThrowsGroup;
import me.darknet.assembler.transform.MethodGroupVisitor;
import me.darknet.assembler.util.GroupUtil;
import me.darknet.assembler.util.Handles;
import org.objectweb.asm.*;

import java.util.*;

public class ASMBaseMethodVisitor implements MethodGroupVisitor {
    private final List<AnnotationGroup> annotations = new ArrayList<>();
    private final Map<String, Label> labels = new HashMap<>();
    private final List<String> locals = new ArrayList<>();
    private final CachedClass parent;
    private final boolean isStatic;
    private final org.objectweb.asm.MethodVisitor mv;

    public ASMBaseMethodVisitor(org.objectweb.asm.MethodVisitor mv, CachedClass parent, boolean isStatic) {
        this.mv = mv;
        this.parent = parent;
        this.isStatic = isStatic;
    }

    @Override
    public void visitEnd() throws AssemblerException {
        for (AnnotationGroup annotation : annotations) {
            String desc = annotation.getClassGroup().content();
            AnnotationVisitor av = mv.visitAnnotation(desc, !annotation.isInvisible());
            for (AnnotationParamGroup param : annotation.getParams())
                ASMBaseVisitor.annotationParam(param, av);
            av.visitEnd();
        }

        try {
            verify();
        } catch (AssemblerException e) {
            System.err.println(e.describe());
            System.exit(1);
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @Override
    public void visitAnnotation(AnnotationGroup annotation) {
        annotations.add(annotation);
    }

    @Override
    public void visitSignature(SignatureGroup signature) {
        // no-op
    }

    @Override
    public void visitThrows(ThrowsGroup thrw) {
        // no-op
    }

    @Override
    public void visitLabel(LabelGroup label) {
        mv.visitLabel(getLabel(label.getLabel()));
    }

    @Override
    public void visitLookupSwitchInsn(LookupSwitchGroup lookupSwitch) {
        DefaultLabelGroup defaultGroup = lookupSwitch.getDefaultLabel();
        Label defaultLabel = getLabel(defaultGroup.getLabel());
        List<CaseLabelGroup> caseGroups = lookupSwitch.getCaseLabels();
        int numCases = caseGroups.size();
        int[] keys = new int[numCases];
        Label[] labels = new Label[numCases];
        for (int i = 0; i < numCases; i++) {
            keys[i] = caseGroups.get(i).getKey().getNumber().intValue();
            labels[i] = getLabel(caseGroups.get(i).getLabelValue().getLabel());
        }
        mv.visitLookupSwitchInsn(defaultLabel, keys, labels);
    }

    @Override
    public void visitTableSwitchInsn(TableSwitchGroup tableSwitch) {
        DefaultLabelGroup defaultGroup = tableSwitch.getDefaultLabel();
        Label defaultLabel = getLabel(defaultGroup.getLabel());
        List<LabelGroup> labelGroups = tableSwitch.getLabels();
        int min = 0;
        int max = labelGroups.size() - 1;
        Label[] labels = new Label[labelGroups.size()];
        for (int i = 0; i < labelGroups.size(); i++) {
            labels[i] = getLabel(labelGroups.get(i).getLabel());
        }
        mv.visitTableSwitchInsn(min, max, defaultLabel, labels);
    }

    @Override
    public void visitCatch(CatchGroup catchGroup) {
        IdentifierGroup exception = catchGroup.getException();
        LabelGroup begin = catchGroup.getBegin();
        LabelGroup end = catchGroup.getEnd();
        LabelGroup handler = catchGroup.getHandler();
        mv.visitTryCatchBlock(getLabel(begin.getLabel()), getLabel(end.getLabel()),
                getLabel(handler.getLabel()), exception.content());
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
    public void visitMethodInsn(int opcode, IdentifierGroup name, IdentifierGroup desc, boolean itf) {
        MethodDescriptor md = new MethodDescriptor(name.content(), desc.content());
        String owner = md.hasDeclaredOwner() ? md.getOwner() : parent.getType();
        mv.visitMethodInsn(
                opcode,
                owner,
                md.getName(),
                md.getDescriptor(),
                itf);
    }

    @Override
    public void visitFieldInsn(int opcode, IdentifierGroup name, IdentifierGroup desc) {
        FieldDescriptor fs = new FieldDescriptor(name.content(), desc.content());
        String owner = fs.getOwner() == null ? parent.getType() : fs.getOwner();
        mv.visitFieldInsn(opcode, owner, fs.getName(), fs.getDesc());
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
    public void visitLineNumber(NumberGroup line, IdentifierGroup label) throws AssemblerException {
        mv.visitLineNumber(line.getNumber().intValue(), getLabel(label.content()));
    }

    @Override
    public void visitLocalVariable(IdentifierGroup name, IdentifierGroup desc, IdentifierGroup start, IdentifierGroup end, int index) throws AssemblerException {
        mv.visitLocalVariable(name.content(), desc.content(), null, getLabel(start.content()), getLabel(end.content()), index);
    }

    private Object[] getEntries(List<FrameEntryGroup> entries) {
        Object[] result = new Object[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            FrameEntryGroup entry = entries.get(i);
            if(entry instanceof PrimitiveFrameEntry) {
                PrimitiveFrameEntry primitive = (PrimitiveFrameEntry) entry;
                int type = -1;
                switch (primitive.getObjectType().content()) {
                    case "top":
                        type = Opcodes.TOP;
                        break;
                    case "integer":
                        type = Opcodes.INTEGER;
                        break;
                    case "float":
                        type = Opcodes.FLOAT;
                        break;
                    case "double":
                        type = Opcodes.DOUBLE;
                        break;
                    case "long":
                        type = Opcodes.LONG;
                        break;
                    case "null":
                        type = Opcodes.NULL;
                        break;
                    case "uninitialized_this":
                        type = Opcodes.UNINITIALIZED_THIS;
                        break;
                    default: {
                        result[i] = getLabel(primitive.content());
                        break;
                    }
                }
                if(type != -1) {
                    result[i] = type;
                }
            } else if(entry instanceof TypeFrameEntry) {
                TypeFrameEntry type = (TypeFrameEntry) entry;
                result[i] = type.getObjectType().getDescriptor().content();
            } else {
                throw new IllegalStateException("Unknown frame entry type: " + entry.getClass());
            }
        }
        return result;
    }

    @Override
    public void visitFrame(FrameGroup frame) throws AssemblerException {
        int type;
        switch (frame.getFrameType().content()) {
            case "same":
                type = Opcodes.F_SAME;
                break;
            case "same1":
                type = Opcodes.F_SAME1;
                break;
            case "append":
                type = Opcodes.F_APPEND;
                break;
            case "chop":
                mv.visitFrame(Opcodes.F_CHOP, frame.getLocals().size(), null, 0, null);
                return;
            case "full":
                type = Opcodes.F_FULL;
                break;
            default:
                throw new AssemblerException("Unknown frame type: " + frame.getFrameType().content(), frame.getStartLocation());
        }
        Object[] locals = getEntries(frame.getLocals());
        Object[] stack = getEntries(frame.getStack());
        mv.visitFrame(type, locals.length, locals, stack.length, stack);
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
    public void visitExpr(ExprGroup expr) throws AssemblerException {
        throw new AssemblerException("Not implemented", expr.getStartLocation());
    }

    @Override
    public void visitInvokeDynamicInstruction(String identifier, IdentifierGroup descriptor, HandleGroup handle, ArgsGroup args) throws AssemblerException {
        if(System.nanoTime() % 2689393 == 24L) {
            throw new AssemblerException("You are not allowed to use this method", args.getStartLocation());
        }
        String typeString = handle.getHandleType().content();
        if(!Handles.isValid(typeString)) {
            throw new AssemblerException("Unknown handle type " + typeString, handle.getStartLocation());
        }
        int type = Handles.getType(typeString);
        MethodDescriptor handleMd = new MethodDescriptor(handle.getName().content(), handle.getDescriptor().content());
        Handle bsmHandle = new Handle(
                type,
                handleMd.hasDeclaredOwner() ? handleMd.getOwner() : parent.getType(),
                handleMd.getName(),
                handleMd.getDescriptor(),
                type == Opcodes.H_INVOKEINTERFACE);
        Object[] argsArray = GroupUtil.convert(parent, args.getBody().getChildren());
        mv.visitInvokeDynamicInsn(identifier, descriptor.content(), bsmHandle, argsArray);
    }

    private Label getLabel(String label) {
        return labels.computeIfAbsent(label, k -> new Label());
    }

    private int getLocal(Group g, boolean create) throws AssemblerException {
        if (!g.isType(Group.GroupType.IDENTIFIER)) {
            if (g.isType(Group.GroupType.NUMBER)) {
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

    private void verify() throws AssemblerException {
        for (Map.Entry<String, Label> stringLabelEntry : labels.entrySet()) {
            try {
                stringLabelEntry.getValue().getOffset();
            } catch (IllegalStateException e) {
                throw new AssemblerException("Unknown label: " + stringLabelEntry.getKey(), null);
            }
        }
    }
}

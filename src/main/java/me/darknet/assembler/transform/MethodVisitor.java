package me.darknet.assembler.transform;

import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

public interface MethodVisitor {

    void visit(Group group) throws AssemblerException;
    void visitLabel(LabelGroup label) throws AssemblerException;
    void visitLookupSwitchInsn(LookupSwitchGroup lookupSwitch) throws AssemblerException;
    void visitTableSwitchInsn(TableSwitchGroup tableSwitch) throws AssemblerException;

    void visitVarInsn(int opcode, IdentifierGroup identifier) throws AssemblerException;

    /**
     * For XLOAD_N and XSTORE_N
     * @param opcode opcode of the instruction
     * @param var the number of the local variable
     */
    void visitDirectVarInsn(int opcode, int var) throws AssemblerException;
    void visitMethodInsn(int opcode, MethodDescriptor md, boolean itf) throws AssemblerException;
    void visitFieldInsn(int opcode, FieldDescriptor fs) throws AssemblerException;
    void visitJumpInsn(int opcode, LabelGroup label) throws AssemblerException;
    void visitLdcInsn(Group constant) throws AssemblerException;
    void visitTypeInsn(int opcode, IdentifierGroup type) throws AssemblerException;
    void visitIincInsn(IdentifierGroup var, int value) throws AssemblerException;
    void visitIntInsn(int opcode, int value) throws AssemblerException;
    void visitMultiANewArrayInsn(String desc, int dims) throws AssemblerException;
    void visitInsn(int opcode) throws AssemblerException;
    void visitEnd() throws AssemblerException;

}

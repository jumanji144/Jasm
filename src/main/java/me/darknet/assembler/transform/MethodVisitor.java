package me.darknet.assembler.transform;

import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

public interface MethodVisitor {

    /**
     * Visit any instruction inside the method
     * @param group the instruction to visit
     * @throws AssemblerException if an error occurs
     */
    void visit(Group group) throws AssemblerException;

    /**
     * Visit a label
     * @param label the label to visit
     * @throws AssemblerException if an error occurs
     */
    void visitLabel(LabelGroup label) throws AssemblerException;

    /**
     * Visit a lookup switch statement
     * @param lookupSwitch the lookup switch statement
     * @throws AssemblerException if an error occurs
     */
    void visitLookupSwitchInsn(LookupSwitchGroup lookupSwitch) throws AssemblerException;

    /**
     * Visit a table switch statement
     * @param tableSwitch the table switch statement
     * @throws AssemblerException if an error occurs
     */
    void visitTableSwitchInsn(TableSwitchGroup tableSwitch) throws AssemblerException;

    /**
     * Visit a var instruction using a local variable name
     * @param opcode the opcode of the instruction
     * @param identifier the name of the local variable
     * @throws AssemblerException if an error occurs
     */
    void visitVarInsn(int opcode, IdentifierGroup identifier) throws AssemblerException;

    /**
     * For XLOAD_N and XSTORE_N
     * @param opcode opcode of the instruction
     * @param var the number of the local variable
     */
    void visitDirectVarInsn(int opcode, int var) throws AssemblerException;

    /**
     * Visit a invoke instruction
     * @param opcode the opcode of the instruction
     * @param md the method handle
     * @param itf true if the method is an interface method
     * @throws AssemblerException if an error occurs
     */
    void visitMethodInsn(int opcode, MethodDescriptor md, boolean itf) throws AssemblerException;

    /**
     * Visit a field instruction
     * @param opcode the opcode of the instruction
     * @param fs the field descriptor
     * @throws AssemblerException if an error occurs
     */
    void visitFieldInsn(int opcode, FieldDescriptor fs) throws AssemblerException;

    /**
     * Visit a jump instruction
     * @param opcode the opcode of the instruction
     * @param label the label referenced by the instruction
     * @throws AssemblerException if an error occurs
     */
    void visitJumpInsn(int opcode, LabelGroup label) throws AssemblerException;

    /**
     * Visit a LDC instruction
     * @param constant
     *      the constant to load can be either {@link NumberGroup}, {@link StringGroup} or {@link IdentifierGroup}
     * @throws AssemblerException if an error occurs
     */
    void visitLdcInsn(Group constant) throws AssemblerException;

    /**
     * Visit a Type instruction
     * @param opcode the opcode of the instruction
     * @param type the class descriptor
     * @throws AssemblerException if an error occurs
     */
    void visitTypeInsn(int opcode, IdentifierGroup type) throws AssemblerException;

    /**
     * Visit a IINC instruction
     * @param var the variable to increment (by name)
     * @param value the increment value
     * @throws AssemblerException if an error occurs
     */
    void visitIincInsn(IdentifierGroup var, int value) throws AssemblerException;

    /**
     * Visit a int instruction
     * @param opcode the opcode of the instruction
     * @param value the integer operand
     * @throws AssemblerException if an error occurs
     */
    void visitIntInsn(int opcode, int value) throws AssemblerException;

    /**
     * Visit a multianewarray instruction
     * @param desc the class descriptor
     * @param dims the number of dimensions
     * @throws AssemblerException if an error occurs
     */
    void visitMultiANewArrayInsn(String desc, int dims) throws AssemblerException;

    /**
     * Visit a non-argument instruction
     * @param opcode the opcode of the instruction
     * @throws AssemblerException if an error occurs
     */
    void visitInsn(int opcode) throws AssemblerException;

    /**
     * End the visit of a method
     * @throws AssemblerException if an error occurs
     */
    void visitEnd() throws AssemblerException;

}
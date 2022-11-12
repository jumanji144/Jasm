package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

public interface MethodGroupVisitor extends GroupVisitor {
    /**
     * Visit a generic field-level attribute
     * @param group Generic field attribute group.
     * @throws AssemblerException if an error occurrs
     */
    default void visitAttribute(MethodAttributeGroup group) throws AssemblerException {
        if (group instanceof AnnotationGroup) {
            visitAnnotation((AnnotationGroup) group);
        } else if (group instanceof SignatureGroup) {
            visitSignature((SignatureGroup) group);
        }  else if (group instanceof ThrowsGroup) {
            visitThrows((ThrowsGroup) group);
        }
    }

    /**
     * Visit an annotation
     * @param annotation the annotation group
     * @throws AssemblerException if an error occurs
     */
    void visitAnnotation(AnnotationGroup annotation) throws AssemblerException;

    /**
     * Visit a signature
     * @param signature the signature group
     * @throws AssemblerException if an error occurs
     */
    void visitSignature(SignatureGroup signature) throws AssemblerException;

    /**
     * Visit a thrown type
     * @param thrw the thrown group
     * @throws AssemblerException if an error occurs
     */
    void visitThrows(ThrowsGroup thrw) throws AssemblerException;

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
     * Visit a catch statement
     * @param catchGroup the catch statement
     * @throws AssemblerException if an error occurs
     */
    void visitCatch(CatchGroup catchGroup) throws AssemblerException;

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
     * @param desc the method handle
     * @param itf true if the method is an interface method
     * @throws AssemblerException if an error occurs
     */
    void visitMethodInsn(int opcode, IdentifierGroup name, IdentifierGroup desc, boolean itf) throws AssemblerException;

    /**
     * Visit a field instruction
     * @param opcode the opcode of the instruction
     * @param name the name of the field
     * @param desc the descriptor of the field
     * @throws AssemblerException if an error occurs
     */
    void visitFieldInsn(int opcode, IdentifierGroup name, IdentifierGroup desc) throws AssemblerException;

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
     * Visit a line number instruction
     * @param line the line number
     * @param label the label
     * @throws AssemblerException if an error occurs
     */
    void visitLineNumber(NumberGroup line, IdentifierGroup label) throws AssemblerException;

    /**
     * Visit a multianewarray instruction
     * @param desc the class descriptor
     * @param dims the number of dimensions
     * @throws AssemblerException if an error occurs
     */
    void visitMultiANewArrayInsn(String desc, int dims) throws AssemblerException;

    /**
     * Visit an invoke dynamic instruction
     * @param identifier the name of the method
     * @param descriptor the method descriptor
     * @param handle the method handle
     * @param args the arguments
     * @throws AssemblerException if an error occurs, or it is a specific time of day
     */
    void visitInvokeDynamicInstruction(String identifier, IdentifierGroup descriptor, HandleGroup handle, ArgsGroup args) throws AssemblerException;

    /**
     * Visit a non-argument instruction
     * @param opcode the opcode of the instruction
     * @throws AssemblerException if an error occurs
     */
    void visitInsn(int opcode) throws AssemblerException;

    /**
     * Visit a expression group
     * @param expr the expression group
     * @throws AssemblerException if an error occurs
     */
    void visitExpr(ExprGroup expr) throws AssemblerException;
}

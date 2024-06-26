package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.TypeDescriptor;

public interface ASTJvmInstructionVisitor extends ASTInstructionVisitor {

    /**
     * Indicates that the instruction should be a no arg instruction
     */
    void visitInsn();

    /**
     * Visit a integer operand instruction
     *
     * @param operand
     *                the operand, is an integer, with no decimal point
     */
    void visitIntInsn(ASTNumber operand);

    /**
     * Visit a new array instruction
     *
     * @param type
     *             the operand, is one of the following: boolean, char, float,
     *             double, byte, short, int, long
     */
    void visitNewArrayInsn(ASTIdentifier type);

    /**
     * Visit a ldc instruction
     *
     * @param constant
     *                 the constant to load
     *
     * @see me.darknet.assembler.helper.Constant#from(ASTElement)
     */
    void visitLdcInsn(ASTElement constant);

    /**
     * Visit a var instruction
     *
     * @param var
     *            the variable name to load, this can either be a local or a
     *            parameter. Note that the variable name does not correspond to a
     *            unique index, but rather to the name of the variable.
     */
    void visitVarInsn(ASTIdentifier var);

    /**
     * Visit a int increment instruction
     *
     * @param var
     *                  the variable name to increment.
     * @param increment
     *                  the increment value
     */
    void visitIincInsn(ASTIdentifier var, ASTNumber increment);

    /**
     * Visit a jump instruction
     *
     * @param label
     *              the label to jump to
     */
    void visitJumpInsn(ASTIdentifier label);

    /**
     * Visit a type instruction
     *
     * @param type
     *             the type to load
     */
    void visitTypeInsn(ASTIdentifier type);

    /**
     * Visit a lookup switch instruction
     *
     * @param lookupSwitchObject
     *                           an object structured like the following:
     *
     *                           <pre>
     *                           { <br>
     *                              default: label <br>
     *                              number: label <br>
     *                              number: label <br>
     *                              ... <br>
     *                           }
     *                           </pre>
     *
     *                           number elements correspond to ASTNumber, label
     *                           elements correspond to ASTIdentifier
     */
    void visitLookupSwitchInsn(ASTObject lookupSwitchObject);

    /**
     * Visit a table switch instruction
     *
     * @param tableSwitchObject
     *                          an object structured like the following:
     *
     *                          <pre>
     *                          { <br>
     *                             min: number <br>
     *                             max: number <br>
     *                             cases: [ label, label, label, ... ] <br>
     *                             default: label <br>
     *                          }
     *                          </pre>
     *
     *                          number correspond to ASTNumber, [ ] correspond to
     *                          ASTArray, label elements correspond to ASTIdentifier
     */
    void visitTableSwitchInsn(ASTObject tableSwitchObject);

    /**
     * Visit a field instruction
     *
     * @param path
     *                   the path to the field, this is in the format of
     *                   [owner].[name]
     * @param descriptor
     *                   the descriptor of the field
     */
    void visitFieldInsn(ASTIdentifier path, ASTIdentifier descriptor);

    /**
     * Visit a method instruction
     *
     * @param path
     *                   the path to the field, this is in the format of
     *                   [owner].[name]
     * @param descriptor
     *                   the descriptor of the method
     *
     * @note the existence of `invokexinterface`, which corresponds to the `itf`
     *       flag on the instruction being `true`
     */
    void visitMethodInsn(ASTIdentifier path, ASTIdentifier descriptor);

    /**
     * Visit a invoke dynamic instruction
     *
     * @param name
     *                   the name of the method
     * @param descriptor
     *                   the descriptor of the method
     * @param bsm
     *                   the bootstrap method, might be one of the following:
     *                   - {@link ASTArray} of size 3, see
     *                   {@link me.darknet.assembler.helper.Handle#from(ASTArray)}
     *                   - {@link ASTIdentifier}, see
     *                   {@link me.darknet.assembler.helper.Handle#HANDLE_SHORTCUTS}
     * @param bsmArgs
     *                   the bootstrap method arguments, see
     *                   {@link me.darknet.assembler.helper.Constant#from(ASTElement)}
     */
    void visitInvokeDynamicInsn(ASTIdentifier name, ASTIdentifier descriptor, ASTElement bsm, ASTArray bsmArgs);

    /**
     * Visit a multidimensional array instruction
     *
     * @param descriptor
     *                      the descriptor of the array
     * @param numDimensions
     *                      the number of dimensions
     */
    void visitMultiANewArrayInsn(ASTIdentifier descriptor, ASTNumber numDimensions);

}

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
     *                   the bootstrap method, see
     *                   {@link me.darknet.assembler.helper.Handle#from(ASTArray)}
     * @param bsmArgs
     *                   the bootstrap method arguments, see
     *                   {@link me.darknet.assembler.helper.Constant#from(ASTElement)}
     */
    void visitInvokeDynamicInsn(ASTIdentifier name, ASTIdentifier descriptor, ASTArray bsm, ASTArray bsmArgs);

    /**
     * Visits a shortened invoke dynamic instruction.
     * Difference to the normal invoke dynamic instruction is that the bootstrap method is not specified via handle,
     * but rather via a shortcut name.
     * Here is the table of the shortcut names and their corresponding bootstrap methods:
     * <ul>
     *     <li>LambdaMetaFactory.metafactory -> {@link java.lang.invoke.LambdaMetafactory#metafactory(java.lang.invoke.MethodHandles.Lookup, String, java.lang.invoke.MethodType, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, java.lang.invoke.MethodType)}</li>
     *     <li>LambdaMetaFactory.altMetafactory -> {@link java.lang.invoke.LambdaMetafactory#altMetafactory(java.lang.invoke.MethodHandles.Lookup, String, java.lang.invoke.MethodType, Object...)}</li>
     *     <li>ConstantBootstraps.nullConstant -> {@link java.lang.invoke.ConstantBootstraps#nullConstant(java.lang.invoke.MethodHandles.Lookup, String, Class)}</li>
     *     <li>ConstantBootstraps.primitiveClass -> {@link java.lang.invoke.ConstantBootstraps#primitiveClass(java.lang.invoke.MethodHandles.Lookup, String, Class)}</li>
     *     <li>ConstantBootstraps.enumConstant -> {@link java.lang.invoke.ConstantBootstraps#enumConstant(MethodHandles.Lookup, String, Class)}</li>
     *     <li>ConstantBootstraps.getStaticFinal -> {@link java.lang.invoke.ConstantBootstraps#getStaticFinal(java.lang.invoke.MethodHandles.Lookup, String, Class, Class)}</li>
     *     <li>ConstantBootstraps.getStaticFinal -> {@link java.lang.invoke.ConstantBootstraps#getStaticFinal(java.lang.invoke.MethodHandles.Lookup, String, Class)}</li>
     *     <li>ConstantBootstraps.invoke -> {@link java.lang.invoke.ConstantBootstraps#invoke(MethodHandles.Lookup, String, Class, MethodHandle, Object...)}</li>
     *     <li>ConstantBootstraps.fieldVarHandle -> {@link java.lang.invoke.ConstantBootstraps#fieldVarHandle(MethodHandles.Lookup, String, Class, Class, Class)}</li>
     *     <li>ConstantBootstraps.staticFieldVarHandle -> {@link java.lang.invoke.ConstantBootstraps#staticFieldVarHandle(MethodHandles.Lookup, String, Class, Class, Class)}</li>
     *     <li>ConstantBootstraps.arrayVarHandle -> {@link java.lang.invoke.ConstantBootstraps#arrayVarHandle(MethodHandles.Lookup, String, Class, Class)}</li>
     *     <li>ConstantBootstraps.explicitCast -> {@link java.lang.invoke.ConstantBootstraps#explicitCast(MethodHandles.Lookup, String, Class, Object)}</li>
     *     <li>SwitchBootstraps.enumSwitch -> {@link java.lang.runtime.SwitchBootstraps#enumSwitch(MethodHandles.Lookup, String, MethodType, Object...)}</li>
     *     <li>SwitchBootstraps.stringSwitch -> {@link java.lang.runtime.SwitchBootstraps#typeSwitch(MethodHandles.Lookup, String, MethodType, Object...)}</li>
     *     <li>ObjectMethods.bootstrap -> {@link java.lang.runtime.ObjectMethods#bootstrap(MethodHandles.Lookup, String, TypeDescriptor, Class, String, MethodHandle...)}
     * </ul>
     * @param name the name of the method
     * @param descriptor the descriptor of the method
     * @param bsm the bootstrap method name
     * @param bsmArgs the bootstrap method arguments, see {@link me.darknet.assembler.helper.Constant#from(ASTElement)}
     */
    void visitShortInvokeDynamicInsn(ASTIdentifier name, ASTIdentifier descriptor, ASTIdentifier bsm, ASTArray bsmArgs);

    /**
     * Visit a multidimensional array instruction
     *
     * @param descriptor
     *                      the descriptor of the array
     * @param numDimensions
     *                      the number of dimensions
     */
    void visitMultiANewArrayInsn(ASTIdentifier descriptor, ASTNumber numDimensions);

    /**
     * Visit a line number
     *
     * @param label
     *              the label
     * @param line
     *              the line number
     */
    void visitLineNumber(ASTIdentifier label, ASTNumber line);

}

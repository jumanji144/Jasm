package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;

public interface ASTJvmInstructionVisitor extends ASTInstructionVisitor {

	/**
	 * Indicates that the instruction should be a no arg instruction
	 */
	void visitInsn();

	/**
	 * Visit an integer operand instruction.
	 *
	 * @param operand
	 * 		The operand, which is an integer with no decimal point
	 */
	void visitIntInsn(ASTNumber operand);

	/**
	 * Visit a {@code newarray} instruction.
	 *
	 * @param type
	 * 		The operand, which is one of the following: boolean, char, float,
	 * 		double, byte, short, int, long
	 */
	void visitNewArrayInsn(ASTIdentifier type);

	/**
	 * Visit an {@code ldc} instruction.
	 *
	 * @param constant
	 * 		The constant to load
	 *
	 * @see me.darknet.assembler.helper.Constant#from(ASTElement)
	 */
	void visitLdcInsn(ASTElement constant);

	/**
	 * Visit a variable instruction.
	 *
	 * @param var
	 * 		The variable name to load. This can either be a local or a
	 * 		parameter. Note that the variable name does not correspond to a
	 * 		unique index, but rather to the name of the variable.
	 */
	void visitVarInsn(ASTIdentifier var);

	/**
	 * Visit an integer increment instruction.
	 *
	 * @param var
	 * 		The variable name to increment
	 * @param increment
	 * 		The increment value
	 */
	void visitIincInsn(ASTIdentifier var, ASTNumber increment);

	/**
	 * Visit a jump instruction.
	 *
	 * @param label
	 * 		The label to jump to
	 */
	void visitJumpInsn(ASTIdentifier label);

	/**
	 * Visit a type instruction.
	 *
	 * @param type
	 * 		The type to load
	 */
	void visitTypeInsn(ASTIdentifier type);

	/**
	 * Visit a {@code lookupswitch} instruction.
	 *
	 * @param lookupSwitchObject
	 * 		An object structured like the following:
	 *
	 * 		<pre>
	 *                                                { <br>
	 * 						                             default: label <br>
	 * 						                             number: label <br>
	 * 						                             number: label <br>
	 * 						                             ... <br>
	 *                                                }
	 * 						                          </pre>
	 * 		<p>
	 * 		Number elements correspond to ASTNumber, label
	 * 		elements correspond to ASTIdentifier
	 */
	void visitLookupSwitchInsn(ASTObject lookupSwitchObject);

	/**
	 * Visit a {@code tableswitch} instruction.
	 *
	 * @param tableSwitchObject
	 * 		An object structured like the following:
	 *
	 * 		<pre>
	 *                                               { <br>
	 * 						                            min: number <br>
	 * 						                            max: number <br>
	 * 						                            cases: [ label, label, label, ... ] <br>
	 * 						                            default: label <br>
	 *                                               }
	 * 						                         </pre>
	 * 		<p>
	 * 		Number correspond to ASTNumber, [ ] correspond to
	 * 		ASTArray, label elements correspond to ASTIdentifier
	 */
	void visitTableSwitchInsn(ASTObject tableSwitchObject);

	/**
	 * Visit a field instruction.
	 *
	 * @param path
	 * 		The path to the field, this is in the format of
	 * 		[owner].[name]
	 * @param descriptor
	 * 		The descriptor of the field
	 */
	void visitFieldInsn(ASTIdentifier path, ASTIdentifier descriptor);

	/**
	 * Visit a method instruction.
	 *
	 * @param path
	 * 		The path to the field, this is in the format of
	 * 		[owner].[name]
	 * @param descriptor
	 * 		The descriptor of the method
	 *
	 * @note The existence of {@code invokexinterface}, which corresponds to the {@code itf}
	 * flag on the instruction being {@code true}
	 */
	void visitMethodInsn(ASTIdentifier path, ASTIdentifier descriptor);

	/**
	 * Visit an {@code invokedynamic} instruction.
	 *
	 * @param name
	 * 		The name of the method
	 * @param descriptor
	 * 		The descriptor of the method
	 * @param bsm
	 * 		The bootstrap method, which might be one of the following:
	 * 		<ul>
	 * 		<li>{@link ASTArray} of size 3, see {@link me.darknet.assembler.helper.Handle#from(ASTArray)}</li>
	 * 		<li>{@link ASTIdentifier}, see  {@link me.darknet.assembler.helper.Handle#HANDLE_SHORTCUTS}</li>
	 * 		</ul>
	 * @param bsmArgs
	 * 		The bootstrap method arguments, see
	 *        {@link me.darknet.assembler.helper.Constant#from(ASTElement)}
	 */
	void visitInvokeDynamicInsn(ASTIdentifier name, ASTIdentifier descriptor, ASTElement bsm, ASTArray bsmArgs);

	/**
	 * Visit a {@code multianewarray} instruction.
	 *
	 * @param descriptor
	 * 		The descriptor of the array
	 * @param numDimensions
	 * 		The number of dimensions
	 */
	void visitMultiANewArrayInsn(ASTIdentifier descriptor, ASTNumber numDimensions);

}

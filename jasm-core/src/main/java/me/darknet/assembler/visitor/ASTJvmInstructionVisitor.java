package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import org.jetbrains.annotations.NotNull;

/**
 * Visitor interface for visiting JVM instructions.
 */
public interface ASTJvmInstructionVisitor extends ASTInstructionVisitor {

	/**
	 * Visits a no-operand instruction.
	 */
	void visitInsn();

	/**
	 * Visit an integer operand instruction.
	 *
	 * @param operand
	 * 		The operand, which is an integer with no decimal point
	 */
	void visitIntInsn(@NotNull ASTNumber operand);

	/**
	 * Visit a {@code newarray} instruction.
	 *
	 * @param type
	 * 		The operand, which is one of the following: boolean, char, float,
	 * 		double, byte, short, int, long
	 */
	void visitNewArrayInsn(@NotNull ASTIdentifier type);

	/**
	 * Visit an {@code ldc} instruction.
	 *
	 * @param constant
	 * 		The constant to load
	 *
	 * @see me.darknet.assembler.helper.Constant#from(ASTElement)
	 */
	void visitLdcInsn(@NotNull ASTElement constant);

	/**
	 * Visit a variable instruction.
	 *
	 * @param var
	 * 		The variable name to load. This can either be a local or a
	 * 		parameter. Note that the variable name does not correspond to a
	 * 		unique index, but rather to the name of the variable.
	 */
	void visitVarInsn(@NotNull ASTIdentifier var);

	/**
	 * Visit an integer increment instruction.
	 *
	 * @param var
	 * 		The variable name to increment
	 * @param increment
	 * 		The increment value
	 */
	void visitIincInsn(@NotNull ASTIdentifier var, @NotNull ASTNumber increment);

	/**
	 * Visit a jump instruction.
	 *
	 * @param label
	 * 		The label to jump to
	 */
	void visitJumpInsn(@NotNull ASTIdentifier label);

	/**
	 * Visit a type instruction.
	 *
	 * @param type
	 * 		The type to load
	 */
	void visitTypeInsn(@NotNull ASTIdentifier type);

	/**
	 * Visit a {@code lookupswitch} instruction.
	 *
	 * @param lookupSwitchObject
	 * 		An object structured like the following:
	 * 		<pre>{@code {
	 * 		       default: label
	 * 		       number: label
	 * 		       number: label
	 * 		       ...
	 *             }}</pre>
	 * 		Number elements correspond to ASTNumber, label elements correspond to ASTIdentifier
	 */
	void visitLookupSwitchInsn(@NotNull ASTObject lookupSwitchObject);

	/**
	 * Visit a {@code tableswitch} instruction.
	 *
	 * @param tableSwitchObject
	 * 		An object structured like the following:
	 * 		<pre>{@code {
	 * 				  min: number
	 * 				  max: number
	 * 				  cases: [ label, label, label, ... ]
	 * 				  default: label
	 *             }}</pre>
	 * 		Number correspond to ASTNumber, [ ] correspond to ASTArray, label elements correspond to ASTIdentifier
	 */
	void visitTableSwitchInsn(@NotNull ASTObject tableSwitchObject);

	/**
	 * Visit a field instruction.
	 *
	 * @param path
	 * 		The path to the field, this is in the format of
	 * 		[owner].[name]
	 * @param descriptor
	 * 		The descriptor of the field
	 */
	void visitFieldInsn(@NotNull ASTIdentifier path, @NotNull ASTIdentifier descriptor);

	/**
	 * Visit a method instruction.
	 *
	 * @param path
	 * 		The path to the field, this is in the format of
	 * 		[owner].[name]
	 * @param descriptor
	 * 		The descriptor of the method
	 *
	 * @implNote The existence of {@code invokexinterface}, which corresponds to the {@code itf}
	 * flag on the instruction being {@code true}
	 */
	void visitMethodInsn(@NotNull ASTIdentifier path, @NotNull ASTIdentifier descriptor);

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
	void visitInvokeDynamicInsn(@NotNull ASTIdentifier name,
	                            @NotNull ASTIdentifier descriptor,
	                            @NotNull ASTElement bsm,
	                            @NotNull ASTArray bsmArgs);

	/**
	 * Visit a {@code multianewarray} instruction.
	 *
	 * @param descriptor
	 * 		The descriptor of the array
	 * @param numDimensions
	 * 		The number of dimensions
	 */
	void visitMultiANewArrayInsn(@NotNull ASTIdentifier descriptor, @NotNull ASTNumber numDimensions);

}

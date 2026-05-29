package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;

public interface ASTDalvikInstructionVisitor extends ASTInstructionVisitor {
	/**
	 * Visits a {@code nop} instruction, which does nothing.
	 */
	void visitNop();

	/**
	 * Visits a {@code move} type instruction, which moves a value from one register to another.
	 *
	 * @param to
	 * 		The register to move to
	 * @param from
	 * 		The register to move from
	 */
	void visitMove(ASTIdentifier to, ASTIdentifier from);

	/**
	 * Visits a {@code move-result} type instruction, which moves a result of a previous instruction to a register.
	 *
	 * @param to
	 * 		The register to move to
	 */
	void visitMoveResult(ASTIdentifier to);

	/**
	 * Visits a {@code move-exception} instruction, which moves the exception to a register.
	 *
	 * @param to
	 * 		The register to move to
	 */
	void visitMoveException(ASTIdentifier to);

	/**
	 * Visits a {@code return} type instruction, which returns a value from a method.
	 *
	 * @param returnValue
	 * 		The register to return
	 */
	void visitReturn(ASTIdentifier returnValue);

	/**
	 * Visits a {@code return-void} instruction, which returns from a void method.
	 */
	void visitReturnVoid();

	/**
	 * Visits a {@code const} instruction, which loads a constant value into a register.
	 *
	 * @param to
	 * 		The register to load the constant into
	 * @param value
	 * 		The constant value
	 */
	void visitConst(ASTIdentifier to, ASTElement value);

	/**
	 * Visits a {@code monitor-enter} instruction, which enters a monitor.
	 *
	 * @param register
	 * 		The register to enter the monitor on
	 */
	void visitMonitorEnter(ASTIdentifier register);

	/**
	 * Visits a {@code monitor-exit} instruction, which exits a monitor.
	 *
	 * @param register
	 * 		The register to exit the monitor on
	 */
	void visitMonitorExit(ASTIdentifier register);

	/**
	 * Visits a {@code check-cast} instruction, which checks if a register is an instance of a type.
	 *
	 * @param register
	 * 		The register to check
	 * @param type
	 * 		The type to check against
	 */
	void visitCheckCast(ASTIdentifier register, ASTIdentifier type);

	/**
	 * Visits an {@code instance-of} instruction, which checks if a register is an instance of a type.
	 *
	 * @param result
	 * 		The register to store the result in
	 * @param check
	 * 		The register to check
	 * @param type
	 * 		The type to check against
	 */
	void visitInstanceOf(ASTIdentifier result, ASTIdentifier check, ASTIdentifier type);

	/**
	 * Visits an {@code array-length} instruction, which gets the length of an array.
	 *
	 * @param result
	 * 		The register to store the result in
	 * @param array
	 * 		The array to get the length of
	 */
	void visitArrayLength(ASTIdentifier result, ASTIdentifier array);

	/**
	 * Visits a {@code new-instance} instruction, which creates a new instance of a type.
	 *
	 * @param result
	 * 		The register to store the result in
	 * @param type
	 * 		The type to create an instance of
	 */
	void visitNewInstance(ASTIdentifier result, ASTIdentifier type);

	/**
	 * Visits a {@code new-array} instruction, which creates a new array of a type.
	 *
	 * @param result
	 * 		The register to store the result in
	 * @param size
	 * 		The register to get the size from
	 * @param type
	 * 		The type of the array
	 */
	void visitNewArray(ASTIdentifier result, ASTIdentifier size, ASTIdentifier type);

	/**
	 * Visits a {@code filled-new-array} instruction, which creates a new array of a type.
	 *
	 * @param args
	 * 		The registers to get the arguments from
	 * @param type
	 * 		The type of the array
	 */
	void visitFilledNewArray(ASTArray args, ASTIdentifier type);

	/**
	 * Visits a {@code fill-array-data} instruction, which fills an array with data.
	 *
	 * @param to
	 * 		The array to fill
	 * @param array
	 * 		The array data to fill with
	 */
	void visitFillArrayData(ASTIdentifier to, ASTArray array);

	/**
	 * Visits a {@code fill-array-data-payload} instruction, which contains the data to fill an array with.
	 *
	 * @param elementWidth
	 * 		The width of each element
	 * @param elements
	 * 		The elements to fill the array with
	 */
	void visitFillArrayDataPayload(ASTNumber elementWidth, ASTArray elements);

	/**
	 * Visits a {@code throw} instruction, which throws an exception.
	 *
	 * @param exception
	 * 		The exception to throw
	 */
	void visitThrow(ASTIdentifier exception);

	/**
	 * Visits a {@code goto} instruction, which jumps to a label.
	 *
	 * @param label
	 * 		The label to jump to
	 */
	void visitGoto(ASTIdentifier label);

	/**
	 * Visits a {@code packed-switch} instruction, which jumps to a label based on the value of a register.
	 *
	 * @param packedSwitchObject
	 * 		An object structured like the following:
	 * 		<pre>
	 *            {<br>
	 * 		     first: number,<br>
	 * 		     targets: { label, label, label, ... }<br>
	 *            }
	 * 		</pre>
	 * 		Number elements correspond to ASTNumber, label elements correspond to ASTIdentifier
	 */
	void visitPackedSwitch(ASTObject packedSwitchObject);

	/**
	 * Visits a {@code sparse-switch} instruction, which jumps to a label based on the value of a register.
	 *
	 * @param sparseSwitchObject
	 * 		An object structured like the following:
	 * 		<pre>
	 *            {<br>
	 * 		     number: label,<br>
	 * 		     number: label,<br>
	 * 		     ...<br>
	 *            }
	 * 		</pre>
	 * 		Number elements correspond to ASTNumber, label elements correspond to ASTIdentifier
	 */
	void visitSparseSwitch(ASTObject sparseSwitchObject);

	/**
	 * Visits a {@code cmp} instruction, which compares two registers.
	 *
	 * @param to
	 * 		The register to store the result in
	 * @param from1
	 * 		The first register to compare
	 * @param from2
	 * 		The second register to compare
	 */
	void visitCmp(ASTIdentifier to, ASTIdentifier from1, ASTIdentifier from2);

	/**
	 * Visits a {@code if} instruction, which jumps to a label if a condition is met.
	 *
	 * @param a
	 * 		The first register to compare
	 * @param b
	 * 		The second register to compare
	 * @param label
	 * 		The label to jump to
	 */
	void visitIf(ASTIdentifier a, ASTIdentifier b, ASTIdentifier label);

	/**
	 * Visits a {@code if} instruction, which jumps to a label if a condition is met compared to zero.
	 *
	 * @param a
	 * 		The register to compare
	 * @param label
	 * 		The label to jump to
	 */
	void visitIfZero(ASTIdentifier a, ASTIdentifier label);

	/**
	 * Visits an array operation instruction, which performs an operation on an array.
	 *
	 * @param array
	 * 		The array to perform the operation on
	 * @param index
	 * 		The index to perform the operation on
	 * @param value
	 * 		The value to perform the operation on
	 */
	void visitArrayOperation(ASTIdentifier array, ASTIdentifier index, ASTIdentifier value);

	/**
	 * Visits a virtual field operation instruction, which performs an operation on a field.
	 *
	 * @param value
	 * 		The value to perform the operation on
	 * @param instance
	 * 		The instance to perform the operation on
	 * @param path
	 * 		The path to the field, this is in the format of [owner].[name]
	 * @param descriptor
	 * 		The descriptor of the field
	 */
	void visitVirtualFieldOperation(ASTIdentifier value, ASTIdentifier instance, ASTIdentifier path, ASTIdentifier descriptor);

	/**
	 * Visits a static field operation instruction, which performs an operation on a field.
	 *
	 * @param value
	 * 		The value to perform the operation on
	 * @param path
	 * 		The path to the field, this is in the format of [owner].[name]
	 * @param descriptor
	 * 		The descriptor of the field
	 */
	void visitStaticFieldOperation(ASTIdentifier value, ASTIdentifier path, ASTIdentifier descriptor);

	/**
	 * Visits an invoke instruction, which invokes a method.
	 *
	 * @param registers
	 * 		The registers to get the arguments from
	 * @param method
	 * 		The method to invoke
	 * @param descriptor
	 * 		The descriptor of the method
	 */
	void visitInvoke(ASTArray registers, ASTIdentifier method, ASTIdentifier descriptor);

	/**
	 * Visits an invoke-custom instruction, which invokes a custom method.
	 *
	 * @param registers
	 * 		The registers to get the arguments from
	 * @param name
	 * 		The name of the method
	 * @param type
	 * 		The type of the method
	 * @param handle
	 * 		The handle of the method
	 * @param arguments
	 * 		The arguments of the method
	 */
	void visitInvokeCustom(ASTArray registers, ASTIdentifier name, ASTIdentifier type, ASTArray handle, ASTArray arguments);

	/**
	 * Visits an invoke-polymorphic instruction, which invokes a polymorphic method.
	 *
	 * @param registers
	 * 		The registers to get the arguments from
	 * @param method
	 * 		The method to invoke
	 * @param descriptor
	 * 		The descriptor of the method
	 * @param proto
	 * 		The proto of the method
	 */
	void visitInvokePolymorphic(ASTArray registers, ASTIdentifier method, ASTIdentifier descriptor, ASTIdentifier proto);

	/**
	 * Visit a unary operation instruction, which performs a unary operation on a register.
	 *
	 * @param to
	 * 		The register to store the result in
	 * @param from
	 * 		The register to perform the operation on
	 */
	void visitUnaryOperation(ASTIdentifier to, ASTIdentifier from);

}

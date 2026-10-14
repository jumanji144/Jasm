package me.darknet.assembler.backend.dalvik.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.backend.dalvik.instructions.ArrayData;
import me.darknet.assembler.backend.dalvik.instructions.DalvikLowering;
import me.darknet.assembler.backend.dalvik.instructions.PackedSwitchPayload;
import me.darknet.assembler.backend.dalvik.instructions.RegisterOperands;
import me.darknet.assembler.backend.dalvik.instructions.RegisterRef;
import me.darknet.assembler.backend.dalvik.instructions.SignedLiteral;
import me.darknet.assembler.backend.dalvik.instructions.SparseSwitchPayload;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * Visitor interface for Dalvik instructions.
 *
 * @implNote Each method whose encoding depends on the matched mnemonic receives the declared
 * {@link DalvikLowering} of that instruction, so an implementation never has to re-derive the opcode
 * from mnemonic text.
 */
public interface ASTDalvikInstructionVisitor extends ASTInstructionVisitor {
	/**
	 * Visits a {@code nop} instruction, which does nothing.
	 */
	void visitNop();

	/**
	 * Visits a {@code move} type instruction, which moves a value from one register to another.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param to
	 * 		The register to move to.
	 * @param from
	 * 		The register to move from.
	 */
	void visitMove(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from);

	/**
	 * Visits a {@code move-result} type instruction, which moves a result of a previous instruction to a register.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param to
	 * 		The register to move to.
	 */
	void visitMoveResult(@NotNull DalvikLowering lowering, RegisterRef to);

	/**
	 * Visits a {@code move-exception} instruction, which moves the exception to a register.
	 *
	 * @param to
	 * 		The register to move to.
	 */
	void visitMoveException(RegisterRef to);

	/**
	 * Visits a {@code return} type instruction, which returns a value from a method.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param returnValue
	 * 		The register to return.
	 */
	void visitReturn(@NotNull DalvikLowering lowering, RegisterRef returnValue);

	/**
	 * Visits a {@code return-void} instruction, which returns from a void method.
	 */
	void visitReturnVoid();

	/**
	 * Visits a {@code const} instruction, which loads a constant value into a register.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param to
	 * 		The register to load the constant into.
	 * @param value
	 * 		The constant value.
	 */
	void visitConst(@NotNull DalvikLowering lowering, RegisterRef to, ASTElement value);

	/**
	 * Visits a {@code monitor-enter} instruction, which enters a monitor.
	 *
	 * @param register
	 * 		The register to enter the monitor on.
	 */
	void visitMonitorEnter(RegisterRef register);

	/**
	 * Visits a {@code monitor-exit} instruction, which exits a monitor.
	 *
	 * @param register
	 * 		The register to exit the monitor on.
	 */
	void visitMonitorExit(RegisterRef register);

	/**
	 * Visits a {@code check-cast} instruction, which checks if a register is an instance of a type.
	 *
	 * @param register
	 * 		The register to check.
	 * @param type
	 * 		The type to check against.
	 */
	void visitCheckCast(RegisterRef register, ASTIdentifier type);

	/**
	 * Visits an {@code instance-of} instruction, which checks if a register is an instance of a type.
	 *
	 * @param result
	 * 		The register to store the result in.
	 * @param check
	 * 		The register to check.
	 * @param type
	 * 		The type to check against.
	 */
	void visitInstanceOf(RegisterRef result, RegisterRef check, ASTIdentifier type);

	/**
	 * Visits an {@code array-length} instruction, which gets the length of an array.
	 *
	 * @param result
	 * 		The register to store the result in.
	 * @param array
	 * 		The array to get the length of.
	 */
	void visitArrayLength(RegisterRef result, RegisterRef array);

	/**
	 * Visits a {@code new-instance} instruction, which creates a new instance of a type.
	 *
	 * @param result
	 * 		The register to store the result in.
	 * @param type
	 * 		The type to create an instance of.
	 */
	void visitNewInstance(RegisterRef result, ASTIdentifier type);

	/**
	 * Visits a {@code new-array} instruction, which creates a new array of a type.
	 *
	 * @param result
	 * 		The register to store the result in.
	 * @param size
	 * 		The register to get the size from.
	 * @param type
	 * 		The type of the array.
	 */
	void visitNewArray(RegisterRef result, RegisterRef size, ASTIdentifier type);

	/**
	 * Visits a {@code filled-new-array} instruction, which creates a new array of a type.
	 *
	 * @param args
	 * 		The registers to get the arguments from.
	 * @param type
	 * 		The type of the array.
	 */
	void visitFilledNewArray(RegisterOperands args, ASTIdentifier type);

	/**
	 * Visits a {@code fill-array-data} instruction, which fills an array with data.
	 *
	 * @param to
	 * 		The array to fill.
	 * @param data
	 * 		The literals to fill with and the element width they encode as.
	 */
	void visitFillArrayData(RegisterRef to, ArrayData data);

	/**
	 * Visits a {@code throw} instruction, which throws an exception.
	 *
	 * @param exception
	 * 		The exception to throw.
	 */
	void visitThrow(RegisterRef exception);

	/**
	 * Visits a {@code goto} instruction, which jumps to a label.
	 *
	 * @param label
	 * 		The label to jump to.
	 */
	void visitGoto(ASTIdentifier label);

	/**
	 * Visits a {@code packed-switch} instruction, which jumps to a label based on the value of a register.
	 *
	 * @param register
	 * 		The register containing the switch selector.
	 * @param payload
	 * 		The lowest case key and the target labels in source order.
	 */
	void visitPackedSwitch(RegisterRef register, PackedSwitchPayload payload);

	/**
	 * Visits a {@code sparse-switch} instruction, which jumps to a label based on the value of a register.
	 *
	 * @param register
	 * 		The register containing the switch selector.
	 * @param payload
	 * 		The case keys paired with their target labels.
	 */
	void visitSparseSwitch(RegisterRef register, SparseSwitchPayload payload);

	/**
	 * Visits a {@code cmp} instruction, which compares two registers.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param to
	 * 		The register to store the result in.
	 * @param from1
	 * 		The first register to compare.
	 * @param from2
	 * 		The second register to compare.
	 */
	void visitCmp(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from1, RegisterRef from2);

	/**
	 * Visits a {@code if} instruction, which jumps to a label if a condition is met.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param a
	 * 		The first register to compare.
	 * @param b
	 * 		The second register to compare.
	 * @param label
	 * 		The label to jump to.
	 */
	void visitIf(@NotNull DalvikLowering lowering, RegisterRef a, RegisterRef b, ASTIdentifier label);

	/**
	 * Visits a {@code if} instruction, which jumps to a label if a condition is met compared to zero.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param a
	 * 		The register to compare.
	 * @param label
	 * 		The label to jump to.
	 */
	void visitIfZero(@NotNull DalvikLowering lowering, RegisterRef a, ASTIdentifier label);

	/**
	 * Visits a three-register binary operation.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param to
	 * 		The destination register.
	 * @param from1
	 * 		The first source register.
	 * @param from2
	 * 		The second source register.
	 */
	void visitBinaryOperation(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from1, RegisterRef from2);

	/**
	 * Visits a two-address binary operation.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param a
	 * 		The destination and first source register.
	 * @param b
	 * 		The second source register.
	 */
	void visitBinary2AddrOperation(@NotNull DalvikLowering lowering, RegisterRef a, RegisterRef b);

	/**
	 * Visits a binary operation with an integer literal.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param to
	 * 		The destination register.
	 * @param from
	 * 		The source register.
	 * @param constant
	 * 		The signed operation literal and the width its operand schema accepted.
	 */
	void visitBinaryLiteralOperation(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from,
	                                 SignedLiteral constant);

	/**
	 * Visits an array operation instruction, which performs an operation on an array.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param array
	 * 		The array to perform the operation on.
	 * @param index
	 * 		The index to perform the operation on.
	 * @param value
	 * 		The value to perform the operation on.
	 */
	void visitArrayOperation(@NotNull DalvikLowering lowering, RegisterRef array, RegisterRef index, RegisterRef value);

	/**
	 * Visits a virtual field operation instruction, which performs an operation on a field.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param value
	 * 		The value to perform the operation on.
	 * @param instance
	 * 		The instance to perform the operation on.
	 * @param path
	 * 		Declaring type and name of the field.
	 * @param descriptor
	 * 		The descriptor of the field.
	 */
	void visitVirtualFieldOperation(@NotNull DalvikLowering lowering, RegisterRef value, RegisterRef instance,
	                                MemberPath path, ASTIdentifier descriptor);

	/**
	 * Visits a static field operation instruction, which performs an operation on a static field.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param value
	 * 		The value to perform the operation on.
	 * @param path
	 * 		Declaring type and name of the field.
	 * @param descriptor
	 * 		The descriptor of the field.
	 */
	void visitStaticFieldOperation(@NotNull DalvikLowering lowering, RegisterRef value, MemberPath path,
	                               ASTIdentifier descriptor);

	/**
	 * Visits an invoke instruction.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param registers
	 * 		The registers holding the receiver and arguments.
	 * @param method
	 * 		Declaring type and name of the method.
	 * @param descriptor
	 * 		The descriptor of the method.
	 */
	void visitInvoke(@NotNull DalvikLowering lowering, RegisterOperands registers, MemberPath method,
	                 ASTIdentifier descriptor);

	/**
	 * Visits an {@code invoke-polymorphic} instruction.
	 *
	 * @param registers
	 * 		The registers holding the receiver and arguments.
	 * @param method
	 * 		Declaring type and name of the method.
	 * @param descriptor
	 * 		The descriptor of the method.
	 * @param proto
	 * 		The call site prototype the invocation is resolved against.
	 */
	void visitInvokePolymorphic(RegisterOperands registers, MemberPath method, ASTIdentifier descriptor, ASTIdentifier proto);

	/**
	 * Visits an {@code invoke-custom} instruction.
	 *
	 * @param registers
	 * 		The registers holding the arguments.
	 * @param name
	 * 		The name of the call site.
	 * @param type
	 * 		The descriptor of the call site.
	 * @param handle
	 * 		The bootstrap method handle.
	 * @param arguments
	 * 		The bootstrap method arguments.
	 */
	void visitInvokeCustom(RegisterOperands registers, ASTIdentifier name, ASTIdentifier type, ASTElement handle, ASTArray arguments);

	/**
	 * Visits a unary operation instruction, which performs an operation on a single register.
	 *
	 * @param lowering
	 * 		Declared dex opcode and encoding form of the matched instruction.
	 * @param to
	 * 		The register to store the result in.
	 * @param from
	 * 		The register to perform the operation on.
	 */
	void visitUnaryOperation(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from);
}

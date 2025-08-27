package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTValue;

public interface ASTDalvikInstructionVisitor extends ASTInstructionVisitor {

    /**
     * Visits a `nop` instruction, which does nothing.
     */
    void visitNop();

    /**
     * Visits a `move` type instruction, which moves a value from one register to another.
     * This is used for the following instructions: move, move-wide, move-object
     * @param to the register to move to
     * @param from the register to move from
     */
    void visitMove(ASTIdentifier to, ASTIdentifier from);

    /**
     * Visits a `move-result` type instruction, which moves a result of a previous instruction to a register.
     * This is used for the following instructions: move-result, move-result-wide, move-result-object
     * @param to the register to move to
     */
    void visitMoveResult(ASTIdentifier to);

    /**
     * Visits a `move-exception` instruction, which moves the exception to a register.
     * @param to the register to move to
     */
    void visitMoveException(ASTIdentifier to);

    /**
     * Visits a `return` type instruction, which returns a value from a method.
     * This is used for the following instructions: return, return-wide, return-object
     * @param returnValue the register to return
     */
    void visitReturn(ASTIdentifier returnValue);

    /**
     * Visits a `return-void` instruction, which returns from a void method.
     */
    void visitReturnVoid();

    /**
     * Visits a `const` instruction, which loads a constant value into a register.
     * This is used for the following instructions: const, const-wide, const-string,
     * const-class, const-method-handle, const-method-type.
     * @param to the register to load the constant into
     * @param value the constant value
     */
    void visitConst(ASTIdentifier to, ASTElement value);

    /**
     * Visits a `monitor-enter` instruction, which enters a monitor.
     * @param register the register to enter the monitor on
     */
    void visitMonitorEnter(ASTIdentifier register);

    /**
     * Visits a `monitor-exit` instruction, which exits a monitor.
     * @param register the register to exit the monitor on
     */
    void visitMonitorExit(ASTIdentifier register);

    /**
     * Visits a `check-cast` instruction, which checks if a register is an instance of a type.
     * @param register the register to check
     * @param type the type to check against
     */
    void visitCheckCast(ASTIdentifier register, ASTIdentifier type);

    /**
     * Visits a `instance-of` instruction, which checks if a register is an instance of a type.
     * @param result the register to store the result in
     * @param check the register to check
     * @param type the type to check against
     */
    void visitInstanceOf(ASTIdentifier result, ASTIdentifier check, ASTIdentifier type);

    /**
     * Visits a `array-length` instruction, which gets the length of an array.
     * @param result the register to store the result in
     * @param array the array to get the length of
     */
    void visitArrayLength(ASTIdentifier result, ASTIdentifier array);

    /**
     * Visits a `new-instance` instruction, which creates a new instance of a type.
     * @param result the register to store the result in
     * @param type the type to create an instance of
     */
    void visitNewInstance(ASTIdentifier result, ASTIdentifier type);

    /**
     * Visits a `new-array` instruction, which creates a new array of a type.
     * @param result the register to store the result in
     * @param size the register to get the size from
     * @param type the type of the array
     */
    void visitNewArray(ASTIdentifier result, ASTIdentifier size, ASTIdentifier type);

    /**
     * Visits a `filled-new-array` instruction, which creates a new array of a type.
     * @param args the registers to get the arguments from
     * @param type the type of the array
     */
    void visitFilledNewArray(ASTArray args, ASTIdentifier type);

    /**
     * Visits a `fill-array-data` instruction, which fills an array with data.
     * @param to the array to fill
     * @param array the array data to fill with
     */
    void visitFillArrayData(ASTIdentifier to, ASTArray array);

    /**
     * Visits a `fill-array-data-payload` instruction, which contains the data to fill an array with.
     * @param elementWidth the width of each element
     * @param elements the elements to fill the array with
     */
    void visitFillArrayDataPayload(ASTNumber elementWidth, ASTArray elements);

    /**
     * Visits a `throw` instruction, which throws an exception.
     * @param exception the exception to throw
     */
    void visitThrow(ASTIdentifier exception);

    /**
     * Visits a `goto` instruction, which jumps to a label.
     * @param label the label to jump to
     */
    void visitGoto(ASTIdentifier label);

    /**
     * Visits a `packed-switch` instruction, which jumps to a label based on the value of a register.
     * @param packedSwitchObject an object structured like the following:
     * <pre>
     *     {<br>
     *      first: number,<br>
     *      targets: { label, label, label, ... }<br>
     *     }
     * </pre>
     * number elements correspond to ASTNumber, label elements correspond to ASTIdentifier
     */
    void visitPackedSwitch(ASTObject packedSwitchObject);

    /**
     * Visits a `sparse-switch` instruction, which jumps to a label based on the value of a register.
     * @param sparseSwitchObject an object structured like the following:
     * <pre>
     *     {<br>
     *      number: label,<br>
     *      number: label,<br>
     *      ...<br>
     *     }
     * </pre>
     * number elements correspond to ASTNumber, label elements correspond to ASTIdentifier
     */
    void visitSparseSwitch(ASTObject sparseSwitchObject);

    /**
     * Visits a `cmp` instruction, which compares two registers. This is used for the following instructions:
     * cmpl-float, cmpg-float, cmpl-double, cmpg-double, cmp-long
     * @param to the register to store the result in
     * @param from1 the first register to compare
     * @param from2 the second register to compare
     */
    void visitCmp(ASTIdentifier to, ASTIdentifier from1, ASTIdentifier from2);

    /**
     * Visits a `if` instruction, which jumps to a label if a condition is met.
     * This is used for the following instructions: if-eq, if-ne, if-lt, if-ge, if-gt, if-le,
     *
     * @param a the first register to compare
     * @param b the second register to compare
     * @param label the label to jump to
     */
    void visitIf(ASTIdentifier a, ASTIdentifier b, ASTIdentifier label);

    /**
     * Visits a `if` instruction, which jumps to a label if a condition is met compared to zero.
     * This is used for the following instructions: if-eqz, if-nez
     * @param a the register to compare
     * @param label the label to jump to
     */
    void visitIfZero(ASTIdentifier a, ASTIdentifier label);

    /**
     * Visits a array operation instruction, which performs an operation on an array.
     * This is used for the following instructions: aget, aget-wide, aget-object, aget-boolean, aget-byte,
     * aget-char, aget-short, aput, aput-wide, aput-object, aput-boolean, aput-byte, aput-char, aput-short
     * @param array the array to perform the operation on
     * @param index the index to perform the operation on
     * @param value the value to perform the operation on
     */
    void visitArrayOperation(ASTIdentifier array, ASTIdentifier index, ASTIdentifier value);

    /**
     * Visits a virtual field operation instruction, which performs an operation on a field.
     * This is used for the following instructions: iget, iget-wide, iget-object, iget-boolean, iget-byte,
     * iget-char, iget-short, iput, iput-wide, iput-object, iput-boolean, iput-byte, iput-char, iput-short
     * @param value the value to perform the operation on
     * @param instance the instance to perform the operation on
     * @param path the path to the field, this is in the format of [owner].[name]
     * @param descriptor the descriptor of the field
     */
    void visitVirtualFieldOperation(ASTIdentifier value, ASTIdentifier instance, ASTIdentifier path, ASTIdentifier descriptor);


    /**
     * Visits a static field operation instruction, which performs an operation on a field.
     * This is used for the following instructions: sget, sget-wide, sget-object, sget-boolean, sget-byte,
     * sget-char, sget-short, sput, sput-wide, sput-object, sput-boolean, sput-byte, sput-char, sput-short
     * @param value the value to perform the operation on
     * @param path the path to the field, this is in the format of [owner].[name]
     * @param descriptor the descriptor of the field
     */
    void visitStaticFieldOperation(ASTIdentifier value, ASTIdentifier path, ASTIdentifier descriptor);

    /**
     * Visits a invoke instruction, which invokes a method.
     * This is used for the following instructions: invoke-virtual, invoke-super, invoke-direct, invoke-static,
     * invoke-interface, invoke-virtual/range, invoke-super/range, invoke-direct/range, invoke-static/range,
     * invoke-interface/range
     * @param registers the registers to get the arguments from
     * @param method the method to invoke
     * @param descriptor the descriptor of the method
     */
    void visitInvoke(ASTArray registers, ASTIdentifier method, ASTIdentifier descriptor);

    /**
     * Visits a invoke-custom instruction, which invokes a custom method.
     * This is used for the following instructions: invoke-custom, invoke-custom/range
     * @param registers the registers to get the arguments from
     * @param name the name of the method
     * @param type the type of the method
     * @param handle the handle of the method
     * @param arguments the arguments of the method
     */
    void visitInvokeCustom(ASTArray registers, ASTIdentifier name, ASTIdentifier type, ASTArray handle, ASTArray arguments);

    /**
     * Visits a invoke-polymorphic instruction, which invokes a polymorphic method.
     * This is used for the following instructions: invoke-polymorphic, invoke-polymorphic/range
     * @param registers the registers to get the arguments from
     * @param method the method to invoke
     * @param descriptor the descriptor of the method
     * @param proto the proto of the method
     */
    void visitInvokePolymorphic(ASTArray registers, ASTIdentifier method, ASTIdentifier descriptor, ASTIdentifier proto);

    /**
     * Visit a unary operation instruction, which performs a unary operation on a register.
     * This is used for the following instructions: neg-int, neg-long, neg-float, neg-double,
     * not-int, not-long, int-to-long, int-to-float, int-to-double, long-to-int, long-to-float,
     * long-to-double, float-to-int, float-to-long, float-to-double, double-to-int,
     * double-to-long, double-to-float, int-to-byte, int-to-char, int-to-short
     * @param to the register to store the result in
     * @param from the register to perform the operation on
     */
    void visitUnaryOperation(ASTIdentifier to, ASTIdentifier from);

}

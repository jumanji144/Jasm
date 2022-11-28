package me.darknet.assembler.transform;

import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;

public interface GroupVisitor {
	/**
	 * Called when the group visitation starts.
	 * @throws AssemblerException if an error occurs
	 */
	default void visitBegin() throws AssemblerException {}

	/**
	 * Visit any child group belonging visited group.
	 * @param group the instruction to visit
	 * @throws AssemblerException if an error occurs
	 */
	default void visit(Group group) throws AssemblerException {}

	/**
	 * Called when the group visitation is complete.
	 * @throws AssemblerException if an error occurs
	 */
	default void visitEnd() throws AssemblerException {}
}

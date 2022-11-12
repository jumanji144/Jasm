package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.AccessModsGroup;
import me.darknet.assembler.parser.groups.FieldDeclarationGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.parser.groups.MethodDeclarationGroup;

public interface TopLevelGroupVisitor extends GroupVisitor {
	/**
	 * visit any group
	 *
	 * @param group
	 * 		the group to visit
	 *
	 * @throws AssemblerException
	 * 		if an error occurs
	 */
	void visit(Group group) throws AssemblerException;

	/**
	 * Visit a class declaration
	 *
	 * @param accessMods
	 * 		the access modifiers of the class
	 * @param identifier
	 * 		the identifier of the class (fully qualified)
	 *
	 * @return a class visitor to visit the class declaration
	 *
	 * @throws AssemblerException
	 * 		if an error occurs
	 */
	ClassGroupVisitor visitClass(AccessModsGroup accessMods, IdentifierGroup identifier) throws AssemblerException;

	/**
	 * Visit a field declaration
	 *
	 * @param decl
	 * 		the field declaration group
	 *
	 * @return a field visitor to visit the field declaration
	 *
	 * @throws AssemblerException
	 * 		if an error occurs
	 */
	FieldGroupVisitor visitField(FieldDeclarationGroup decl) throws AssemblerException;

	/**
	 * Visit a method declaration
	 *
	 * @param decl
	 * 		the method declaration group
	 *
	 * @return a method visitor to visit the method declaration
	 *
	 * @throws AssemblerException
	 * 		if an error occurs
	 */
	MethodGroupVisitor visitMethod(MethodDeclarationGroup decl) throws AssemblerException;

}

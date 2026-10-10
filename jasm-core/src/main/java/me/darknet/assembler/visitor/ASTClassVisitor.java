package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import me.darknet.assembler.processing.ProcessedMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Visitor for class declarations and their class, field, method, and record-component members.
 */
public interface ASTClassVisitor extends ASTDeclarationVisitor {
	/**
	 * Visits the class superclass declaration.
	 *
	 * @param superClass
	 * 		Superclass identifier, or {@code null} when no explicit superclass is declared.
	 */
	void visitSuperClass(@Nullable ASTIdentifier superClass);

	/**
	 * Visits an interface implemented by the class.
	 *
	 * @param interfaceName
	 * 		Implemented interface identifier.
	 */
	void visitInterface(@NotNull ASTIdentifier interfaceName);

	/**
	 * Visits the source-file attribute.
	 *
	 * @param sourceFile
	 * 		Source file name, or {@code null} when absent.
	 */
	void visitSourceFile(@Nullable ASTString sourceFile);

	/**
	 * Visits the enclosing-class attribute.
	 *
	 * @param outerClass
	 * 		Enclosing class element, or {@code null} when absent.
	 */
	void visitOuterClass(@Nullable ASTElement outerClass);

	/**
	 * Visits the enclosing-method attribute.
	 *
	 * @param outerMethod
	 * 		Enclosing method descriptor, or {@code null} when absent.
	 */
	void visitOuterMethod(@Nullable ASTOuterMethod outerMethod);

	/**
	 * Visits a permitted subclass of the class.
	 *
	 * @param subclass
	 * 		Permitted subclass identifier.
	 */
	void visitPermittedSubclass(@NotNull ASTIdentifier subclass);

	/**
	 * Visits the nest-host attribute.
	 *
	 * @param nestHost
	 * 		Nest host identifier, or {@code null} when absent.
	 */
	void visitNestHost(@Nullable ASTIdentifier nestHost);

	/**
	 * Visits a member of the class nest.
	 *
	 * @param nestMember
	 * 		Nest member identifier.
	 */
	void visitNestMember(@NotNull ASTIdentifier nestMember);

	/**
	 * Begins visiting a record component.
	 *
	 * @param name
	 * 		Record component name.
	 * @param descriptor
	 * 		Record component descriptor.
	 * @param signature
	 * 		Generic signature, or {@code null} when absent.
	 *
	 * @return Visitor for the record component.
	 */
	ASTRecordComponentVisitor visitRecordComponent(@NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor, @Nullable ASTString signature);

	/**
	 * Visits an entry in the class inner-classes attribute.
	 *
	 * @param modifiers
	 * 		Inner-class modifiers.
	 * @param name
	 * 		Inner class simple name, or {@code null} when anonymous.
	 * @param outerClass
	 * 		Outer class identifier, or {@code null} when unavailable.
	 * @param innerClass
	 * 		Inner class identifier.
	 */
	void visitInnerClass(@NotNull Modifiers modifiers, @Nullable ASTIdentifier name, @Nullable ASTIdentifier outerClass,
	                     @Nullable ASTIdentifier innerClass);

	/**
	 * Begins visiting a field declared by the class.
	 *
	 * @param modifiers
	 * 		Field modifiers.
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 *
	 * @return Visitor for the field declaration.
	 */
	ASTFieldVisitor visitField(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull ASTIdentifier descriptor);

	/**
	 * Begins visiting a method declared by the class.
	 *
	 * @param method
	 * 		Source method declaration.
	 * @param processed
	 * 		Validated semantic method view.
	 *
	 * @return Visitor for the method declaration.
	 */
	ASTMethodVisitor visitMethod(@NotNull ASTMethod method, @NotNull ProcessedMethod processed);
}

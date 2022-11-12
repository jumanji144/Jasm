package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.groups.*;

public interface ClassGroupVisitor extends GroupVisitor {
	/**
	 * Visit a generic field-level attribute
	 * @param group Generic field attribute group.
	 * @throws AssemblerException if an error occurrs
	 */
	default void visitAttribute(ClassAttributeGroup group) throws AssemblerException {
		if (group instanceof AnnotationGroup) {
			visitAnnotation((AnnotationGroup) group);
		} else if (group instanceof SignatureGroup) {
			visitSignature((SignatureGroup) group);
		} else if (group instanceof ImplementsGroup) {
			visitImplements((ImplementsGroup) group);
		} else if (group instanceof ExtendsGroup) {
			visitExtends((ExtendsGroup) group);
		}
	}

	/**
	 * Visit a super-type extension
	 * @param group the extended type.
	 */
	void visitExtends(ExtendsGroup group);

	/**
	 * Visit an interface implementation
	 * @param group the implemented type.
	 */
	void visitImplements(ImplementsGroup group);

	/**
	 * Visit an annotation
	 * @param annotation the annotation group
	 * @throws AssemblerException if an error occurs
	 */
	void visitAnnotation(AnnotationGroup annotation) throws AssemblerException;

	/**
	 * Visit a signature
	 * @param signature the signature group
	 * @throws AssemblerException if an error occurs
	 */
	void visitSignature(SignatureGroup signature) throws AssemblerException;
}

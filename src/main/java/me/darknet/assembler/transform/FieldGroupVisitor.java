package me.darknet.assembler.transform;

import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.groups.annotation.AnnotationGroup;
import me.darknet.assembler.parser.groups.attributes.DeprecatedGroup;
import me.darknet.assembler.parser.groups.attributes.FieldAttributeGroup;
import me.darknet.assembler.parser.groups.attributes.SignatureGroup;

public interface FieldGroupVisitor extends GroupVisitor {
	/**
	 * Visit a generic field-level attribute
	 * @param group Generic field attribute group.
	 * @throws AssemblerException if an error occurrs
	 */
	default void visitAttribute(FieldAttributeGroup group) throws AssemblerException {
		if (group instanceof AnnotationGroup) {
			visitAnnotation((AnnotationGroup) group);
		} else if (group instanceof SignatureGroup) {
			visitSignature((SignatureGroup) group);
		} else if (group instanceof DeprecatedGroup) {
			visitDeprecated((DeprecatedGroup) group);
		}
	}

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

	/**
	 * Visit a deprecated attribute
	 * @param deprecated the deprecated group
	 * @throws AssemblerException if an error occurs
	 */
	void visitDeprecated(DeprecatedGroup deprecated) throws AssemblerException;
}

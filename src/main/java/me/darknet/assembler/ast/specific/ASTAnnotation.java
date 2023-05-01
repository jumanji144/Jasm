package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.util.ElementMap;

public class ASTAnnotation extends ASTElement {

	private final ASTIdentifier classType;
	private final ElementMap<ASTIdentifier, ASTElement> values;

	public ASTAnnotation(ASTIdentifier classType, ElementMap<ASTIdentifier, ASTElement> values) {
		super(ElementType.ANNOTATION, CollectionUtil.merge(values.getElements(), classType));
		this.classType = classType;
		this.values = values;
	}

	public ASTIdentifier getClassType() {
		return classType;
	}

	public ElementMap<ASTIdentifier, ASTElement> getValues() {
		return values;
	}

}

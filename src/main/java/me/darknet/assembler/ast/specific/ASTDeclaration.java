package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.util.CollectionUtil;

import java.util.List;

public class ASTDeclaration extends ASTElement {

	private final ASTIdentifier keyword;
	private final List<ASTElement> elements;

	public ASTDeclaration(ASTIdentifier keyword, List<ASTElement> elements) {
		super(ElementType.DECLARATION, CollectionUtil.merge(elements, keyword));
		this.keyword = keyword;
		this.elements = elements;
	}

	public ASTIdentifier getKeyword() {
		return keyword;
	}

	public List<ASTElement> getElements() {
		return elements;
	}

}

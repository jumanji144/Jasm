package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.util.CollectionUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * AST element representing a declaration (.keyword) where it's children are the elements of the declaration.
 * A declaration can contain sub-declarations.
 * @see #getElements() for the elements of the declaration.
 */
public class ASTDeclaration extends ASTElement {

	private final ASTIdentifier keyword;
	private final List<@Nullable ASTElement> elements;

	public ASTDeclaration(ASTIdentifier keyword, List<@Nullable ASTElement> elements) {
		super(ElementType.DECLARATION, CollectionUtil.merge(elements, keyword));
		if(keyword == null) {
			this.value = elements.get(0).getValue();
		} else {
			this.value = keyword.getValue();
		}
		this.keyword = keyword;
		this.elements = elements;
	}

	public ASTIdentifier getKeyword() {
		return keyword;
	}

	public List<@Nullable ASTElement> getElements() {
		return elements;
	}

}

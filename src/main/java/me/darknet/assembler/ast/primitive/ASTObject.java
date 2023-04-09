package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.util.ElementMap;
import org.jetbrains.annotations.Nullable;

public class ASTObject extends ASTElement {

	private final ElementMap<ASTIdentifier, @Nullable ASTElement> values;

	public ASTObject(ElementMap<ASTIdentifier, @Nullable ASTElement> values) {
		super(ElementType.OBJECT, values.getElements());
		this.values = values;
	}

	public ElementMap<ASTIdentifier, @Nullable ASTElement> getValues() {
		return values;
	}
}

package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ASTObject extends ASTElement {

	private final ElementMap<ASTIdentifier, ASTElement> values;

	public ASTObject(ElementMap<ASTIdentifier, ASTElement> values) {
		super(ElementType.OBJECT, values.getElements());
		this.values = values;
	}

	public ElementMap<ASTIdentifier, ASTElement> getValues() {
		return values;
	}
}

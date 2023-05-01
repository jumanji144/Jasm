package me.darknet.assembler.ast;

import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.Location;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ASTElement {

	protected ElementType type;
	protected ASTElement parent;
	protected final List<ASTElement> children;
	protected Token value;

	public ASTElement(ElementType type) {
		this(type, Collections.emptyList());
	}

	public ASTElement(ElementType type, ASTElement... children) {
		this(type, Arrays.asList(children));
	}

	@SuppressWarnings("unchecked")
	public ASTElement(ElementType type, List<? extends ASTElement> children) {
		for (ASTElement child : children) {
			if(child != null) {
				child.parent = this;
			}
		}
		this.type = type;
		this.children = (List<ASTElement>) children;
	}

	public String getContent() {
		return value == null ? null : value.getContent();
	}

	public Token getValue() {
		return value;
	}

	public ElementType getType() {
		return type;
	}

	public ASTElement getParent() {
		return parent;
	}

	public List<ASTElement> getChildren() {
		return children;
	}

	public Location getLocation() {
		if(value == null) {
			// go through children
			for (ASTElement child : children) {
				Location location = child.getLocation();
				if(location != null) {
					return location;
				}
			}
		} else {
			return value.getLocation();
		}
		return null;
	}
}

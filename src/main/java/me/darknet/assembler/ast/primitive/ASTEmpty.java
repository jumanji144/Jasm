package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.util.ElementMap;

import java.util.Collections;

/**
 * Represents an empty element, either and empty array or an empty object.
 */
public class ASTEmpty extends ASTElement {

	public static final ASTArray EMPTY_ARRAY = new ASTArray(Collections.emptyList());
	public static final ASTCode EMPTY_CODE = new ASTCode(Collections.emptyList());
	public static final ASTObject EMPTY_OBJECT = new ASTObject(ElementMap.empty());

	public ASTEmpty() {
		super(ElementType.EMPTY);
	}

}

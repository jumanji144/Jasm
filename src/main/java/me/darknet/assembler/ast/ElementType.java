package me.darknet.assembler.ast;

public enum ElementType {

	// Primitives
	ARRAY,
	OBJECT,
	DECLARATION,
	IDENTIFIER,
	STRING,
	NUMBER,
	CODE,
	CODE_INSTRUCTION,
	COMMENT,
	// Specific
	CLASS,
	METHOD,
	FIELD,
	ANNOTATION,
	SIGNATURE,
	CLASS_TYPE,
	METHOD_TYPE

}

package me.darknet.assembler.ast;

public enum ElementType {

    // Primitives
    ARRAY,
    OBJECT,
    DECLARATION,
    IDENTIFIER,
    STRING,
    CHARACTER,
    NUMBER,
    BOOL,
    CODE,
    CODE_INSTRUCTION,
    COMMENT,
    EMPTY,
    // Specific
    CLASS,
    METHOD,
    FIELD,
    ANNOTATION,
    ENUM,
    INNER_CLASS,
    SIGNATURE,
    CLASS_TYPE,
    METHOD_TYPE,
    EXCEPTION

}

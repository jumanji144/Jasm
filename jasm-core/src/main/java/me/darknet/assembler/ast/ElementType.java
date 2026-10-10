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
    EMPTY,
    // Specific
    CLASS,
    METHOD,
    FIELD,
    ANNOTATION,
    ENUM,
    INNER_CLASS,
    RECORD_COMPONENT,
    OUTER_METHOD,
    EXCEPTION

}

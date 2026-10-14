package me.darknet.assembler.instructions;

/**
 * Target-neutral semantic traits attached to registered instructions.
 * <p>
 * Traits are declared once per definition, so a query about an instruction's behaviour never has
 * to be re-derived from its mnemonic.
 */
public enum InstructionTrait {
    FALLTHROUGH,
    CONDITIONAL_BRANCH,
    UNCONDITIONAL_BRANCH,
    SWITCH,
    RETURN,
    THROW,
    INVOKE,
    TYPE_REFERENCE,
    FIELD_REFERENCE,
    METHOD_REFERENCE,
    VARIABLE_READ,
    VARIABLE_WRITE,
    VARIABLE_INCREMENT,
    REGISTER_READ,
    REGISTER_WRITE,
    PAYLOAD,
    DEBUG_METADATA,
    PSEUDO,
    INTERFACE_INVOKE
}

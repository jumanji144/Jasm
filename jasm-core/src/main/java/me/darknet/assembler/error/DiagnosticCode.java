package me.darknet.assembler.error;

/**
 * Stable identifier for a class of {@link Diagnostic}.
 * <p>
 * Codes exist so tooling can react to a problem without matching on message text, which is free to change. A code
 * is assigned where the problem is detected, not where it is reported, so a single funnel that reports many shapes of
 * the same problem can still be specific.
 */
public enum DiagnosticCode {
	/**
	 * No more specific code applies.
	 */
	UNCLASSIFIED,

	/**
	 * A string, character or multiline comment literal was opened and never closed.
	 */
	UNTERMINATED_LITERAL,

	/**
	 * An escape sequence was incomplete or did not name a valid character.
	 */
	INVALID_ESCAPE,

	/**
	 * A numeric or character literal was malformed, such as a bad hex float or a character literal of the wrong
	 * length.
	 */
	INVALID_LITERAL,

	/**
	 * A token appeared where the grammar did not allow it.
	 */
	UNEXPECTED_TOKEN,

	/**
	 * A declaration or member was structurally malformed.
	 */
	MALFORMED_DECLARATION,

	/**
	 * A declaration keyword is not registered for the active target.
	 */
	UNKNOWN_DECLARATION,

	/**
	 * An instruction mnemonic is not registered for the active target.
	 */
	UNKNOWN_INSTRUCTION,

	/**
	 * An instruction is known but explicitly unavailable for the active target.
	 */
	UNAVAILABLE_INSTRUCTION,

	/**
	 * An element appeared where the expected type did not allow it.
	 */
	UNEXPECTED_ELEMENT,

	/**
	 * A type or member descriptor could not be parsed.
	 */
	INVALID_DESCRIPTOR,

	/**
	 * A literal value was outside the domain of the operand that consumes it.
	 */
	INVALID_LITERAL_VALUE,

	/**
	 * A structured payload, such as an annotation or handle, did not match its schema.
	 */
	PAYLOAD_SHAPE,

	/**
	 * An instruction's operands do not match its schema.
	 */
	OPERAND_SHAPE,

	/**
	 * A declaration uses a form the active target cannot represent.
	 */
	UNSUPPORTED_FORM,

	/**
	 * A declaration requires a target capability that is not available.
	 */
	UNSUPPORTED_CAPABILITY,

	/**
	 * A register reference exceeds the register count declared for the method.
	 */
	REGISTER_LIMIT,

	/**
	 * The registers declared for a method do not cover its inputs.
	 */
	REGISTER_DECLARATION,

	/**
	 * A label was defined more than once in a method.
	 */
	DUPLICATE_LABEL,

	/**
	 * A label was referenced but never defined in a method.
	 */
	MISSING_LABEL,

	/**
	 * The emitted representation would be inconsistent, for example a value written twice.
	 */
	INVALID_EMISSION,

	/**
	 * The backend failed while producing output.
	 */
	BACKEND_FAILURE,

	/**
	 * The output is well-formed but exceeds a hard limit of the target format, such as the JVM's 64 KiB limit
	 * on the code array of one method or its limit on constants in a pool. This is distinct from
	 * {@link #BACKEND_FAILURE}, which means the backend could not produce output at all, and from
	 * {@link #MALFORMED_DECLARATION}, which means the source itself was wrong.
	 */
	TARGET_LIMIT,

	/**
	 * An internal invariant was violated, which indicates a bug rather than a problem with the input.
	 */
	INTERNAL_INVARIANT,

	/**
	 * Static analysis of the emitted representation rejected the output.
	 */
	ANALYSIS_FAILURE,

	/**
	 * Verification of the emitted representation produced a warning.
	 */
	VERIFICATION_WARNING
}

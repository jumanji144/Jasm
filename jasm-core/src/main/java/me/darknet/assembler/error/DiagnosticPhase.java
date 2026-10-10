package me.darknet.assembler.error;

/**
 * Phase of the pipeline that produced a {@link Diagnostic}.
 */
public enum DiagnosticPhase {
	/**
	 * Lexing of raw source text into tokens.
	 */
	LEXER,

	/**
	 * Parsing of tokens into the declaration AST.
	 */
	SYNTAX,

	/**
	 * Target-aware validation of declaration structure and capabilities.
	 */
	TARGET_VALIDATION,

	/**
	 * Target instruction lookup, operand validation, and lowering of the AST into per-target instruction models.
	 */
	SEMANTIC_LOWERING,

	/**
	 * Emission of the class or dex representation by a backend.
	 */
	BACKEND_EMISSION,

	/**
	 * Verification and analysis of the emitted representation.
	 */
	OUTPUT_VERIFICATION
}

package me.darknet.assembler.backend.jvm.compile;

/**
 * Policy for emitting local-variable tables on compiled JVM methods.
 */
public enum JvmVariableMode {
	/** Emit a local-variable table for every generated method. */
	ALWAYS_WRITE,
	/** Emit a local-variable table only when the overlay method already had local-variable entries. */
	WRITE_IF_ALREADY_PRESENT,
	/** Do not emit local-variable tables. */
	NEVER_WRITE,
}

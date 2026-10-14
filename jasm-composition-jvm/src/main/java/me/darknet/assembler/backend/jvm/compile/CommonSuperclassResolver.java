package me.darknet.assembler.backend.jvm.compile;

import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.TypeAwareness;
import me.darknet.assembler.error.DiagnosticSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.darknet.assembler.error.DiagnosticCode.VERIFICATION_WARNING;
import static me.darknet.assembler.error.DiagnosticPhase.OUTPUT_VERIFICATION;
import static me.darknet.assembler.util.Location.UNKNOWN;

/**
 * Resolves common superclasses for frame types and reports unknown types.
 * <p>
 * If either type is unknown, the result is {@code java/lang/Object}
 * and the configured sink receives a verification warning when available.
 */
final class CommonSuperclassResolver {
	private static final String OBJECT = "java/lang/Object";

	private final InheritanceChecker checker;
	private final TypeAwareness awareness;
	private final DiagnosticSink sink;

	/**
	 * @param checker
	 * 		Service answering type-hierarchy questions; must not be {@code null}.
	 * @param awareness
	 * 		Service that reports types the compiler cannot resolve, or {@code null} to treat every type as known.
	 * @param sink
	 * 		Sink receiving one warning per unknown type, or {@code null} to stay silent.
	 *
	 * @throws IllegalArgumentException
	 * 		If no inheritance checker was supplied.
	 */
	CommonSuperclassResolver(@Nullable InheritanceChecker checker, @Nullable TypeAwareness awareness, @Nullable DiagnosticSink sink) {
		if (checker == null)
			throw new IllegalArgumentException("Class writer requires an inheritance checker implementation");
		this.checker = checker;
		this.awareness = awareness;
		this.sink = sink;
	}

	/**
	 * @param type1
	 * 		First frame type.
	 * @param type2
	 * 		Second frame type.
	 *
	 * @return Superclass common to both types, or {@code java/lang/Object} when either type is unknown.
	 */
	@NotNull String resolve(@Nullable String type1, @Nullable String type2) {
		// Examine both inputs even when the first is unknown, so each unknown type receives a warning.
		boolean unknown = noteUnknown(type1);
		unknown |= noteUnknown(type2);
		return unknown ? OBJECT : checker.getCommonSuperclass(type1, type2);
	}

	/**
	 * @param type
	 * 		Type to examine.
	 *
	 * @return {@code true} when the type is unknown to the compiler, in which case a warning was reported.
	 */
	private boolean noteUnknown(@Nullable String type) {
		if (type == null || awareness == null || awareness.isAwareOf(type))
			return false;
		if (sink != null)
			sink.warning(OUTPUT_VERIFICATION, VERIFICATION_WARNING, awareness.notifyUnknownType(type), UNKNOWN);
		return true;
	}
}

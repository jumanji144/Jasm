package me.darknet.assembler.backend.jvm.compile;

import me.darknet.assembler.descriptor.DescriptorParser;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Target of one annotation inside an existing class file. Expected path formats are:
 * <ul>
 *     <li>{@code path/to/class.<index>} for a class annotation</li>
 *     <li>{@code path/to/class.field.<name>.<descriptor>.<index>} for a field annotation</li>
 *     <li>{@code path/to/class.method.<name>.<descriptor>.<index>} for a method annotation</li>
 * </ul>
 */
public sealed interface AnnotationTarget {
	/**
	 * @return Zero-based slot index within the addressed annotation list.
	 * When the index is past the end of the list the annotation is appended.
	 */
	int index();

	/**
	 * Parses an annotation target path.
	 *
	 * @param path
	 * 		User-supplied target path, or {@code null} when none was supplied.
	 *
	 * @return Parsed target, or errors describing why the path cannot be used.
	 */
	static @NotNull Outcome<AnnotationTarget> parse(@Nullable String path) {
		if (path == null || path.isBlank())
			return Outcome.failure(targetError("Annotation target path was not specified"));

		String[] parts = path.split("\\.");
		if (parts.length < 2)
			return Outcome.failure(targetError("Invalid annotation target path: " + path));

		int last = parts.length - 1;
		int index;
		try {
			index = Integer.parseInt(parts[last]);
		} catch (NumberFormatException exception) {
			return Outcome.failure(targetError("Invalid annotation target index: " + parts[last]));
		}
		if (index < 0)
			return Outcome.failure(targetError("Annotation target index must be nonnegative: " + index));

		// Recognize a member only when its marker is exactly three segments before the index.
		// This keeps dotted class paths without a member marker as class targets.
		if (parts.length >= 4) {
			MemberKind kind = MemberKind.from(parts[last - 3]);
			if (kind != null) {
				String name = parts[last - 2];
				String descriptor = parts[last - 1];
				if (name.isEmpty())
					return Outcome.failure(targetError("Annotation target member name is missing: " + path));
				if (!kind.accepts(descriptor))
					return Outcome.failure(targetError("Invalid annotation target member descriptor: " + descriptor));
				return Outcome.success(new MemberTarget(kind, name, descriptor, index));
			}
		}
		return Outcome.success(new ClassTarget(index));
	}

	/**
	 * @param message
	 * 		Description of why the path cannot be used.
	 *
	 * @return Diagnostic for a malformed annotation target path, which has no source position because the path comes
	 * from the command line.
	 */
	private static @NotNull Diagnostic targetError(@NotNull String message) {
		return Diagnostic.error(DiagnosticPhase.TARGET_VALIDATION, DiagnosticCode.MALFORMED_DECLARATION, message, null);
	}

	/**
	 * Class-level annotation target.
	 *
	 * @param index
	 * 		Slot index within the addressed annotation list.
	 */
	record ClassTarget(int index) implements AnnotationTarget {}

	/**
	 * Field or method annotation target.
	 *
	 * @param kind
	 * 		Member kind addressed by the path.
	 * @param name
	 * 		Member name.
	 * @param descriptor
	 * 		Member descriptor.
	 * @param index
	 * 		Slot index within the addressed annotation list.
	 */
	record MemberTarget(@NotNull MemberKind kind, @NotNull String name, @NotNull String descriptor, int index) implements AnnotationTarget {}

	/**
	 * Member kinds a target path may address.
	 */
	enum MemberKind {
		/**
		 * Field member addressed by a field descriptor.
		 */
		FIELD("field") {
			@Override
			boolean accepts(@NotNull String descriptor) {
				return DescriptorParser.isValidFieldDescriptor(descriptor);
			}
		},
		/**
		 * Method member addressed by a method descriptor.
		 */
		METHOD("method") {
			@Override
			boolean accepts(@NotNull String descriptor) {
				return DescriptorParser.isValidMethodDescriptor(descriptor);
			}
		};

		private final String marker;

		MemberKind(String marker) {
			this.marker = marker;
		}

		/**
		 * @param descriptor
		 * 		Descriptor to validate.
		 *
		 * @return {@code true} when the descriptor can describe this member kind.
		 */
		abstract boolean accepts(@NotNull String descriptor);

		/**
		 * @param marker
		 * 		Path segment to match.
		 *
		 * @return Member kind for the marker, or {@code null} when the segment is not a member marker.
		 */
		static @Nullable MemberKind from(@NotNull String marker) {
			for (MemberKind kind : values())
				if (kind.marker.equals(marker))
					return kind;
			return null;
		}
	}
}

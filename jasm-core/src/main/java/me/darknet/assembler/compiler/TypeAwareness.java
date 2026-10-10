package me.darknet.assembler.compiler;

/**
 * Debugging interface used to report unknown types to the user.
 * This helps diagnose cases where frame generation fails because of missing types,
 * and allows the user to add them to the classpath.
 */
public interface TypeAwareness {
	/**
	 * Tests whether a type is known to the configured environment.
	 *
	 * @param type
	 * 		Internal name of the type, like "java/lang/Object"
	 *
	 * @return {@code true} when the {@link InheritanceChecker} is at least aware of the type existing.
	 * {@code false} when the type is completely unknown.
	 */
	boolean isAwareOf(String type);

	/**
	 * Describes an unknown type for a diagnostic.
	 *
	 * @param type
	 * 		Internal name of the type, like "java/lang/Object"
	 *
	 * @return A message to display to the user when the type is unknown.
	 * This can be used to suggest adding a dependency or classpath entry.
	 */
	String notifyUnknownType(String type);
}

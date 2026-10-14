package me.darknet.assembler.compiler;

/**
 * Checks subtype relationships and computes common superclass names.
 */
public interface InheritanceChecker {
    /**
     * Tests whether one internal class name is a subtype of another.
     *
     * @param child
     * 		Internal name of the candidate subtype.
     * @param parent
     * 		Internal name of the candidate supertype.
     *
     * @return {@code true} if {@code child} is a subtype of {@code parent}, {@code false} otherwise.
     */
    boolean isSubclassOf(String child, String parent);

    /**
     * Finds a common superclass for two internal class names.
     *
     * @param type1
     * 		First internal class name.
     * @param type2
     * 		Second internal class name.
     *
     * @return Internal name of the common superclass, or {@code null} when it cannot be resolved.
     */
    String getCommonSuperclass(String type1, String type2);
}

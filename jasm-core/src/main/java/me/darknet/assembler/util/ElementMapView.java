package me.darknet.assembler.util;

import me.darknet.assembler.ast.ASTElement;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Read-only view over ordered AST key/value pairs.
 *
 * @param <A>
 *            The key type.
 * @param <B>
 *            The value type.
 */
public interface ElementMapView<A extends ASTElement, B extends ASTElement> {
    /**
     * @param index
     * 		Zero-based pair index.
     *
     * @return Value at the index. Behavior for an out-of-range index is implementation-specific.
     */
    <T extends B> @Nullable T get(int index);

    /**
     * @param content
     * 		Key content to look up.
     *
     * @return Matching value, or {@code null} when no pair has the key.
     */
    <T extends B> @Nullable T get(String content);

    /**
     * @param index
     * 		Zero-based pair index.
     *
     * @return Key at the index. Behavior for an out-of-range index is implementation-specific.
     */
    @Nullable A key(int index);

    /**
     * @param content
     * 		Key content to look up.
     *
     * @return Matching key, or {@code null} when no pair has the key.
     */
    @Nullable A key(String content);

    /**
     * @param index
     * 		Zero-based pair index.
     *
     * @return Key/value pair at the index. Behavior for an out-of-range index is implementation-specific.
     */
    @Nullable Pair<A, B> pair(int index);

    /**
     * @return Ordered collection of key/value pairs.
     */
    Collection<Pair<A, B>> pairs();

    /**
     * @param content
     * 		Key content to find.
     *
     * @return {@code true} if the view contains a pair with the key, {@code false} otherwise.
     */
    boolean containsKey(String content);

    /**
     * @return Ordered list of map elements.
     */
    List<ASTElement> elements();

    /**
     * @return Number of key/value pairs.
     */
    int size();
}

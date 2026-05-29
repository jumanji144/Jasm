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
    <T extends B> @Nullable T get(int index);

    <T extends B> @Nullable T get(String content);

    @Nullable A key(int index);

    @Nullable A key(String content);

    @Nullable Pair<A, B> pair(int index);

    Collection<Pair<A, B>> pairs();

    boolean containsKey(String content);

    List<ASTElement> elements();

    int size();
}

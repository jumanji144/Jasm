package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compiler.InheritanceChecker;

import dev.xdark.blw.type.ClassType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Common outline of a stack-frame, agnostic of level-of-detail for the
 * stack/locals.
 *
 * @see TypedFrame Frame tracking only type information.
 * @see ValuedFrame Frame tracking type and value information.
 */
public sealed interface Frame permits TypedFrame, ValuedFrame {
    /**
     * @return Copy of the current frame.
     */
    @NotNull
    Frame copy();

    /**
     * @param index
     *              Index of variable to check.
     *
     * @return Type of the variable, or {@code null} if not a known variable or a known {@code null} value.
     */
    @Nullable
    ClassType getLocalType(int index);

    /**
     * @return Stream of locals in the current frame.
     */
    @NotNull
    Stream<? extends Local> locals();

    /**
     * Push {@code null} onto the stack.
     */
    void pushNull();

    /**
     * @param type
     *             Type to push onto the stack. Can be {@code null} for {@code null} stack values.
     */
    void pushType(@Nullable ClassType type);

    /**
     * @param types
     *              Type to push onto the stack. Can be {@code null} for {@code null} stack values.
     */
    default void pushTypes(@Nullable ClassType... types) {
        for (ClassType type : types) {
            pushType(type);
        }
    }

    /**
     * @param n
     *          Number of items to pop off the stack.
     */
    void pop(int n);

    /**
     * Merges types of variables and stack items, taking place in this frame.
     *
     * @param checker
     *                Inheritance checker to use for determining common super-types.
     * @param other
     *                Frame to merge into this one.
     *
     * @return {@code true} when changes were made during the merge process.
     *         {@code false} if no changes were made, indicating equal frames.
     *
     * @throws FrameMergeException
     *                             When the stack sizes do not match, or this
     *                             frame's implementing class is not the same as the
     *                             other's.
     */
    boolean merge(@NotNull InheritanceChecker checker, @NotNull Frame other) throws FrameMergeException;
}

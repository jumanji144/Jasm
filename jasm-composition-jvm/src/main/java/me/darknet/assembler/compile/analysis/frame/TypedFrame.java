package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.Local;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Frame that tracks only type info of stack/local entries.
 */
public non-sealed interface TypedFrame extends Frame {
    /**
     * @return Stack.
     */
    @NotNull
    Deque<ClassType> getStack();

    /**
     * @return Map of local variables.
     */
    @NotNull
    Map<Integer, Local> getLocals();

    @Override
    default @NotNull Stream<Local> locals() {
        return getLocals().values().stream();
    }

    /**
     * @param index
     *              Index of variable to get.
     *
     * @return Variable, or {@code null} if not a known variable.
     */
    @Nullable
    default Local getLocal(int index) {
        return getLocals().get(index);
    }

    @Override
    default boolean hasLocal(int index) {
        return getLocal(index) != null;
    }

    @Nullable
    @Override
    default ClassType getLocalType(int index) {
        Local local = getLocal(index);
        if (local == null)
            return null;
        return local.type();
    }

    /**
     * @param index
     *              Index of variable to assign.
     * @param local
     *              Variable info to assign.
     */
    void setLocal(int index, @NotNull Local local);

    /**
     * Removes the top item from the stack, returning that value.
     *
     * @return Top type on the stack.
     */
    @Nullable
    ClassType pop();

    /**
     * @return Top type <i>(offset by one)</i> on the stack.
     */
    @Nullable
    default ClassType pop2() {
        if (pop() == null) throw new IllegalStateException("Couldn't pop wide, found null");
        return pop();
    }

    /**
     * @param type
     *             Type to pop off the stack.
     * @return Type on the stack. {@code null} if a known {@code null} value was on the stack.
     */
    @Nullable
    default ClassType pop(@NotNull ClassType type) {
        if (type == Types.LONG || type == Types.DOUBLE) {
            return pop2();
        } else {
            return pop();
        }
    }

    /**
     * @return Top type on the stack. {@code null} if a known {@code null} value was on the stack.
     */
    @Nullable
    ClassType peek();
}

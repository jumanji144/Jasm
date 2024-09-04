package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.ValuedLocal;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Frame that tracks type and value info of stack/local entries.
 */
public non-sealed interface ValuedFrame extends Frame {
    /**
     * @return Stack.
     */
    @NotNull
    Deque<Value> getStack();

    /**
     * @return Map of local variables.
     */
    @NotNull
    Map<Integer, ValuedLocal> getLocals();

    @Override
    default @NotNull Stream<ValuedLocal> locals() {
        return getLocals().values().stream();
    }

    /**
     * @param index
     *              Index of variable to get.
     *
     * @return Variable, or {@code null} if not a known variable.
     */
    @Nullable
    default ValuedLocal getLocal(int index) {
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
    void setLocal(int index, @NotNull ValuedLocal local);

    /**
     * Removes the top item from the stack, returning that value.
     *
     * @return Top value on the stack.
     */
    @NotNull
    Value pop();

    /**
     * @return Top value <i>(offset by one)</i> on the stack.
     */
    @NotNull
    default Value pop2() {
        pop();
        return pop();
    }

    /**
     * @param type
     *             Type to pop off the stack.
     *
     * @return Value popped.
     */
    @NotNull
    default Value pop(@NotNull ClassType type) {
        if (type == Types.LONG || type == Types.DOUBLE) {
            return pop2();
        } else {
            return pop();
        }
    }

    /**
     * @return Top value on the stack.
     */
    @NotNull
    Value peek();

    /**
     * @param value
     *              Value to push onto the stack.
     */
    void push(@NotNull Value value);

    /**
     * Pushes a raw value onto the stack, without pushing a {@link me.darknet.assembler.compile.analysis.Values#VOID_VALUE}
     * for {@link Types#LONG} and {@link Types#DOUBLE}.
     * @param value Value to push onto the stack.
     */
    void pushRaw(@NotNull Value value);

    /**
     * @param values
     *               Value to push onto the stack.
     */
    default void push(@NotNull Value... values) {
        for (Value value : values) {
            push(value);
        }
    }

    default void pushRaw(@NotNull Value... values) {
        for (Value value : values) {
            pushRaw(value);
        }
    }
}

package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.*;
import me.darknet.assembler.compiler.InheritanceChecker;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ValuedFrameImpl implements ValuedFrame {
    private final Deque<Value> stack;
    private final Map<Integer, ValuedLocal> locals;

    /**
     * New frame with a given stack/variable table.
     *
     * @param stack
     *               Stack state.
     * @param locals
     *               Variable table.
     */
    public ValuedFrameImpl(@NotNull Deque<Value> stack, @NotNull Map<Integer, ValuedLocal> locals) {
        this.stack = stack;
        this.locals = locals;
    }

    /**
     * New frame with an empty stack.
     *
     * @param locals
     *               Variable map to copy.
     */
    public ValuedFrameImpl(@NotNull Map<Integer, ValuedLocal> locals) {
        this(new ArrayDeque<>(), new TreeMap<>(locals));
    }

    /**
     * New frame with an empty stack and no variables.
     */
    public ValuedFrameImpl() {
        this(new ArrayDeque<>(), new TreeMap<>());
    }

    @Override
    public boolean merge(@NotNull InheritanceChecker checker, @NotNull Frame other) throws FrameMergeException {
        if (other instanceof ValuedFrameImpl simpleOther)
            return merge(checker, simpleOther);
        throw new FrameMergeException(this, other, "Cannot merge into differently typed frame");
    }

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
     *                             When the stack sizes do not match.
     */
    public boolean merge(@NotNull InheritanceChecker checker, @NotNull ValuedFrame other) throws FrameMergeException {
        boolean changed = false;
        for (Map.Entry<Integer, ValuedLocal> entry : other.getLocals().entrySet()) {
            int index = entry.getKey();
            ValuedLocal local = getLocal(index);
            ValuedLocal otherLocal = entry.getValue();

            // If we don't have the local, copy it from the other frame.
            if (local == null) {
                // We do not set 'changed' since expanding local variable scope is not going to change
                // behavior of frames that previously passed analysis.
                setLocal(index, otherLocal);
                continue;
            }

            // Merge the local values.
            try {
                ValuedLocal mergedLocal = local.mergeWith(checker, otherLocal);
                if (!Objects.equals(local, mergedLocal)) {
                    setLocal(index, mergedLocal);
                    changed = true;
                }
            } catch (ValueMergeException ex) {
                throw new FrameMergeException(this, other, ex.getMessage());
            }
        }

        Deque<Value> otherStack = other.getStack();
        Deque<Value> thisStack = stack;
        if (thisStack.size() != otherStack.size())
            throw new FrameMergeException(this, other,
                    "Stack size mismatch, " + thisStack.size() + " != " + otherStack.size());

        Deque<Value> newStack = new ArrayDeque<>();
        Iterator<Value> it1 = thisStack.iterator();
        Iterator<Value> it2 = otherStack.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            Value value1 = it1.next();
            Value value2 = it2.next();
            if (value1 == value2) {
                newStack.add(value1);
                continue;
            } else if (value1 == Values.VOID_VALUE || value2 == Values.VOID_VALUE) {
                newStack.add(Values.VOID_VALUE);
                continue;
            }
            Value merged;
            try {
                merged = value1.mergeWith(checker, value2);
            } catch (ValueMergeException ex) {
                throw new FrameMergeException(this, other, ex.getMessage());
            }
            if (!Objects.equals(merged, value1)) {
                changed = true;
                it1.remove();
                newStack.add(merged);
            } else {
                newStack.add(value1);
            }
        }
        thisStack.clear();
        thisStack.addAll(newStack);
        return changed;
    }

    @NotNull
    @Override
    public Deque<Value> getStack() {
        return stack;
    }

    @NotNull
    @Override
    public Map<Integer, ValuedLocal> getLocals() {
        return locals;
    }

    @Override
    public void setLocal(int index, @NotNull ValuedLocal local) {
        locals.put(index, local);
    }

    @Override
    public void pushType(@Nullable ClassType type) {
        if (type == null) {
            stack.push(Values.NULL_VALUE);
        } else {
            stack.push(Values.valueOf(type));
            if (type == Types.LONG || type == Types.DOUBLE)
                stack.push(Values.VOID_VALUE);
        }
    }

    @Override
    public void pushNull() {
        stack.push(Values.NULL_VALUE);
    }

    @Override
    public void push(@NotNull Value value) {
        stack.push(value);
        ClassType type = value.type();
        if (type == Types.LONG || type == Types.DOUBLE)
            stack.push(Values.VOID_VALUE);
    }

    @Override
    public void pushRaw(@NotNull Value value) {
        stack.push(value);
    }

    @NotNull
    @Override
    public Value peek() {
        if (stack.isEmpty())
            throw new IllegalStateException("Cannot peek from empty stack");
        return stack.peek();
    }

    @NotNull
    @Override
    public Value pop() {
        try {
            return stack.pop();
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("Cannot pop from empty stack");
        }
    }

    @Override
    public void pop(int n) {
        for (int i = 0; i < n; i++) {
            pop();
        }
    }

    /**
     * @param frame
     *              Frame to copy stack/locals from.
     *
     * @return Self.
     */
    private ValuedFrameImpl copyFrom(@NotNull ValuedFrameImpl frame) {
        locals.putAll(frame.locals);
        stack.addAll(frame.stack);
        return this;
    }

    @NotNull
    @Override
    public ValuedFrameImpl copy() {
        return new ValuedFrameImpl().copyFrom(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ValuedFrameImpl frame = (ValuedFrameImpl) o;

        if (!stack.equals(frame.stack))
            return false;
        return locals.equals(frame.locals);
    }

    @Override
    public int hashCode() {
        int result = stack.hashCode();
        result = 31 * result + locals.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Stack:" + stack.size() + ", Locals:" + locals.size();
    }
}

package me.darknet.assembler.compile.analysis.frame;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.AnalysisUtils;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TypedFrameImpl implements TypedFrame {
    private final Deque<ClassType> stack;
    private final Map<Integer, Local> locals;

    /**
     * New frame with a given stack/variable table.
     *
     * @param stack
     *               Stack state.
     * @param locals
     *               Variable table.
     */
    public TypedFrameImpl(@NotNull Deque<ClassType> stack, @NotNull Map<Integer, Local> locals) {
        this.stack = stack;
        this.locals = locals;
    }

    /**
     * New frame with an empty stack.
     *
     * @param locals
     *               Variable map to copy.
     */
    public TypedFrameImpl(@NotNull Map<Integer, Local> locals) {
        this(new ArrayDeque<>(), new TreeMap<>(locals));
    }

    /**
     * New frame with an empty stack and no variables.
     */
    public TypedFrameImpl() {
        this(new ArrayDeque<>(), new TreeMap<>());
    }

    @Override
    public boolean merge(@NotNull InheritanceChecker checker, @NotNull Frame other) throws FrameMergeException {
        if (other instanceof TypedFrameImpl simpleOther)
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
    public boolean merge(@NotNull InheritanceChecker checker, @NotNull TypedFrame other) throws FrameMergeException {
        boolean changed = false;
        for (Map.Entry<Integer, Local> entry : other.getLocals().entrySet()) {
            int index = entry.getKey();
            Local otherLocal = entry.getValue();
            ClassType otherType = otherLocal.type();
            ClassType ourType = getLocalType(index);
            if (otherType == Types.VOID || ourType == Types.VOID) {
                continue;
            }
            if (ourType == null) {
                changed = true;
                setLocal(index, otherLocal);
            } else {
                ClassType merged = AnalysisUtils.commonType(checker, ourType, otherType);
                if (!Objects.equals(merged, ourType)) {
                    changed = true;
                    locals.put(index, otherLocal.adaptType(merged));
                }
            }
        }

        Deque<ClassType> otherStack = other.getStack();
        if (stack.size() != otherStack.size())
            throw new FrameMergeException(
                    this, other, "Stack size mismatch, " + stack.size() + " != " + otherStack.size()
            );

        Deque<ClassType> newStack = new ArrayDeque<>();
        Iterator<ClassType> it1 = stack.iterator();
        Iterator<ClassType> it2 = otherStack.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            ClassType type1 = it1.next();
            ClassType type2 = it2.next();
            if (type1 == Types.VOID || type2 == Types.VOID) {
                newStack.add(Types.VOID);
                continue;
            }
            ClassType merged = AnalysisUtils.commonType(checker, type1, type2);
            if (!Objects.equals(merged, type1)) {
                changed = true;
                it1.remove();
                newStack.add(merged);
            } else {
                newStack.add(type1);
            }
        }
        stack.clear();
        stack.addAll(newStack);
        return changed;
    }

    @NotNull
    @Override
    public Deque<ClassType> getStack() {
        return stack;
    }

    @NotNull
    @Override
    public Map<Integer, Local> getLocals() {
        return locals;
    }

    @Override
    public void setLocal(int index, @NotNull Local local) {
        if (local.type() == AnalysisUtils.NULL)
            local = local.adaptType(Types.OBJECT);
        locals.put(index, local);
    }

    @Override
    @SuppressWarnings("ConstantValue")
    public void pushType(@NotNull ClassType type) {
        if (type == null)
            throw new IllegalStateException("Cannot push null as typed value to stack");
        stack.push(type);
        if (type == Types.LONG || type == Types.DOUBLE)
            stack.push(Types.VOID);
    }

    @Override
    public void pushNull() {
        // Not handled in this implementation
        pushType(Types.OBJECT);
    }

    @NotNull
    @Override
    public ClassType peek() {
        if (stack.isEmpty())
            throw new IllegalStateException("Cannot peek from empty stack");
        return stack.peek();
    }

    @NotNull
    @Override
    public ClassType pop() {
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
    private TypedFrameImpl copyFrom(@NotNull TypedFrameImpl frame) {
        locals.putAll(frame.locals);
        stack.addAll(frame.stack);
        return this;
    }

    @NotNull
    @Override
    public TypedFrameImpl copy() {
        return new TypedFrameImpl().copyFrom(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TypedFrameImpl frame = (TypedFrameImpl) o;

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

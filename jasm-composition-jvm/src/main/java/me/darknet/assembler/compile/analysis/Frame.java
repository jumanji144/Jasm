package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.*;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Frame {

    public static final ClassType NULL = Types.instanceTypeFromDescriptor("null");
    public static final ClassType OBJECT = Types.instanceType(Object.class);

    private final Deque<ClassType> stack;
    private final Map<Integer, LocalInfo> locals;

    /**
     * New frame with a given stack/variable table.
     * @param stack Stack state.
     * @param locals Variable table.
     */
    public Frame(@NotNull Deque<ClassType> stack, @NotNull Map<Integer, LocalInfo> locals) {
        this.stack = stack;
        this.locals = locals;
    }

    /**
     * New frame with an empty stack.
     * @param locals Variable map to copy.
     */
    public Frame(@NotNull Map<Integer, LocalInfo> locals) {
        this(new ArrayDeque<>(), new TreeMap<>(locals));
    }

    /**
     * New frame with an empty stack and no variables.
     */
    public Frame() {
        this(new ArrayDeque<>(), new TreeMap<>());
    }

    /**
     * Merges types of variables and stack items, taking place in this frame.
     *
     * @param checker Inheritance checker to use for determining common super-types.
     * @param other Frame to merge into this one.
     *
     * @return {@code true} when changes were made during the merge process.
     * {@code false} if no changes were made, indicating equal frames.
     */
    public boolean merge(@NotNull InheritanceChecker checker, @NotNull Frame other) {
        boolean changed = false;
        for (Map.Entry<Integer, LocalInfo> entry : other.locals.entrySet()) {
            int index = entry.getKey();
            LocalInfo otherLocal = entry.getValue();
            ClassType otherType = otherLocal.type();
            ClassType ourType = getLocalType(index);
            if (otherType == Types.VOID || ourType == Types.VOID) {
                continue;
            }
            if (ourType == null) {
                changed = true;
                setLocal(index, otherLocal);
            } else {
                ClassType merged = commonType(checker, ourType, otherType);
                if (!Objects.equals(merged, ourType)) {
                    changed = true;
                    locals.put(index, otherLocal.adaptType(merged));
                }
            }
        }
        Deque<ClassType> newStack = new ArrayDeque<>();
        Iterator<ClassType> it1 = stack.iterator();
        Iterator<ClassType> it2 = other.stack.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            ClassType type1 = it1.next();
            ClassType type2 = it2.next();
            if(type1 == Types.VOID || type2 == Types.VOID){
                newStack.add(Types.VOID);
                continue;
            }
            ClassType merged = commonType(checker, type1, type2);
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

    /**
     * @return Stack.
     */
    @NotNull
    public Deque<ClassType> getStack() {
        return stack;
    }

    /**
     * @return Map of variables.
     */
    @NotNull
    public Map<Integer, LocalInfo> getLocals() {
        return locals;
    }

    /**
     * @param index Index of variable to assign.
     * @param local Variable info to assign.
     */
    public void setLocal(int index, LocalInfo local) {
        if (local.type() == NULL)
            local = local.adaptType(OBJECT);
        locals.put(index, local);
    }

    /**
     * @param index Index of variable to get.
     * @return Variable info.
     */
    @Nullable
    public LocalInfo getLocal(int index) {
        return locals.get(index);
    }

    /**
     * @param index Index of variable to check.
     * @return Type of the variable, or {@code null} if not a known variable.
     */
    @Nullable
    public ClassType getLocalType(int index) {
        LocalInfo local = getLocal(index);
        if (local == null)
            return null;
        return local.type();
    }

    /**
     * @param index Index of variable to check.
     * @return {@code true} when that index is a known variable.
     */
    public boolean hasLocal(int index) {
        return locals.containsKey(index);
    }

    /**
     * @param type Type to push onto the stack.
     */
    public void push(ClassType type) {
        if (type == null)
            throw new IllegalStateException("Cannot push null as typed value to stack");
        stack.push(type);
    }

    /**
     * Push null value onto the stack.
     */
    public void pushNull() {
        // TODO: track null state
        //stack.push(NULL);
        push(OBJECT);
    }

    /**
     * @param types Types to push onto the stack.
     */
    public void push(ClassType... types) {
        for (ClassType type : types) {
            push(type);
        }
    }

    /**
     * @param type Type to push onto the stack.
     */
    public void pushType(ClassType type) {
        push(type);
        if (type == Types.LONG || type == Types.DOUBLE) {
            push(Types.VOID);
        }
    }

    /**
     * @return Top type on the stack.
     */
    @Nullable
    public ClassType peek() {
        return stack.peek();
    }

    /**
     * Removes the top item from the stack, returning that value.
     *
     * @return Top type on the stack.
     */
    @NotNull
    public ClassType pop() {
        try {
            return stack.pop();
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("Cannot pop from empty stack");
        }
    }

    /**
     * @param n Number of items to pop off the stack.
     */
    public void pop(int n) {
        for (int i = 0; i < n; i++) {
            pop();
        }
    }

    /**
     * @return Top type <i>(offset by one)</i> on the stack.
     */
    @NotNull
    public ClassType pop2() {
        pop();
        return pop();
    }

    /**
     * @param type Type to pop off the stack.
     */
    @NotNull
    public ClassType pop(ClassType type) {
        if (type == Types.LONG || type == Types.DOUBLE) {
            return pop2();
        } else {
            return pop();
        }
    }

    /**
     * @param frame Frame to copy stack/locals from.
     * @return Self.
     */
    public Frame copyFrom(@NotNull Frame frame) {
         locals.putAll(frame.locals);
         stack.addAll(frame.stack);
         return this;
    }

    /**
     * @return Copy of the current frame.
     */
    @NotNull
    public Frame copy() {
        return new Frame().copyFrom(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Frame frame = (Frame) o;

        if (!stack.equals(frame.stack)) return false;
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

    /**
     * @param checker Inheritance checker to use for determining common super-types.
     * @param a Some type.
     * @param b Some type.
     * @return Common type between the two.
     */
    public static ClassType commonType(@NotNull InheritanceChecker checker, @NotNull ClassType a, @NotNull ClassType b) {
        if (a instanceof PrimitiveType || b instanceof PrimitiveType) {
            if (isInteger(a) && isInteger(b)) {
                return Types.INT;
            }
            if (a == b) {
                return a;
            } else {
                return Types.VOID;
            }
        } else {
            ObjectType aObj = (ObjectType) a;
            ObjectType bObj = (ObjectType) b;

            if (aObj.equals(bObj)) return aObj;

            String commonType = checker.getCommonSuperclass(aObj.internalName(), bObj.internalName());

            if (commonType == null) return OBJECT;
            else return Types.objectTypeFromInternalName(commonType);
        }
    }

    /**
     * @param type Some type to check.
     * @return {@code true} when the type is within the scope of an {@code int}.
     */
    public static boolean isInteger(ClassType type) {
        return type == Types.BYTE || type == Types.SHORT || type == Types.INT || type == Types.CHAR || type == Types.BOOLEAN;
    }
}

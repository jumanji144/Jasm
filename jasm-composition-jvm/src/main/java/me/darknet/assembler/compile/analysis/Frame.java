package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.PrimitiveType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compiler.InheritanceChecker;

import java.util.*;

public class Frame {

    public static final ClassType NULL = Types.instanceTypeFromDescriptor("null");
    public static final ClassType OBJECT = Types.instanceType(Object.class);
    private final Deque<ClassType> stack;
    private final Map<Integer, ClassType> locals;
    private ClassType lastPopped, lastPushed;

    public Frame(Deque<ClassType> stack, Map<Integer, ClassType> locals) {
        this.stack = stack;
        this.locals = locals;
    }

    public Frame(Map<Integer, ClassType> locals) {
        this(new ArrayDeque<>(), locals);
    }

    public Frame() {
        this(new ArrayDeque<>(), new HashMap<>());
    }

    public static ClassType merge(InheritanceChecker checker, ClassType a, ClassType b) {
        if(a instanceof PrimitiveType || b instanceof PrimitiveType) {
            if(isInteger(a) && isInteger(b)) {
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
            String commonType = checker.getCommonSuperclass(aObj.internalName(), bObj.internalName());

            if(commonType == null) return OBJECT;
            else return Types.objectTypeFromInternalName(commonType);
        }
    }

    public static boolean isInteger(ClassType p) {
        return p == Types.BYTE || p == Types.SHORT || p == Types.INT || p == Types.CHAR || p == Types.BOOLEAN;
    }

    public boolean merge(InheritanceChecker checker, Frame other) {
        boolean changed = false;
        for (Map.Entry<Integer, ClassType> entry : other.locals.entrySet()) {
            int index = entry.getKey();
            ClassType type = entry.getValue();
            ClassType local = locals.get(index);
            if (type == Types.VOID || local == Types.VOID) {
                continue;
            }
            if (local == null) {
                changed = true;
                locals.put(index, type);
            } else {
                ClassType merged = merge(checker, local, type);
                if (!Objects.equals(merged, local)) {
                    changed = true;
                    locals.put(index, merged);
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
            ClassType merged = merge(checker, type1, type2);
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

    public Deque<ClassType> stack() {
        return stack;
    }

    public Map<Integer, ClassType> locals() {
        return locals;
    }

    public void local(int index, ClassType type) {
        if(type == NULL)
            type = OBJECT;
        locals.put(index, type);
    }

    public ClassType local(int index) {
        return locals.get(index);
    }

    public void push(ClassType type) {
        if (type == null)
            throw new IllegalStateException("Cannot push null as typed value to stack");
        stack.push(type);
        lastPushed = type;
    }

    public void pushNull() {
        // TODO: track null state
        //stack.push(NULL);
        push(OBJECT);
    }

    public void push(ClassType... types) {
        for (ClassType type : types) {
            push(type);
        }
    }

    public ClassType pop() {
        lastPopped = stack.peek();
        try {
            return stack.pop();
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("Cannot pop from empty stack");
        }
    }

    public void pop(int n) {
        for (int i = 0; i < n; i++) {
            pop();
        }
    }

    public void pop2() {
        pop();
        pop();
    }

    public void pop(ClassType type) {
        if (type == Types.LONG || type == Types.DOUBLE) {
            pop2();
        } else {
            pop();
        }
    }

    public void pushType(ClassType type) {
        push(type);
        if (type == Types.LONG || type == Types.DOUBLE) {
            push(Types.VOID);
        }
    }

    public ClassType peek() {
        return stack.peek();
    }

    public void locals(Collection<ClassType> locals, int offset) {
        int index = 0;
        for (ClassType type : locals) {
            local(index + offset, type);
            index++;
        }
    }

    public boolean hasLocal(int index) {
        return locals.containsKey(index);
    }

    public void copyInto(Frame frame) {
        frame.locals.putAll(locals);
        frame.stack.addAll(stack);
    }

    public Frame copy() {
        var frame = new Frame();
        copyInto(frame);
        return frame;
    }

    public ClassType lastPopped() {
        return lastPopped;
    }

    public ClassType lastPushed() {
        return lastPushed;
    }

}

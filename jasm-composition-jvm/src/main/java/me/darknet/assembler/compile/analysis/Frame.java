package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;

import java.util.*;

public class Frame {

    public static final ClassType NULL = Types.instanceTypeFromDescriptor("null");
    private final Deque<ClassType> stack;
    private final Map<Integer, ClassType> locals;
    private ClassType lastPopped, lastPushed;

    public Frame(Deque<ClassType> stack, Map<Integer, ClassType> locals) {
        this.stack = stack;
        this.locals = locals;
    }

    public Frame() {
        this(new ArrayDeque<>(), new HashMap<>());
    }

    public Deque<ClassType> stack() {
        return stack;
    }

    public Map<Integer, ClassType> locals() {
        return locals;
    }

    public void local(int index, ClassType type) {
        if(type == NULL)
            type = Types.instanceType(Object.class);
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
        stack.push(NULL);
    }

    public void push(ClassType... types) {
        for (ClassType type : types) {
            push(type);
        }
    }

    public ClassType pop() {
        lastPopped = stack.peek();
        return stack.pop();
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

package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

public class Frame {

    private final Deque<ClassType> stack;
    private final ClassType[] locals;

    public Frame(Deque<ClassType> stack, ClassType[] locals) {
        this.stack = stack;
        this.locals = locals;
    }

    public Frame(int numLocals) {
        this(new ArrayDeque<>(), new ClassType[numLocals]);
    }

    public Deque<ClassType> stack() {
        return stack;
    }

    public ClassType[] locals() {
        return locals;
    }

    public void local(int index, ClassType type) {
        locals[index] = type;
    }

    public ClassType local(int index) {
        return locals[index];
    }

    public void push(ClassType type) {
        stack.push(type);
    }

    public void push(ClassType... types) {
        for (ClassType type : types) {
            stack.push(type);
        }
    }

    public ClassType pop() {
        return stack.pop();
    }

    public void pop(int n) {
        for (int i = 0; i < n; i++) {
            stack.pop();
        }
    }

    public void pop2() {
        stack.pop();
        stack.pop();
    }

    public ClassType peek() {
        return stack.peek();
    }

    public void locals(Collection<ClassType> locals, int offset) {
        int index = 0;
        for (ClassType type : locals) {
            this.locals[index + offset] = type;
            index++;
        }
    }

    public void copyInto(Frame frame) {
        System.arraycopy(locals, 0, frame.locals, 0, locals.length);
        frame.stack.addAll(stack);
    }

    public Frame copy() {
        var frame = new Frame(locals.length);
        copyInto(frame);
        return frame;
    }

}

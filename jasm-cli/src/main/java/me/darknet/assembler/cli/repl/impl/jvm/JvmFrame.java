package me.darknet.assembler.cli.repl.impl.jvm;

import me.darknet.assembler.cli.repl.executor.Frame;

import java.util.*;

public class JvmFrame implements Frame {

    private final Stack<Object> stack = new Stack<>();
    private final Map<Integer, Local> locals = new HashMap<>();

    public Map<Integer, Local> locals() {
        return locals;
    }

    public Stack<Object> stack() {
        return stack;
    }

    public void setLocal(int idx, Local value) {
        locals.put(idx, value);
    }

    public Local getLocal(int idx) {
        return locals.get(idx);
    }

    public void push(Object value) {
        stack.push(value);
    }

    public void push(Object... values) {
        for (Object value : values) {
            push(value);
        }
    }

    public Object pop() {
        return stack.pop();
    }

    public Object peek() {
        return stack.peek();
    }

    public Object pop2() {
        pop();
        return pop();
    }

    @Override
    public String view() {
        return null;
    }
}

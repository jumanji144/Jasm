package me.darknet.assembler.cli.repl.impl;

import me.darknet.assembler.cli.repl.Repl;
import me.darknet.assembler.cli.repl.executor.Executor;
import me.darknet.assembler.cli.repl.impl.jvm.JvmExecutor;
import me.darknet.assembler.cli.repl.impl.jvm.JvmFrame;
import me.darknet.assembler.cli.repl.impl.jvm.Local;
import me.darknet.assembler.parser.BytecodeFormat;

import java.util.EmptyStackException;
import java.util.Map;
import java.util.Set;

public class JvmRepl extends Repl {

    private final JvmExecutor executor;

    public JvmRepl() {
        super(new JvmExecutor(), BytecodeFormat.JVM);
        this.executor = (JvmExecutor) super.executor;
    }

    @Override
    protected void execute(String command) {
        switch (command) {
            case "peek":
                System.out.println(peek());
                return;
            case "stack": {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < executor.frame().stack().size(); i++) {
                    builder.append(String.format("[%d] %s%n", i, view(executor.frame().stack().get(i))));
                }
                System.out.println(builder);
                return;
            }
            case "locals": {
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<Integer, Local> entry : executor.frame().locals().entrySet()) {
                    Local local = entry.getValue();
                    builder.append(String.format("[%d] %s: %s%n", entry.getKey(), local.name(), view(local.value())));
                }
                System.out.println(builder);
                return;
            }
            case "frame": {
                JvmFrame frame = executor.frame();
                StringBuilder builder = new StringBuilder();
                builder.append("Stack:\n");
                for (int i = 0; i < frame.stack().size(); i++) {
                    builder.append(String.format("[%d] %s%n", i, view(frame.stack().get(i))));
                }
                builder.append("Locals:\n");
                for (Map.Entry<Integer, Local> entry : frame.locals().entrySet()) {
                    Local local = entry.getValue();
                    builder.append(String.format("[%d] %s: %s%n", entry.getKey(), local.name(), view(local.value())));
                }
                System.out.println(builder);
                return;
            }
        }
        try {
            super.execute(command);
        } catch (EmptyStackException e) {
            System.out.println("StackUnderflowError");
        } catch (ArithmeticException e) {
            System.out.println("ArithmeticException: " + e.getMessage());
        }
    }

    private String peek() {
        JvmFrame frame = executor.frame();
        Object peek = frame.stack().peek();
        return String.format("[%d] %s", frame.stack().size() - 1, view(peek));
    }

    private String view(Object value) {
        return String.format("%s [%s]", value, value.getClass().getSimpleName());
    }
}

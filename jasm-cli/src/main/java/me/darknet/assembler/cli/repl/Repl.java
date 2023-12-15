package me.darknet.assembler.cli.repl;

import me.darknet.assembler.cli.repl.compiler.StatementCompiler;
import me.darknet.assembler.cli.repl.executor.Executor;
import me.darknet.assembler.cli.repl.executor.Frame;
import me.darknet.assembler.cli.repl.impl.JvmRepl;
import me.darknet.assembler.cli.repl.impl.jvm.JvmExecutor;
import me.darknet.assembler.cli.repl.impl.jvm.JvmFrame;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.instructions.ParsedInstruction;
import me.darknet.assembler.parser.BytecodeFormat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class Repl {

    private String prompt = "jasm > ";
    private String preamble = "Welcome to the JASM REPL!";
    private String commandPrefix = "/";
    protected final Executor<?> executor;
    protected final StatementCompiler compiler;

    private final Map<String, Consumer<List<String>>> commands = new HashMap<>();

    private boolean shouldRun = true;

    public Repl(Executor<?> executor, BytecodeFormat format) {
        this.executor = executor;
        this.compiler = new StatementCompiler(format);
        registerCommands();
    }

    public void preamble() {
        System.out.println(preamble);
    }

    public void loop() {
        while (shouldRun) {
            System.out.print(prompt);
            String command = readCommand();
            execute(command);
        }
    }

    private String readCommand() {
        // read from stdin
        InputStream in = System.in;
        StringBuilder builder = new StringBuilder();
        int c;
        try {
            while ((c = in.read()) != '\n') {
                builder.append((char) c);
            }
        } catch (Exception e) {
            System.out.println("Error reading from stdin: " + e.getMessage());
        }
        return builder.toString();
    }

    protected void execute(String command) {
        if(command.isBlank()) {
            return;
        }
        if(command.startsWith(commandPrefix)) {
            String[] split = command.split(" ");
            String name = split[0].substring(1);
            String[] args = new String[split.length - 1];
            System.arraycopy(split, 1, args, 0, args.length);
            Consumer<List<String>> consumer = commands.get(name);
            if(consumer == null) {
                System.out.println("Unknown command: " + name);
                return;
            }
            consumer.accept(List.of(args));
        } else {
            compile(command);
        }
    }

    protected void compile(String content) {
        Result<ParsedInstruction> result = compiler.compile(content);
        if(result.hasErr()) {

            for (Error error : result.errors()) {
                System.out.println("|   Error:");
                String message = error.getMessage();
                // lowercase first letter
                message = message.substring(0, 1).toLowerCase() + message.substring(1);
                System.out.println("|   " + message);
                // print location arrows
                System.out.println("|   " + content);
                int offendingStart = error.getLocation().column() - error.getLocation().length() - 1;
                System.out.print("|   ");
                for (int i = 0; i < offendingStart; i++) {
                    System.out.print(" ");
                }
                for (int i = 0; i < error.getLocation().length(); i++) {
                    System.out.print("^");
                }
                System.out.println();
            }

            return;
        }
        executor.execute(result.get());
    }

    private void registerCommands() {
        addCommand("exit", args -> shouldRun = false);
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setPreamble(String preamble) {
        this.preamble = preamble;
    }

    public void addCommand(String name, Consumer<List<String>> consumer) {
        commands.put(name, consumer);
    }

    public static void main(String[] args) {
        JvmRepl repl = new JvmRepl();
        repl.preamble();
        repl.loop();
    }

}

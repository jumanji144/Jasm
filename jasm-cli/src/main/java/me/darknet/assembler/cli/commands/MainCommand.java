package me.darknet.assembler.cli.commands;

import picocli.CommandLine;

@CommandLine.Command(
        name = "jasm", subcommands = { CompileCommand.class,
                DecompileCommand.class, }, description = "Java Assembler CLI", version = "2.0.0", mixinStandardHelpOptions = true
)
public class MainCommand implements Runnable {

    protected enum Target { JVM, DALVIK }

    @CommandLine.Option(
            names = { "-t", "--target" }, description = "Target platform\n"
                    + "Possible values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})", defaultValue = "JVM"
    )
    protected static Target target;

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}

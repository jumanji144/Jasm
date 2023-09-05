package me.darknet.assembler.cli.commands;

import me.darknet.assembler.parser.BytecodeFormat;
import picocli.CommandLine;

@CommandLine.Command(
        name = "jasm", subcommands = { CompileCommand.class,
                DecompileCommand.class, }, description = "Java Assembler CLI", version = "2.0.0", mixinStandardHelpOptions = true
)
public class MainCommand implements Runnable {

    @CommandLine.Option(
            names = { "-t", "--target" }, description = "Target platform\n"
                    + "Possible values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})", defaultValue = "JVM"
    )
    protected static BytecodeFormat target;

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}

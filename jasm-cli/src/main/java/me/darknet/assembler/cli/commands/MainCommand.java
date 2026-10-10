package me.darknet.assembler.cli.commands;

import me.darknet.assembler.cli.targets.CliTargetCandidates;
import picocli.CommandLine;

@CommandLine.Command(
        name = "jasm", subcommands = { CompileCommand.class,
                DecompileCommand.class, }, description = "Java Assembler CLI", version = "2.0.0", mixinStandardHelpOptions = true
)
public class MainCommand implements Runnable {

    @CommandLine.Option(
            names = { "-t", "--target" }, completionCandidates = CliTargetCandidates.class,
            description = "Target platform\nPossible values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})",
            defaultValue = "JVM"
    )
    private String target = "JVM";

    String target() {
        return target;
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}

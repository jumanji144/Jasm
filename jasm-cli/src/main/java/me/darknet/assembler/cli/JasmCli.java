package me.darknet.assembler.cli;

import me.darknet.assembler.cli.commands.MainCommand;
import picocli.CommandLine;

/**
 * Process entry point for the Jasm command-line application.
 * <p>
 * Builds a Picocli {@link CommandLine} around {@link MainCommand}; this class is the jar's
 * {@code Main-Class}.
 */
public class JasmCli {
	public static void main(String[] args) {
		CommandLine commandLine = new CommandLine(new MainCommand());
		commandLine.setCaseInsensitiveEnumValuesAllowed(true);
		commandLine.setExecutionExceptionHandler((e, cmd, parseResult) -> {
			if (e instanceof CommandLine.ExecutionException) {
				cmd.getErr().println(e.getMessage());
			} else {
				e.printStackTrace(cmd.getErr());
			}
			return 1;
		});
		System.exit(commandLine.execute(args));
	}
}

package me.darknet.assembler.cli.targets;

import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.Severity;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

/**
 * Shared Picocli streams and command-local failure creation.
 *
 * @param commandLine
 * 		The Picocli command line context.
 */
public record CliRuntime(@NotNull CommandLine commandLine) {
	public CommandLine.ExecutionException failure(String message) {
		return new CommandLine.ExecutionException(commandLine, message);
	}

	public CommandLine.ExecutionException failure(String message, Throwable cause) {
		return new CommandLine.ExecutionException(commandLine,
				message + " (" + cause.getClass().getSimpleName() + ": " + cause.getMessage() + ")");
	}

	public void info(String message) {
		commandLine.getErr().println(message);
	}

	public void stdout(String text) {
		commandLine.getOut().print(text);
		commandLine.getOut().flush();
	}

	public void warnings(Iterable<Diagnostic> diagnostics) {
		for (Diagnostic diagnostic : diagnostics) {
			if (diagnostic.severity() == Severity.WARNING) {
				commandLine.getErr().println(diagnostic);
			}
		}
	}
}

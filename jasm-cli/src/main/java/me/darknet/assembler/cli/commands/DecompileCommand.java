package me.darknet.assembler.cli.commands;

import me.darknet.assembler.cli.targets.CliRuntime;
import me.darknet.assembler.cli.targets.CliTarget;
import me.darknet.assembler.cli.targets.CliTargetCandidates;
import me.darknet.assembler.cli.targets.CliTargets;
import me.darknet.assembler.cli.targets.DecompileRequest;
import me.darknet.assembler.printer.PrintContext;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "decompile", description = "Decompile Java Assembler bytecode", mixinStandardHelpOptions = true
)
public class DecompileCommand implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.ParentCommand
    private MainCommand parentCommand;

    @CommandLine.Parameters(index = "0", description = "Source file", arity = "1", paramLabel = "file")
    private File source;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output file or directory")
    private Optional<File> output = Optional.empty();

    @CommandLine.Option(names = {"-i", "--indent"}, description = "Indentation", defaultValue = "    ")
    private String indent;

    @CommandLine.Option(
            names = {"-c", "--class"},
            description = "Class name (in Java or internal-name form) for an archive or DEX file",
            paramLabel = "name"
    )
    private Optional<String> className = Optional.empty();

    @CommandLine.Option(
            names = {"-t", "--target"}, completionCandidates = CliTargetCandidates.class,
            description = "Target platform (overrides the root target)", paramLabel = "target"
    )
    private Optional<String> targetOverride = Optional.empty();

    @CommandLine.Option(
            names = {"--float-representation", "--float-format"},
            description = "Floating-point representation: standard, hex or binary",
            defaultValue = "standard", paramLabel = "mode"
    )
    private String floatRepresentation;

    @Override
    public Integer call() {
        CliRuntime runtime = new CliRuntime(commandSpec.commandLine());
        CliTarget target = resolveTarget(runtime);
        PrintContext.FloatPrintMode floatMode;
        try {
            floatMode = PrintContext.FloatPrintMode.fromString(floatRepresentation.toLowerCase());
        } catch (IllegalArgumentException exception) {
            throw runtime.failure("Invalid float representation: " + floatRepresentation);
        }
        DecompileRequest request = new DecompileRequest(
                runtime,
                source,
                output,
                indent,
                className,
                floatMode
        );
        try {
            return target.decompile(request);
        } catch (CommandLine.ExecutionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw runtime.failure("Failed to decompile file: " + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            throw runtime.failure("Failed to decompile file: " + exception.getMessage(), exception);
        }
    }

    private CliTarget resolveTarget(CliRuntime runtime) {
        String value = targetOverride.orElse(parentCommand.target());
        CliTarget target = CliTargets.findStrategy(value);
        if (target == null) {
            String validIds = String.join(", ", CliTargets.registry().ids().stream().map(id -> id.value()).toList());
            throw runtime.failure("Unknown target: " + value + ". Valid targets: " + validIds);
        }
        return target;
    }

}

package me.darknet.assembler.cli.commands;

import me.darknet.assembler.cli.targets.CliRuntime;
import me.darknet.assembler.cli.targets.CliTarget;
import me.darknet.assembler.cli.targets.CliTargetCandidates;
import me.darknet.assembler.cli.targets.CliTargets;
import me.darknet.assembler.cli.targets.CompileRequest;
import me.darknet.assembler.cli.targets.SourceUnit;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "compile", description = "Compile Java Assembler source code", mixinStandardHelpOptions = true
)
public class CompileCommand implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.ParentCommand
    private MainCommand parentCommand;

    @CommandLine.Parameters(index = "0..*", description = "Source file(s)", arity = "0..*", paramLabel = "file")
    private List<File> sources = new ArrayList<>();

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output file", paramLabel = "file")
    private File output;

    @CommandLine.Option(names = {"-s", "--source"}, description = "Source code", paramLabel = "code")
    private Optional<String> sourceCode = Optional.empty();

    @CommandLine.Option(names = {"-ov", "--overlay"}, description = "Overlay class file\nRequired for non-class code", paramLabel = "file")
    private Optional<File> overlay = Optional.empty();

    @CommandLine.Option(names = {"-at", "--annotation-target"}, description = "Annotation target", paramLabel = "target")
    private Optional<String> annotationTarget = Optional.empty();

    @CommandLine.Option(names = {"-bv", "--bytecode-version"}, description = "Bytecode version", paramLabel = "version")
    private Optional<Integer> bytecodeVersion = Optional.empty();

    @CommandLine.Option(names = {"-lib", "--library-folder"}, description = "Library folder path", paramLabel = "path")
    private Optional<String> libraryFolder = Optional.empty();

    @CommandLine.Option(
            names = {"-ic", "--inheritance-checker"},
            description = "Enable the inheritance checker (default: ${DEFAULT-VALUE})",
            defaultValue = "true", paramLabel = "boolean"
    )
    private boolean enableInheritanceChecker;

    @CommandLine.Option(
            names = {"-t", "--target"}, completionCandidates = CliTargetCandidates.class,
            description = "Target platform (overrides the root target)", paramLabel = "target"
    )
    private Optional<String> targetOverride = Optional.empty();

    @Override
    public Integer call() {
        CliRuntime runtime = new CliRuntime(commandSpec.commandLine());
        CliTarget target = resolveTarget(runtime);
        List<SourceUnit> units = readSources(runtime);
        CompileRequest request = new CompileRequest(
                runtime,
                units,
                Optional.ofNullable(output),
                overlay,
                annotationTarget,
                bytecodeVersion.orElse(target.defaultBytecodeVersion()),
                libraryFolder,
                enableInheritanceChecker
        );
        try {
            return target.compile(request);
        } catch (CommandLine.ExecutionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw runtime.failure("Failed to compile source file: " + exception.getMessage(), exception);
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

    private List<SourceUnit> readSources(CliRuntime runtime) {
        if (sourceCode.isPresent() && !sources.isEmpty()) {
            throw runtime.failure("--source cannot be combined with source files");
        }
        if (sources.isEmpty()) {
            return List.of(new SourceUnit("<stdin>", sourceCode.map(String::trim).orElse(""), null));
        }

        List<SourceUnit> units = new ArrayList<>(sources.size());
        for (File source : sources) {
            try {
                units.add(new SourceUnit(source.getAbsolutePath(), Files.readString(source.toPath()), source.getName()));
            } catch (IOException exception) {
                throw runtime.failure("Failed to read source file: " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(units);
    }
}

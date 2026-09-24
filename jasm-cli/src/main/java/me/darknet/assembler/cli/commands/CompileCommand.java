package me.darknet.assembler.cli.commands;

import me.darknet.assembler.DalvikClassRepresentation;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.cli.compile.jvm.SafeClassLoader;
import me.darknet.assembler.compile.DalvikCompiler;
import me.darknet.assembler.compile.DalvikCompilerOptions;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.ClassResult;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.EmptyInheritanceChecker;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.ReflectiveInheritanceChecker;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.error.Severity;
import me.darknet.assembler.error.Warn;
import me.darknet.assembler.helper.Processor;
import me.darknet.assembler.io.DalvikDexIO;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.target.TargetContext;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "compile", description = "Compile Java Assembler source code", mixinStandardHelpOptions = true
)
public class CompileCommand implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Parameters(index = "0..*", description = "Source file(s)", arity = "0..*", paramLabel = "file")
    private List<File> sources = new ArrayList<>();

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output file", paramLabel = "file")
    private File output;

    @CommandLine.Option(names = {"-s", "--source"}, description = "Source code", paramLabel = "code")
    private Optional<String> sourceCode = Optional.empty();

    @CommandLine.Option(
            names = {"-ov", "--overlay"}, description = "Overlay class file\nRequired for non-class code", paramLabel = "file"
    )
    private Optional<File> overlay = Optional.empty();

    @CommandLine.Option(
            names = {"-at", "--annotation-target"}, description = "Annotation target", paramLabel = "target"
    )
    private Optional<String> annotationTarget = Optional.empty();

    @CommandLine.Option(
            names = {"-bv", "--bytecode-version"}, description = "Bytecode version", paramLabel = "version"
    )
    private Optional<Integer> bytecodeVersion = Optional.empty();

    @CommandLine.Option(
            names = {"-lib", "--library-folder"}, description = "Library folder path", paramLabel = "path"
    )
    private Optional<String> libraryFolder = Optional.empty();

    @CommandLine.Option(
            names = {"-ic", "--inheritance-checker"}, description = "Enable the inheritance checker (default: ${DEFAULT-VALUE})", defaultValue = "true", paramLabel = "boolean"
    )
    private boolean enableInheritanceChecker;

    private Compiler compiler;
    private CompilerOptions<?> options;
    private int resolvedVersion;

    private CommandLine.ExecutionException failure(String message) {
        return new CommandLine.ExecutionException(commandSpec.commandLine(), message);
    }

    private CommandLine.ExecutionException failure(String message, Throwable cause) {
        return new CommandLine.ExecutionException(commandSpec.commandLine(), message + " ("
                + cause.getClass().getSimpleName() + ": " + cause.getMessage() + ")");
    }

    @Override
    public Integer call() {
        List<SourceUnit> units = readSources();
        configureCompiler(units.size());
        return switch (MainCommand.target) {
            case DALVIK -> compileDalvik(units);
            case JVM -> compileJvm(units.getFirst());
            default -> throw failure("Unknown target: " + MainCommand.target);
        };
    }

    private List<SourceUnit> readSources() {
        if (sourceCode.isPresent() && !sources.isEmpty()) {
            throw failure("--source cannot be combined with source files");
        }
        if (sources.isEmpty()) {
            return List.of(new SourceUnit("<stdin>", sourceCode.map(String::trim).orElse(""), null));
        }

        List<SourceUnit> units = new ArrayList<>(sources.size());
        for (File source : sources) {
            try {
                units.add(new SourceUnit(source.getAbsolutePath(), Files.readString(source.toPath()), source.getName()));
            } catch (IOException exception) {
                throw failure("Failed to read source file: " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(units);
    }

    private void configureCompiler(int unitCount) {
        resolvedVersion = bytecodeVersion.orElse(MainCommand.target == BytecodeFormat.DALVIK ? 35 : 8);
        switch (MainCommand.target) {
            case JVM -> configureJvmCompiler(unitCount);
            case DALVIK -> configureDalvikCompiler(unitCount);
            default -> throw failure("Unknown target: " + MainCommand.target);
        }
    }

    private void configureDalvikCompiler(int unitCount) {
        if (resolvedVersion < 35 || resolvedVersion > 41) {
            throw failure("Dalvik bytecode version must be between 35 and 41");
        }
        if (libraryFolder.isPresent()) {
            throw failure("--library-folder is only supported for the JVM target");
        }
        if (annotationTarget.isPresent()) {
            throw failure("--annotation-target is only supported for the JVM target");
        }
        if (overlay.isPresent() && unitCount != 1) {
            throw failure("Dalvik overlays require exactly one source unit");
        }

        compiler = new DalvikCompiler();
        DalvikCompilerOptions dalvikOptions = new DalvikCompilerOptions()
                .version(resolvedVersion)
                .inheritanceChecker(EmptyInheritanceChecker.INSTANCE);
        options = dalvikOptions;

        overlay.ifPresent(file -> {
            if (file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) {
                throw failure("Dalvik overlays must be standalone DEX files");
            }
            try {
                DexFile dex = DalvikDexIO.read(Files.readAllBytes(file.toPath()));
                if (dex.definitions().size() != 1) {
                    throw failure("Dalvik overlay must contain exactly one class");
                }
                dalvikOptions.overlay(new DalvikClassRepresentation(dex.definitions().getFirst()));
            } catch (IOException exception) {
                throw failure("Failed to read overlay file: " + exception.getMessage(), exception);
            }
        });
    }

    private void configureJvmCompiler(int unitCount) {
        if (unitCount != 1) {
            throw failure("The JVM target accepts exactly one source unit");
        }
        compiler = new JvmCompiler();
        options = new JvmCompilerOptions();

        InheritanceChecker inheritanceChecker;
        if (!enableInheritanceChecker) {
            inheritanceChecker = EmptyInheritanceChecker.INSTANCE;
        } else {
            ClassLoader classLoader = new SafeClassLoader(new URL[0]);
            if (libraryFolder.isPresent()) {
                URL[] urls;
                try (var stream = Files.walk(Paths.get(libraryFolder.get()))) {
                    urls = stream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".class") || path.toString().endsWith(".jar"))
                            .map(Path::toUri)
                            .map(uri -> {
                                try {
                                    return uri.toURL();
                                } catch (Exception exception) {
                                    throw failure("Failed to convert path to URL: " + exception.getMessage(), exception);
                                }
                            }).toArray(URL[]::new);
                } catch (IOException exception) {
                    throw failure("Failed to read library folder: " + exception.getMessage(), exception);
                }
                classLoader = new SafeClassLoader(urls);
            }
            inheritanceChecker = new ReflectiveInheritanceChecker(classLoader);
        }

        options.version(resolvedVersion)
                .annotationPath(annotationTarget.orElse(null))
                .inheritanceChecker(inheritanceChecker);

        overlay.ifPresent(file -> {
            try {
                options.overlay(new JavaClassRepresentation(Files.readAllBytes(file.toPath())));
            } catch (IOException exception) {
                throw failure("Failed to read overlay file: " + exception.getMessage(), exception);
            }
        });
    }

    private Integer compileJvm(SourceUnit unit) {
        ClassRepresentation representation = compileUnit(unit);
        if (!(representation instanceof JavaClassRepresentation javaRepresentation)) {
            throw failure("JVM compiler returned an invalid class representation");
        }

        Path outputPath = output == null
                ? Paths.get(defaultOutputName(unit.fileName(), ".class"))
                : output.toPath();
        writeOutput(outputPath, javaRepresentation.classFile());
        return 0;
    }

    private Integer compileDalvik(List<SourceUnit> units) {
        List<ClassDefinition> definitions = new ArrayList<>(units.size());
        Set<String> names = new HashSet<>();
        for (SourceUnit unit : units) {
            ClassRepresentation representation = compileUnit(unit);
            if (!(representation instanceof DalvikClassRepresentation dalvikRepresentation)) {
                throw failure("Dalvik compiler returned an invalid class representation");
            }
            ClassDefinition definition = dalvikRepresentation.definition();
            String name = definition.getType().internalName();
            if (!names.add(name)) {
                throw failure("Duplicate class definition: " + name);
            }
            definitions.add(definition);
        }

        Path outputPath = output == null
                ? Paths.get(units.size() == 1 && units.getFirst().fileName() != null
                ? defaultOutputName(units.getFirst().fileName(), ".dex")
                : "output.dex")
                : output.toPath();
        if (outputPath.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) {
            throw failure("Dalvik compilation writes a standalone DEX file, not an APK");
        }
        try {
            writeOutput(outputPath, DalvikDexIO.write(new DexFile(resolvedVersion, List.copyOf(definitions))));
        } catch (IOException exception) {
            throw failure("Failed to write output file: " + exception.getMessage(), exception);
        }
        return 0;
    }

    private ClassRepresentation compileUnit(SourceUnit unit) {
        TargetContext target = switch (MainCommand.target) {
            case JVM -> JvmTargetContext.INSTANCE;
            case DALVIK -> DalvikTargetContext.INSTANCE;
            default -> throw failure("Unknown target: " + MainCommand.target);
        };
        Outcome<List<ASTElement>> parsed = Processor.processSourceResult(
                unit.code(), unit.sourceName(), target
        );
        printParserWarnings(parsed.diagnostics());
        if (parsed.hasErrors()) {
            throw failure("Failed to parse source file:\n" + formatDiagnostics(parsed.errors()));
        }
        List<ASTElement> ast = parsed.requireValue();
        validateAst(ast);

        Result<? extends ClassResult> compileResult = compiler.compile(ast, options);
        printWarnings(compileResult.getWarns());
        if (compileResult.hasErr()) {
            throw failure("Failed to compile source file:\n" + formatErrors(compileResult.errors()));
        }
        ClassResult result = compileResult.get();
        if (result == null || result.representation() == null) {
            throw failure("Compiler returned no class representation");
        }
        return result.representation();
    }

    private void validateAst(List<ASTElement> ast) {
        if (ast.size() != 1) {
            throw failure("Expected exactly one class, method or field declaration");
        }

        switch (ast.getFirst().type()) {
            case CLASS -> {
            }
            case METHOD, FIELD -> {
                if (overlay.isEmpty()) {
                    throw failure("Overlay is required for non-class code");
                }
            }
            case ANNOTATION -> {
                if (overlay.isEmpty() || annotationTarget.isEmpty()) {
                    throw failure("Overlay and annotation target are required for annotation code");
                }
            }
            default -> throw failure("Expected exactly one class, method or field declaration");
        }
    }

    private void writeOutput(Path outputPath, byte[] bytes) {
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputPath, bytes);
        } catch (IOException exception) {
            throw failure("Failed to write output file: " + exception.getMessage(), exception);
        }
    }

    private static String defaultOutputName(String fileName, String extension) {
        if (fileName == null) {
            return "output" + extension;
        }
        return fileName.endsWith(".jasm")
                ? fileName.substring(0, fileName.length() - ".jasm".length()) + extension
                : fileName + extension;
    }

    private static String formatErrors(List<Error> errors) {
        return String.join("\n", errors.stream().map(String::valueOf).toList());
    }

    private static String formatDiagnostics(List<Diagnostic> diagnostics) {
        return String.join("\n", diagnostics.stream().map(String::valueOf).toList());
    }

    private void printParserWarnings(List<Diagnostic> diagnostics) {
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.severity() == Severity.WARNING)
                commandSpec.commandLine().getErr().println(diagnostic);
        }
    }

    private void printWarnings(Iterable<Warn> warnings) {
        for (Warn warning : warnings) {
            commandSpec.commandLine().getErr().println(warning);
        }
    }

    private record SourceUnit(String sourceName, String code, String fileName) {
    }
}

package me.darknet.assembler.cli.commands;

import me.darknet.assembler.backend.dalvik.io.DalvikDexIO;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.backend.jvm.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.zip.ZipFile;

@CommandLine.Command(
        name = "decompile", description = "Decompile Java Assembler bytecode", mixinStandardHelpOptions = true
)
public class DecompileCommand implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Parameters(index = "0", description = "Source file", arity = "1", paramLabel = "file")
    private File source;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output file")
    private Optional<File> output = Optional.empty();

    @CommandLine.Option(names = {"-i", "--indent"}, description = "Indentation", defaultValue = "    ")
    private String indent;

    @CommandLine.Option(names = {"-c", "--class"}, description = "Class name (in Java or internal-name form) for an archive or DEX file", paramLabel = "name")
    private Optional<String> className = Optional.empty();

    private CommandLine.ExecutionException failure(String message) {
        return new CommandLine.ExecutionException(commandSpec.commandLine(), message);
    }

    private CommandLine.ExecutionException failure(String message, Throwable cause) {
        return new CommandLine.ExecutionException(commandSpec.commandLine(), message + " ("
                + cause.getClass().getSimpleName() + ": " + cause.getMessage() + ")");
    }

    @Override
    public Integer call() {
        try {
            return switch (MainCommand.target) {
                case JVM -> decompileJvm();
                case DALVIK -> decompileDalvik();
                default -> throw failure("Unknown target: " + MainCommand.target);
            };
        } catch (CommandLine.ExecutionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure("Failed to decompile file: " + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            throw failure("Failed to decompile file: " + exception.getMessage(), exception);
        }
    }

    private int decompileJvm() throws IOException {
        String sourceName = source.getName().toLowerCase();
        if (sourceName.endsWith(".jar")) {
            try (ZipFile zipFile = new ZipFile(source)) {
                if (className.isEmpty()) {
                    if (output.isEmpty()) {
                        throw failure("Output folder or target class name is required for decompiling jar files");
                    }
                    Path outputPath = output.get().toPath();
                    for (var entry : zipFile.stream().toList()) {
                        if (!entry.getName().endsWith(".class")) {
                            continue;
                        }
                        String name = entry.getName().replace(".class", ".jasm");
                        Path outputFile = outputPath.resolve(name);
                        try (InputStream stream = zipFile.getInputStream(entry)) {
                            writePrinter(new JvmClassPrinter(stream), outputFile);
                        }
                        commandSpec.commandLine().getErr().println("Decompiled: " + name);
                    }
                    return 0;
                }

                String entryName = className.get().replace('.', '/') + ".class";
                var entry = zipFile.getEntry(entryName);
                if (entry == null) {
                    throw failure("Class not found: " + className.get());
                }
                try (InputStream stream = zipFile.getInputStream(entry)) {
                    writeOrPrint(new JvmClassPrinter(stream));
                }
                return 0;
            }
        }

        try (InputStream stream = Files.newInputStream(source.toPath())) {
            writeOrPrint(new JvmClassPrinter(stream));
        }
        return 0;
    }

    private int decompileDalvik() throws IOException {
        List<ClassDefinition> definitions = flatten(DalvikDexIO.read(source.toPath()));
        String requestedName = className.map(DecompileCommand::internalName).orElse(null);
        if (requestedName != null) {
            ClassDefinition definition = definitions.stream()
                    .filter(candidate -> candidate.getType().internalName().equals(requestedName))
                    .findFirst()
                    .orElseThrow(() -> failure("Class not found: " + className.get()));
            String text = render(new DalvikClassPrinter(definition));
            if (output.isPresent()) {
                writeText(output.get().toPath(), text);
                commandSpec.commandLine().getErr().println("Decompiled: " + requestedName);
            } else {
                printStdout(text);
            }
            return 0;
        }

        if (output.isEmpty()) {
            if (definitions.size() != 1) {
                throw failure("Output folder or target class name is required for decompiling DEX/APK files");
            }
            printStdout(render(new DalvikClassPrinter(definitions.getFirst())));
            return 0;
        }

        Path outputDirectory = output.get().toPath();
        Map<Path, String> rendered = new LinkedHashMap<>();
        for (ClassDefinition definition : definitions) {
            Path relative = classOutputPath(definition.getType().internalName());
            if (rendered.put(relative, render(new DalvikClassPrinter(definition))) != null) {
                throw failure("Duplicate class output path: " + relative);
            }
        }
        if (Files.exists(outputDirectory) && !Files.isDirectory(outputDirectory)) {
            throw failure("Output path is not a directory: " + outputDirectory);
        }
        Files.createDirectories(outputDirectory);
        for (var entry : rendered.entrySet()) {
            Path destination = outputDirectory.resolve(entry.getKey());
            writeText(destination, entry.getValue());
            commandSpec.commandLine().getErr().println("Decompiled: " + entry.getKey());
        }
        return 0;
    }

    private void writeOrPrint(Printer printer) throws IOException {
        if (output.isPresent()) {
            writePrinter(printer, output.get().toPath());
            commandSpec.commandLine().getErr().println("Decompiled successfully");
        } else {
            printStdout(render(printer));
        }
    }

    private void writePrinter(Printer printer, Path outputPath) throws IOException {
        writeText(outputPath, render(printer));
    }

    private String render(Printer printer) {
        PrintContext<?> context = new PrintContext<>(indent);
        printer.print(context);
        return context.toString();
    }

    private void printStdout(String text) {
        commandSpec.commandLine().getOut().print(text);
        commandSpec.commandLine().getOut().flush();
    }

    private static void writeText(Path outputPath, String text) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputPath, text, StandardCharsets.UTF_8);
    }

    private static List<ClassDefinition> flatten(List<DexFile> files) throws IOException {
        Map<String, ClassDefinition> definitions = new LinkedHashMap<>();
        for (DexFile file : files) {
            for (ClassDefinition definition : file.definitions()) {
                String name = definition.getType().internalName();
                if (definitions.putIfAbsent(name, definition) != null) {
                    throw new IOException("Duplicate class definition: " + name);
                }
            }
        }
        return List.copyOf(definitions.values());
    }

    private static String internalName(String name) {
        return name.replace('.', '/');
    }

    private static Path classOutputPath(String internalName) {
        Path relative = Paths.get(internalName.replace('/', File.separatorChar) + ".jasm").normalize();
        if (relative.isAbsolute() || relative.getNameCount() == 0 || relative.startsWith("..")) {
            throw new IllegalArgumentException("Unsafe class output path: " + internalName);
        }
        return relative;
    }
}

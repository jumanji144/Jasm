package me.darknet.assembler.cli.commands;

import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipFile;

@CommandLine.Command(
        name = "decompile", description = "Decompile Java Assembler bytecode", mixinStandardHelpOptions = true
)
public class DecompileCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "Source file", arity = "1", paramLabel = "file")
    private File source;

    @CommandLine.Option(names = { "-o", "--output" }, description = "Output file")
    private Optional<File> output;

    @CommandLine.Option(names = { "-i", "--indent" }, description = "Indentation", defaultValue = "    ")
    private String indent;

    @CommandLine.Option(names = { "-c", "--class" }, description = "Class name (in java format a.b.c) if a archive file is used", paramLabel = "name")
    private Optional<String> className;

    @Override
    public void run() {
        OutputStream out = System.out;

        try {
            InputStream classStream = Files.newInputStream(source.toPath());
            if (source.getName().endsWith(".jar")) {
                ZipFile zipFile = new ZipFile(source);
                if (className.isEmpty()) {
                    // decompile all classes
                    if (output.isEmpty()) {
                        System.err.println("Output folder or target class name is required for decompiling jar files");
                        System.exit(1);
                    }

                    Path outputPath = output.get().toPath();

                    zipFile.stream().forEach(entry -> {
                        try {
                            if (entry.getName().endsWith(".class")) {
                                InputStream stream = zipFile.getInputStream(entry);
                                String name = entry.getName().replace(".class", ".jasm");
                                Path outputPathFile = outputPath.resolve(name);
                                // make sure parent directories exist
                                Files.createDirectories(outputPathFile.getParent());
                                decompile(stream, Files.newOutputStream(outputPathFile));
                                System.out.println("Decompiled: " + name);
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to decompile file: " + e.getMessage());
                            e.printStackTrace();
                            System.exit(1);
                        }
                    });

                    return;
                }

                classStream = zipFile.getInputStream(zipFile.getEntry(className.get().replace('.', '/') + ".class"));
            }

            if (output.isPresent()) {
                try {
                    out = Files.newOutputStream(output.get().toPath());
                } catch (IOException e) {
                    System.err.println("Failed to open output file: " + e.getMessage());
                    System.exit(1);
                }
            }

            decompile(classStream, out);

            System.out.println("\nDecompiled successfully");
        } catch (IOException e) {
            System.err.println("Failed to decompile file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void decompile(InputStream input, OutputStream output) throws IOException {
        PrintContext<?> ctx = new PrintContext<>(indent);

        Printer printer;

        switch (MainCommand.target) {
            case JVM -> printer = new JvmClassPrinter(input);
            case DALVIK -> throw new UnsupportedOperationException("Dalvik target is not supported yet");
            default -> throw new UnsupportedOperationException("Unknown target: " + MainCommand.target);
        }

        printer.print(ctx);

        output.write(ctx.toString().getBytes());
    }
}

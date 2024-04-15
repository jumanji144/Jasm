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
        if (output.isPresent()) {
            try {
                out = Files.newOutputStream(output.get().toPath());
            } catch (IOException e) {
                System.err.println("Failed to open output file: " + e.getMessage());
                System.exit(1);
            }
        }


        try {
            InputStream classStream = Files.newInputStream(source.toPath());
            if (source.getName().endsWith(".jar")) {
                if (className.isEmpty()) {
                    System.err.println("Class name is required for archive files");
                    System.exit(1);
                }

                ZipFile zipFile = new ZipFile(source);
                classStream = zipFile.getInputStream(zipFile.getEntry(className.get().replace('.', '/') + ".class"));
            }

            PrintContext<?> ctx = new PrintContext<>(indent);

            Printer printer;

            switch (MainCommand.target) {
                case JVM -> printer = new JvmClassPrinter(classStream);
                case DALVIK -> throw new UnsupportedOperationException("Dalvik target is not supported yet");
                default -> throw new UnsupportedOperationException("Unknown target: " + MainCommand.target);
            }

            printer.print(ctx);

            out.write(ctx.toString().getBytes());
        } catch (IOException e) {
            System.err.println("Failed to decompile source file: " + e.getMessage());
            System.exit(1);
        }
    }
}

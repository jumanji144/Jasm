package me.darknet.assembler.cli.commands;

import me.darknet.assembler.printer.BlwClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Optional;

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
            PrintContext<?> ctx = new PrintContext<>(indent);

            BlwClassPrinter printer = new BlwClassPrinter(Files.newInputStream(source.toPath()));
            printer.print(ctx);

            out.write(ctx.toString().getBytes());
        } catch (IOException e) {
            System.err.println("Failed to decompile source file: " + e.getMessage());
            System.exit(1);
        }
    }
}

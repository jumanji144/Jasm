package me.darknet.assembler.cli.commands;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.cli.compile.jvm.SafeClassLoader;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compiler.*;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.helper.Processor;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@CommandLine.Command(
        name = "compile", description = "Compile Java Assembler source code", mixinStandardHelpOptions = true
)
public class CompileCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "Source file", arity = "0..1", paramLabel = "file")
    private Optional<File> source;

    @CommandLine.Option(names = { "-o", "--output" }, description = "Output file", required = true, paramLabel = "file")
    private File output;

    @CommandLine.Option(names = { "-s", "--source" }, description = "Source code", paramLabel = "code")
    private Optional<String> sourceCode;

    @CommandLine.Option(
            names = { "-ov",
                    "--overlay" }, description = "Overlay class file\nRequired for non-class code", paramLabel = "file"
    )
    private Optional<File> overlay;

    @CommandLine.Option(
            names = { "-at", "--annotation-target" }, description = "Annotation target", paramLabel = "target"
    )
    private Optional<String> annotationTarget;

    @CommandLine.Option(
            names = { "-bv",
                    "--bytecode-version" }, description = "Bytecode version (default: ${DEFAULT-VALUE})", defaultValue = "8", paramLabel = "version"
    )
    private int bytecodeVersion;

    @CommandLine.Option(
            names = { "-lib",
                    "--library" }, description = "Library folder path", paramLabel = "path"
    )
    private Optional<String> libraryFolder;

    private Compiler compiler;
    private CompilerOptions<?> options;

    private void configureCompiler() {
        switch (MainCommand.target) {
            case JVM -> {
                compiler = new JvmCompiler();
                options = new JvmCompilerOptions();
            }
            case DALVIK -> throw new UnsupportedOperationException("Dalvik target is not supported yet");
            default -> throw new UnsupportedOperationException("Unknown target: " + MainCommand.target);
        }

        InheritanceChecker inheritanceChecker = new ReflectiveInheritanceChecker(new SafeClassLoader(new URL[0]));
        if (this.libraryFolder.isPresent()) {
            URL[] urls = new URL[0];
            try (var stream = Files.walk(Paths.get(this.libraryFolder.get()))) {
                urls = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".class") || path.toString().endsWith(".jar"))
                        .map(Path::toUri)
                        .map(uri -> {
                            try {
                                return uri.toURL();
                            } catch (Exception e) {
                                System.err.println("Failed to convert path to URL: " + e.getMessage());
                                System.exit(1);
                                return null;
                            }
                        }).toArray(URL[]::new);
            } catch (IOException e) {
                System.err.println("Failed to read library folder: " + e.getMessage());
                System.exit(1);
            }
            inheritanceChecker = new ReflectiveInheritanceChecker(new SafeClassLoader(urls));
        }

        options.version(bytecodeVersion).overlay(new JavaClassRepresentation(overlay.map(file -> {
            try {
                return Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                System.err.println("Failed to read overlay file: " + e.getMessage());
                System.exit(1);
                return null;
            }
        }).orElse(null))).annotationPath(annotationTarget.orElse(null))
        .inheritanceChecker(inheritanceChecker);
    }

    private void validateAst(List<ASTElement> ast) {
        if (ast.size() != 1) {
            System.err.println("Expected exactly one class, method or field declaration");
            System.exit(1);
        }

        switch (ast.get(0).type()) {
            case CLASS -> {
            }
            case METHOD, FIELD -> {
                if (overlay.isEmpty()) {
                    System.err.println("Overlay is required for non-class code");
                    System.exit(1);
                }
            }
            case ANNOTATION -> {
                if (overlay.isEmpty() || annotationTarget.isEmpty()) {
                    System.err.println("Overlay and annotation target are required for annotation code");
                    System.exit(1);
                }
            }
            default -> {
                System.err.println("Expected exactly one class, method or field declaration");
                System.exit(1);
            }
        }
    }

    @Override
    public void run() {

        String code = sourceCode.map(String::trim).orElse("");
        String src = source.map(File::getAbsolutePath).orElse("<stdin>");

        if (source.isPresent()) {
            try {
                code = Files.readString(source.get().toPath());
            } catch (IOException e) {
                System.err.println("Failed to read source file: " + e.getMessage());
                System.exit(1);
            }
        }

        configureCompiler();

        Processor.processSource(code, src, ast -> {
            validateAst(ast);

            compiler.compile(ast, options).ifErr((unused, errors) -> {
                System.err.println("Failed to compile source file:");
                errors.forEach(System.err::println);
                System.exit(1);
            }).ifOk((result) -> {
                ClassRepresentation representation = result.representation();
                switch (MainCommand.target) {
                    case JVM -> {
                        try {
                            Files.write(output.toPath(), ((JavaClassRepresentation) representation).classFile());
                        } catch (IOException e) {
                            System.err.println("Failed to write output file: " + e.getMessage());
                            System.exit(1);
                        }
                    }
                    case DALVIK -> throw new UnsupportedOperationException("Dalvik target is not supported yet");
                    default -> throw new UnsupportedOperationException("Unknown target: " + MainCommand.target);
                }
            });
        }, errors -> {
            System.err.println("Failed to parse source file:");
            errors.forEach(System.err::println);
            System.exit(1);
        }, MainCommand.target);
    }
}

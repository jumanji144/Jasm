package me.darknet.assembler.cli.targets;

import me.darknet.assembler.backend.jvm.JvmTarget;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.backend.jvm.compile.JavaClassRepresentation;
import me.darknet.assembler.backend.jvm.compile.JavaCompileResult;
import me.darknet.assembler.backend.jvm.compile.JvmCompiler;
import me.darknet.assembler.backend.jvm.compile.JvmCompilerOptions;
import me.darknet.assembler.backend.jvm.printer.JvmClassPrinter;
import me.darknet.assembler.backend.jvm.util.SafeClassLoader;
import me.darknet.assembler.compiler.EmptyInheritanceChecker;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.ReflectiveInheritanceChecker;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.target.TargetId;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.zip.ZipFile;

/**
 * CLI strategy owning JVM compilation and decompilation.
 */
public final class JvmCliTarget implements CliTarget {
	public static final JvmCliTarget INSTANCE = new JvmCliTarget();

	private JvmCliTarget() {}

	@Override
	public @NotNull TargetId id() {
		return JvmTarget.INSTANCE.id();
	}

	@Override
	public int defaultBytecodeVersion() {
		return 8;
	}

	@Override
	public int compile(@NotNull CompileRequest request) throws IOException {
		if (request.units().size() != 1) {
			throw request.runtime().failure("The JVM target accepts exactly one source unit");
		}
		JvmCompilerOptions options = configure(request);
		SourceUnit unit = request.units().getFirst();
		JavaClassRepresentation representation = compileUnit(request, options, unit);
		Path outputPath = request.output().isPresent()
				? request.output().get().toPath()
				: Paths.get(CliUtils.defaultOutputName(unit.fileName(), ".class"));
		try {
			CliUtils.writeBytes(outputPath, representation.classFile());
		} catch (IOException exception) {
			throw request.runtime().failure("Failed to write output file: " + exception.getMessage(), exception);
		}
		return 0;
	}

	@Override
	public int decompile(@NotNull DecompileRequest request) throws IOException {
		File source = request.source();
		if (source.getName().toLowerCase().endsWith(".jar")) {
			return decompileJar(request, source);
		}
		try (InputStream stream = Files.newInputStream(source.toPath())) {
			writeOrPrint(request, new JvmClassPrinter(stream));
		} catch (IOException exception) {
			throw request.runtime().failure("Failed to decompile file: " + exception.getMessage(), exception);
		}
		return 0;
	}

	private JvmCompilerOptions configure(CompileRequest request) {
		CliRuntime runtime = request.runtime();
		JvmCompilerOptions options = new JvmCompilerOptions();

		InheritanceChecker inheritanceChecker;
		if (!request.inheritanceChecker()) {
			inheritanceChecker = EmptyInheritanceChecker.INSTANCE;
		} else {
			ClassLoader classLoader = new SafeClassLoader(new URL[0]);
			if (request.libraryFolder().isPresent()) {
				URL[] urls;
				try (var stream = Files.walk(Paths.get(request.libraryFolder().get()))) {
					urls = stream
							.filter(Files::isRegularFile)
							.filter(path -> path.toString().endsWith(".class") || path.toString().endsWith(".jar"))
							.map(Path::toUri)
							.map(uri -> {
								try {
									return uri.toURL();
								} catch (Exception exception) {
									throw runtime.failure("Failed to convert path to URL: " + exception.getMessage(), exception);
								}
							}).toArray(URL[]::new);
				} catch (IOException exception) {
					throw runtime.failure("Failed to read library folder: " + exception.getMessage(), exception);
				}
				classLoader = new SafeClassLoader(urls);
			}
			inheritanceChecker = new ReflectiveInheritanceChecker(classLoader);
		}

		options.withVersion(request.bytecodeVersion())
				.withAnnotationPath(request.annotationTarget().orElse(null))
				.withInheritanceChecker(inheritanceChecker);

		request.overlay().ifPresent(file -> {
			try {
				options.withOverlay(new JavaClassRepresentation(Files.readAllBytes(file.toPath())));
			} catch (IOException exception) {
				throw runtime.failure("Failed to read overlay file: " + exception.getMessage(), exception);
			}
		});
		return options;
	}

	private JavaClassRepresentation compileUnit(CompileRequest request, JvmCompilerOptions options, SourceUnit source) {
		CliRuntime runtime = request.runtime();
		ValidatedUnit unit = CliCompilePipeline.process(request, source, JvmTargetContext.INSTANCE);
		Outcome<JavaCompileResult> result = new JvmCompiler().compile(unit, options);
		runtime.warnings(result.warnings());
		if (result.hasErrors()) {
			throw runtime.failure("Failed to compile source file:\n" + CliUtils.formatErrors(result.errors()));
		}
		JavaCompileResult compiled = result.requireValue();
		if (compiled.representation() == null) {
			throw runtime.failure("Compiler returned no class representation");
		}
		return compiled.representation();
	}

	private int decompileJar(DecompileRequest request, File source) throws IOException {
		CliRuntime runtime = request.runtime();
		try (ZipFile zipFile = new ZipFile(source)) {
			if (request.className().isEmpty()) {
				if (request.output().isEmpty()) {
					throw runtime.failure("Output folder or target class name is required for decompiling jar files");
				}
				Path outputDirectory = request.output().get().toPath();
				for (var entry : zipFile.stream().toList()) {
					if (!entry.getName().endsWith(".class")) {
						continue;
					}
					String internalName = entry.getName().substring(0, entry.getName().length() - ".class".length());
					Path relative;
					try {
						relative = CliUtils.classOutputPath(internalName);
					} catch (IllegalArgumentException exception) {
						throw runtime.failure(exception.getMessage(), exception);
					}
					try (InputStream stream = zipFile.getInputStream(entry)) {
						CliUtils.writeText(outputDirectory.resolve(relative),
								render(new JvmClassPrinter(stream), request.indent(), request.floatPrintMode()));
					}
					runtime.info("Decompiled: " + relative);
				}
				return 0;
			}

			String entryName = request.className().get().replace('.', '/') + ".class";
			var entry = zipFile.getEntry(entryName);
			if (entry == null) {
				throw runtime.failure("Class not found: " + request.className().get());
			}
			try (InputStream stream = zipFile.getInputStream(entry)) {
				writeOrPrint(request, new JvmClassPrinter(stream));
			}
			return 0;
		} catch (IOException exception) {
			throw runtime.failure("Failed to decompile file: " + exception.getMessage(), exception);
		}
	}

	private void writeOrPrint(DecompileRequest request, Printer printer) throws IOException {
		CliRuntime runtime = request.runtime();
		if (request.output().isPresent()) {
			try {
				CliUtils.writeText(request.output().get().toPath(),
						render(printer, request.indent(), request.floatPrintMode()));
			} catch (IOException exception) {
				throw runtime.failure("Failed to write output file: " + exception.getMessage(), exception);
			}
			runtime.info("Decompiled successfully");
		} else {
			runtime.stdout(render(printer, request.indent(), request.floatPrintMode()));
		}
	}

	private static String render(Printer printer, String indent, PrintContext.FloatPrintMode floatPrintMode) {
		PrintContext<?> context = new PrintContext<>(indent);
		context.setFloatPrintMode(floatPrintMode);
		printer.print(context);
		return context.toString();
	}
}

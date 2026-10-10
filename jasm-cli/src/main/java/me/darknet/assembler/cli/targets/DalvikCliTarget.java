package me.darknet.assembler.cli.targets;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTarget;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompilerOptions;
import me.darknet.assembler.backend.dalvik.io.DalvikDexIO;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.compiler.EmptyInheritanceChecker;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.target.TargetId;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CLI strategy owning Dalvik compilation and DEX/APK decompilation.
 */
public final class DalvikCliTarget implements CliTarget {
	public static final DalvikCliTarget INSTANCE = new DalvikCliTarget();

	private DalvikCliTarget() {}

	@Override
	public @NotNull TargetId id() {
		return DalvikTarget.INSTANCE.id();
	}

	@Override
	public int defaultBytecodeVersion() {
		return 35;
	}

	@Override
	public int compile(@NotNull CompileRequest request) throws IOException {
		CliRuntime runtime = request.runtime();
		DalvikCompilerOptions options = configure(request);
		List<SourceUnit> units = request.units();
		List<ClassDefinition> definitions = new ArrayList<>(units.size());
		Set<String> names = new java.util.LinkedHashSet<>(units.size());
		for (SourceUnit unit : units) {
			ClassDefinition definition = compileUnit(request, options, unit).definition();
			String name = definition.getType().internalName();
			if (!names.add(name)) {
				throw runtime.failure("Duplicate class definition: " + name);
			}
			definitions.add(definition);
		}

		Path outputPath = request.output().isPresent()
				? request.output().get().toPath()
				: Paths.get(units.size() == 1 && units.getFirst().fileName() != null
				? CliUtils.defaultOutputName(units.getFirst().fileName(), ".dex")
				: "output.dex");
		Path outputName = outputPath.getFileName();
		if (outputName != null && outputName.toString().toLowerCase().endsWith(".apk")) {
			throw runtime.failure("Dalvik compilation writes a standalone DEX file, not an APK");
		}
		try {
			byte[] dexBytes = DalvikDexIO.write(new DexFile(request.bytecodeVersion(), List.copyOf(definitions)));
			CliUtils.writeBytes(outputPath, dexBytes);
		} catch (IOException exception) {
			throw runtime.failure("Failed to write output file: " + exception.getMessage(), exception);
		} catch (RuntimeException exception) {
			throw runtime.failure("Failed to encode DEX output", exception);
		}
		return 0;
	}

	@Override
	public int decompile(@NotNull DecompileRequest request) throws IOException {
		CliRuntime runtime = request.runtime();
		List<ClassDefinition> definitions;
		try {
			definitions = flatten(DalvikDexIO.read(request.source().toPath()));
		} catch (IOException exception) {
			throw runtime.failure("Failed to decompile file: " + exception.getMessage(), exception);
		} catch (RuntimeException exception) {
			throw runtime.failure("Failed to decompile file: " + exception.getMessage(), exception);
		}
		String requestedName = request.className().map(name -> name.replace('.', '/')).orElse(null);

		if (requestedName != null) {
			ClassDefinition definition = definitions.stream()
					.filter(candidate -> candidate.getType().internalName().equals(requestedName))
					.findFirst()
					.orElseThrow(() -> runtime.failure("Class not found: " + request.className().get()));
			String text = render(new DalvikClassPrinter(definition), request.indent(), request.floatPrintMode());
			if (request.output().isPresent()) {
				try {
					CliUtils.writeText(request.output().get().toPath(), text);
				} catch (IOException exception) {
					throw runtime.failure("Failed to write output file: " + exception.getMessage(), exception);
				}
				runtime.info("Decompiled: " + requestedName);
			} else {
				runtime.stdout(text);
			}
			return 0;
		}

		if (request.output().isEmpty()) {
			if (definitions.size() != 1) {
				throw runtime.failure("Output folder or target class name is required for decompiling DEX/APK files");
			}
			runtime.stdout(render(new DalvikClassPrinter(definitions.getFirst()), request.indent(), request.floatPrintMode()));
			return 0;
		}

		Path outputDirectory = request.output().get().toPath();
		Map<Path, String> rendered = new LinkedHashMap<>();
		for (ClassDefinition definition : definitions) {
			Path relative;
			try {
				relative = CliUtils.classOutputPath(definition.getType().internalName());
			} catch (IllegalArgumentException exception) {
				throw runtime.failure(exception.getMessage(), exception);
			}
			if (rendered.putIfAbsent(relative,
					render(new DalvikClassPrinter(definition), request.indent(), request.floatPrintMode())) != null) {
				throw runtime.failure("Duplicate class output path: " + relative);
			}
		}
		try {
			if (Files.exists(outputDirectory) && !Files.isDirectory(outputDirectory)) {
				throw runtime.failure("Output path is not a directory: " + outputDirectory);
			}
			Files.createDirectories(outputDirectory);
			for (var entry : rendered.entrySet()) {
				CliUtils.writeText(outputDirectory.resolve(entry.getKey()), entry.getValue());
				runtime.info("Decompiled: " + entry.getKey());
			}
		} catch (IOException exception) {
			throw runtime.failure("Failed to write output files: " + exception.getMessage(), exception);
		}
		return 0;
	}

	private DalvikCompilerOptions configure(CompileRequest request) {
		CliRuntime runtime = request.runtime();
		int version = request.bytecodeVersion();
		// TODO: What versions do we really support?
		// if (version < min || version > max) {
		//    throw runtime.failure("Dalvik bytecode version must be between {MIN} and {MAX}");
		// }
		if (request.libraryFolder().isPresent()) {
			throw runtime.failure("--library-folder is only supported for the JVM target");
		}
		if (request.annotationTarget().isPresent()) {
			throw runtime.failure("--annotation-target is only supported for the JVM target");
		}
		if (request.overlay().isPresent() && request.units().size() != 1) {
			throw runtime.failure("Dalvik overlays require exactly one source unit");
		}

		DalvikCompilerOptions options = new DalvikCompilerOptions()
				.withVersion(version)
				.withInheritanceChecker(EmptyInheritanceChecker.INSTANCE);
		request.overlay().ifPresent(file -> {
			if (file.getName().toLowerCase().endsWith(".apk")) {
				throw runtime.failure("Dalvik overlays must be standalone DEX files");
			}
			DexFile dex;
			try {
				dex = DalvikDexIO.read(Files.readAllBytes(file.toPath()));
			} catch (IOException exception) {
				throw runtime.failure("Failed to read overlay file: " + exception.getMessage(), exception);
			} catch (RuntimeException exception) {
				throw runtime.failure("Failed to read overlay file: " + exception.getMessage(), exception);
			}
			if (dex.definitions().size() != 1) {
				throw runtime.failure("Dalvik overlay must contain exactly one class");
			}
			options.withOverlay(new DalvikClassRepresentation(dex.definitions().getFirst()));
		});
		return options;
	}

	private DalvikClassRepresentation compileUnit(CompileRequest request, DalvikCompilerOptions options, SourceUnit source) {
		CliRuntime runtime = request.runtime();
		ValidatedUnit unit = CliCompilePipeline.process(request, source, DalvikTargetContext.INSTANCE);
		Outcome<DalvikCompileResult> result = new DalvikCompiler().compile(unit, options);
		runtime.warnings(result.warnings());
		if (result.hasErrors()) {
			throw runtime.failure("Failed to compile source file:\n" + CliUtils.formatErrors(result.errors()));
		}
		DalvikCompileResult compiled = result.requireValue();
		if (compiled.representation() == null) {
			throw runtime.failure("Compiler returned no class representation");
		}
		return compiled.representation();
	}

	private static String render(Printer printer, String indent, PrintContext.FloatPrintMode floatPrintMode) {
		PrintContext<?> context = new PrintContext<>(indent);
		context.setFloatPrintMode(floatPrintMode);
		printer.print(context);
		return context.toString();
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
}

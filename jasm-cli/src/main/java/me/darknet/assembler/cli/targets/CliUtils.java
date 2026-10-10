package me.darknet.assembler.cli.targets;

import me.darknet.assembler.error.Diagnostic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Output and formatting helpers shared by CLI target strategies.
 */
final class CliUtils {
	private CliUtils() {}

	static String defaultOutputName(@Nullable String fileName, @NotNull String extension) {
		if (fileName == null)
			return "output" + extension;
		return fileName.endsWith(".jasm")
				? fileName.substring(0, fileName.length() - ".jasm".length()) + extension
				: fileName + extension;
	}

	static void writeBytes(@NotNull Path outputPath, byte @NotNull [] bytes) throws IOException {
		createParent(outputPath);
		Files.write(outputPath, bytes);
	}

	static void writeText(@NotNull Path outputPath, @NotNull String text) throws IOException {
		createParent(outputPath);
		Files.writeString(outputPath, text, StandardCharsets.UTF_8);
	}

	static @NotNull String formatErrors(@NotNull List<Diagnostic> diagnostics) {
		return String.join("\n", diagnostics.stream().map(Diagnostic::format).toList());
	}

	static @NotNull Path classOutputPath(@NotNull String internalName) {
		if (internalName.isEmpty()) {
			throw new IllegalArgumentException("Unsafe class output path: " + internalName);
		}
		Path relative = Paths.get(internalName.replace('/', File.separatorChar) + ".jasm").normalize();
		if (relative.isAbsolute() || relative.getNameCount() == 0 || relative.startsWith("..")) {
			throw new IllegalArgumentException("Unsafe class output path: " + internalName);
		}
		return relative;
	}

	private static void createParent(@NotNull Path outputPath) throws IOException {
		Path parent = outputPath.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
	}
}

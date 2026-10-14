package me.darknet.assembler.cli.targets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One source input supplied to the CLI compiler.
 *
 * @param sourceName
 * 		The name of the source unit (e.g., class name or file name).
 * @param code
 * 		The source code as a string.
 * @param fileName
 * 		The optional file name from which the source was read.
 * 		Can be {@code null} for sources that are not read from a file.
 */
public record SourceUnit(@NotNull String sourceName, @NotNull String code, @Nullable String fileName) {}

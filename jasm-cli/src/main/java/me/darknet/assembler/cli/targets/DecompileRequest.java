package me.darknet.assembler.cli.targets;

import me.darknet.assembler.printer.PrintContext;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Optional;

/**
 * Immutable decompile request passed to a target strategy.
 *
 * @param runtime
 * 		The CLI runtime context.
 * @param source
 * 		The source file to decompile.
 * @param output
 * 		The optional output file for the decompiled result.
 * @param indent
 * 		The indentation string to use for formatting.
 * @param className
 * 		The optional class name to decompile (if applicable).
 * @param floatPrintMode
 * 		The float print mode for formatting floating-point numbers.
 */
public record DecompileRequest(@NotNull CliRuntime runtime,
                               @NotNull File source,
                               @NotNull Optional<File> output,
                               @NotNull String indent,
                               @NotNull Optional<String> className,
                               @NotNull PrintContext.FloatPrintMode floatPrintMode) {}

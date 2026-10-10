package me.darknet.assembler.cli.targets;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Compile request passed to a target strategy.
 *
 * @param runtime
 * 		The CLI runtime context.
 * @param units
 * 		The source units to compile.
 * @param output
 * 		The optional output file for the compiled result.
 * @param overlay
 * 		The optional overlay file for non-class code.
 * @param annotationTarget
 * 		The optional annotation target for annotation code.
 * @param bytecodeVersion
 * 		The bytecode version to use for compilation.
 * @param libraryFolder
 * 		The optional library folder path for resolving dependencies.
 * @param inheritanceChecker
 * 		Whether to enable the inheritance checker during compilation.
 */
public record CompileRequest(@NotNull CliRuntime runtime,
                             @NotNull List<SourceUnit> units,
                             @NotNull Optional<File> output,
                             @NotNull Optional<File> overlay,
                             @NotNull Optional<String> annotationTarget,
                             int bytecodeVersion,
                             @NotNull Optional<String> libraryFolder,
                             boolean inheritanceChecker) {}

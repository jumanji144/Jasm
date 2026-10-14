package me.darknet.assembler.cli.targets;

import me.darknet.assembler.target.TargetId;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * CLI strategy for one bundled assembly target.
 */
public interface CliTarget {
	@NotNull TargetId id();

	int defaultBytecodeVersion();

	int compile(@NotNull CompileRequest request) throws IOException;

	int decompile(@NotNull DecompileRequest request) throws IOException;
}

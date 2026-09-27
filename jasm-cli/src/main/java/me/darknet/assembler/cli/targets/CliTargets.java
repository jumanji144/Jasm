package me.darknet.assembler.cli.targets;

import me.darknet.assembler.backend.dalvik.DalvikTarget;
import me.darknet.assembler.backend.jvm.JvmTarget;
import me.darknet.assembler.target.AssemblyTarget;
import me.darknet.assembler.target.TargetId;
import me.darknet.assembler.target.TargetRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Registry and resolver for the target strategies shipped with the CLI.
 */
public final class CliTargets {
	private static final TargetRegistry REGISTRY = TargetRegistry.of(List.of(JvmTarget.INSTANCE, DalvikTarget.INSTANCE));

	private CliTargets() {}

	public static @NotNull TargetRegistry registry() {
		return REGISTRY;
	}

	public static @Nullable CliTarget findStrategy(@NotNull String value) {
		Objects.requireNonNull(value, "value");
		if (value.isBlank()) {
			return null;
		}
		AssemblyTarget resolved = REGISTRY.find(new TargetId(value));
		if (resolved == null) {
			return null;
		}
		if (resolved.id().equals(JvmTarget.INSTANCE.id())) {
			return JvmCliTarget.INSTANCE;
		}
		if (resolved.id().equals(DalvikTarget.INSTANCE.id())) {
			return DalvikCliTarget.INSTANCE;
		}
		return null;
	}
}

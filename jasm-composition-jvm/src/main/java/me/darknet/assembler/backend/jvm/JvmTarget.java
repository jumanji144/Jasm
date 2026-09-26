package me.darknet.assembler.backend.jvm;

import me.darknet.assembler.target.AssemblyTarget;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.target.TargetId;
import org.jetbrains.annotations.NotNull;

/**
 * Bundled JVM assembly target.
 */
public final class JvmTarget implements AssemblyTarget {
	public static final JvmTarget INSTANCE = new JvmTarget();
	private static final TargetId ID = new TargetId("JVM");

	private JvmTarget() {}

	@Override
	public @NotNull TargetId id() {
		return ID;
	}

	@Override
	public @NotNull String displayName() {
		return "JVM";
	}

	@Override
	public @NotNull TargetContext context() {
		return JvmTargetContext.INSTANCE;
	}
}

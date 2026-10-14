package me.darknet.assembler.backend.dalvik;

import me.darknet.assembler.target.AssemblyTarget;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.target.TargetId;
import org.jetbrains.annotations.NotNull;

/**
 * Bundled Dalvik assembly target.
 */
public final class DalvikTarget implements AssemblyTarget {
    public static final DalvikTarget INSTANCE = new DalvikTarget();
    private static final TargetId ID = new TargetId("DALVIK");

    private DalvikTarget() {}

    @Override
    public @NotNull TargetId id() {
        return ID;
    }

    @Override
    public @NotNull String displayName() {
        return "Dalvik";
    }

    @Override
    public @NotNull TargetContext context() {
        return DalvikTargetContext.INSTANCE;
    }
}

package me.darknet.assembler.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Annotation storage kind, where storage is just for the internal class model,
 * IE visibility and if it's a type-anno or not.
 */
public enum AnnotationKind {
    VIS_ANNO("vis-a"),
    INVIS_ANNO("invis-a"),
    // TODO: Type annotation support in BLW
    //  VIS_TYPE_ANNO("vis-t"),
    //  INVIS_TYPE_ANNO("invis-t")
    ;

    private final String display;

    AnnotationKind(String display) {
        this.display = display;
    }

    /**
     * @param name
     *         Name of annotation kind.
     *
     * @return Kind matching the display or enum name.
     * Falls back to {@link #VIS_ANNO} if unrecognized.
     */
    public static @NotNull AnnotationKind from(@Nullable String name) {
        for (AnnotationKind value : values())
            if (value.display.equalsIgnoreCase(name) || value.name().equalsIgnoreCase(name))
                return value;
        return VIS_ANNO;
    }
}

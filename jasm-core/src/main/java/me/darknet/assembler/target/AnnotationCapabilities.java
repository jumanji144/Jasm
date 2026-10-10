package me.darknet.assembler.target;

import org.jetbrains.annotations.NotNull;

/**
 * Target capability queries for optional annotation output forms.
 */
public interface AnnotationCapabilities {
    /**
     * @param capability
     * 		Annotation capability to query.
     *
     * @return {@code true} when this target can emit the requested annotation form.
     */
    boolean supports(@NotNull AnnotationCapability capability);
}
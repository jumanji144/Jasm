package me.darknet.assembler.target;

/**
 * Optional annotation output capabilities a target may or may not support.
 * <p>
 * Each constant corresponds to one annotation form whose absence is enforced before emission, so
 * an unsupported form is a source-local diagnostic rather than a backend failure. Constants are
 * added only when a target actually differs and a gate consumes them.
 */
public enum AnnotationCapability {
    /**
     * Annotations carried with system visibility.
     */
    SYSTEM_VISIBILITY("system visibility"),
    /**
     * Annotations carrying a type reference and optional type path.
     */
    TYPE_ANNOTATIONS("type annotations"),
    /**
     * Annotations attached to individual method parameters.
     */
    PARAMETER_ANNOTATIONS("parameter annotations"),
    /**
     * Annotation method default values.
     */
    ANNOTATION_DEFAULT_VALUES("annotation default values");

    private final String displayName;

    AnnotationCapability(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return Human-readable capability name used in diagnostics.
     */
    public String displayName() {
        return displayName;
    }
}
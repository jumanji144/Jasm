package me.darknet.assembler.parser.processor;

import java.util.Set;

/**
 * Keyword names owned by the AST processor.
 */
public final class ProcessorKeywords {
    /** Method parameter list key. */
    public static final String PARAMETERS = "parameters";
    /** Method parameter annotation map key. */
    public static final String PARAMETER_ANNOTATIONS = "parameter-annotations";
    /** Method declared exceptions key. */
    public static final String THROWS = "throws";
    /** Method annotation default value key. */
    public static final String DEFAULT_VALUE = "default-value";
    /** Method exception table key. */
    public static final String EXCEPTIONS = "exceptions";
    /** Method bytecode body key. */
    public static final String CODE = "code";
    /** Dalvik method register count key. */
    public static final String REGISTERS = "registers";

    private static final Set<String> COMMON_METHOD_BODY_KEYWORDS = Set.of(
            PARAMETERS, PARAMETER_ANNOTATIONS, THROWS, DEFAULT_VALUE, EXCEPTIONS, CODE
    );
    private static final Set<String> DALVIK_ONLY_KEYWORDS = Set.of(REGISTERS);

    private ProcessorKeywords() {}

    /**
     * @return Immutable set of method-body keywords shared by the supported formats.
     */
    public static Set<String> getCommonMethodBodyKeywords() {
        return COMMON_METHOD_BODY_KEYWORDS;
    }

    /**
     * @return Immutable set of keywords used only by the Dalvik format.
     */
    public static Set<String> getDalvikOnlyKeywords() {
        return DALVIK_ONLY_KEYWORDS;
    }
}

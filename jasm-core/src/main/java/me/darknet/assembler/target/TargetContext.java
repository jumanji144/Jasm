package me.darknet.assembler.target;

import me.darknet.assembler.instructions.Instructions;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Target-provided services used while processing JASM source.
 */
public interface TargetContext {
    /**
     * @return Instruction registry used to resolve source mnemonics for this target.
     */
    @NotNull Instructions<?> instructions();

    /**
     * @return Target-owned method-body attribute parsers.
     */
    @NotNull MethodAttributeRegistry methodAttributes();

    /**
     * @return Annotation visibility and placement capabilities of this target.
     */
    @NotNull AnnotationCapabilities annotationCapabilities();

    /**
     * @return Immutable target-specific keyword names used by keyword generation.
     */
    default @NotNull Set<String> keywordNames() {
        Set<String> keywords = new HashSet<>(instructions().getSourceKeywords());
        keywords.addAll(methodAttributes().getKeywords());
        return Set.copyOf(keywords);
    }
}
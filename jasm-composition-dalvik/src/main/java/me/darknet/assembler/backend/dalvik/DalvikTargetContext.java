package me.darknet.assembler.backend.dalvik;

import me.darknet.assembler.backend.dalvik.instructions.DalvikInstructions;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.target.AnnotationCapabilities;
import me.darknet.assembler.target.AnnotationCapability;
import me.darknet.assembler.target.MethodAttributeRegistry;
import me.darknet.assembler.target.TargetContext;
import org.jetbrains.annotations.NotNull;

/**
 * Processing services owned by the Dalvik backend.
 */
public final class DalvikTargetContext implements TargetContext {
    public static final DalvikTargetContext INSTANCE = new DalvikTargetContext();

    private static final AnnotationCapabilities ANNOTATION_CAPABILITIES =
            capability -> capability != AnnotationCapability.TYPE_ANNOTATIONS;
    private static final MethodAttributeRegistry METHOD_ATTRIBUTES = MethodAttributeRegistry.empty();

    private DalvikTargetContext() {}

    @Override
    public @NotNull Instructions<?> instructions() {
        return DalvikInstructions.INSTANCE;
    }

    @Override
    public @NotNull MethodAttributeRegistry methodAttributes() {
        return METHOD_ATTRIBUTES;
    }

    @Override
    public @NotNull AnnotationCapabilities annotationCapabilities() {
        return ANNOTATION_CAPABILITIES;
    }
}

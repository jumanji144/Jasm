package me.darknet.assembler.backend.jvm;

import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.instructions.jvm.JvmInstructions;
import me.darknet.assembler.target.AnnotationCapabilities;
import me.darknet.assembler.target.AnnotationCapability;
import me.darknet.assembler.target.MethodAttributeRegistry;
import me.darknet.assembler.target.TargetContext;
import org.jetbrains.annotations.NotNull;

/**
 * Target services used when processing JVM assembly sources.
 */
public final class JvmTargetContext implements TargetContext {
	public static final JvmTargetContext INSTANCE = new JvmTargetContext();

	private static final MethodAttributeRegistry METHOD_ATTRIBUTES = MethodAttributeRegistry.empty();
	private static final AnnotationCapabilities ANNOTATION_CAPABILITIES = capability ->
			capability == AnnotationCapability.TYPE_ANNOTATIONS
					|| capability == AnnotationCapability.PARAMETER_ANNOTATIONS
					|| capability == AnnotationCapability.ANNOTATION_DEFAULT_VALUES;

	private JvmTargetContext() {}

	@Override
	public @NotNull Instructions<?> instructions() {
		return JvmInstructions.INSTANCE;
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

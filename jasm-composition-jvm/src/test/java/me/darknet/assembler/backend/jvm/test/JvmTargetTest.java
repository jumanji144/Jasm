package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.backend.jvm.JvmTarget;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.target.AnnotationCapabilities;
import me.darknet.assembler.target.AnnotationCapability;
import me.darknet.assembler.target.TargetId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmTargetTest {
	@Test
	void exposesStableSingletonIdentityAndContext() {
		JvmTarget target = JvmTarget.INSTANCE;

		assertSame(JvmTarget.INSTANCE, target);
		assertEquals(new TargetId("JVM"), target.id());
		assertEquals("JVM", target.displayName());
		assertSame(JvmTargetContext.INSTANCE, target.context());
	}

	@Test
	void exposesJvmAnnotationCapabilities() {
		AnnotationCapabilities capabilities = JvmTarget.INSTANCE.context().annotationCapabilities();

		assertFalse(capabilities.supports(AnnotationCapability.SYSTEM_VISIBILITY));
		assertTrue(capabilities.supports(AnnotationCapability.TYPE_ANNOTATIONS));
		assertTrue(capabilities.supports(AnnotationCapability.PARAMETER_ANNOTATIONS));
		assertTrue(capabilities.supports(AnnotationCapability.ANNOTATION_DEFAULT_VALUES));
	}
}

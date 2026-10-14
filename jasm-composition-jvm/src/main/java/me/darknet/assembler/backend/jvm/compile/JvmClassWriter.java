package me.darknet.assembler.backend.jvm.compile;

import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.TypeAwareness;
import me.darknet.assembler.error.DiagnosticSink;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Class writer that resolves common superclasses using a configured inheritance checker and type awareness.
 */
class JvmClassWriter extends ClassWriter {
	private final CommonSuperclassResolver resolver;

	JvmClassWriter(int flags, DiagnosticSink sink, TypeAwareness typeAwareness, InheritanceChecker checker) {
		this(null, flags, sink, typeAwareness, checker);
	}

	JvmClassWriter(ClassReader reader, int flags, DiagnosticSink sink, TypeAwareness typeAwareness,
	               InheritanceChecker checker) {
		super(reader, flags);
		this.resolver = new CommonSuperclassResolver(checker, typeAwareness, sink);
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		return resolver.resolve(type1, type2);
	}
}

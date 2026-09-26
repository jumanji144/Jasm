package me.darknet.assembler.backend.jvm.compile;

import me.darknet.assembler.compiler.InheritanceChecker;

import me.darknet.assembler.compiler.TypeAwareness;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.util.Location;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class JvmClassWriter extends ClassWriter {
    private final InheritanceChecker checker;
    private final TypeAwareness typeAwareness;
    private final DiagnosticSink sink;

    public JvmClassWriter(int flags, DiagnosticSink sink, TypeAwareness typeAwareness, InheritanceChecker checker) {
        this(null, flags, sink, typeAwareness, checker);
    }

    public JvmClassWriter(ClassReader reader, int flags, DiagnosticSink sink, TypeAwareness typeAwareness, InheritanceChecker checker) {
        super(reader, flags);
        if (checker == null)
            throw new IllegalArgumentException("Class writer requires an inheritance checker implementation");
        this.checker = checker;
        this.typeAwareness = typeAwareness;
        this.sink = sink;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        boolean unknownType = false;
        if (isUnknownType(type1)) {
            checkAware(type1);
            unknownType = true;
        }
        if (isUnknownType(type2)) {
            checkAware(type2);
            unknownType = true;
        }

        if (unknownType)
            return "java/lang/Object";

        return checker.getCommonSuperclass(type1, type2);
    }

    private boolean isUnknownType(String type) {
        return type != null && typeAwareness != null && !typeAwareness.isAwareOf(type);
    }

    protected void checkAware(String type) {
        if (isUnknownType(type) && sink != null)
            sink.warning(DiagnosticPhase.OUTPUT_VERIFICATION, DiagnosticCode.VERIFICATION_WARNING,
                    typeAwareness.notifyUnknownType(type), Location.UNKNOWN);
    }
}

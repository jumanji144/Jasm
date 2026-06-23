package me.darknet.assembler.compile;

import me.darknet.assembler.compiler.InheritanceChecker;

import me.darknet.assembler.compiler.TypeAwareness;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.Location;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class JvmClassWriter extends ClassWriter {

    private final InheritanceChecker checker;
    private final TypeAwareness typeAwareness;
    private final ErrorCollector errorCollector;

    public JvmClassWriter(int flags, ErrorCollector errorCollector,TypeAwareness typeAwareness, InheritanceChecker checker) {
        this(null, flags, errorCollector, typeAwareness, checker);
    }

    public JvmClassWriter(ClassReader reader, int flags, ErrorCollector errorCollector, TypeAwareness typeAwareness, InheritanceChecker checker) {
        super(reader, flags);
        if (checker == null)
            throw new IllegalArgumentException("Class writer requires an inheritance checker implementation");
        this.checker = checker;
        this.typeAwareness = typeAwareness;
        this.errorCollector =errorCollector;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        checkAware(type1);
        checkAware(type2);

        return checker.getCommonSuperclass(type1, type2);
    }

    protected void checkAware(String type) {
        if (type != null && typeAwareness != null && !typeAwareness.isAwareOf(type))
            errorCollector.addWarn(typeAwareness.notifyUnknownType(type), Location.UNKNOWN);
    }
}

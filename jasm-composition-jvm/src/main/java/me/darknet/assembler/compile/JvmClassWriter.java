package me.darknet.assembler.compile;

import me.darknet.assembler.compiler.InheritanceChecker;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class JvmClassWriter extends ClassWriter {

    private final InheritanceChecker checker;

    public JvmClassWriter(int flags, InheritanceChecker checker) {
        this(null, flags, checker);
    }

    public JvmClassWriter(ClassReader reader, int flags, InheritanceChecker checker) {
        super(reader, flags);
        if (checker == null)
            throw new IllegalArgumentException("Class writer requires an inheritance checker implementation");
        this.checker = checker;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        return checker.getCommonSuperclass(type1, type2);
    }
}

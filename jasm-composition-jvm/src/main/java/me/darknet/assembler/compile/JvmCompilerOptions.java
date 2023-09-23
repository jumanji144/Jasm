package me.darknet.assembler.compile;

import dev.xdark.blw.version.JavaVersion;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.ReflectiveInheritanceChecker;
import org.objectweb.asm.ClassWriter;

public class JvmCompilerOptions implements CompilerOptions<JvmCompilerOptions> {

    int asmArgs = 0;
    JavaVersion version;
    JavaClassRepresentation overlay;
    String annotationPath;
    InheritanceChecker inheritanceChecker = ReflectiveInheritanceChecker.INSTANCE;

    public JvmCompilerOptions() {
        this.asmArgs = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        this.version = JavaVersion.V8;
    }

    public JvmCompilerOptions computeFrames(boolean computeFrames) {
        if (computeFrames) {
            this.asmArgs |= ClassWriter.COMPUTE_FRAMES;
        } else {
            this.asmArgs &= ~ClassWriter.COMPUTE_FRAMES;
        }
        return this;
    }

    public JvmCompilerOptions computeMaxs(boolean computeMaxs) {
        if (computeMaxs) {
            this.asmArgs |= ClassWriter.COMPUTE_MAXS;
        } else {
            this.asmArgs &= ~ClassWriter.COMPUTE_MAXS;
        }
        return this;
    }

    public JvmCompilerOptions version(int version) {
        this.version = JavaVersion.jdkVersion(version);
        return this;
    }

    @Override
    public int version() {
        return this.version.majorVersion();
    }

    @Override
    public JvmCompilerOptions annotationPath(String path) {
        this.annotationPath = path;
        return this;
    }

    @Override
    public String annotationPath() {
        return this.annotationPath;
    }

    @Override
    public JvmCompilerOptions overlay(ClassRepresentation representation) {
        if(!(representation instanceof JavaClassRepresentation))
            throw new IllegalArgumentException("ClassRepresentation must be a JavaClassRepresentation");
        this.overlay = (JavaClassRepresentation) representation;
        return this;
    }

    @Override
    public ClassRepresentation overlay() {
        return this.overlay;
    }

    @Override
    public InheritanceChecker inheritanceChecker() {
        return this.inheritanceChecker;
    }

    @Override
    public JvmCompilerOptions inheritanceChecker(InheritanceChecker checker) {
        this.inheritanceChecker = checker;
        return this;
    }
}
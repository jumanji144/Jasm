package me.darknet.assembler.compile;

import dev.xdark.blw.version.JavaVersion;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.objectweb.asm.ClassWriter;

public class BlwCompilerOptions implements CompilerOptions<BlwCompilerOptions> {

    int asmArgs = 0;
    JavaVersion version;
    JavaClassRepresentation overlay;
    String annotationPath;
    InheritanceChecker inheritanceChecker;

    public BlwCompilerOptions() {
        this.asmArgs = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        this.version = JavaVersion.V8;
    }

    public BlwCompilerOptions computeFrames(boolean computeFrames) {
        if (computeFrames) {
            this.asmArgs |= ClassWriter.COMPUTE_FRAMES;
        } else {
            this.asmArgs &= ~ClassWriter.COMPUTE_FRAMES;
        }
        return this;
    }

    public BlwCompilerOptions computeMaxs(boolean computeMaxs) {
        if (computeMaxs) {
            this.asmArgs |= ClassWriter.COMPUTE_MAXS;
        } else {
            this.asmArgs &= ~ClassWriter.COMPUTE_MAXS;
        }
        return this;
    }

    public BlwCompilerOptions version(int version) {
        this.version = JavaVersion.jdkVersion(version);
        return this;
    }

    @Override
    public int version() {
        return this.version.majorVersion();
    }

    @Override
    public BlwCompilerOptions annotationPath(String path) {
        this.annotationPath = path;
        return this;
    }

    @Override
    public String annotationPath() {
        return this.annotationPath;
    }

    @Override
    public BlwCompilerOptions overlay(ClassRepresentation representation) {
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
    public BlwCompilerOptions inheritanceChecker(InheritanceChecker checker) {
        this.inheritanceChecker = checker;
        return this;
    }
}

package me.darknet.assembler.compile;

import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;

public class DalvikCompilerOptions implements CompilerOptions<DalvikCompilerOptions> {

    private int dalvikVersion;
    private InheritanceChecker inheritanceChecker;
    private ClassRepresentation overlay;

    @Override
    public DalvikCompilerOptions overlay(ClassRepresentation representation) {
        this.overlay = representation;
        return this;
    }

    @Override
    public ClassRepresentation overlay() {
        return overlay;
    }

    @Override
    public DalvikCompilerOptions version(int version) {
        this.dalvikVersion = version;
        return this;
    }

    @Override
    public int version() {
        return dalvikVersion;
    }

    @Override
    public DalvikCompilerOptions annotationPath(String path) {
        return null;
    }

    @Override
    public String annotationPath() {
        return "";
    }

    @Override
    public DalvikCompilerOptions inheritanceChecker(InheritanceChecker checker) {
        this.inheritanceChecker = checker;
        return this;
    }

    @Override
    public InheritanceChecker inheritanceChecker() {
        if (inheritanceChecker == null) {
            throw new IllegalStateException("Inheritance checker is not set");
        }
        return inheritanceChecker;
    }
}

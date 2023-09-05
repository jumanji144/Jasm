package me.darknet.assembler.compiler;

public interface CompilerOptions<B extends CompilerOptions<?>> {

    /**
     * Overlay a class file with the given bytes
     *
     * @param bytes
     *              The bytes to overlay
     */
    B overlay(byte[] bytes);

    byte[] overlay();

    B version(int version);

    int version();

    B annotationPath(String path);

    String annotationPath();
}

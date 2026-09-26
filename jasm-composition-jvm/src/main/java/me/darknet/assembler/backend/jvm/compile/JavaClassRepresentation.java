package me.darknet.assembler.backend.jvm.compile;

/**
 * Target-local JVM class-file representation.
 *
 * @param classFile
 * 		Raw class file.
 */
public record JavaClassRepresentation(byte[] classFile) {}

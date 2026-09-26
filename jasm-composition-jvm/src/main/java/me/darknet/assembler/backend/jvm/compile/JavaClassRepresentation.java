package me.darknet.assembler.backend.jvm.compile;

/**
 * Emitted JVM class file.
 *
 * @param classFile
 * 		Raw class file bytes.
 */
public record JavaClassRepresentation(byte[] classFile) {}

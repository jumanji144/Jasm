package me.darknet.assembler.compile;

/**
 * Target-local JVM class-file representation.
 *
 * @param classFile
 * 		Raw class file.
 */
public record JavaClassRepresentation(byte[] classFile) {}

package me.darknet.assembler.compile;

import me.darknet.assembler.compiler.ClassRepresentation;

/**
 * @param classFile
 *                  Raw class file.
 */
public record JavaClassRepresentation(byte[] classFile) implements ClassRepresentation {
}

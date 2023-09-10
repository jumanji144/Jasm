package me.darknet.assembler.compile;

import me.darknet.assembler.compiler.ClassRepresentation;

public record JavaClassRepresentation(byte[] data) implements ClassRepresentation {}

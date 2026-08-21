package me.darknet.assembler.compile;

import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.ClassResult;

public record DalvikClassResult(ClassRepresentation representation) implements ClassResult {
}

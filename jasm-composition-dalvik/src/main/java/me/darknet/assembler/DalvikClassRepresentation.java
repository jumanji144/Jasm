package me.darknet.assembler;

import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.dex.tree.definitions.ClassDefinition;

public record DalvikClassRepresentation(ClassDefinition definition) implements ClassRepresentation {
}

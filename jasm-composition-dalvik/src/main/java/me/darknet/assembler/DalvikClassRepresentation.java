package me.darknet.assembler;

import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Dalvik class representation.
 *
 * @param definition
 * 		Target class definition.
 *
 * @see DexFile#definitions()
 */
public record DalvikClassRepresentation(@NotNull ClassDefinition definition) {}

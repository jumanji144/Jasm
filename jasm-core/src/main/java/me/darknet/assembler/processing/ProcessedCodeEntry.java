package me.darknet.assembler.processing;

import me.darknet.assembler.instructions.SemanticInstruction;

/**
 * One source-preserving entry in a processed method body.
 */
public sealed interface ProcessedCodeEntry extends SemanticInstruction permits ProcessedLabel, ProcessedInstruction {}

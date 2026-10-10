package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.InstructionLowering;

/**
 * Dalvik lowering identity.
 *
 * @param opcode
 * 		Dex opcode constant from {@link me.darknet.dex.file.instructions.Opcodes}.
 */
public record DalvikLowering(int opcode) implements InstructionLowering {}

package me.darknet.assembler.backend.jvm.instructions;

import me.darknet.assembler.instructions.InstructionLowering;

/**
 * JVM lowering identity.
 *
 * @param opcode
 * 		Opcode constant from {@link org.objectweb.asm.Opcodes}.
 */
public record JvmLowering(int opcode) implements InstructionLowering {}

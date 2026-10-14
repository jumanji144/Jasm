package me.darknet.assembler.test;

import me.darknet.assembler.instructions.InstructionLowering;

/**
 * Fixture lowering identity standing in for a real target's encoding.
 *
 * @param canonicalName canonical mnemonic the fixture instruction encodes as.
 */
public record FixtureLowering(String canonicalName) implements InstructionLowering {
}

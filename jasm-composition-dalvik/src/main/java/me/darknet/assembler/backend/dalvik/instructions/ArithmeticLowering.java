package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.InstructionLowering;

/**
 * Dex encodings of one canonical arithmetic operation.
 * <p>
 * The source mnemonic names only the operation. Each component is an encoding the operation can take, and the emitter
 * chooses among them once the physical registers and any literal are known. {@link #NONE} marks an encoding the
 * operation does not have.
 *
 * @param register
 * 		Three-register form, such as {@code add-int dest a b}, or {@link #NONE} when the second source is only a
 * 		literal, as for {@code rsub-int}.
 * @param twoAddress
 * 		Two-address form, which writes its first source, or {@link #NONE}. It applies only when the destination is
 * 		that same register.
 * @param literal8
 * 		Form taking a signed eight-bit literal, or {@link #NONE}.
 * @param literal16
 * 		Form taking a signed sixteen-bit literal, or {@link #NONE}.
 */
public record ArithmeticLowering(int register, int twoAddress, int literal8, int literal16) implements InstructionLowering {
    /** Marks an encoding the operation does not have. */
    public static final int NONE = -1;
}

package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.OperandValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One Dalvik register operand.
 *
 * @param name
 * 		Source register name, either a numeric {@code vN} form or a declared name.
 * @param index
 * 		Register index for a numeric {@code vN} form; {@code null} for a named register, whose slot is
 * 		only known once the method's register allocation runs at emission.
 * @param width
 * 		Width of the value the register holds. A wide value occupies the slot after its own, so the
 * 		allocator has to reserve one more word than the index suggests.
 */
public record RegisterRef(@NotNull String name, @Nullable Integer index, @NotNull OperandRole.WidthPolicy width) implements OperandValue {}

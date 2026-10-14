package me.darknet.assembler.query.resolution;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution of an instruction.
 *
 * @param parentClass
 * 		The class containing the instruction, or {@code null} if resolving at the top-level is a method.
 * @param method
 * 		The method containing the instruction.
 * @param instruction
 * 		The instruction being resolved.
 */
public record InstructionResolution(@Nullable ASTClass parentClass,
                                    @NotNull ASTMethod method,
                                    @NotNull ASTInstruction instruction) implements Resolution {
	@Override
	public @NotNull ASTElement element() {
		return instruction;
	}
}

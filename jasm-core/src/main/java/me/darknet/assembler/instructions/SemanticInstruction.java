package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Translation-time view of one source instruction: the source node plus one validated operand per
 * declared operand, index-aligned.
 * <p>
 * Also exposes the source node's own argument accessors, so a translator for an operand whose
 * schema declares no typed value reads the same node it always did.
 */
public interface SemanticInstruction {
    /**
     * @return Source instruction node, retaining range and ownership information.
     */
    @NotNull
    ASTInstruction source();

    /**
     * @return Location of the source instruction, or {@code null} when the node carries none.
     */
    default @Nullable Location location() {
        return source().location();
    }

    /**
     * @return One entry per operand, index-aligned: {@code operands().get(i).index() == i}.
     */
    @NotNull
    List<ValidatedOperand> operands();

    /**
     * @param index
     * 		Operand position.
     * @return Typed value at {@code index}, or {@code null} when absent or the schema has no resolver.
     */
    @Nullable
    OperandValue operand(int index);

    /**
     * @param index
     * 		Operand position.
     * @return Semantic role, or {@code null} when out of range or undeclared.
     */
    default @Nullable OperandRole role(int index) {
        return null;
    }

    /**
     * @param index
     * 		Operand position.
     * @param type
     * 		Expected value type.
     * @param <T>
     * 		Expected value type.
     * @return Typed value at {@code index}, cast to {@code type}.
     * @throws IllegalStateException
     * 		if the operand does not carry a value of {@code type}.
     */
    <T extends OperandValue> @NotNull T operand(int index, @NotNull Class<T> type);

    /**
     * @return Lowering identity declared by the matched definition, or {@code null} for source-only forms.
     */
    default @Nullable InstructionLowering lowering() {
        return null;
    }

    /**
     * @param type
     * 		Expected lowering type.
     * @param <T>
     * 		Expected lowering type.
     * @return Lowering identity cast to {@code type}.
     * @throws IllegalStateException
     * 		if the definition declares no lowering of {@code type}.
     */
    default <T extends InstructionLowering> @NotNull T lowering(@NotNull Class<T> type) {
        InstructionLowering value = lowering();
        if (!type.isInstance(value))
            throw new IllegalStateException("Instruction does not carry a " + type.getSimpleName());
        return type.cast(value);
    }

    /**
     * @return Source argument at {@code index} adapted to {@code type}.
     */
    default <T extends ASTElement> T argument(int index, Class<T> type) {
        return source().argument(index, type);
    }

    /**
     * @return Source argument at {@code index} as an identifier.
     */
    default ASTIdentifier argument(int index) {
        return source().argument(index);
    }

    /**
     * @return Source argument at {@code index} as an array.
     */
    default ASTArray argumentArray(int index) {
        return source().argumentArray(index);
    }

    /**
     * @return Source argument at {@code index} as an object.
     */
    default ASTObject argumentObject(int index) {
        return source().argumentObject(index);
    }
}

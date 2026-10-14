package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.util.CollectionUtil;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * Code block node containing an ordered sequence of {@link ASTInstruction} nodes.
 */
public class ASTCode extends ASTElement implements Iterable<@NotNull ASTInstruction> {

    private final List<@NotNull ASTInstruction> instructions;

    public ASTCode(List<@NotNull ASTInstruction> instructions) {
        super(ElementType.CODE, instructions);
        this.instructions = CollectionUtil.immutableCopy(instructions);
    }

    public List<@NotNull ASTInstruction> getInstructions() {
        return instructions;
    }

    @Override
    public @NotNull Iterator<@NotNull ASTInstruction> iterator() {
        return getInstructions().iterator();
    }
}

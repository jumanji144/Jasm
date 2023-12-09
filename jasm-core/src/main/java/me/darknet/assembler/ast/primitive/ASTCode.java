package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ASTCode extends ASTElement {

    private final List<@NotNull ASTInstruction> instructions;

    public ASTCode(List<@NotNull ASTInstruction> instructions) {
        super(ElementType.CODE, instructions);
        this.instructions = instructions;
    }

    public List<@NotNull ASTInstruction> instructions() {
        return instructions;
    }

}

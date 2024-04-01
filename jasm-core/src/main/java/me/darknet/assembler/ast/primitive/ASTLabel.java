package me.darknet.assembler.ast.primitive;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class ASTLabel extends ASTInstruction {

    public ASTLabel(ASTIdentifier identifier) {
        super(identifier, Collections.emptyList());
    }

    @Override
    public @NotNull String content() {
        return identifier().content() + ":";
    }
}

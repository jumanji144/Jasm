package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.util.Range;
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

    @Override
    protected @NotNull Range createRange(int start, int end) {
        // Add + 1 to the end to include the ':'
        return super.createRange(start, end + 1);
    }
}

package me.darknet.assembler.ast.specific;

import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.NotNull;

public interface ASTAccessed {
    @NotNull
    Modifiers getModifiers();
}

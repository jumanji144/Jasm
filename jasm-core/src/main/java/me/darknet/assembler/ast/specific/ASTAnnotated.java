package me.darknet.assembler.ast.specific;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ASTAnnotated {
    @NotNull
    List<ASTAnnotation> getAnnotations();

    void setAnnotations(@Nullable List<ASTAnnotation> annotations);

    void addAnnotation(@NotNull ASTAnnotation annotation);
}

package me.darknet.assembler.ast.specific;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ASTAnnotated {
    @NotNull
    List<ASTAnnotation> getVisibleAnnotations();
    @NotNull
    List<ASTAnnotation> getInvisibleAnnotations();

    void setVisibleAnnotations(@Nullable List<ASTAnnotation> annotations);
    void setInvisibleAnnotations(@Nullable List<ASTAnnotation> annotations);

    void addVisibleAnnotation(@NotNull ASTAnnotation annotation);
    void addInvisibleAnnotation(@NotNull ASTAnnotation annotation);
}

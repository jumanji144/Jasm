package me.darknet.assembler.ast.specific;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ASTAnnotated {
    @NotNull
    List<ASTAnnotation> getVisibleAnnotations();
    @NotNull
    List<ASTAnnotation> getInvisibleAnnotations();

    @NotNull
    List<ASTAnnotation> getVisibleTypeAnnotations();
    @NotNull
    List<ASTAnnotation> getInvisibleTypeAnnotations();

    void setVisibleAnnotations(@Nullable List<ASTAnnotation> annotations);
    void setInvisibleAnnotations(@Nullable List<ASTAnnotation> annotations);

    void addVisibleAnnotation(@NotNull ASTAnnotation annotation);
    void addInvisibleAnnotation(@NotNull ASTAnnotation annotation);

	void setVisibleTypeAnnotations(@NotNull List<ASTAnnotation> annotations);
    void setInvisibleTypeAnnotations(@NotNull List<ASTAnnotation> annotations);

    void addVisibleTypeAnnotation(@NotNull ASTAnnotation annotation);
    void addInvisibleTypeAnnotation(@NotNull ASTAnnotation annotation);
}

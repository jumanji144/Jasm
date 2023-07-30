package me.darknet.assembler.printer;

import org.jetbrains.annotations.Nullable;

public interface AnnotationHolder {

    @Nullable
    AnnotationPrinter annotation(int index);

}

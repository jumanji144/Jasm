package me.darknet.assembler.printer;

import org.jetbrains.annotations.Nullable;

/**
 * Provides access to annotation printers by visibility and index.
 */
@SuppressWarnings("unused")
public interface AnnotationHolder {
    /**
     * @param index
     *         Index into <i>all</i> annotations,
     *         where the list is formed by {@code RuntimeVisibleAnnotations + RuntimeInvisibleAnnotations}.
     *
     * @return Printer for annotation. {@code null} if the index does not point to a known annotation.
     */
    @Nullable
    AnnotationPrinter annotation(int index);

    /**
     * @param index
     *         Index into the {@code RuntimeVisibleAnnotations} attribute.
     *
     * @return Printer for annotation. {@code null} if the index does not point to a known annotation.
     */
    @Nullable
    AnnotationPrinter visibleAnnotation(int index);

    /**
     * @param index
     *         Index into the {@code RuntimeInvisibleAnnotations} attribute.
     *
     * @return Printer for annotation. {@code null} if the index does not point to a known annotation.
     */
    @Nullable
    AnnotationPrinter invisibleAnnotation(int index);
}

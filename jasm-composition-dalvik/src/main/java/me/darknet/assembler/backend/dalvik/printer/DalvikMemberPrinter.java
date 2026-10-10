package me.darknet.assembler.backend.dalvik.printer;

import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.printer.AnnotationPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.tree.definitions.Accessible;
import me.darknet.dex.tree.definitions.Annotated;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for printing member definitions in Dalvik assembly format.
 *
 * @param annotated
 * 		Annotation access for the member, can be {@code null} if no annotations are present.
 * @param accessible
 * 		Access flags for the member, can be {@code null} if no access flags are present.
 * @param type
 * 		The type of member being printed <i>(class, field, or method)</i>.
 */
public record DalvikMemberPrinter(@Nullable Annotated annotated, @Nullable Accessible accessible, @NotNull Type type) {
	/**
	 * Prints the annotations of the member to the given print context.
	 *
	 * @param ctx
	 * 		The print context to use for printing.
	 */
	public void printAttributes(@NotNull PrintContext<?> ctx) {
		if (annotated != null) {
			for (Annotation annotation : annotated.getAnnotations()) {
				new DalvikAnnotationPrinter(annotation).print(ctx);
			}
		}
	}

	/**
	 * @param ctx
	 * 		The print context to use for printing.
	 *
	 * @return The updated print context after printing the declaration.
	 */
	public @NotNull PrintContext<?> printDeclaration(@NotNull PrintContext<?> ctx) {
		if (accessible != null) {
			String elementName = switch (type) {
				case CLASS -> ".class";
				case FIELD -> ".field";
				case METHOD -> ".method";
			};
			int modifierType = switch (type) {
				case CLASS -> DalvikModifiers.CLASS;
				case FIELD -> DalvikModifiers.FIELD;
				case METHOD -> DalvikModifiers.METHOD;
			};
			return ctx.begin().element(elementName)
					.print(DalvikModifiers.modifiers(accessible.getAccess(), modifierType));
		}
		return ctx;
	}

	/**
	 * @param index
	 * 		The index of the annotation to print.
	 *
	 * @return Printer for the annotation at the specified index, or {@code null} if not available.
	 */
	public @Nullable AnnotationPrinter printAnnotation(int index) {
		if (annotated != null)
			return new DalvikAnnotationPrinter(annotated.getAnnotations().get(index));
		return null;
	}

	/**
	 * Type of member being printed.
	 */
	public enum Type {
		CLASS,
		FIELD,
		METHOD
	}
}

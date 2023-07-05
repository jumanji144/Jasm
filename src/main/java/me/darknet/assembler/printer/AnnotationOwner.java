package me.darknet.assembler.printer;

public abstract class AnnotationOwner {

	/**
	 * Prints an annotation at the given index.
	 * @param index annotation index corresponding to the annotation on the member, the index represents a index
	 *              into the attributes table of the member, the table should be ordered as follows:
	 *              <ul>
	 *              <li>RuntimeVisibleAnnotations</li>
	 *              <li>RuntimeInvisibleAnnotations</li>
	 *              <li>RuntimeVisibleParameterAnnotations</li>
	 *              <li>RuntimeInvisibleParameterAnnotations</li>
	 *              <li>RuntimeVisibleTypeAnnotations</li>
	 *              <li>RuntimeInvisibleTypeAnnotations</li>
	 *              </ul>
	 *              Note: for parameter annotations the indices must be flattened, as in all parameter annotations follow
	 *              each other
	 * @see me.darknet.assembler.printer.impl.asm.ASMAnnotationHolder
	 * @return annotation printer
	 */
	public abstract AnnotationPrinter printAnnotation(int index);

}

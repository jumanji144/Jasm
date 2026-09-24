package me.darknet.assembler.ast;

/**
 * Source-syntax visibility metadata carried by an annotation.
 *
 * <p>This value describes the annotation's spelling in source and does not
 * express whether a target supports that annotation category.</p>
 */
public enum AnnotationVisibility {
	VISIBLE,
	INVISIBLE,
	SYSTEM
}

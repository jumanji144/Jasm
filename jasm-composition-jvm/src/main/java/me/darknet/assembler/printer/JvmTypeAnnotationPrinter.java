package me.darknet.assembler.printer;

import dev.xdark.blw.annotation.Annotation;
import dev.xdark.blw.annotation.TypeAnnotation;

public class JvmTypeAnnotationPrinter extends JvmAnnotationPrinter {
	private final TypeAnnotation annotation;

	protected JvmTypeAnnotationPrinter(TypeAnnotation annotation, boolean visible) {
		super(annotation, visible);
		this.annotation = annotation;
	}

	protected JvmTypeAnnotationPrinter(TypeAnnotation annotation) {
		super(annotation);
		this.annotation = annotation;
	}

	@Override
	public void print(PrintContext<?> ctx) {
		// For embedded annotations (an annotation inside another) we do not have any concept
		// of 'visible' vs 'invisible' annotations, so we'll shorten the name.
		String token = visible == null ? ".type-annotation" :
				visible ? ".type-visible-annotation" : ".type-invisible-annotation";

		TypeAnnotation annotation = this.annotation;
		ctx.begin().element(token).literal(annotation.type().internalName()).print(" ");

		var wrapper = ctx.object();
		var location = wrapper.literalValue("location").object();
		location.literalValue("ref").literal("0b" + Integer.toBinaryString(annotation.typeRef())).next();
		location.literalValue("path").literal(annotation.typePath() == null ? "_" : annotation.typePath().toString());
		location.end();
		location.next();

		var entries = wrapper.literalValue("values").object();
		entries.print(annotation, this::printEntry);
		entries.end();
		wrapper.end();
	}
}

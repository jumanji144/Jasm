package me.darknet.assembler.printer;

import dev.xdark.blw.annotation.Annotation;
import dev.xdark.blw.classfile.Accessible;
import dev.xdark.blw.classfile.Annotated;
import dev.xdark.blw.classfile.Member;
import dev.xdark.blw.classfile.Signed;
import me.darknet.assembler.printer.util.Modifiers;
import org.jetbrains.annotations.Nullable;

public record MemberPrinter(@Nullable Annotated annotated, @Nullable Signed signed, @Nullable Accessible accessible,
							Type type) {

	public MemberPrinter(Member member, Type type) {
		this(member, member, member, type);
	}

	public void printAttributes(PrintContext<?> ctx) {
		if (annotated != null) {
			for (Annotation invisibleRuntimeAnnotation : annotated.invisibleRuntimeAnnotations()) {
				BlwAnnotationPrinter printer = new BlwAnnotationPrinter(invisibleRuntimeAnnotation);
				printer.print(ctx);
				ctx.next();
			}
			for (Annotation invisibleTypeAnnotation : annotated.visibleRuntimeAnnotations()) {
				BlwAnnotationPrinter printer = new BlwAnnotationPrinter(invisibleTypeAnnotation);
				printer.print(ctx);
				ctx.next();
			}
		}
		if (signed != null && signed.signature() != null) {
			ctx.begin().element(".signature").print(signed.signature()).next();
		}
	}

	public PrintContext<?> printDeclaration(PrintContext<?> ctx) {
		if (accessible != null) {
			return ctx.begin().element(switch (type) {
				case CLASS -> ".class";
				case FIELD -> ".field";
				case METHOD -> ".method";
			}).print(Modifiers.modifiers(accessible.accessFlags(), switch (type) {
				case CLASS -> Modifiers.CLASS;
				case FIELD -> Modifiers.FIELD;
				case METHOD -> Modifiers.METHOD;
			}));
		}
		return ctx;
	}

	public AnnotationPrinter printAnnotation(int index) {
		if (annotated != null) {
			// go through the annotations
			for (int i = 0; i < annotated.visibleRuntimeAnnotations().size(); i++) {
				if (i == index) {
					return new BlwAnnotationPrinter(annotated.visibleRuntimeAnnotations().get(i));
				}
			}
			for (int i = annotated.visibleRuntimeAnnotations().size();
				 i < annotated.invisibleRuntimeAnnotations().size() + annotated.visibleRuntimeAnnotations().size();
				 i++) {
				if (i == index) {
					return new BlwAnnotationPrinter(annotated.invisibleRuntimeAnnotations().get(i));
				}
			}
		}
		return null;
	}

	enum Type {
		CLASS,
		FIELD,
		METHOD
	}
}

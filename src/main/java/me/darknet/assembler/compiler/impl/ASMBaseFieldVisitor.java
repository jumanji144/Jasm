package me.darknet.assembler.compiler.impl;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.groups.AnnotationGroup;
import me.darknet.assembler.parser.groups.AnnotationParamGroup;
import me.darknet.assembler.parser.groups.SignatureGroup;
import me.darknet.assembler.transform.FieldGroupVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;

import java.util.ArrayList;
import java.util.List;

public class ASMBaseFieldVisitor implements FieldGroupVisitor {
	private final List<AnnotationGroup> annotations = new ArrayList<>();
	private final FieldVisitor visitor;

	public ASMBaseFieldVisitor(FieldVisitor visitor) {
		this.visitor = visitor;
	}

	@Override
	public void visitEnd() throws AssemblerException {
		for (AnnotationGroup annotation : annotations) {
			String desc = annotation.getClassGroup().content();
			AnnotationVisitor av = visitor.visitAnnotation(desc, !annotation.isInvisible());
			for (AnnotationParamGroup param : annotation.getParams())
				ASMBaseVisitor.annotationParam(param, av);
			av.visitEnd();
		}
		visitor.visitEnd();
	}


	@Override
	public void visitAnnotation(AnnotationGroup annotation) {
		annotations.add(annotation);
	}

	@Override
	public void visitSignature(SignatureGroup signature) {
		// no-op
	}
}
package me.darknet.assembler.compiler.impl;

import lombok.Getter;
import lombok.Setter;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.transform.ClassGroupVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CachedClass implements ClassGroupVisitor {
	private int version;
	private int access;
	private String type;
	private String superType = "java/lang/Object";
	private List<String> implementedTypes = new ArrayList<>();
	private List<AnnotationGroup> annotations = new ArrayList<>();
	private String signatureType;
	private String sourceFile;

	public void build(ClassVisitor cv) throws AssemblerException {
		cv.visit(version, access, type, signatureType, superType, implementedTypes.toArray(new String[0]));
		for (AnnotationGroup annotation : annotations) {
			String desc = annotation.getClassGroup().content();
			AnnotationVisitor av = cv.visitAnnotation(desc, !annotation.isInvisible());
			for (AnnotationParamGroup param : annotation.getParams())
				ASMBaseVisitor.annotationParam(param, av);
			av.visitEnd();
		}
		cv.visitSource(sourceFile, null);
	}

	@Override
	public void visitExtends(ExtendsGroup group) {
		superType = group.getClassName().content();
	}

	@Override
	public void visitImplements(ImplementsGroup group) {
		implementedTypes.add(group.getClassName().content());
	}

	@Override
	public void visitAnnotation(AnnotationGroup annotation) {
		annotations.add(annotation);
	}

	@Override
	public void visitSignature(SignatureGroup signature) {
		signatureType = signature.getDescriptor().content();
	}

	@Override
	public void visitVersion(VersionGroup version) {
		this.version = version.getVersion();
	}

	@Override
	public void visitSourceFile(SourceFileGroup sourceFile) throws AssemblerException {
		this.sourceFile = sourceFile.getSourceFile();
	}
}

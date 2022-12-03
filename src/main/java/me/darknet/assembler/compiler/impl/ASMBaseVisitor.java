package me.darknet.assembler.compiler.impl;

import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Keyword;
import me.darknet.assembler.parser.Keywords;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.parser.groups.annotation.AnnotationGroup;
import me.darknet.assembler.parser.groups.annotation.AnnotationParamGroup;
import me.darknet.assembler.parser.groups.annotation.EnumGroup;
import me.darknet.assembler.parser.groups.attributes.AccessModGroup;
import me.darknet.assembler.parser.groups.attributes.AccessModsGroup;
import me.darknet.assembler.parser.groups.attributes.SignatureGroup;
import me.darknet.assembler.parser.groups.declaration.ClassDeclarationGroup;
import me.darknet.assembler.parser.groups.declaration.FieldDeclarationGroup;
import me.darknet.assembler.parser.groups.declaration.MethodDeclarationGroup;
import me.darknet.assembler.parser.groups.method.ThrowsGroup;
import me.darknet.assembler.transform.AbstractTopLevelGroupVisitor;
import me.darknet.assembler.transform.ClassGroupVisitor;
import me.darknet.assembler.transform.FieldGroupVisitor;
import me.darknet.assembler.transform.MethodGroupVisitor;
import me.darknet.assembler.util.GroupUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

import static me.darknet.assembler.parser.Group.GroupType;
import static org.objectweb.asm.Opcodes.*;

public class ASMBaseVisitor extends AbstractTopLevelGroupVisitor {
	private final Keywords keywords;
	private final ClassWriter cw;
	private final int version;
	private CachedClass currentClass;

	public ASMBaseVisitor(int version, Keywords keywords) {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		this.version = version;
		this.keywords = keywords;
	}

	public byte[] toByteArray() {
		return cw.toByteArray();
	}

	@Override
	public void visitEnd() throws AssemblerException {
		super.visitEnd();
		if (currentClass != null) {
			currentClass.build(cw);
		}
	}

	@Override
	public ClassGroupVisitor visitImplClass(ClassDeclarationGroup decl) {
		int access = getAccess(decl.getAccessMods());
		String fullyQualifiedClassName = decl.getName().content();
		CachedClass cachedClass = new CachedClass();
		cachedClass.setAccess(access);
		cachedClass.setVersion(version);
		cachedClass.setType(fullyQualifiedClassName);
		if(decl.getExtendsGroup() != null) {
			cachedClass.setSuperType(decl.getExtendsGroup().getClassName().content());
		}
		for (ImplementsGroup implementsGroup : decl.getImplementsGroups()) {
			cachedClass.getImplementedTypes().add(implementsGroup.getClassName().content());
		}
		cachedClass.setAsmBaseVisitor(this);
		currentClass = cachedClass;
		return cachedClass;
	}

	@Override
	public FieldGroupVisitor visitImplField(FieldDeclarationGroup decl) throws AssemblerException {
		SignatureGroup signatureGroup = getAttributeStore().getFirstFieldAttribute(SignatureGroup.class);
		String signature = signatureGroup == null ? null : signatureGroup.getDescriptor().content();
		FieldVisitor fv = cw.visitField(getAccess(decl.getAccessMods()),
				decl.getName().content(),
				decl.getDescriptor().content(),
				signature,
				decl.getConstantValue() == null ?
						null : GroupUtil.convert(currentClass, decl.getConstantValue()));

		ASMBaseFieldVisitor visitor = new ASMBaseFieldVisitor(fv);
		getAttributeStore().accept(visitor);
		getAttributeStore().clear();
		return visitor;
	}

	@Override
	public MethodGroupVisitor visitImplMethod(MethodDeclarationGroup decl) throws AssemblerException {
		SignatureGroup signatureGroup = getAttributeStore().getFirstMethodAttribute(SignatureGroup.class);
		String signature = signatureGroup == null ? null : signatureGroup.getDescriptor().content();

		List<ThrowsGroup> throwsGroups = getAttributeStore().getMethodAttributesOfType(ThrowsGroup.class);
		String[] throwsArray = throwsGroups.isEmpty() ? null : throwsGroups.stream()
				.map(t -> t.getClassName().content())
				.toArray(String[]::new);

		String dsc = decl.buildDescriptor();
		int access = getAccess(decl.getAccessMods());
		MethodVisitor mv =
				cw.visitMethod(access, decl.getName().content(), dsc, signature, throwsArray);

		boolean isStatic = (access & ACC_STATIC) != 0;
		ASMBaseMethodVisitor visitor = new ASMBaseMethodVisitor(mv, currentClass, isStatic);
		getAttributeStore().accept(visitor);
		getAttributeStore().clear();
		return visitor;
	}

	public static void annotationParam(AnnotationParamGroup annotationParam, AnnotationVisitor av) throws AssemblerException {
		Group paramValue = annotationParam.getParamValue();
		String nameContent = annotationParam.getName().content();
		if (paramValue.isType(GroupType.ARGS)) {
			ArgsGroup args = (ArgsGroup) paramValue;
			AnnotationVisitor arrayVis = av.visitArray(nameContent);
			for (Group group : args.getBody().getChildren()) {
				paramValue(nameContent, group, arrayVis);
			}
			arrayVis.visitEnd();
		} else {
			paramValue(nameContent, paramValue, av);
		}
	}

	private static void paramValue(String name, Group value, AnnotationVisitor av) throws AssemblerException {
		if (value.isType(GroupType.ARGS)) {
			ArgsGroup args = (ArgsGroup) value;
			AnnotationVisitor arrayVis = av.visitArray(name);
			for (Group group : args.getBody().getChildren()) {
				paramValue(name, group, arrayVis);
			}
			arrayVis.visitEnd();
		} else if (value.isType(GroupType.ENUM)) {
			EnumGroup enumGroup = (EnumGroup) value;
			av.visitEnum(name, enumGroup.getDescriptor().content(), enumGroup.getEnumValue().content());
		} else if (value.isType(GroupType.ANNOTATION)) {
			AnnotationGroup annotationGroup = (AnnotationGroup) value;
			AnnotationVisitor annotationVis = av.visitAnnotation(name, annotationGroup.getClassGroup().content());
			for (AnnotationParamGroup param : annotationGroup.getParams()) {
				annotationParam(param, annotationVis);
			}
			annotationVis.visitEnd();
		} else {
			av.visit(name, value.content());
		}
	}

	public int getAccess(AccessModsGroup access) {
		int accessFlags = 0;
		for (AccessModGroup g : access.getAccessMods()) {
			Keyword keyword = keywords.fromGroup(g);
			switch (keyword) {
				case KEYWORD_PUBLIC:
					accessFlags |= ACC_PUBLIC;
					break;
				case KEYWORD_PRIVATE:
					accessFlags |= ACC_PRIVATE;
					break;
				case KEYWORD_STATIC:
					accessFlags |= ACC_STATIC;
					break;
				case KEYWORD_FINAL:
					accessFlags |= ACC_FINAL;
					break;
			}
		}
		return accessFlags;
	}
}

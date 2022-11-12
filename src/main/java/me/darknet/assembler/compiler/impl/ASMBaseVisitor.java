package me.darknet.assembler.compiler.impl;

import me.darknet.assembler.parser.AnnotationTarget;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Keyword;
import me.darknet.assembler.parser.Keywords;
import me.darknet.assembler.parser.groups.AccessModGroup;
import me.darknet.assembler.parser.groups.AccessModsGroup;
import me.darknet.assembler.parser.groups.AnnotationGroup;
import me.darknet.assembler.parser.groups.AnnotationParamGroup;
import me.darknet.assembler.parser.groups.ArgsGroup;
import me.darknet.assembler.parser.groups.EnumGroup;
import me.darknet.assembler.parser.groups.ExprGroup;
import me.darknet.assembler.parser.groups.ExtendsGroup;
import me.darknet.assembler.parser.groups.FieldDeclarationGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.parser.groups.ImplementsGroup;
import me.darknet.assembler.parser.groups.MethodDeclarationGroup;
import me.darknet.assembler.parser.groups.SignatureGroup;
import me.darknet.assembler.parser.groups.ThrowsGroup;
import me.darknet.assembler.transform.MethodVisitor;
import me.darknet.assembler.transform.Visitor;
import me.darknet.assembler.util.GroupUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;

import java.util.ArrayList;
import java.util.List;

import static me.darknet.assembler.parser.Group.GroupType;
import static org.objectweb.asm.Opcodes.*;

public class ASMBaseVisitor implements Visitor {
    private final Keywords keywords;
    private final ClassWriter cw;
    private final int version;
    private final List<String> currentThrows = new ArrayList<>();
    private CachedClass currentClass;
    private AnnotationGroup currentAnnotation;
    private SignatureGroup currentSignature;

    public ASMBaseVisitor(int version, Keywords keywords) {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        this.version = version;
        this.keywords =keywords;
    }


    public String getSignature() {
        if(currentSignature != null) {
            String signature = currentSignature.getDescriptor().content();
            currentSignature = null;
            return signature;
        }
        return null;
    }

    public List<String> getThrows() {
        List<String> throwss = new ArrayList<>(currentThrows);
        currentThrows.clear();
        return throwss;
    }

    @Override
    public void visitClass(AccessModsGroup accessMods, IdentifierGroup identifier) {
        int access = getAccess(accessMods);
        String fullyQualifiedClassName = identifier.content();
        CachedClass cachedClass = new CachedClass();
        cachedClass.setAccess(access);
        cachedClass.setVersion(version);
        cachedClass.setFullyQualifiedName(fullyQualifiedClassName);
        cachedClass.setSignature(getSignature());
        currentClass = cachedClass;
    }

    @Override
    public void visitSuper(ExtendsGroup extendsGroup) {
        if(currentClass != null) {
            currentClass.setSuperGroup(extendsGroup.className.content());
        }
    }

    @Override
    public void visitImplements(ImplementsGroup implementsGroup) {
        if(currentClass != null) {
            currentClass.addImplements(implementsGroup.className.content());
        }
    }

    @Override
    public void visit(Group group) throws AssemblerException {
        if(currentClass != null && !currentClass.hasBuilt) {
            // finish class build
            GroupType type = group.type;
            if (type != GroupType.CLASS_DECLARATION
                    && type != GroupType.IMPLEMENTS_DIRECTIVE
                    && type != GroupType.EXTENDS_DIRECTIVE) {
                // build class
                if(currentAnnotation != null && currentAnnotation.getTarget() == AnnotationTarget.CLASS) {
                    String desc = currentAnnotation.getClassGroup().content();
                    AnnotationParamGroup[] params = currentAnnotation.getParams();
                    AnnotationVisitor av = cw.visitAnnotation(desc, !currentAnnotation.isInvisible());
                    for(AnnotationParamGroup param : params) {
                        annotationParam(param, av);
                    }
                    av.visitEnd();
                    currentAnnotation = null;
                }
                currentClass.build(cw);
            }
        }
    }


    @Override
    public me.darknet.assembler.transform.FieldVisitor visitField(FieldDeclarationGroup decl) throws AssemblerException {
        FieldVisitor fv = cw.visitField(getAccess(decl.accessMods),
                decl.name.content(),
                decl.descriptor.content(),
                getSignature(),
                decl.constantValue == null ?
                        null : GroupUtil.convert(currentClass, decl.constantValue));
        if(currentAnnotation != null && currentAnnotation.getTarget() == AnnotationTarget.FIELD) {
            String desc = currentAnnotation.getClassGroup().content();
            AnnotationParamGroup[] params = currentAnnotation.getParams();
            AnnotationVisitor av = fv.visitAnnotation(desc, !currentAnnotation.isInvisible());
            for(AnnotationParamGroup param : params) {
                annotationParam(param, av);
            }
            av.visitEnd();
            currentAnnotation = null;
        }

        return new ASMBaseFieldVisitor(fv);
    }

    @Override
    public MethodVisitor visitMethod(MethodDeclarationGroup decl) throws AssemblerException {
        String dsc = decl.buildDescriptor();
        int access = getAccess(decl.accessMods);
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(access, decl.name.content(), dsc, getSignature(), getThrows().toArray(new String[0]));

        if(currentAnnotation != null && currentAnnotation.getTarget() == AnnotationTarget.METHOD) {
            String desc = currentAnnotation.getClassGroup().content();
            AnnotationParamGroup[] params = currentAnnotation.getParams();
            AnnotationVisitor av = mv.visitAnnotation(desc, !currentAnnotation.isInvisible());
            for(AnnotationParamGroup param : params) {
                annotationParam(param, av);
            }
            av.visitEnd();
            currentAnnotation = null;
        }

        boolean isStatic = (access & ACC_STATIC) != 0;

        return new ASMBaseMethodVisitor(mv, currentClass, isStatic);
    }

    private void paramValue(String name, Group value, AnnotationVisitor av) throws AssemblerException {
        if (value.type == GroupType.ARGS) {
            ArgsGroup args = (ArgsGroup) value;
            AnnotationVisitor arrayVis = av.visitArray(name);
            for (Group group : args.getBody().children) {
                paramValue(name, group, arrayVis);
            }
            arrayVis.visitEnd();
        } else if (value.type == GroupType.ENUM) {
            EnumGroup enumGroup = (EnumGroup) value;
            av.visitEnum(name, enumGroup.getDescriptor().content(), enumGroup.getEnumValue().content());
        } else if (value.type == GroupType.ANNOTATION) {
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

    private void annotationParam(AnnotationParamGroup annotationParam, AnnotationVisitor av) throws AssemblerException {
        if (annotationParam.value.type == GroupType.ARGS) {
            ArgsGroup args = (ArgsGroup) annotationParam.value;
            AnnotationVisitor arrayVis = av.visitArray(annotationParam.name.content());
            for (Group group : args.getBody().children) {
                paramValue(annotationParam.name.content(), group, arrayVis);
            }
            arrayVis.visitEnd();
        } else {
            paramValue(annotationParam.name.content(), annotationParam.value, av);
        }
    }

    @Override
    public void visitAnnotation(AnnotationGroup group) throws AssemblerException {
        currentAnnotation = group; // withhold the annotation until target is met
    }

    @Override
    public void visitSignature(SignatureGroup signature) throws AssemblerException {
        currentSignature = signature;
    }

    @Override
    public void visitThrows(ThrowsGroup throwsGroup) throws AssemblerException {
        currentThrows.add(throwsGroup.getClassName().content());
    }

    @Override
    public void visitExpression(ExprGroup expr) throws AssemblerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visitEndClass() {

    }

    public byte[] toByteArray() {
        return cw.toByteArray();
    }

    private int getAccess(AccessModsGroup access) {
        int accessFlags = 0;
        for (AccessModGroup g : access.accessMods) {
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

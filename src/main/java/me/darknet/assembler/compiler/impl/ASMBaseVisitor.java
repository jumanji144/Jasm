package me.darknet.assembler.compiler.impl;

import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.parser.AnnotationTarget;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Parser;
import me.darknet.assembler.parser.groups.*;
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

    ClassWriter cw;
    int version;

    CachedClass currentClass;

    AnnotationGroup currentAnnotation;

    SignatureGroup currentSignature;

    List<String> currentThrows = new ArrayList<>();

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

    public ASMBaseVisitor(int version) {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        this.version = version;
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
    public void visitField(AccessModsGroup accessMods, IdentifierGroup name, IdentifierGroup descriptor) throws AssemblerException {
        FieldVisitor fv = cw.visitField(getAccess(accessMods), name.content(), descriptor.content(), getSignature(), null);
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
        fv.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(AccessModsGroup accessMods, IdentifierGroup descriptor, BodyGroup body) throws AssemblerException {
        MethodDescriptor md = new MethodDescriptor(descriptor.content());
        int access = getAccess(accessMods);
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(access, md.name, md.desc, getSignature(), getThrows().toArray(new String[0]));

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

        return new ASMBaseMethodVisitor(mv, md, currentClass, isStatic);
    }

    public void paramValue(String name, Group value, AnnotationVisitor av) throws AssemblerException{

        if(value.type == GroupType.ARGS){
            ArgsGroup args = (ArgsGroup) value;
            AnnotationVisitor arrayVis = av.visitArray(name);
            for (Group group : args.getBody().children) {
                paramValue(name, group, arrayVis);
            }
            arrayVis.visitEnd();
        } else if(value.type == GroupType.ENUM) {
            EnumGroup enumGroup = (EnumGroup) value;
            av.visitEnum(name, enumGroup.getDescriptor().content(), enumGroup.getEnumValue().content());
        } else if(value.type == GroupType.ANNOTATION) {
            AnnotationGroup annotationGroup = (AnnotationGroup) value;
            AnnotationVisitor annotationVis = av.visitAnnotation(name, annotationGroup.getClassGroup().content());
            for(AnnotationParamGroup param : annotationGroup.getParams()) {
                annotationParam(param, annotationVis);
            }
            annotationVis.visitEnd();
        } else {
            av.visit(name, value.content());
        }

    }

    public void annotationParam(AnnotationParamGroup annotationParam, AnnotationVisitor av) throws AssemblerException {
        if(annotationParam.value.type == GroupType.ARGS) {
            ArgsGroup args = (ArgsGroup) annotationParam.value;
            AnnotationVisitor arrayVis = av.visitArray(annotationParam.name.content());
            for (Group group : args.getBody().children) {
                paramValue(annotationParam.name.content(), group, arrayVis);
            }
            arrayVis.visitEnd();
        }else {
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
    public void visitEnd() {

    }

    public byte[] toByteArray() {
        return cw.toByteArray();
    }

    public static int getAccess(AccessModsGroup access) {
        int accessFlags = 0;
        for (AccessModGroup g : access.accessMods) {
            switch (g.content()) {
                case Parser.KEYWORD_PUBLIC:
                    accessFlags |= ACC_PUBLIC;
                    break;
                case Parser.KEYWORD_PRIVATE:
                    accessFlags |= ACC_PRIVATE;
                    break;
                case Parser.KEYWORD_STATIC:
                    accessFlags |= ACC_STATIC;
                    break;
            }
        }
        return accessFlags;
    }

}

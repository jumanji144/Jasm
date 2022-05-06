package me.darknet.assembler.compiler.impl;

import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Parser;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.transform.MethodVisitor;
import me.darknet.assembler.transform.Visitor;
import org.objectweb.asm.ClassWriter;

import java.util.ArrayList;
import java.util.List;

import static me.darknet.assembler.parser.Group.GroupType;
import static org.objectweb.asm.Opcodes.*;

public class ASMBaseVisitor implements Visitor {

    ClassWriter cw;
    int version;

    CachedClass currentClass;

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
    public void visit(Group group) {
        if(currentClass != null && !currentClass.hasBuilt) {
            GroupType type = group.type;
            if (type != GroupType.CLASS_DECLARATION
                    && type != GroupType.IMPLEMENTS_DIRECTIVE
                    && type != GroupType.EXTENDS_DIRECTIVE) {
                // build class
                currentClass.build(cw);
            }
        }
    }


    @Override
    public void visitField(AccessModsGroup accessMods, IdentifierGroup name, IdentifierGroup descriptor) {
        cw.visitField(getAccess(accessMods), name.content(), descriptor.content(), null, null);
    }

    @Override
    public MethodVisitor visitMethod(AccessModsGroup accessMods, IdentifierGroup descriptor, BodyGroup body) {
        MethodDescriptor md = new MethodDescriptor(descriptor.content());
        int access = getAccess(accessMods);
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(access, md.name, md.desc, null, null);

        boolean isStatic = (access & ACC_STATIC) != 0;

        return new ASMBaseMethodVisitor(mv, md, currentClass, isStatic);
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
                case Parser.KEYWORD_PUBLIC -> accessFlags |= ACC_PUBLIC;
                case Parser.KEYWORD_PRIVATE -> accessFlags |= ACC_PRIVATE;
                case Parser.KEYWORD_STATIC -> accessFlags |= ACC_STATIC;
            }
        }
        return accessFlags;
    }

}

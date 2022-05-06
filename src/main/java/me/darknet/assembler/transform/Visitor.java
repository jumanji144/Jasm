package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

public interface Visitor {

    void visit(Group group) throws AssemblerException;
    void visitClass(AccessModsGroup accessMods, IdentifierGroup identifier) throws AssemblerException;
    void visitSuper(ExtendsGroup extendsGroup) throws AssemblerException;
    void visitImplements(ImplementsGroup implementsGroup) throws AssemblerException;
    void visitField(AccessModsGroup accessMods, IdentifierGroup name, IdentifierGroup descriptor) throws AssemblerException;
    MethodVisitor visitMethod(AccessModsGroup accessMods, IdentifierGroup descriptor, BodyGroup body) throws AssemblerException;
    void visitEnd() throws AssemblerException;
}

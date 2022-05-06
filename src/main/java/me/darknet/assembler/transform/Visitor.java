package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

public interface Visitor {

    /**
     * visit any group
     * @param group the group to visit
     * @throws AssemblerException if an error occurs
     */
    void visit(Group group) throws AssemblerException;

    /**
     * Visit a class declaration
     * @param accessMods the access modifiers of the class
     * @param identifier the identifier of the class (fully qualified)
     * @throws AssemblerException if an error occurs
     */
    void visitClass(AccessModsGroup accessMods, IdentifierGroup identifier) throws AssemblerException;

    /**
     * Visit a extends directive
     * @param extendsGroup the extends group containing the name of the super class
     * @throws AssemblerException if an error occurs
     */
    void visitSuper(ExtendsGroup extendsGroup) throws AssemblerException;

    /**
     * Visit a implements directive
     * @param implementsGroup the implements group containing the names of the interface
     * @throws AssemblerException if an error occurs
     */
    void visitImplements(ImplementsGroup implementsGroup) throws AssemblerException;

    /**
     * Visit a field declaration
     * @param accessMods the access modifiers of the field
     * @param name the name of the field
     * @param descriptor the descriptor of the field
     * @throws AssemblerException if an error occurs
     */
    void visitField(AccessModsGroup accessMods, IdentifierGroup name, IdentifierGroup descriptor) throws AssemblerException;

    /**
     * Visit a method declaration
     * @param accessMods the access modifiers of the method
     * @param descriptor the descriptor of the method
     * @param body the body of the method (list of instructions)
     * @return a method visitor to visit the method body
     * @throws AssemblerException if an error occurs
     */
    MethodVisitor visitMethod(AccessModsGroup accessMods, IdentifierGroup descriptor, BodyGroup body) throws AssemblerException;

    /**
     * Visit an end instruction (end of a method)
     * @throws AssemblerException if an error occurs
     */
    void visitEnd() throws AssemblerException;

}

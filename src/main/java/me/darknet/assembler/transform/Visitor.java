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
     * @param decl the field declaration group
     * @throws AssemblerException if an error occurs
     */
    FieldVisitor visitField(FieldDeclarationGroup decl) throws AssemblerException;

    /**
     * Visit a method declaration
     * @param decl the method declaration group
     * @return a method visitor to visit the method body
     * @throws AssemblerException if an error occurs
     */
    MethodVisitor visitMethod(MethodDeclarationGroup decl) throws AssemblerException;

    /**
     * Visit an annotation
     * @param annotation the annotation group
     * @throws AssemblerException if an error occurs
     */
    void visitAnnotation(AnnotationGroup annotation) throws AssemblerException;

    /**
     * Visit a signature
     * @param signature the signature group
     * @throws AssemblerException if an error occurs
     */
    void visitSignature(SignatureGroup signature) throws AssemblerException;

    /**
     * Visit a throws directive
     * @param throwsGroup the throws group containing the name of the exception
     *                    that can be thrown by the method
     * @throws AssemblerException if an error occurs
     */
    void visitThrows(ThrowsGroup throwsGroup) throws AssemblerException;

    /**
     * Visit a expression
     * @param expr the expression group
     * @throws AssemblerException if an error occurs
     */
    void visitExpression(ExprGroup expr) throws AssemblerException;


    /**
     * Visited at the end of all the elements
     * @throws AssemblerException if an error occurs
     */
    void visitEndClass() throws AssemblerException;

}

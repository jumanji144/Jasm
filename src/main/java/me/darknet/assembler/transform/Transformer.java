package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

import java.util.Collection;

public class Transformer {

    Visitor visitor;

    /**
     * Constructs a new {@link Transformer} instance.
     * @param visitor the visitor to use
     */
    public Transformer(Visitor visitor) {
        this.visitor = visitor;
    }

    /**
     * Transforms the given {@param groups} using the given {@param visitor}.
     * @param groups the groups to transform
     * @throws AssemblerException if an error occurs
     */
    public void transform(Collection<Group> groups) throws AssemblerException {

        for(Group group : groups) {
            try {
                visitor.visit(group);
                switch (group.type) {
                    case CLASS_DECLARATION -> {
                        ClassDeclarationGroup classDcl = (ClassDeclarationGroup) group;
                        visitor.visitClass(classDcl.accessMods, classDcl.name);
                    }
                    case FIELD_DECLARATION -> {
                        FieldDeclarationGroup fieldDcl = (FieldDeclarationGroup) group;
                        visitor.visitField(fieldDcl.accessMods, fieldDcl.name, fieldDcl.descriptor);
                    }
                    case METHOD_DECLARATION -> {
                        MethodDeclarationGroup methodDcl = (MethodDeclarationGroup) group;
                        // create a new method visitor
                        MethodVisitor mv = visitor.visitMethod(methodDcl.accessMods, methodDcl.descriptor, methodDcl.body);
                        // call it using a method transformer
                        MethodTransformer mt = new MethodTransformer(mv);
                        mt.transform(methodDcl.body);
                    }
                    case ANNOTATION -> {
                        AnnotationGroup annotation = (AnnotationGroup) group;
                        visitor.visitAnnotation(annotation);
                    }
                    case EXTENDS_DIRECTIVE -> visitor.visitSuper((ExtendsGroup) group);
                    case IMPLEMENTS_DIRECTIVE -> visitor.visitImplements((ImplementsGroup) group);
                }
            }catch (AssemblerException e) {
                throw e;
            }catch  (Exception e1) {
                throw new AssemblerException(e1, group.location());
            }
        }

        visitor.visitEnd();

    }

}

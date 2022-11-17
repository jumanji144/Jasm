package me.darknet.assembler.transform;

import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.*;

import java.util.Collection;

public class Transformer {
    private final TopLevelGroupVisitor visitor;

    /**
     * Constructs a new {@link Transformer} instance.
     * @param visitor the visitor to use
     */
    public Transformer(TopLevelGroupVisitor visitor) {
        this.visitor = visitor;
    }

    /**
     * Transforms the given {@param groups} using the given {@param visitor}.
     * @param groups the groups to transform
     * @throws AssemblerException if an error occurs
     */
    public void transform(Collection<Group> groups) throws AssemblerException {
        visitor.visitBegin();
        for(Group group : groups) {
            try {
                visitor.visit(group);
                switch (group.getType()) {
                    case CLASS_DECLARATION:
                        ClassDeclarationGroup classDcl = (ClassDeclarationGroup) group;
                        ClassGroupVisitor cv = visitor.visitClass(classDcl);
                        cv.visitBegin();
                        cv.visitEnd();
                        break;
                    case FIELD_DECLARATION:
                        FieldDeclarationGroup fieldDcl = (FieldDeclarationGroup) group;
                        FieldGroupVisitor fv = visitor.visitField(fieldDcl);
                        fv.visitBegin();
                        fv.visitEnd();
                        break;
                    case METHOD_DECLARATION:
                        MethodDeclarationGroup methodDcl = (MethodDeclarationGroup) group;
                        MethodGroupVisitor mv = visitor.visitMethod(methodDcl);
                        mv.visitBegin();
                        MethodTransformer mt = new MethodTransformer(mv);
                        mt.transform(methodDcl.getBody());
                        mv.visitEnd();
                        break;
                    case ANNOTATION:
                    case EXTENDS_DIRECTIVE:
                    case IMPLEMENTS_DIRECTIVE:
                    case SIGNATURE_DIRECTIVE:
                    case VERSION_DIRECTIVE:
                    case SOURCE_FILE_DIRECTIVE:
                    case INNER_CLASS_DIRECTIVE:
                    case NEST_HOST_DIRECTIVE:
                    case NEST_MEMBER_DIRECTIVE:
                    case THROWS:
                    case EXPR:
                    case MACRO_DIRECTIVE:
                        break; // ignore
                    default:
                        throw new AssemblerException("Unexpected identifier: " + group.content(), group.getStartLocation());
                }
            }catch (AssemblerException e) {
                throw e;
            }catch  (Exception e1) {
                e1.printStackTrace();
                throw new AssemblerException(e1, group.getStartLocation());
            }
        }

        visitor.visitEnd();
    }

}

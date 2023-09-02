package me.darknet.assembler.transformer;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.visitor.ASTRootVisitor;

import java.util.List;

public class Transformer {

    private final ASTRootVisitor visitor;

    public Transformer(ASTRootVisitor visitor) {
        this.visitor = visitor;
    }

    /**
     * Transform a list of declarations into the transformers {@link Transformer#visitor}
     * @param declarations the declarations to transform
     * @return the result of the transformation
     */
    public Result<Void> transform(List<ASTElement> declarations) {
        ErrorCollector collector = new ErrorCollector();
        for (ASTElement declaration : declarations) {
            if(declaration instanceof ASTField field) {
                field.accept(collector, visitor.visitField(field.modifiers(), field.name(), field.descriptor()));
            } else if (declaration instanceof ASTMethod method) {
                method.accept(collector, visitor.visitMethod(method.modifiers(), method.name(), method.descriptor()));
            } else if (declaration instanceof ASTClass clazz) {
                clazz.accept(collector, visitor.visitClass(clazz.modifiers(), clazz.name()));
            } else {
                collector.addError("Don't know how to process: "
                        + declaration.type(), declaration.location());
            }
        }
        return new Result<>(null, collector.getErrors());
    }

}

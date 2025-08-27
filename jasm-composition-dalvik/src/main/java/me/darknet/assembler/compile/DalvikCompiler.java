package me.darknet.assembler.compile;

import me.darknet.assembler.DalvikClassRepresentation;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.compile.visitor.DalvikRootVisitor;
import me.darknet.assembler.compiler.ClassResult;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.transformer.Transformer;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DalvikCompiler implements Compiler {
    @Override
    public @NotNull Result<? extends ClassResult> compile(List<ASTElement> ast, CompilerOptions<?> options) {
        DalvikCompilerOptions dalvikOptions = (DalvikCompilerOptions) options;

        DalvikClassRepresentation overlayRepresentation = (DalvikClassRepresentation) dalvikOptions.overlay();
        ClassDefinition overlay = overlayRepresentation != null ? overlayRepresentation.definition() : null;

        ErrorCollector collector = new ErrorCollector();

        if (ast.size() != 1) {
            collector.addError("Expected exactly one declaration", ast.get(1).location());
            return new Result<>(new DalvikClassResult(null), collector.getErrors(), collector.getWarns());
        }

        DalvikRootVisitor visitor = new DalvikRootVisitor(overlay);

        Transformer transformer = new Transformer(visitor);
        transformer.transform(ast).ifErr(collector::addErrors).ifWarn(collector::addWarnings);

        return Result.ok(null);
    }
}

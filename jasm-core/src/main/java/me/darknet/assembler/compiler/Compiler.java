package me.darknet.assembler.compiler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Result;

import java.util.List;

public interface Compiler {

    /**
     * Compile the given AST, the must be valid with no errors
     *
     * @param ast
     *            The AST to compile
     *
     * @return The result of the compilation process
     */
    Result<? extends ClassRepresentation> compile(List<ASTElement> ast, CompilerOptions<?> options);

}

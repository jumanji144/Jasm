package me.darknet.assembler.cli.repl.executor;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.instructions.ParsedInstruction;

public interface Executor<T extends Frame> {

    T execute(ParsedInstruction instruction);

    T execute();

    T frame();

}

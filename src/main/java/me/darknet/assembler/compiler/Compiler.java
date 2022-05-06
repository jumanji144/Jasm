package me.darknet.assembler.compiler;

import me.darknet.assembler.compiler.impl.ASMBaseVisitor;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.ParserContext;
import me.darknet.assembler.transform.Transformer;

public class Compiler {

    public ASMBaseVisitor visitor;

    public Compiler(int version) {
        visitor = new ASMBaseVisitor(version);
    }

    public void compile(ParserContext ctx) throws AssemblerException {

        Transformer transformer = new Transformer(visitor);
        transformer.transform(ctx.groups);

    }

    public byte[] finish() throws AssemblerException {
        return visitor.toByteArray();
    }

}

package me.darknet.assembler.compiler;

import me.darknet.assembler.compiler.impl.ASMBaseVisitor;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Keywords;
import me.darknet.assembler.parser.ParserContext;
import me.darknet.assembler.transform.Transformer;

public class Compiler {

    public ASMBaseVisitor visitor;

    public Compiler(int version) {
        this(version, new Keywords());
    }

    public Compiler(int version, Keywords keywords) {
        visitor = new ASMBaseVisitor(version, keywords);
    }

    public void compile(ParserContext ctx) throws AssemblerException {

        Transformer transformer = new Transformer(visitor);
        transformer.transform(ctx.groups);

    }

    public byte[] finish() throws AssemblerException {
        return visitor.toByteArray();
    }

}

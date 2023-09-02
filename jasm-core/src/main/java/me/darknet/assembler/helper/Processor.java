package me.darknet.assembler.helper;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.parser.processor.ASTProcessor;

import java.util.List;
import java.util.function.Consumer;

public class Processor {

    public static void processSource(String code, String source, Consumer<List<ASTElement>> consumer,
                                     Consumer<List<Error>> error, BytecodeFormat format) {
        new DeclarationParser()
                .parseDeclarations(new Tokenizer()
                        .tokenize(source, code))
                    .ifOk(lAst ->
                        new ASTProcessor(format).processAST(lAst)
                            .ifOk(consumer)
                            .ifErr((pAst, err) -> error.accept(err))
                     )
                    .ifErr((pAst, err) -> error.accept(err));
    }

}

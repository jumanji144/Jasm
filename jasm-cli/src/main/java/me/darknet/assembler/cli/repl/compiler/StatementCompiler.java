package me.darknet.assembler.cli.repl.compiler;

import me.darknet.assembler.error.Result;
import me.darknet.assembler.instructions.ParsedInstruction;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.parser.processor.ASTProcessor;

public class StatementCompiler {

    private final BytecodeFormat bytecodeFormat;

    public static final int instructionLine = 2;

    public StatementCompiler(BytecodeFormat bytecodeFormat) {
        this.bytecodeFormat = bytecodeFormat;
    }

    public Result<ParsedInstruction> compile(String statement) {
        Tokenizer tokenizer = new Tokenizer();
        return tokenizer.tokenize("<repl>", statement)
                .flatMap(tokens -> {
                    DeclarationParser parser = new DeclarationParser();
                    return parser.parseInstruction(tokens).flatMap(instruction -> {
                        ASTProcessor processor = new ASTProcessor(bytecodeFormat);
                        return processor.processInstruction(instruction).flatMap(processed ->
                                Result.ok(new ParsedInstruction(instruction, processed)));
                    });
                });
    }

}

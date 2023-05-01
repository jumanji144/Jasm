package me.darknet.assembler.parser;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.error.Result;

import java.util.List;

public class Parser {

	public static Result<List<ASTElement>> parse(String source, String input) {
		Tokenizer tokenizer = new Tokenizer();
		List<Token> tokens = tokenizer.tokenize(source, input);
		DeclarationParser declParser = new DeclarationParser();
		Result<List<ASTElement>> declarations = declParser.parseDeclarations(tokens);
		if(declarations.isErr()) {
			return declarations;
		}
		return ASTProcessor.processAST(declarations.get());
	}

}

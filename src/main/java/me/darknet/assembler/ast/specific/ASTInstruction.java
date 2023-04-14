package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.util.CollectionUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ASTInstruction extends ASTElement {

	private final ASTIdentifier identifier;
	private final List<@Nullable ASTElement> arguments;

	public ASTInstruction(ASTIdentifier identifier, List<@Nullable ASTElement> arguments) {
		super(ElementType.CODE_INSTRUCTION, CollectionUtil.merge(arguments, identifier));
		this.identifier = identifier;
		this.arguments = arguments;
	}

	public ASTIdentifier getIdentifier() {
		return identifier;
	}

	public List<@Nullable ASTElement> getArguments() {
		return arguments;
	}

}

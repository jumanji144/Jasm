package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ASTField extends ASTMember {

	private final ASTIdentifier descriptor;
	private final @Nullable ASTValue value;

	public ASTField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor,
					@Nullable List<ASTAnnotation> annotations,
					@Nullable ASTIdentifier signature,
					@Nullable ASTValue value) {
		super(ElementType.FIELD, modifiers, name, signature, annotations);
		this.descriptor = descriptor;
		this.value = value;
	}

	public ASTIdentifier getDescriptor() {
		return descriptor;
	}

	public @Nullable ASTValue getFieldValue() {
		return value;
	}
}

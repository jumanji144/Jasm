package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.primitive.ASTString;
import org.jetbrains.annotations.Nullable;

public interface ASTSigned {
	@Nullable
	ASTString getSignature();

	void setSignature(@Nullable ASTString signature);
}

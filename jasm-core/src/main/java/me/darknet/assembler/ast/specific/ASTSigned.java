package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.primitive.ASTString;
import org.jetbrains.annotations.Nullable;

/**
 * AST model of an element that can have a generic signature.
 */
public interface ASTSigned {
	/**
	 * @return Generic signature of the element, or {@code null} if it does not have one.
	 */
	@Nullable
	ASTString getSignature();

	/**
	 * @param signature
	 * 		Generic signature of the element, or {@code null} to remove it.
	 */
	void setSignature(@Nullable ASTString signature);
}

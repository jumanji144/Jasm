package me.darknet.assembler.ast;

import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a comment in the source code that is not associated with any specific AST element.
 *
 * @param token
 * 		The token representing the comment.
 */
public record MetaComment(@NotNull Token token) {
	public MetaComment {
		Objects.requireNonNull(token, "token");
	}

	/**
	 * @return The content of the comment.
	 */
	public @NotNull String content() {
		return token.content();
	}

	/**
	 * @return The location of the comment in the source code.
	 */
	public @NotNull Location location() {
		return token.location();
	}
}

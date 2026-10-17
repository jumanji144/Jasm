package me.darknet.assembler.printer;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Simple implementation of {@link DisassemblyCommentProvider} that uses a map to store comments.
 */
public final class DisassemblyCommentMap implements DisassemblyCommentProvider {
	private static final List<String> EMPTY = Collections.emptyList();
	private final Map<DisassemblyCommentSite, List<String>> comments;

	/**
	 * @param comments
	 * 		Map of comment sites to their associated comments.
	 */
	public DisassemblyCommentMap(@NotNull Map<DisassemblyCommentSite, List<String>> comments) {
		Map<DisassemblyCommentSite, List<String>> copy = new HashMap<>();
		comments.forEach((site, bodies) -> copy.put(
				Objects.requireNonNull(site, "site"), List.copyOf(Objects.requireNonNull(bodies, "bodies"))));
		this.comments = Collections.unmodifiableMap(copy);
	}

	@Override
	public @NotNull List<String> commentsFor(@NotNull DisassemblyCommentSite site) {
		return comments.getOrDefault(site, EMPTY);
	}
}

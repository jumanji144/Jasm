package me.darknet.assembler.printer;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provider for comments to be printed alongside disassembly.
 * <p>
 * This is a library consumer extension point.
 * A consumer can implement this interface to provide their own comment storage and retrieval mechanism.
 */
@FunctionalInterface
public interface DisassemblyCommentProvider {
    /** No-op provider that provides no comments. */
    DisassemblyCommentProvider NONE = site -> List.of();

    /**
     * @param site
     * 		Site to get comments for.
     *
     * @return Comments for the site.
     */
    @NotNull List<String> commentsFor(@NotNull DisassemblyCommentSite site);
}

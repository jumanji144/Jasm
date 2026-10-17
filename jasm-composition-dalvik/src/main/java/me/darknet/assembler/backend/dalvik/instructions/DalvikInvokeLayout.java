package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.descriptor.Descriptors;
import me.darknet.dex.tree.type.ClassType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Structural grouping of the register words of an invoke into its logical arguments.
 * <p>
 * The grouping reads only the prototype, whether a receiver leads, and the words. It never consults register values
 * or what the analysis could infer about them, so a call whose arguments are unknown groups exactly as a known one does.
 */
public final class DalvikInvokeLayout {
    private DalvikInvokeLayout() {}

    /**
     * @param parameters
     * 		Parameter types of the prototype the words are grouped against.
     * @param receiver
     * 		{@code true} when the first word is the receiver of an instance invoke.
     * @param words
     * 		Register words in the order they are encoded.
     *
     * @return {@code true} when the words are exactly the receiver, if there is one, followed by one group per parameter,
     * where each long or double parameter occupies two consecutive registers in order. A word count that differs from
     * the prototype, or a wide pair that is not consecutive, is not a faithful grouping.
     */
    public static boolean groupsFaithfully(@NotNull List<ClassType> parameters, boolean receiver, int @NotNull [] words) {
        int next = 0;
        if (receiver) {
            if (words.length == 0)
                return false;
            next = 1;
        }
        for (ClassType parameter : parameters) {
            boolean wide = Descriptors.isWideType(parameter.descriptor());
            int width = wide ? 2 : 1;
            if (words.length - next < width)
                return false;
            if (wide && words[next + 1] != words[next] + 1)
                return false;
            next += width;
        }
        return next == words.length;
    }
}

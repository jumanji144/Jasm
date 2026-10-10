package me.darknet.assembler.descriptor;

import org.jetbrains.annotations.NotNull;

/**
 * A parsed JVM/Dalvik type or method descriptor.
 * <p>
 * Both bundled targets describe types with the same grammar, so the model is target-neutral: it
 * holds the parsed shape and the canonical descriptor text, and leaves conversion into a target
 * library's own type objects to the backend.
 *
 * @see DescriptorType
 * @see MethodDescriptor
 */
public sealed interface Descriptor permits DescriptorType, MethodDescriptor {

    /**
     * @return Canonical descriptor text in the form the target writes it, for example
     * 		{@code Ljava/lang/String;} or {@code (I)Ljava/lang/String;}.
     */
    @NotNull
    String descriptor();
}

package me.darknet.assembler.descriptor;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A method signature, which is a descriptor that names parameters and a return type rather than a
 * single value type.
 *
 * @param parameters
 * 		Parameter types in declaration order. An array component may never be void, and neither may a
 * 		parameter, which the parser enforces before construction.
 * @param returnType
 * 		Return type, which is the only position permitted to be void.
 */
public record MethodDescriptor(@NotNull List<DescriptorType> parameters, @NotNull DescriptorType returnType)
        implements Descriptor {

    public MethodDescriptor {
        parameters = List.copyOf(parameters);
    }

    @Override
    public @NotNull String descriptor() {
        return parameters.stream()
                .map(DescriptorType::descriptor)
                .collect(Collectors.joining("", "(", ")")) + returnType.descriptor();
    }
}

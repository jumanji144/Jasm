package me.darknet.assembler.analysis.registry;

import me.darknet.assembler.analysis.MethodReference;
import me.darknet.assembler.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Basic implementation of {@link MethodValueLookup} with some common methods implemented.
 */
public class BasicMethodValueLookup implements MethodValueLookup {
    private static final MethodValueRegistry DEFAULT_REGISTRY = MethodValueRegistry.builder()
            .mergeAll(
                    MathMethodValueRegistry.create(),
                    SystemMethodValueRegistry.create(),
                    StringMethodValueRegistry.create(),
                    LongMethodValueRegistry.create(),
                    DoubleMethodValueRegistry.create(),
                    FloatMethodValueRegistry.create(),
                    IntegerMethodValueRegistry.create(),
                    CharacterMethodValueRegistry.create(),
                    ShortMethodValueRegistry.create(),
                    ByteMethodValueRegistry.create(),
                    BooleanMethodValueRegistry.create()
            )
            .build();

    private final MethodValueRegistry registry;

    public BasicMethodValueLookup() {
        this(DEFAULT_REGISTRY);
    }

    public BasicMethodValueLookup(@NotNull MethodValueRegistry registry) {
        this.registry = registry;
    }

    @Override
    public @Nullable Value accept(@NotNull MethodReference method, Value.@Nullable ObjectValue context,
                                  @NotNull List<Value> parameters) {
        return registry.accept(method, context, parameters);
    }
}

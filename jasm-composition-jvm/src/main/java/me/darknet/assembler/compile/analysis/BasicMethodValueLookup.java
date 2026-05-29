package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.instruction.MethodInstruction;
import me.darknet.assembler.compile.analysis.jvm.MethodValueLookup;
import me.darknet.assembler.compile.analysis.registry.BooleanMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.ByteMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.CharacterMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.DoubleMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.FloatMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.IntegerMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.LongMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.MathMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.MethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.ShortMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.StringMethodValueRegistry;
import me.darknet.assembler.compile.analysis.registry.SystemMethodValueRegistry;
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
    public @Nullable Value accept(@NotNull MethodInstruction instruction, Value.@Nullable ObjectValue context,
                                  @NotNull List<Value> parameters) {
        return registry.accept(instruction, context, parameters);
    }
}

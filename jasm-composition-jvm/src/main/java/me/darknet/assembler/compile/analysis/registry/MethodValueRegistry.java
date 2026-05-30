package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.func.StaticFunc;
import me.darknet.assembler.compile.analysis.func.StringFunc;
import me.darknet.assembler.compile.analysis.jvm.MethodValueLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable registry of method value handlers.
 */
public final class MethodValueRegistry implements MethodValueLookup {
    private static final MethodValueRegistry EMPTY = new MethodValueRegistry(Map.of(), Map.of());

    private final Map<String, StringFunc> stringInstanceFuncs;
    private final Map<String, StaticFunc> staticFuncs;

    private MethodValueRegistry(@NotNull Map<String, StringFunc> stringInstanceFuncs,
                                @NotNull Map<String, StaticFunc> staticFuncs) {
        this.stringInstanceFuncs = Map.copyOf(stringInstanceFuncs);
        this.staticFuncs = Map.copyOf(staticFuncs);
    }

    public static @NotNull MethodValueRegistry empty() {
        return EMPTY;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public @Nullable Value accept(@NotNull MethodInsnNode instruction, Value.@Nullable ObjectValue context,
                                  @NotNull List<Value> parameters) {
        if (context instanceof Value.KnownStringValue stringValue) {
            StringFunc func = stringInstanceFuncs.get(instanceKey(instruction.name, instruction.desc));
            if (func != null)
                return func.apply(stringValue.value(), parameters);
        } else if (context == null) {
            StaticFunc func = staticFuncs.get(staticKey(
                    instruction.owner,
                    instruction.name,
                    instruction.desc
            ));
            if (func != null)
                return func.apply(parameters);
        }
        return null;
    }

    public static final class Builder {
        private final Map<String, StringFunc> stringInstanceFuncs = new HashMap<>();
        private final Map<String, StaticFunc> staticFuncs = new HashMap<>();

        public @NotNull Builder registerStatic(@NotNull String owner, @NotNull String name,
                                               @NotNull String descriptor, @NotNull StaticFunc handler) {
            staticFuncs.put(staticKey(owner, name, descriptor), handler);
            return this;
        }

        public @NotNull Builder registerStringInstance(@NotNull String name, @NotNull String descriptor,
                                                       @NotNull StringFunc handler) {
            stringInstanceFuncs.put(instanceKey(name, descriptor), handler);
            return this;
        }

        public @NotNull Builder merge(@NotNull MethodValueRegistry other) {
            stringInstanceFuncs.putAll(other.stringInstanceFuncs);
            staticFuncs.putAll(other.staticFuncs);
            return this;
        }

        public @NotNull Builder mergeAll(@NotNull MethodValueRegistry... registries) {
            for (MethodValueRegistry registry : registries)
                merge(registry);
            return this;
        }

        Builder copyStaticOwner(@NotNull String fromOwner, @NotNull String toOwner) {
            String fromPrefix = fromOwner + ".";
            String toPrefix = toOwner + ".";
            Map<String, StaticFunc> copies = new HashMap<>();
            for (Map.Entry<String, StaticFunc> entry : staticFuncs.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(fromPrefix))
                    copies.put(toPrefix + key.substring(fromPrefix.length()), entry.getValue());
            }
            staticFuncs.putAll(copies);
            return this;
        }

        public @NotNull MethodValueRegistry build() {
            if (stringInstanceFuncs.isEmpty() && staticFuncs.isEmpty())
                return MethodValueRegistry.empty();
            return new MethodValueRegistry(stringInstanceFuncs, staticFuncs);
        }
    }

    private static @NotNull String staticKey(@NotNull String owner, @NotNull String name, @NotNull String descriptor) {
        return owner + "." + name + descriptor;
    }

    private static @NotNull String instanceKey(@NotNull String name, @NotNull String descriptor) {
        return name + descriptor;
    }
}

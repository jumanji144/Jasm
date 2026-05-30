package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.Objects;

public class Local {
    protected final int index;
    protected final String name;
    protected final Type type;

    public Local(int index, @NotNull String name, @Nullable Type type) {
        this.index = index;
        this.name = name;
        this.type = type;
    }

    public boolean isNull() {
        return type() == null;
    }

    public int index() {
        return index;
    }

    @NotNull
    public String name() {
        return name;
    }

    @Nullable
    public Type type() {
        return type;
    }

    @NotNull
    public Type safeType() {
        return type == null ? JvmTypeUtils.OBJECT : type;
    }

    @NotNull
    public Local adaptType(@NotNull Type newType) {
        if (Objects.equals(type, newType))
            return this;
        return new Local(index, name, newType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Local local = (Local) o;

        if (index != local.index)
            return false;
        if (!name.equals(local.name))
            return false;
        return Objects.equals(type, local.type);
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + name.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Local{" + "index=" + index + ", name='" + name + '\'' + (isNull() ?  "null=true" : ", type=" + type) + '}';
    }

    /**
     * @return Type size of this variable. {@code 1} for all types except for
     *         {@link double}/{@code long} which is {@code 2}.
     */
    public int size() {
        return JvmTypeUtils.category(type);
    }
}

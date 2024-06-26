package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ValuedLocal extends Local {
    private final Value value;

    public ValuedLocal(@NotNull Local other, @NotNull Value value) {
        this(other.index(), other.name(), other.type(), value);
    }

    public ValuedLocal(int index, @NotNull String name, @NotNull Value value) {
        this(index, name, value.type(), value);
    }

    public ValuedLocal(int index, @NotNull String name, @Nullable ClassType type, @NotNull Value value) {
        super(index, name, type);
        this.value = value;
    }

    @NotNull
    public ValuedLocal asNull() {
        if (isNull()) return this;
        return new ValuedLocal(index, name, null, value);
    }

    @NotNull
    public ValuedLocal adaptType(@NotNull ClassType newType) {
        if (Objects.equals(type, newType))
            return this;
        return new ValuedLocal(index, name, newType, Values.valueOf(newType));
    }

    @NotNull
    public ValuedLocal mergeWith(@NotNull InheritanceChecker checker, @NotNull ValuedLocal other) throws ValueMergeException {
        if (isNull() && !other.isNull())
            return other;
        else if (!isNull() && other.isNull())
            return this;
        Value newValue = value.mergeWith(checker, other.value);
        return new ValuedLocal(index, name, newValue.type(), newValue);
    }

    @NotNull
    public Value value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o))
            return false;

        ValuedLocal that = (ValuedLocal) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ValuedLocal{" + "index=" + index + "'" + ", name='" + name + '\'' + (isNull() ? ", null=true" : ", value=" + value) + '}';
    }
}

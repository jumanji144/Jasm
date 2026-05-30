package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

/**
 * Outline of possible value states.
 */
public sealed interface Value {
    @Nullable
    Type type();

    @NotNull
    Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException;

    @Nullable
    default String valueAsString() {
        return null;
    }

    default boolean isKnown() {
        return false;
    }

    sealed interface PrimitiveValue extends Value {
        @Override
        @NotNull
        Type type();

        default boolean isWide() {
            return JvmTypeUtils.isWide(type());
        }

        default boolean isReserved() {
            return type().equals(JvmTypeUtils.VOID);
        }

        @Override
        @NotNull
        default Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other))
                return this;
            if (other instanceof PrimitiveValue primitiveValue)
                return Values.valueOfPrimitive(JvmTypeUtils.commonType(checker, type(), primitiveValue.type()));
            throw new ValueMergeException("Cannot merge primitive with non-primitive");
        }

        @NotNull
        default PrimitiveValue cast(Type type) {
            if (type().equals(type)) return this;
            return Values.valueOfPrimitive(type);
        }

        @NotNull
        PrimitiveValue negate();
    }

    sealed interface IntValue extends PrimitiveValue {
        @Override
        @NotNull
        default Type type() {
            return JvmTypeUtils.INT;
        }
    }

    record KnownIntValue(int value) implements IntValue {
        @Override
        public @NotNull PrimitiveValue cast(Type type) {
            return switch (type.getSort()) {
                case Type.BOOLEAN -> Values.valueOf(value == 1);
                case Type.BYTE -> Values.valueOf((byte) value);
                case Type.CHAR -> Values.valueOf((char) value);
                case Type.SHORT -> Values.valueOf((short) value);
                case Type.INT -> this;
                case Type.FLOAT -> Values.valueOf((float) value);
                case Type.LONG -> Values.valueOf((long) value);
                case Type.DOUBLE -> Values.valueOf((double) value);
                case Type.VOID -> throw new IllegalStateException("Cannot cast to void");
                default -> throw new IllegalStateException("Unknown primitive type: " + type.getDescriptor());
            };
        }

        @Override
        public @NotNull PrimitiveValue negate() {
            return Values.valueOf(-value);
        }

        @Override
        public @NotNull String valueAsString() {
            return String.valueOf(value);
        }

        @Override
        public boolean isKnown() {
            return true;
        }
    }

    record UnknownIntValue() implements IntValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    sealed interface FloatValue extends PrimitiveValue {
        @Override
        @NotNull
        default Type type() {
            return JvmTypeUtils.FLOAT;
        }
    }

    record KnownFloatValue(float value) implements FloatValue {
        @Override
        public @NotNull PrimitiveValue cast(Type type) {
            return switch (type.getSort()) {
                case Type.BOOLEAN -> Values.valueOf(value == 1);
                case Type.BYTE -> Values.valueOf((byte) value);
                case Type.CHAR -> Values.valueOf((char) value);
                case Type.SHORT -> Values.valueOf((short) value);
                case Type.INT -> Values.valueOf((int) value);
                case Type.FLOAT -> this;
                case Type.LONG -> Values.valueOf((long) value);
                case Type.DOUBLE -> Values.valueOf((double) value);
                case Type.VOID -> throw new IllegalStateException("Cannot cast to void");
                default -> throw new IllegalStateException("Unknown primitive type: " + type.getDescriptor());
            };
        }

        @Override
        public @NotNull PrimitiveValue negate() {
            return Values.valueOf(-value);
        }

        @Override
        public @NotNull String valueAsString() {
            return String.valueOf(value);
        }

        @Override
        public boolean isKnown() {
            return true;
        }
    }

    record UnknownFloatValue() implements FloatValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    sealed interface LongValue extends PrimitiveValue {
        @Override
        @NotNull
        default Type type() {
            return JvmTypeUtils.LONG;
        }
    }

    record KnownLongValue(long value) implements LongValue {
        @Override
        public @NotNull PrimitiveValue cast(Type type) {
            return switch (type.getSort()) {
                case Type.BOOLEAN -> Values.valueOf(value == 1);
                case Type.BYTE -> Values.valueOf((byte) value);
                case Type.CHAR -> Values.valueOf((char) value);
                case Type.SHORT -> Values.valueOf((short) value);
                case Type.INT -> Values.valueOf((int) value);
                case Type.FLOAT -> Values.valueOf((float) value);
                case Type.LONG -> this;
                case Type.DOUBLE -> Values.valueOf((double) value);
                case Type.VOID -> throw new IllegalStateException("Cannot cast to void");
                default -> throw new IllegalStateException("Unknown primitive type: " + type.getDescriptor());
            };
        }

        @Override
        public @NotNull PrimitiveValue negate() {
            return Values.valueOf(-value);
        }

        @Override
        public @NotNull String valueAsString() {
            return String.valueOf(value);
        }

        @Override
        public boolean isKnown() {
            return true;
        }
    }

    record UnknownLongValue() implements LongValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    sealed interface DoubleValue extends PrimitiveValue {
        @Override
        @NotNull
        default Type type() {
            return JvmTypeUtils.DOUBLE;
        }
    }

    record KnownDoubleValue(double value) implements DoubleValue {
        @Override
        public @NotNull PrimitiveValue cast(Type type) {
            return switch (type.getSort()) {
                case Type.BOOLEAN -> Values.valueOf(value == 1);
                case Type.BYTE -> Values.valueOf((byte) value);
                case Type.CHAR -> Values.valueOf((char) value);
                case Type.SHORT -> Values.valueOf((short) value);
                case Type.INT -> Values.valueOf((int) value);
                case Type.FLOAT -> Values.valueOf((float) value);
                case Type.LONG -> Values.valueOf((long) value);
                case Type.DOUBLE -> this;
                case Type.VOID -> throw new IllegalStateException("Cannot cast to void");
                default -> throw new IllegalStateException("Unknown primitive type: " + type.getDescriptor());
            };
        }

        @Override
        public @NotNull PrimitiveValue negate() {
            return Values.valueOf(-value);
        }

        @Override
        public @NotNull String valueAsString() {
            return String.valueOf(value);
        }

        @Override
        public boolean isKnown() {
            return true;
        }
    }

    record UnknownDoubleValue() implements DoubleValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    record VoidValue() implements ObjectValue {
        @Override
        public @NotNull Type type() {
            return JvmTypeUtils.BOX_VOID;
        }

        @Override
        public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other))
                return this;
            throw new ValueMergeException("Invalid void (top) merge");
        }
    }

    non-sealed interface ObjectValue extends Value {
        @Override
        @Nullable
        Type type();

        @Override
        default @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other) || other instanceof NullValue)
                return this;
            if (other instanceof ObjectValue objectValue) {
                Type thisType = type();
                Type otherType = objectValue.type();
                String commonSuperclass = checker.getCommonSuperclass(
                        JvmTypeUtils.internalName(thisType),
                        JvmTypeUtils.internalName(otherType)
                );
                return Values.valueOfInstance(
                        commonSuperclass == null ? JvmTypeUtils.OBJECT : Type.getObjectType(commonSuperclass)
                );
            }
            throw new ValueMergeException("Invalid merge of object and non-object value");
        }
    }

    record NullValue() implements ObjectValue {
        @Override
        public @Nullable Type type() {
            return null;
        }

        @Override
        public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other))
                return this;
            if (other instanceof ObjectValue)
                return other;
            throw new ValueMergeException("Invalid merge of 'null' and non-object value");
        }

        @Override
        public @NotNull String valueAsString() {
            return "null";
        }

        @Override
        public boolean isKnown() {
            return true;
        }
    }

    interface ArrayValue extends ObjectValue {
        @NotNull
        Type arrayType();

        @Override
        default @NotNull Type type() {
            return arrayType();
        }

        @Override
        default @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other))
                return this;
            if (other instanceof ObjectValue)
                return Values.OBJECT_VALUE;
            throw new ValueMergeException("Invalid array merge with non-object value");
        }
    }

    record UnknownLengthArrayValue(@NotNull Type arrayType) implements ArrayValue {
    }

    record KnownLengthArrayValue(@NotNull Type arrayType, int length) implements ArrayValue {
        @Override
        public boolean isKnown() {
            return true;
        }

        @Override
        public @NotNull String valueAsString() {
            return "Length: " + length;
        }
    }

    record KnownStringValue(@NotNull String value) implements ObjectValue {
        @Override
        public @NotNull Type type() {
            return JvmTypeUtils.STRING;
        }

        @Override
        public @NotNull String valueAsString() {
            return value;
        }

        @Override
        public boolean isKnown() {
            return true;
        }
    }

    record UnknownObjectValue(@NotNull Type type) implements ObjectValue {
    }
}

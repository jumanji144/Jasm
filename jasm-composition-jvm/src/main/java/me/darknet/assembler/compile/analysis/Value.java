package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.*;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outline of possible value states.
 */
public sealed interface Value {
    @Nullable
    ClassType type();

    @NotNull
    Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException;

    @Nullable
    default String valueAsString() {
        return null;
    }

    default boolean isKnown() { return false; }

    /** Value of primitive content */
    sealed interface PrimitiveValue extends Value {
        @Override
        @NotNull
        PrimitiveType type();

        @Override
        @NotNull
        default Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other))
                return this;
            if (other instanceof PrimitiveValue primitiveValue)
                return Values.valueOfPrimitive(type().widen(primitiveValue.type()));
            throw new ValueMergeException("Cannot merge primitive with non-primitive");
        }

        @NotNull
        default PrimitiveValue cast(PrimitiveType type) {
            return Values.valueOfPrimitive(type);
        }

        @NotNull
        PrimitiveValue negate();
    }

    /** Value of int content. */
    sealed interface IntValue extends PrimitiveValue {
        @Override
        @NotNull
        default PrimitiveType type() {
            return Types.INT;
        }
    }

    /**
     * Value of known int content.
     *
     * @param value
     *              Literal
     */
    record KnownIntValue(int value) implements IntValue {
        @Override
        public @NotNull PrimitiveValue cast(PrimitiveType type) {
            return switch (type.kind()) {
                case PrimitiveKind.T_BOOLEAN -> Values.valueOf(value == 1);
                case PrimitiveKind.T_BYTE -> Values.valueOf((byte) value);
                case PrimitiveKind.T_CHAR -> Values.valueOf((char) value);
                case PrimitiveKind.T_SHORT -> Values.valueOf((short) value);
                case PrimitiveKind.T_INT -> this;
                case PrimitiveKind.T_FLOAT -> Values.valueOf((float) value);
                case PrimitiveKind.T_LONG -> Values.valueOf((long) value);
                case PrimitiveKind.T_DOUBLE -> Values.valueOf((double) value);
                case PrimitiveKind.T_VOID -> throw new IllegalStateException("Cannot cast to void");
                default -> throw new IllegalStateException("Unknown primitive type: " + type.descriptor());
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

    /** Value of unknown int content. */
    record UnnownIntValue() implements IntValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    /** Value of float content. */
    sealed interface FloatValue extends PrimitiveValue {
        @Override
        @NotNull
        default PrimitiveType type() {
            return Types.FLOAT;
        }
    }

    /**
     * Value of known float content.
     *
     * @param value
     *              Literal
     */
    record KnownFloatValue(float value) implements FloatValue {
        @Override
        public @NotNull PrimitiveValue cast(PrimitiveType type) {
            return switch (type.kind()) {
                case PrimitiveKind.T_BOOLEAN -> Values.valueOf(value == 1);
                case PrimitiveKind.T_BYTE -> Values.valueOf((byte) value);
                case PrimitiveKind.T_CHAR -> Values.valueOf((char) value);
                case PrimitiveKind.T_SHORT -> Values.valueOf((short) value);
                case PrimitiveKind.T_INT -> Values.valueOf((int) value);
                case PrimitiveKind.T_FLOAT -> this;
                case PrimitiveKind.T_LONG -> Values.valueOf((long) value);
                case PrimitiveKind.T_DOUBLE -> Values.valueOf((double) value);
                case PrimitiveKind.T_VOID -> throw new IllegalStateException("Cannot cast to void");
                default -> throw new IllegalStateException("Unknown primitive type: " + type.descriptor());
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

    /** Value of unknown float content. */
    record UnknownFloatValue() implements FloatValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    /** Value of long content. */
    sealed interface LongValue extends PrimitiveValue {
        @Override
        @NotNull
        default PrimitiveType type() {
            return Types.LONG;
        }
    }

    /**
     * Value of known long content.
     *
     * @param value
     *              Literal
     */
    record KnownLongValue(long value) implements LongValue {
        @Override
        public @NotNull PrimitiveValue cast(PrimitiveType type) {
            return switch (type.kind()) {
                case PrimitiveKind.T_BOOLEAN -> Values.valueOf(value == 1);
                case PrimitiveKind.T_BYTE -> Values.valueOf((byte) value);
                case PrimitiveKind.T_CHAR -> Values.valueOf((char) value);
                case PrimitiveKind.T_SHORT -> Values.valueOf((short) value);
                case PrimitiveKind.T_INT -> Values.valueOf((int) value);
                case PrimitiveKind.T_FLOAT -> Values.valueOf((float) value);
                case PrimitiveKind.T_LONG -> this;
                case PrimitiveKind.T_DOUBLE -> Values.valueOf((double) value);
                case PrimitiveKind.T_VOID -> throw new IllegalStateException("Cannot cast to void");
                default -> throw new IllegalStateException("Unknown primitive type: " + type.descriptor());
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

    /** Value of unknown long content. */
    record UnknownLongValue() implements LongValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    /** Value of double content. */
    sealed interface DoubleValue extends PrimitiveValue {
        @Override
        @NotNull
        default PrimitiveType type() {
            return Types.DOUBLE;
        }
    }

    /**
     * Value of known double content.
     *
     * @param value
     *              Literal
     */
    record KnownDoubleValue(double value) implements DoubleValue {
        @Override
        public @NotNull PrimitiveValue cast(PrimitiveType type) {
            return switch (type.kind()) {
                case PrimitiveKind.T_BOOLEAN -> Values.valueOf(value == 1);
                case PrimitiveKind.T_BYTE -> Values.valueOf((byte) value);
                case PrimitiveKind.T_CHAR -> Values.valueOf((char) value);
                case PrimitiveKind.T_SHORT -> Values.valueOf((short) value);
                case PrimitiveKind.T_INT -> Values.valueOf((int) value);
                case PrimitiveKind.T_FLOAT -> Values.valueOf((float) value);
                case PrimitiveKind.T_LONG -> Values.valueOf((long) value);
                case PrimitiveKind.T_DOUBLE -> this;
                case PrimitiveKind.T_VOID -> throw new IllegalStateException("Cannot cast to void");
                default -> throw new IllegalStateException("Unknown primitive type: " + type.descriptor());
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

    /** Value of unknown double content. */
    record UnknownDoubleValue() implements DoubleValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    /** Value of a void, used for padding spaces after wide types */
    record VoidValue() implements ObjectValue {
        @Override
        public @NotNull ObjectType type() {
            return Types.BOX_VOID;
        }

        @Override
        public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other))
                return this;
            throw new ValueMergeException("Invalid void (top) merge");
        }
    }

    /** Value of object content. */
    sealed interface ObjectValue extends Value {
        /**
         * @return Value's type. Can be {@code null} for the {@link NullValue} subtype.
         */
        @Nullable
        ObjectType type();

        @Override
        @SuppressWarnings("DataFlowIssue") // Null warnings in the 2nd block isn't an issue, caught in 1st block.
        default @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other) || other instanceof NullValue)
                return this;
            if (other instanceof ObjectValue objectValue) {
                String type1 = type().internalName();
                String type2 = objectValue.type().internalName();
                String commonSuperclass = checker.getCommonSuperclass(type1, type2);
                return Values.valueOfInstance(Types.instanceTypeFromInternalName(commonSuperclass));
            }
            throw new ValueMergeException("Invalid merge of object and non-object value");
        }
    }

    /** Value of null object content. */
    record NullValue() implements ObjectValue {
        @Override
        public @Nullable ObjectType type() {
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

    /**
     * Value of T[] content.
     *
     * @param arrayType
     *                  More specific declared type than {@link ObjectValue#type()}.
     */
    record ArrayValue(@NotNull ArrayType arrayType) implements ObjectValue {
        @Override
        public @NotNull ObjectType type() {
            return arrayType();
        }

        @Override
        public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other))
                return this;
            if (other instanceof ObjectValue)
                return Values.OBJECT_VALUE;
            throw new ValueMergeException("Invalid array merge with non-object value");
        }
    }

    /**
     * Value of {@link String} content.
     *
     * @param value
     *              Known string value.
     */
    record KnownStringValue(@NotNull String value) implements ObjectValue {
        @Override
        public @NotNull ObjectType type() {
            return Types.STRING;
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

    /** Value of unknown object content. */
    record UnknownObjectValue(@NotNull ObjectType type) implements ObjectValue {
    }
}
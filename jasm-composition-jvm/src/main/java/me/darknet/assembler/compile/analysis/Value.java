package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.*;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outline of possible value states.
 */
public sealed interface Value {
    @NotNull
    ClassType type();

    @NotNull
    Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other);

    @Nullable
    default String valueAsString() {
        return null;
    }

    /** Value of primitive content */
    sealed interface PrimitiveValue extends Value {
        @Override
        @NotNull
        PrimitiveType type();

        @Override
        @NotNull
        default Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) {
            if (equals(other))
                return this;
            if (other instanceof PrimitiveValue primitiveValue)
                return Values.valueOfPrimitive(type().widen(primitiveValue.type()));
            throw new IllegalStateException("Cannot merge primitive with non-primitive");
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
        public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) {
            if (equals(other))
                return this;
            throw new IllegalStateException("Invalid void (top) merge");
        }
    }

    /** Value of object content. */
    sealed interface ObjectValue extends Value {
        /**
         * @return Value's type.
         */
        @NotNull
        ObjectType type();

        @Override
        default @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) {
            if (equals(other) || other instanceof NullValue)
                return this;
            if (other instanceof ObjectValue objectValue) {
                String commonSuperclass = checker
                        .getCommonSuperclass(type().internalName(), objectValue.type().internalName());
                return Values.valueOfInstance(Types.instanceTypeFromInternalName(commonSuperclass));
            }
            throw new IllegalStateException("Invalid object merge");
        }
    }

    /** Value of null object content. */
    record NullValue() implements ObjectValue {
        @Override
        public @NotNull ObjectType type() {
            return AnalysisUtils.NULL;
        }

        @Override
        public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) {
            if (equals(other))
                return this;
            return other;
        }

        @Override
        public @NotNull String valueAsString() {
            return "null";
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
        public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) {
            if (equals(other))
                return this;
            if (other instanceof ObjectValue)
                return Values.OBJECT_VALUE;
            throw new IllegalStateException("Invalid array merge");
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
    }

    /** Value of unknown object content. */
    record UnknownObjectValue(@NotNull ObjectType type) implements ObjectValue {
    }
}
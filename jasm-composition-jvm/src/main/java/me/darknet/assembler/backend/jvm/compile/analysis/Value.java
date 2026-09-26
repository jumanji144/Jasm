package me.darknet.assembler.backend.jvm.compile.analysis;

import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.backend.jvm.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

/**
 * Outline of possible value states.
 */
public sealed interface Value {
    /**
     * @return Verifier type, or {@code null} for the known null reference.
     */
    @Nullable
    Type type();

    /**
     * Merges this value with another value at a control-flow join.
     *
     * @param checker
     * 		Inheritance checker used for reference types.
     * @param other
     * 		Value from the other control-flow path.
     *
     * @return Merged verifier value.
     *
     * @throws ValueMergeException
     * 		If the values cannot be merged safely.
     */
    @NotNull
    Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException;

    /**
     * @return Value text, or {@code null} when no display form is available.
     */
    @Nullable
    default String valueAsString() {
        return null;
    }

    /**
     * @return {@code true} if the value has known constant contents, {@code false} otherwise.
     */
    default boolean isKnown() {
        return false;
    }

    /**
     * Verifier {@code TOP}/undefined value. This is distinct from a known {@link NullValue}.
     */
    record TopValue() implements Value {
        @Override
        public @NotNull Type type() {
            return JvmTypeUtils.TOP;
        }

        @Override
        public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) {
            return this;
        }
    }

    /**
     * Verifier uninitialized reference produced by {@code NEW} or used as constructor {@code this}.
     *
     * @param marker
     * 		Verifier type marker identifying this allocation.
     * @param owner
     * 		Type whose instance is represented by the uninitialized reference.
     */
    record UninitializedObjectValue(@NotNull Type marker, @NotNull Type owner) implements ObjectValue {
        @Override
        public @NotNull Type type() {
            return marker;
        }

        @Override
        public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) {
            if (equals(other))
                return this;

            // Distinct uninitialized allocation identities merge to verifier TOP.
            // They cannot be treated as an initialized common reference.
            return Values.TOP_VALUE;
        }
    }

    /**
     * Verifier value for a JVM primitive type, with operations for primitive-category checks, casting, and
     * negation.
     */
    sealed interface PrimitiveValue extends Value {
        /**
         * @return Primitive verifier type.
         */
        @Override
        @NotNull
        Type type();

        /**
         * @return {@code true} if the primitive is a wide type, {@code false} otherwise.
         */
        default boolean isWide() {
            return JvmTypeUtils.isWide(type());
        }

        /**
         * @return {@code true} if the primitive is a wode-reserved type, {@code false} otherwise.
         */
        default boolean isReserved() {
            return type().equals(JvmTypeUtils.VOID);
        }

        /**
         * Merges compatible primitive verifier values.
         *
         * @param checker
         * 		Inheritance checker; unused for primitive values.
         * @param other
         * 		Value from the other control-flow path.
         *
         * @return Merged primitive value.
         *
         * @throws ValueMergeException
         * 		If the other value is not a compatible primitive.
         */
        @Override
        @NotNull
        default Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other))
                return this;
            if (other instanceof PrimitiveValue primitiveValue)
                if (JvmTypeUtils.verificationType(type()).equals(JvmTypeUtils.verificationType(primitiveValue.type())))
                    return Values.valueOfPrimitive(JvmTypeUtils.verificationType(type()));
            throw new ValueMergeException("Cannot merge primitive with non-primitive");
        }

        /**
         * Casts this primitive value to a target primitive type.
         *
         * @param type
         * 		Target primitive type.
         *
         * @return Value represented in the target type.
         */
        @NotNull
        default PrimitiveValue cast(Type type) {
            if (type().equals(type)) return this;
            return Values.valueOfPrimitive(type);
        }

        /**
         * @return Negated primitive value.
         */
        @NotNull
        PrimitiveValue negate();
    }

    /**
     * Primitive verifier values represented by the JVM {@code int} category.
     */
    sealed interface IntValue extends PrimitiveValue {
        /**
         * @return The JVM {@code int} type.
         */
        @Override
        @NotNull
        default Type type() {
            return JvmTypeUtils.INT;
        }
    }

    /**
     * Int-category verifier value whose constant {@code int} content is known.
     *
     * @param value
     * 		Known {@code int} content.
     */
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

    /**
     * Int-category verifier value whose {@code int} content is not determinable by the analysis.
     */
    record UnknownIntValue() implements IntValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    /**
     * Primitive verifier values represented by the JVM {@code float} category.
     */
    sealed interface FloatValue extends PrimitiveValue {
        /**
         * @return The JVM {@code float} type.
         */
        @Override
        @NotNull
        default Type type() {
            return JvmTypeUtils.FLOAT;
        }
    }

    /**
     * Float-category verifier value whose constant floating-point content is known.
     *
     * @param value
     * 		Known {@code float} content.
     */
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

    /**
     * Float-category verifier value whose floating-point content is not determinable by the analysis.
     */
    record UnknownFloatValue() implements FloatValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    /**
     * Primitive verifier values represented by the JVM {@code long} category.
     */
    sealed interface LongValue extends PrimitiveValue {
        /**
         * @return The JVM {@code long} type.
         */
        @Override
        @NotNull
        default Type type() {
            return JvmTypeUtils.LONG;
        }
    }

    /**
     * Long-category verifier value whose constant {@code long} content is known.
     *
     * @param value
     * 		Known {@code long} content.
     */
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

    /**
     * Long-category verifier value whose {@code long} content is not determinable by the analysis.
     */
    record UnknownLongValue() implements LongValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    /**
     * Primitive verifier values represented by the JVM {@code double} category.
     */
    sealed interface DoubleValue extends PrimitiveValue {
        /**
         * @return The JVM {@code double} type.
         */
        @Override
        @NotNull
        default Type type() {
            return JvmTypeUtils.DOUBLE;
        }
    }

    /**
     * Double-category verifier value whose constant floating-point content is known.
     *
     * @param value
     * 		Known {@code double} content.
     */
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

    /**
     * Double-category verifier value whose floating-point content is not determinable by the analysis.
     */
    record UnknownDoubleValue() implements DoubleValue {
        @Override
        public @NotNull PrimitiveValue negate() {
            return this;
        }
    }

    /**
     * Verifier marker for the second slot of a category-2 value and for {@code void}.
     */
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

    /**
     * Verifier value treated as an object or reference, whose {@link #type()} may be {@code null} for the null
     * reference.
     */
    non-sealed interface ObjectValue extends Value {
        /**
         * @return Reference type, or {@code null} for the known null reference.
         */
        @Override
        @Nullable
        Type type();

        /**
         * Merges object verifier values using their common superclass.
         *
         * @param checker
         * 		Inheritance checker used to resolve common supertypes.
         * @param other
         * 		Value from the other control-flow path.
         *
         * @return Merged object value.
         *
         * @throws ValueMergeException
         * 		If the other value is not an object value.
         */
        @Override
        default @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other) || other instanceof NullValue)
                return this;
			if (other instanceof UninitializedObjectValue)
				return Values.TOP_VALUE;
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

    /**
     * Verifier value representing the known {@code null} reference.
     */
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

    /**
     * Verifier value representing an array reference and exposing its array type through {@link #arrayType()}.
     */
    interface ArrayValue extends ObjectValue {
        /**
         * @return Array verifier type.
         */
        @NotNull
        Type arrayType();

        /**
         * @return Array verifier type.
         */
        @Override
        default @NotNull Type type() {
            return arrayType();
        }

        /**
         * Merges array verifier values using their common array type.
         *
         * @param checker
         * 		Inheritance checker used to resolve common supertypes.
         * @param other
         * 		Value from the other control-flow path.
         *
         * @return Merged array or object value.
         *
         * @throws ValueMergeException
         * 		If the other value is not an object value.
         */
        @Override
        default @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
            if (equals(other))
                return this;
            if (other instanceof ArrayValue otherArray) {
				Type common = JvmTypeUtils.commonType(checker, arrayType(), otherArray.arrayType());
				if (common != null && common.getSort() == Type.ARRAY)
					return Values.valueOfArray(common);
				return Values.OBJECT_VALUE;
            }
			if (other instanceof UninitializedObjectValue)
				return Values.TOP_VALUE;
            if (other instanceof ObjectValue)
                return Values.OBJECT_VALUE;
            throw new ValueMergeException("Invalid array merge with non-object value");
        }
    }

    /**
     * Array value with a known array type but an element count that is not determinable by the analysis.
     *
     * @param arrayType
     * 		Array type represented by this value.
     */
    record UnknownLengthArrayValue(@NotNull Type arrayType) implements ArrayValue {
    }

    /**
     * Array value whose array type and element count are known to the analysis.
     *
     * @param arrayType
     * 		Array type represented by this value.
     * @param length
     * 		Known number of elements.
     */
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

    /**
     * Object value for a known string constant.
     *
     * @param value
     * 		Known string content.
     */
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

    /**
     * Object value with a known reference type but no determinable constant contents.
     *
     * @param type
     * 		Reference type represented by this value.
     */
    record UnknownObjectValue(@NotNull Type type) implements ObjectValue {
    }
}

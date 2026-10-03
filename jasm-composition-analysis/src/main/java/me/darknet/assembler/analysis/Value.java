package me.darknet.assembler.analysis;

import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.assembler.descriptor.PrimitiveType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outline of a value in JVM or Dalvik analysis.
 */
public sealed interface Value permits Value.TopValue, Value.PrimitiveValue, Value.ObjectValue, Value.BackendMarker {
	/**
	 * @return Type of the value, or {@code null} if unknown.
	 */
	@Nullable DescriptorType type();

	/**
	 * @return String representation of the value, or {@code null} if unknown.
	 */
	@Nullable
	default String valueAsString() {return null;}

	/**
	 * @return {@code true} if the value is known, {@code false} otherwise.
	 */
	default boolean isKnown() {return false;}

	/**
	 * Marker interface for values that are only used in backend analysis.
	 */
	non-sealed interface BackendMarker extends Value {
		@Override
		default @Nullable DescriptorType type() {return null;}
	}

	/**
	 * Marker value for the top <i>(See JVMS chapter 4 - "Verification Type System" for context)</i> of the stack.
	 */
	record TopValue() implements Value {
		@Override
		public @Nullable DescriptorType type() {return null;}
	}

	/**
	 * Outline of an uninitialized reference value.
	 */
	record UninitializedReferenceValue(@NotNull ClassDescriptor owner, int allocationIdentity) implements ObjectValue {
		@Override
		public @NotNull DescriptorType type() {return owner;}
	}

	/**
	 * Outline of a primitive value.
	 */
	sealed interface PrimitiveValue extends Value permits IntValue, FloatValue, LongValue, DoubleValue {
		@Override
		@NotNull PrimitiveType type();

		/**
		 * @return {@code true} if the value is a wide primitive type ({@code long} or {@code double}), {@code false} otherwise.
		 */
		default boolean isWide() {
			return type() == PrimitiveType.LONG || type() == PrimitiveType.DOUBLE;
		}

		/**
		 * Casts the value to the given primitive type.
		 *
		 * @param targetType
		 * 		Target primitive type.
		 *
		 * @return Value of the target primitive type. Self if the value is already of the target type.
		 */
		default @NotNull PrimitiveValue cast(@NotNull PrimitiveType targetType) {
			if (targetType == PrimitiveType.VOID)
				throw new IllegalStateException("Illegal value of void type");
			return type() == targetType ? this : Values.valueOfPrimitive(targetType);
		}

		/**
		 * @return Negated value. Self if unknown.
		 */
		@NotNull PrimitiveValue negate();
	}

	/**
	 * Outline of an integer value.
	 */
	sealed interface IntValue extends PrimitiveValue permits KnownIntValue, UnknownIntValue {
		@Override
		default @NotNull PrimitiveType type() {return PrimitiveType.INT;}
	}

	/**
	 * Known integer value.
	 *
	 * @param value
	 * 		Known integer value.
	 */
	record KnownIntValue(int value) implements IntValue {
		@Override
		public @NotNull PrimitiveValue cast(@NotNull PrimitiveType type) {
			return switch (type) {
				case BOOLEAN -> Values.valueOf(value == 1);
				case BYTE -> Values.valueOf((byte) value);
				case CHAR -> Values.valueOf((char) value);
				case SHORT -> Values.valueOf((short) value);
				case INT -> this;
				case FLOAT -> Values.valueOf((float) value);
				case LONG -> Values.valueOf((long) value);
				case DOUBLE -> Values.valueOf((double) value);
				case VOID -> throw new IllegalStateException("Cannot cast to void");
			};
		}

		@Override
		public @NotNull PrimitiveValue negate() {return Values.valueOf(-value);}

		@Override
		public @NotNull String valueAsString() {return String.valueOf(value);}

		@Override
		public boolean isKnown() {return true;}
	}

	/**
	 * Unknown integer value.
	 */
	record UnknownIntValue() implements IntValue {
		@Override
		public @NotNull PrimitiveValue negate() {return this;}
	}

	/**
	 * Outline of a float value.
	 */
	sealed interface FloatValue extends PrimitiveValue permits KnownFloatValue, UnknownFloatValue {
		@Override
		default @NotNull PrimitiveType type() {return PrimitiveType.FLOAT;}
	}

	/**
	 * Known float value.
	 *
	 * @param value
	 * 		Known float value.
	 */
	record KnownFloatValue(float value) implements FloatValue {
		@Override
		public @NotNull PrimitiveValue cast(@NotNull PrimitiveType type) {
			return switch (type) {
				case BOOLEAN -> Values.valueOf(value == 1);
				case BYTE -> Values.valueOf((byte) value);
				case CHAR -> Values.valueOf((char) value);
				case SHORT -> Values.valueOf((short) value);
				case INT -> Values.valueOf((int) value);
				case FLOAT -> this;
				case LONG -> Values.valueOf((long) value);
				case DOUBLE -> Values.valueOf((double) value);
				case VOID -> throw new IllegalStateException("Cannot cast to void");
			};
		}

		@Override
		public @NotNull PrimitiveValue negate() {return Values.valueOf(-value);}

		@Override
		public @NotNull String valueAsString() {return String.valueOf(value);}

		@Override
		public boolean isKnown() {return true;}
	}

	/**
	 * Unknown float value.
	 */
	record UnknownFloatValue() implements FloatValue {
		@Override
		public @NotNull PrimitiveValue negate() {return this;}
	}

	/**
	 * Outline of a long value.
	 */
	sealed interface LongValue extends PrimitiveValue permits KnownLongValue, UnknownLongValue {
		@Override
		default @NotNull PrimitiveType type() {return PrimitiveType.LONG;}
	}

	/**
	 * Known long value.
	 *
	 * @param value
	 * 		Known long value.
	 */
	record KnownLongValue(long value) implements LongValue {
		@Override
		public @NotNull PrimitiveValue cast(@NotNull PrimitiveType type) {
			return switch (type) {
				case BOOLEAN -> Values.valueOf(value == 1);
				case BYTE -> Values.valueOf((byte) value);
				case CHAR -> Values.valueOf((char) value);
				case SHORT -> Values.valueOf((short) value);
				case INT -> Values.valueOf((int) value);
				case FLOAT -> Values.valueOf((float) value);
				case LONG -> this;
				case DOUBLE -> Values.valueOf((double) value);
				case VOID -> throw new IllegalStateException("Cannot cast to void");
			};
		}

		@Override
		public @NotNull PrimitiveValue negate() {return Values.valueOf(-value);}

		@Override
		public @NotNull String valueAsString() {return String.valueOf(value);}

		@Override
		public boolean isKnown() {return true;}
	}

	/**
	 * Unknown long value.
	 */
	record UnknownLongValue() implements LongValue {
		@Override
		public @NotNull PrimitiveValue negate() {return this;}
	}

	/**
	 * Outline of a double value.
	 */
	sealed interface DoubleValue extends PrimitiveValue permits KnownDoubleValue, UnknownDoubleValue {
		@Override
		default @NotNull PrimitiveType type() {return PrimitiveType.DOUBLE;}
	}

	/**
	 * Known double value.
	 *
	 * @param value
	 * 		Known double value.
	 */
	record KnownDoubleValue(double value) implements DoubleValue {
		@Override
		public @NotNull PrimitiveValue cast(@NotNull PrimitiveType type) {
			return switch (type) {
				case BOOLEAN -> Values.valueOf(value == 1);
				case BYTE -> Values.valueOf((byte) value);
				case CHAR -> Values.valueOf((char) value);
				case SHORT -> Values.valueOf((short) value);
				case INT -> Values.valueOf((int) value);
				case FLOAT -> Values.valueOf((float) value);
				case LONG -> Values.valueOf((long) value);
				case DOUBLE -> this;
				case VOID -> throw new IllegalStateException("Cannot cast to void");
			};
		}

		@Override
		public @NotNull PrimitiveValue negate() {return Values.valueOf(-value);}

		@Override
		public @NotNull String valueAsString() {return String.valueOf(value);}

		@Override
		public boolean isKnown() {return true;}
	}

	/**
	 * Unknown double value.
	 */
	record UnknownDoubleValue() implements DoubleValue {
		@Override
		public @NotNull PrimitiveValue negate() {return this;}
	}

	/**
	 * Outline of an object value.
	 */
	non-sealed interface ObjectValue extends Value {
		@Override
		@Nullable DescriptorType type();
	}

	/**
	 * Null constant value. For cases like {@code aconst_null}.
	 */
	record NullValue() implements ObjectValue {
		@Override
		public @Nullable DescriptorType type() {return null;}

		@Override
		public @NotNull String valueAsString() {return "null";}

		@Override
		public boolean isKnown() {return true;}
	}

	/**
	 * Outline of an array value.
	 */
	interface ArrayValue extends ObjectValue {
		/**
		 * @return Array type of the value.
		 */
		@NotNull ArrayDescriptor arrayType();

		@Override
		default @NotNull ArrayDescriptor type() {return arrayType();}
	}

	/**
	 * Unknown array value.
	 */
	record UnknownLengthArrayValue(@NotNull ArrayDescriptor arrayType) implements ArrayValue {}

	/**
	 * Known array value with a known length.
	 *
	 * @param arrayType
	 * 		Array type of the value.
	 * @param length
	 * 		Known length of the array.
	 */
	record KnownLengthArrayValue(@NotNull ArrayDescriptor arrayType, int length) implements ArrayValue {
		@Override
		public boolean isKnown() {return true;}

		@Override
		public @NotNull String valueAsString() {return "Length: " + length;}
	}

	/**
	 * Known string value.
	 *
	 * @param value
	 * 		Known string value.
	 */
	record KnownStringValue(@NotNull String value) implements ObjectValue {
		@Override
		public @NotNull ClassDescriptor type() {return Values.STRING_VALUE.type();}

		@Override
		public @NotNull String valueAsString() {return value;}

		@Override
		public boolean isKnown() {return true;}
	}

	/**
	 * Unknown object value.
	 *
	 * @param type
	 * 		Type of the object value.
	 */
	record UnknownObjectValue(@NotNull ClassDescriptor type) implements ObjectValue {}
}

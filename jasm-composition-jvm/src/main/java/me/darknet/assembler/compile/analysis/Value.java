package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ArrayType;
import dev.xdark.blw.type.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * Outline of possible value states.
 */
public sealed interface Value {
	/** Value of int content. */
	sealed interface IntValue extends Value {}
	/** Value of known int content. */
	non-sealed interface KnownIntValue extends IntValue {
		int value();
	}
	/** Value of unknown int content. */
	non-sealed interface UnnownIntValue extends IntValue {}
	/** Value of float content. */
	sealed interface FloatValue extends Value {}
	/** Value of known float content. */
	non-sealed interface KnownFloatValue extends FloatValue {
		float value();
	}
	/** Value of unknown float content. */
	non-sealed interface UnknownFloatValue extends FloatValue {}
	/** Value of long content. */
	sealed interface LongValue extends Value {}
	/** Value of known long content. */
	non-sealed interface KnownLongValue extends LongValue {
		long value();
	}
	/** Value of unknown long content. */
	non-sealed interface UnknownLongValue extends LongValue {}

	/** Value of double content. */
	sealed interface DoubleValue extends Value {}
	/** Value of known double content. */
	non-sealed interface KnownDoubleValue extends DoubleValue {
		double value();
	}
	/** Value of unknown double content. */
	non-sealed interface UnknownDoubleValue extends DoubleValue {}
	/** Value of object content <i>(Also covers arrays)</i>. */
	sealed interface ObjectValue extends Value {
		/**
		 * @return Value's type.
		 */
		@NotNull
		ObjectType type();
	}
	/** Value of null object content. */
	non-sealed interface NullValue extends ObjectValue {}
	/** Value of T[] content. */
	non-sealed interface ArrayValue extends ObjectValue {
		/**
		 * @return More specific declared type than {@link ObjectValue#type()}.
		 */
		@NotNull
		ArrayType arrayType();
	}
	/** Value of {@link String} content. */
	non-sealed interface StringValue extends ObjectValue {
		/**
		 * @return Known string value.
		 */
		@NotNull
		String value();
	}
	/** Value of unknown object content. */
	non-sealed interface UnknownObjectValue extends ObjectValue {}
}

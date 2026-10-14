package me.darknet.assembler.analysis;

import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.assembler.descriptor.PrimitiveType;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Common utils for {@link Value}
 */
public final class Values {
	private static final int NUM_INT_VALS = 128;
	private static final int NUM_LONG_VALS = 128;
	private static final Map<String, Value.UnknownLengthArrayValue> ARRAY_VALUES = new HashMap<>();
	private static final Map<String, Value.ObjectValue> INSTANCE_VALUES = new HashMap<>();
	private static final Map<String, Value.KnownStringValue> STRING_VALUES = new HashMap<>();
	public static final Value.KnownIntValue INT_M1 = new Value.KnownIntValue(-1);
	public static final Value.KnownIntValue INT_0 = new Value.KnownIntValue(0);
	public static final Value.KnownIntValue INT_1 = new Value.KnownIntValue(1);
	public static final Value.KnownIntValue INT_MIN = new Value.KnownIntValue(Integer.MIN_VALUE);
	public static final Value.KnownIntValue INT_MAX = new Value.KnownIntValue(Integer.MAX_VALUE);
	public static final Value.KnownLongValue LONG_M1 = new Value.KnownLongValue(-1);
	public static final Value.KnownLongValue LONG_MIN = new Value.KnownLongValue(Long.MIN_VALUE);
	public static final Value.KnownLongValue LONG_MAX = new Value.KnownLongValue(Long.MAX_VALUE);
	public static final Value.KnownFloatValue FLOAT_M1 = new Value.KnownFloatValue(-1);
	public static final Value.KnownFloatValue FLOAT_0 = new Value.KnownFloatValue(0);
	public static final Value.KnownFloatValue FLOAT_1 = new Value.KnownFloatValue(1);
	public static final Value.KnownFloatValue FLOAT_MIN = new Value.KnownFloatValue(Float.MIN_VALUE);
	public static final Value.KnownFloatValue FLOAT_MAX = new Value.KnownFloatValue(Float.MAX_VALUE);
	public static final Value.KnownFloatValue FLOAT_NAN = new Value.KnownFloatValue(Float.NaN);
	public static final Value.KnownDoubleValue DOUBLE_M1 = new Value.KnownDoubleValue(-1);
	public static final Value.KnownDoubleValue DOUBLE_0 = new Value.KnownDoubleValue(0);
	public static final Value.KnownDoubleValue DOUBLE_1 = new Value.KnownDoubleValue(1);
	public static final Value.KnownDoubleValue DOUBLE_MIN = new Value.KnownDoubleValue(Double.MIN_VALUE);
	public static final Value.KnownDoubleValue DOUBLE_MAX = new Value.KnownDoubleValue(Double.MAX_VALUE);
	public static final Value.KnownDoubleValue DOUBLE_NAN = new Value.KnownDoubleValue(Double.NaN);
	private static final Value.KnownIntValue[] INT_VALUES;
	private static final Value.KnownLongValue[] LONG_VALUES;
	public static final Value.IntValue INT_VALUE = new Value.UnknownIntValue();
	public static final Value.FloatValue FLOAT_VALUE = new Value.UnknownFloatValue();
	public static final Value.LongValue LONG_VALUE = new Value.UnknownLongValue();
	public static final Value.DoubleValue DOUBLE_VALUE = new Value.UnknownDoubleValue();
	public static final Value.UnknownObjectValue OBJECT_VALUE = new Value.UnknownObjectValue(new ClassDescriptor("java/lang/Object"));
	public static final Value.UnknownObjectValue STRING_VALUE = new Value.UnknownObjectValue(new ClassDescriptor("java/lang/String"));
	public static final Value.TopValue TOP_VALUE = new Value.TopValue();
	public static final Value.NullValue NULL_VALUE = new Value.NullValue();

	private Values() {}

	/**
	 * @param value
	 * 		Primitive value.
	 *
	 * @return {@link Value} representing the given primitive value.
	 */
	public static @NotNull Value.KnownIntValue valueOf(boolean value) {return value ? INT_1 : INT_0;}

	/**
	 * @param value
	 * 		Primitive value.
	 *
	 * @return {@link Value} representing the given primitive value.
	 */
	public static @NotNull Value.KnownIntValue valueOf(byte value) {return valueOf((int) value);}

	/**
	 * @param value
	 * 		Primitive value.
	 *
	 * @return {@link Value} representing the given primitive value.
	 */
	public static @NotNull Value.KnownIntValue valueOf(char value) {return valueOf((int) value);}

	/**
	 * @param value
	 * 		Primitive value.
	 *
	 * @return {@link Value} representing the given primitive value.
	 */
	public static @NotNull Value.KnownIntValue valueOf(short value) {return valueOf((int) value);}

	/**
	 * @param value
	 * 		Primitive value.
	 *
	 * @return {@link Value} representing the given primitive value.
	 */
	public static @NotNull Value.KnownIntValue valueOf(int value) {
		if (value >= 0 && value < NUM_INT_VALS) return INT_VALUES[value];
		if (value == -1) return INT_M1;
		if (value == Integer.MIN_VALUE) return INT_MIN;
		if (value == Integer.MAX_VALUE) return INT_MAX;
		return new Value.KnownIntValue(value);
	}

	/**
	 * @param value
	 * 		Primitive value.
	 *
	 * @return {@link Value} representing the given primitive value.
	 */
	public static @NotNull Value.KnownLongValue valueOf(long value) {
		if (value >= 0 && value < NUM_LONG_VALS) return LONG_VALUES[(int) value];
		if (value == -1) return LONG_M1;
		if (value == Long.MIN_VALUE) return LONG_MIN;
		if (value == Long.MAX_VALUE) return LONG_MAX;
		return new Value.KnownLongValue(value);
	}

	/**
	 * @param value
	 * 		Primitive value.
	 *
	 * @return {@link Value} representing the given primitive value.
	 */
	public static @NotNull Value.KnownFloatValue valueOf(float value) {
		if (value == 0) return FLOAT_0;
		if (value == 1) return FLOAT_1;
		if (value == -1) return FLOAT_M1;
		if (value == Float.MIN_VALUE) return FLOAT_MIN;
		if (value == Float.MAX_VALUE) return FLOAT_MAX;
		if (Float.isNaN(value)) return FLOAT_NAN;
		return new Value.KnownFloatValue(value);
	}

	/**
	 * @param value
	 * 		Primitive value.
	 *
	 * @return {@link Value} representing the given primitive value.
	 */
	public static @NotNull Value.KnownDoubleValue valueOf(double value) {
		if (value == 0) return DOUBLE_0;
		if (value == 1) return DOUBLE_1;
		if (value == -1) return DOUBLE_M1;
		if (value == Double.MIN_VALUE) return DOUBLE_MIN;
		if (value == Double.MAX_VALUE) return DOUBLE_MAX;
		if (Double.isNaN(value)) return DOUBLE_NAN;
		return new Value.KnownDoubleValue(value);
	}

	/**
	 * @param type
	 * 		Descriptor type.
	 *
	 * @return {@link Value} representing the given descriptor type.
	 *
	 * @throws IllegalArgumentException
	 * 		When the type is {@link PrimitiveType#VOID}.
	 */
	public static @NotNull Value valueOf(@NotNull DescriptorType type) {
		if (type instanceof PrimitiveType primitiveType) {
			if (primitiveType == PrimitiveType.VOID)
				throw new IllegalArgumentException("Void does not represent a value");
			return valueOfPrimitive(primitiveType);
		}
		if (type instanceof ArrayDescriptor arrayType)
			return valueOfArray(arrayType);
		return valueOfInstance((ClassDescriptor) type);
	}

	/**
	 * @param type
	 * 		Primitive type.
	 *
	 * @return {@link Value} representing the given primitive type.
	 *
	 * @throws IllegalArgumentException
	 * 		When the type is {@link PrimitiveType#VOID}.
	 */
	public static @NotNull Value.PrimitiveValue valueOfPrimitive(@NotNull PrimitiveType type) {
		return switch (type) {
			case BOOLEAN, BYTE, CHAR, SHORT, INT -> INT_VALUE;
			case FLOAT -> FLOAT_VALUE;
			case LONG -> LONG_VALUE;
			case DOUBLE -> DOUBLE_VALUE;
			case VOID -> throw new IllegalArgumentException("Void does not represent a value");
		};
	}

	/**
	 * @param arrayType
	 * 		Array descriptor type.
	 *
	 * @return {@link Value} representing the given array descriptor type.
	 */
	public static @NotNull Value.UnknownLengthArrayValue valueOfArray(@NotNull ArrayDescriptor arrayType) {
		String descriptor = arrayType.descriptor();
		Value.UnknownLengthArrayValue value = ARRAY_VALUES.get(descriptor);
		return value != null ? value : new Value.UnknownLengthArrayValue(arrayType);
	}

	/**
	 * @param arrayType
	 * 		Array descriptor type.
	 * @param length
	 * 		Length of the array.
	 *
	 * @return {@link Value} representing the given array descriptor type and length.
	 */
	public static @NotNull Value.KnownLengthArrayValue valueOfArray(@NotNull ArrayDescriptor arrayType, int length) {
		return new Value.KnownLengthArrayValue(arrayType, length);
	}

	/**
	 * @param instanceType
	 * 		Class descriptor type.
	 *
	 * @return {@link Value} representing the given class descriptor type.
	 */
	public static @NotNull Value.ObjectValue valueOfInstance(@NotNull ClassDescriptor instanceType) {
		Value.ObjectValue value = INSTANCE_VALUES.get(instanceType.internalName());
		return value != null ? value : new Value.UnknownObjectValue(instanceType);
	}

	/**
	 * @param content
	 * 		String content.
	 *
	 * @return {@link Value} representing the given string content.
	 */
	public static @NotNull Value.KnownStringValue valueOfString(@NotNull String content) {
		Value.KnownStringValue value = STRING_VALUES.get(content);
		return value != null ? value : new Value.KnownStringValue(content);
	}

	static {
		// Known values
		INT_VALUES = new Value.KnownIntValue[NUM_INT_VALS];
		LONG_VALUES = new Value.KnownLongValue[NUM_LONG_VALS];
		for (int i = 0; i < INT_VALUES.length; i++) INT_VALUES[i] = new Value.KnownIntValue(i);
		for (int i = 0; i < LONG_VALUES.length; i++) LONG_VALUES[i] = new Value.KnownLongValue(i);

		// Common strings
		for (String string : List.of("", " ", "  ", "   ", "    ", "\n", "\t", "\0", ",", ":", "-", "="))
			STRING_VALUES.put(string, new Value.KnownStringValue(string));

		// Primitive arrays
		List<PrimitiveType> primitives = List.of(PrimitiveType.BYTE, PrimitiveType.CHAR, PrimitiveType.FLOAT,
				PrimitiveType.DOUBLE, PrimitiveType.BOOLEAN, PrimitiveType.INT, PrimitiveType.LONG);
		for (PrimitiveType primitive : primitives) {
			ArrayDescriptor oneDimensional = new ArrayDescriptor(primitive);
			ArrayDescriptor twoDimensional = new ArrayDescriptor(oneDimensional);
			ARRAY_VALUES.put(oneDimensional.descriptor(), new Value.UnknownLengthArrayValue(oneDimensional));
			ARRAY_VALUES.put(twoDimensional.descriptor(), new Value.UnknownLengthArrayValue(twoDimensional));
		}

		// Instance types
		List<Class<?>> commonJdkTypes = List.of(
				// java.lang
				AutoCloseable.class, Boolean.class, Byte.class, Character.class, CharSequence.class, Class.class,
				ClassLoader.class, Cloneable.class, Comparable.class, Double.class, Enum.class, Exception.class,
				Float.class, IllegalStateException.class, Integer.class, Iterable.class, Long.class,
				NullPointerException.class, Object.class, Process.class, ProcessHandle.class, Short.class, String.class,
				StringBuffer.class, StringBuilder.class, Thread.class, Throwable.class, Void.class,

				// java.lang.reflect
				Constructor.class, Field.class, Member.class, Method.class, java.lang.reflect.Type.class,

				// java.io
				Closeable.class, DataInputStream.class, DataOutputStream.class, EOFException.class, File.class,
				FileInputStream.class, FileNotFoundException.class, FileOutputStream.class, InputStream.class,
				Serializable.class, OutputStream.class,

				// java.nio
				Buffer.class, ByteBuffer.class, Path.class, Charset.class,

				// java.net
				InetAddress.class, Inet4Address.class, Inet6Address.class, URI.class, URL.class,

				// java.util
				Collection.class, Comparator.class, Deque.class, Enumeration.class, EnumMap.class, EnumSet.class,
				HashMap.class, HashSet.class, Hashtable.class, IdentityHashMap.class, Iterator.class,
				LinkedHashMap.class, LinkedHashSet.class, LinkedList.class, List.class, ListIterator.class, Map.class,
				NavigableMap.class, NavigableSet.class, Optional.class, OptionalDouble.class, OptionalInt.class,
				OptionalLong.class, Properties.class, Queue.class, Random.class, RandomAccess.class, Set.class,
				SortedMap.class, SortedSet.class, Spliterator.class, Stack.class, TreeMap.class, TreeSet.class,
				UUID.class, Vector.class, WeakHashMap.class,

				// java.util.concurrent
				BlockingDeque.class, BlockingQueue.class, Callable.class, CompletableFuture.class,
				CompletionException.class, ConcurrentHashMap.class, ConcurrentMap.class, ConcurrentNavigableMap.class,
				Executor.class, ExecutorService.class, Flow.class, Future.class, ScheduledExecutorService.class,
				ScheduledFuture.class, TimeUnit.class
		);
		for (Class<?> type : commonJdkTypes) {
			ClassDescriptor descriptor = new ClassDescriptor(type.getName().replace('.', '/'));
			INSTANCE_VALUES.put(descriptor.internalName(), new Value.UnknownObjectValue(descriptor));
		}
	}
}

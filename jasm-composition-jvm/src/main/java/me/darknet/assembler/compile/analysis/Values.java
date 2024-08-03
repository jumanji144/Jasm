package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Common utils for {@link Value}
 */
public class Values {
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
    public static final Value.IntValue INT_VALUE = new Value.UnnownIntValue();
    public static final Value.FloatValue FLOAT_VALUE = new Value.UnknownFloatValue();
    public static final Value.LongValue LONG_VALUE = new Value.UnknownLongValue();
    public static final Value.DoubleValue DOUBLE_VALUE = new Value.UnknownDoubleValue();
    public static final Value.UnknownObjectValue OBJECT_VALUE = new Value.UnknownObjectValue(
            Types.instanceType(Object.class)
    );
    public static final Value.UnknownObjectValue STRING_VALUE = new Value.UnknownObjectValue(
            Types.instanceType(String.class)
    );
    public static final Value.NullValue NULL_VALUE = new Value.NullValue();
    public static final Value.VoidValue VOID_VALUE = new Value.VoidValue();

    private Values() {
    }

    public static @NotNull Value.KnownIntValue valueOf(boolean v) {
        return v ? INT_1 : INT_0;
    }

    public static @NotNull Value.KnownIntValue valueOf(byte v) {
        return valueOf((int) v);
    }

    public static @NotNull Value.KnownIntValue valueOf(char v) {
        return valueOf((int) v);
    }

    public static @NotNull Value.KnownIntValue valueOf(short v) {
        return valueOf((int) v);
    }

    public static @NotNull Value.KnownIntValue valueOf(int v) {
        if (v >= 0 && v < NUM_INT_VALS)
            return INT_VALUES[v];
        if (v == -1)
            return INT_M1;
        if (v == Integer.MIN_VALUE)
            return INT_MIN;
        if (v == Integer.MAX_VALUE)
            return INT_MAX;
        return new Value.KnownIntValue(v);
    }

    public static @NotNull Value.KnownLongValue valueOf(long v) {
        if (v >= 0 && v < NUM_LONG_VALS)
            return LONG_VALUES[(int) v];
        if (v == -1)
            return LONG_M1;
        if (v == Long.MIN_VALUE)
            return LONG_MIN;
        if (v == Long.MAX_VALUE)
            return LONG_MAX;
        return new Value.KnownLongValue(v);
    }

    public static @NotNull Value.KnownFloatValue valueOf(float v) {
        if (v == 0)
            return FLOAT_0;
        if (v == 1)
            return FLOAT_1;
        if (v == -1)
            return FLOAT_M1;
        if (v == Float.MIN_VALUE)
            return FLOAT_MIN;
        if (v == Float.MAX_VALUE)
            return FLOAT_MAX;
        if (Float.isNaN(v))
            return FLOAT_NAN;
        return new Value.KnownFloatValue(v);
    }

    public static @NotNull Value.KnownDoubleValue valueOf(double v) {
        if (v == 0)
            return DOUBLE_0;
        if (v == 1)
            return DOUBLE_1;
        if (v == -1)
            return DOUBLE_M1;
        if (v == Float.MIN_VALUE)
            return DOUBLE_MIN;
        if (v == Float.MAX_VALUE)
            return DOUBLE_MAX;
        if (Double.isNaN(v))
            return DOUBLE_NAN;
        return new Value.KnownDoubleValue(v);
    }

    public static @NotNull Value valueOf(@NotNull ClassType type) {
        if (type instanceof InstanceType instanceType) {
            return valueOfInstance(instanceType);
        } else if (type instanceof PrimitiveType primitiveType) {
            if (primitiveType.kind() == PrimitiveKind.T_VOID)
                return VOID_VALUE;
            return valueOfPrimitive(primitiveType);
        } else if (type instanceof ArrayType arrayType) {
            return valueOfArray(arrayType);
        }
        throw new IllegalStateException("Unknown type: " + type);
    }

    public static @NotNull Value.PrimitiveValue valueOfPrimitive(@NotNull PrimitiveType primitiveType) {
        return switch (primitiveType.kind()) {
            case PrimitiveKind.T_BOOLEAN, PrimitiveKind.T_BYTE, PrimitiveKind.T_CHAR, PrimitiveKind.T_SHORT, PrimitiveKind.T_INT -> INT_VALUE;
            case PrimitiveKind.T_FLOAT -> FLOAT_VALUE;
            case PrimitiveKind.T_LONG -> LONG_VALUE;
            case PrimitiveKind.T_DOUBLE -> DOUBLE_VALUE;
            case PrimitiveKind.T_VOID -> throw new IllegalStateException("Illegal value of void type");
            default -> throw new IllegalStateException("Unknown primitive type: " + primitiveType);
        };
    }

    public static @NotNull Value.UnknownLengthArrayValue valueOfArray(@NotNull ArrayType arrayType) {
        String desc = arrayType.descriptor();
        Value.UnknownLengthArrayValue value = ARRAY_VALUES.get(desc);
        if (value != null)
            return value;
        return new Value.UnknownLengthArrayValue(arrayType);
    }

    public static @NotNull Value.KnownLengthArrayValue valueOfArray(@NotNull ArrayType arrayType, int length) {
        return new Value.KnownLengthArrayValue(arrayType, length);
    }

    public static @NotNull Value.ObjectValue valueOfInstance(@NotNull InstanceType instanceType) {
        String name = instanceType.internalName();
        Value.ObjectValue value = INSTANCE_VALUES.get(name);
        if (value != null)
            return value;
        return new Value.UnknownObjectValue(instanceType);
    }

    public static @NotNull Value.ObjectValue valueOfString(@NotNull String content) {
        Value.KnownStringValue value = STRING_VALUES.get(content);
        if (value != null)
            return value;
        return new Value.KnownStringValue(content);
    }

    static {
        // Known values
        INT_VALUES = new Value.KnownIntValue[NUM_INT_VALS];
        LONG_VALUES = new Value.KnownLongValue[NUM_LONG_VALS];
        for (int i = 0; i < INT_VALUES.length; i++)
            INT_VALUES[i] = new Value.KnownIntValue(i);
        for (int i = 0; i < LONG_VALUES.length; i++)
            LONG_VALUES[i] = new Value.KnownLongValue(i);

        // Common strings
        List<String> strings = List.of("", " ", "  ", "   ", "    ", "\n", "\t", "\0");
        for (String string : strings)
            STRING_VALUES.put(string, new Value.KnownStringValue(string));

        // Primitive arrays
        String[] prims = "B,C,F,D,Z,I,J".split(",");
        for (String prim : prims) {
            String desc1 = "[" + prim;
            String desc2 = "[[" + prim;
            ARRAY_VALUES.put(desc1, new Value.UnknownLengthArrayValue(Types.arrayTypeFromDescriptor(desc1)));
            ARRAY_VALUES.put(desc2, new Value.UnknownLengthArrayValue(Types.arrayTypeFromDescriptor(desc2)));
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
                Constructor.class, Field.class, Member.class, Method.class, Type.class,

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
        for (Class<?> cls : commonJdkTypes) {
            InstanceType instanceType = Types.instanceType(cls);
            INSTANCE_VALUES.put(instanceType.internalName(), new Value.UnknownObjectValue(instanceType));
        }
    }
}

package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;

import dev.xdark.blw.code.instruction.MethodInstruction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic implementation of {@link MethodValueLookup} with some common methods
 * implemented.
 */
public class BasicMethodValueLookup implements MethodValueLookup {
    protected static final Map<String, StringFunc> INSTANCE_STRING_FUNCS = new HashMap<>();
    protected static final Map<String, StaticFunc> STATIC_FUNCS = new HashMap<>();

    @Override
    public @Nullable Value accept(@NotNull MethodInstruction instruction, Value.@Nullable ObjectValue context,
            @NotNull List<Value> parameters) {
        if (context instanceof Value.KnownStringValue stringValue) {
            String method = instruction.name() + instruction.type().descriptor();
            StringFunc func = INSTANCE_STRING_FUNCS.get(method);
            if (func != null)
                return func.apply(stringValue.value(), parameters);
        } else if (context == null) {
            String method = instruction.owner().internalName() + "." + instruction.name()
                    + instruction.type().descriptor();
            StaticFunc func = STATIC_FUNCS.get(method);
            if (func != null)
                return func.apply(parameters);
        }
        return null;
    }

    static {
        initStringFuncs();
        initMathStaticFuncs();
        initSystemStaticFuncs();
        initIntegerStaticFuncs();
        // TODO: Support more methods
        //  - Byte
        //  - Short
        //  - Character
        //  - Float
        //  - Double
        //  - Long
    }

    /**
     * Add {@link Integer} methods to {@link #STATIC_FUNCS}.
     */
    private static void initIntegerStaticFuncs() {
        STATIC_FUNCS.put("java/lang/Integer.parseInt(Ljava/lang/String;)I", params -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Integer.parseInt(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.parseInt(Ljava/lang/String;I)I", params -> {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (
                params.get(0)instanceof Value.KnownStringValue text && params.get(1)instanceof Value.KnownIntValue radix
            )
                try {
                    return Values.valueOf(Integer.parseInt(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.parseUnsignedInt(Ljava/lang/String;)I", params -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Integer.parseUnsignedInt(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.parseUnsignedInt(Ljava/lang/String;I)I", params -> {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (
                params.get(0)instanceof Value.KnownStringValue text && params.get(1)instanceof Value.KnownIntValue radix
            )
                try {
                    return Values.valueOf(Integer.parseUnsignedInt(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.signum(I)I", (UnIntStaticFunc) Integer::signum);
        STATIC_FUNCS.put("java/lang/Integer.hashCode(I)I", (UnIntStaticFunc) Integer::hashCode);
        STATIC_FUNCS.put("java/lang/Integer.reverse(I)I", (UnIntStaticFunc) Integer::reverse);
        STATIC_FUNCS.put("java/lang/Integer.reverseBytes(I)I", (UnIntStaticFunc) Integer::reverseBytes);
        STATIC_FUNCS.put("java/lang/Integer.lowestOneBit(I)I", (UnIntStaticFunc) Integer::lowestOneBit);
        STATIC_FUNCS.put("java/lang/Integer.highestOneBit(I)I", (UnIntStaticFunc) Integer::highestOneBit);
        STATIC_FUNCS.put("java/lang/Integer.numberOfLeadingZeros(I)I", (UnIntStaticFunc) Integer::numberOfLeadingZeros);
        STATIC_FUNCS
                .put("java/lang/Integer.numberOfTrailingZeros(I)I", (UnIntStaticFunc) Integer::numberOfTrailingZeros);
        STATIC_FUNCS.put("java/lang/Integer.bitCount(I)I", (UnIntStaticFunc) Integer::bitCount);
        STATIC_FUNCS.put("java/lang/Integer.compare(II)I", (BiIntStaticFunc) Integer::compare);
        STATIC_FUNCS.put("java/lang/Integer.compareUnsigned(II)I", (BiIntStaticFunc) Integer::compareUnsigned);
        STATIC_FUNCS.put("java/lang/Integer.divideUnsigned(II)I", (BiIntStaticFunc) Integer::divideUnsigned);
        STATIC_FUNCS.put("java/lang/Integer.remainderUnsigned(II)I", (BiIntStaticFunc) Integer::remainderUnsigned);
        STATIC_FUNCS.put("java/lang/Integer.min(II)I", (BiIntStaticFunc) Integer::min);
        STATIC_FUNCS.put("java/lang/Integer.max(II)I", (BiIntStaticFunc) Integer::max);
        STATIC_FUNCS.put("java/lang/Integer.rotateLeft(II)I", (BiIntStaticFunc) Integer::rotateLeft);
        STATIC_FUNCS.put("java/lang/Integer.rotateRight(II)I", (BiIntStaticFunc) Integer::rotateRight);
        STATIC_FUNCS.put("java/lang/Integer.toUnsignedLong(I)J", params -> {
            if (params.isEmpty())
                return Values.LONG_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue a) {
                return Values.valueOf(Integer.toUnsignedLong(a.value()));
            }
            return Values.LONG_VALUE;
        });
        STATIC_FUNCS.put(
                "java/lang/Integer.toBinaryString(I)Ljava/lang/String;", (IntToStringStaticFunc) Integer::toBinaryString
        );
        STATIC_FUNCS.put(
                "java/lang/Integer.toHexString(I)Ljava/lang/String;", (IntToStringStaticFunc) Integer::toHexString
        );
        STATIC_FUNCS.put(
                "java/lang/Integer.toOctalString(I)Ljava/lang/String;", (IntToStringStaticFunc) Integer::toOctalString
        );
        STATIC_FUNCS.put(
                "java/lang/Integer.toUnsignedString(I)Ljava/lang/String;",
                (IntToStringStaticFunc) Integer::toUnsignedString
        );
        STATIC_FUNCS.put("java/lang/Integer.toUnsignedString(II)Ljava/lang/String;", params -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue a && params.get(1)instanceof Value.KnownIntValue b) {
                return Values.valueOfString(Integer.toUnsignedString(a.value(), b.value()));
            }
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.toString(I)Ljava/lang/String;", (IntToStringStaticFunc) Integer::toString);
        STATIC_FUNCS.put("java/lang/Integer.toString(II)Ljava/lang/String;", params -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue a && params.get(1)instanceof Value.KnownIntValue b) {
                return Values.valueOfString(Integer.toString(a.value(), b.value()));
            }
            return Values.STRING_VALUE;
        });
    }

    /**
     * Add {@link Math} methods to {@link #STATIC_FUNCS}.
     */
    private static void initMathStaticFuncs() {
        STATIC_FUNCS.put("java/lang/Math.max(II)I", (BiIntStaticFunc) Math::max);
        STATIC_FUNCS.put("java/lang/Math.max(JJ)J", (BiLongStaticFunc) Math::max);
        STATIC_FUNCS.put("java/lang/Math.max(FF)F", (BiFloatStaticFunc) Math::max);
        STATIC_FUNCS.put("java/lang/Math.max(DD)D", (BiDoubleStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.min(II)I", (BiIntStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.min(JJ)J", (BiLongStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.min(FF)F", (BiFloatStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.min(DD)D", (BiDoubleStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.floorDiv(II)I", (BiIntThrowingStaticFunc) Math::floorDiv);
        STATIC_FUNCS.put("java/lang/Math.floorDiv(JI)I", params -> {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownLongValue a && params.get(1)instanceof Value.KnownIntValue b) {
                try {
                    return Values.valueOf(Math.floorDiv(a.value(), b.value()));
                } catch (ArithmeticException ignored) {
                }
            }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.floorDiv(JJ)J", (BiLongThrowingStaticFunc) Math::floorDiv);
        STATIC_FUNCS.put("java/lang/Math.floorMod(II)I", (BiIntThrowingStaticFunc) Math::floorMod);
        STATIC_FUNCS.put("java/lang/Math.floorMod(JI)I", params -> {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownLongValue a && params.get(1)instanceof Value.KnownIntValue b) {
                try {
                    return Values.valueOf(Math.floorMod(a.value(), b.value()));
                } catch (ArithmeticException ignored) {
                }
            }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.floorMod(JJ)J", (BiLongThrowingStaticFunc) Math::floorMod);
        STATIC_FUNCS.put("java/lang/Math.addExact(II)I", (BiIntThrowingStaticFunc) Math::addExact);
        STATIC_FUNCS.put("java/lang/Math.addExact(JJ)J", (BiLongThrowingStaticFunc) Math::addExact);
        STATIC_FUNCS.put("java/lang/Math.multiplyHigh(JJ)J", (BiLongThrowingStaticFunc) Math::multiplyHigh);
        STATIC_FUNCS.put("java/lang/Math.multiplyFull(II)J", params -> {
            if (params.size() != 2)
                return Values.LONG_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue a && params.get(1)instanceof Value.KnownIntValue b) {
                try {
                    return Values.valueOf(Math.multiplyFull(a.value(), b.value()));
                } catch (ArithmeticException ignored) {
                }
            }
            return Values.LONG_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.multiplyExact(II)I", (BiIntThrowingStaticFunc) Math::multiplyExact);
        STATIC_FUNCS.put("java/lang/Math.multiplyExact(JI)J", params -> {
            if (params.size() != 2)
                return Values.LONG_VALUE;
            if (params.get(0)instanceof Value.KnownLongValue a && params.get(1)instanceof Value.KnownIntValue b) {
                try {
                    return Values.valueOf(Math.multiplyExact(a.value(), b.value()));
                } catch (ArithmeticException ignored) {
                }
            }
            return Values.LONG_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.multiplyExact(JJ)J", (BiLongThrowingStaticFunc) Math::multiplyExact);
        STATIC_FUNCS.put("java/lang/Math.pow(DD)D", (BiDoubleStaticFunc) Math::pow);
        STATIC_FUNCS.put("java/lang/Math.nextAfter(DD)D", (BiDoubleStaticFunc) Math::nextAfter);
        STATIC_FUNCS.put("java/lang/Math.nextAfter(FD)F", params -> {
            if (params.size() != 2)
                return Values.FLOAT_VALUE;
            if (params.get(0)instanceof Value.KnownFloatValue a && params.get(1)instanceof Value.KnownDoubleValue b) {
                return Values.valueOf(Math.nextAfter(a.value(), b.value()));
            }
            return Values.FLOAT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.floor(D)D", (UnDoubleStaticFunc) Math::floor);
        STATIC_FUNCS.put("java/lang/Math.nextUp(F)F", (UnFloatStaticFunc) Math::nextUp);
        STATIC_FUNCS.put("java/lang/Math.nextUp(D)D", (UnDoubleStaticFunc) Math::nextUp);
        STATIC_FUNCS.put("java/lang/Math.nextDown(F)F", (UnFloatStaticFunc) Math::nextDown);
        STATIC_FUNCS.put("java/lang/Math.nextDown(D)D", (UnDoubleStaticFunc) Math::nextDown);
        STATIC_FUNCS.put("java/lang/Math.absExact(I)I", (UnIntStaticFunc) Math::absExact);
        STATIC_FUNCS.put("java/lang/Math.absExact(J)J", (UnLongStaticFunc) Math::absExact);
        STATIC_FUNCS.put("java/lang/Math.toIntExact(J)I", (LongToIntThrowingStaticFunc) Math::toIntExact);
        STATIC_FUNCS.put("java/lang/Math.incrementExact(I)I", (UnIntStaticFunc) Math::incrementExact);
        STATIC_FUNCS.put("java/lang/Math.incrementExact(J)J", (UnLongStaticFunc) Math::incrementExact);
        STATIC_FUNCS.put("java/lang/Math.decrementExact(I)I", (UnIntStaticFunc) Math::decrementExact);
        STATIC_FUNCS.put("java/lang/Math.decrementExact(J)J", (UnLongStaticFunc) Math::decrementExact);
        STATIC_FUNCS.put("java/lang/Math.subtractExact(II)I", (BiIntThrowingStaticFunc) Math::subtractExact);
        STATIC_FUNCS.put("java/lang/Math.subtractExact(JJ)J", (BiLongThrowingStaticFunc) Math::subtractExact);
        STATIC_FUNCS.put("java/lang/Math.abs(I)I", (UnIntStaticFunc) Math::abs);
        STATIC_FUNCS.put("java/lang/Math.abs(J)J", (UnLongStaticFunc) Math::abs);
        STATIC_FUNCS.put("java/lang/Math.abs(F)F", (UnFloatStaticFunc) Math::abs);
        STATIC_FUNCS.put("java/lang/Math.abs(D)D", (UnDoubleStaticFunc) Math::abs);
        STATIC_FUNCS.put("java/lang/Math.atan2(DD)D", (BiDoubleStaticFunc) Math::atan2);
        STATIC_FUNCS.put("java/lang/Math.copySign(DD)D", (BiDoubleStaticFunc) Math::copySign);
        STATIC_FUNCS.put("java/lang/Math.copySign(FF)F", (BiFloatStaticFunc) Math::copySign);
        STATIC_FUNCS.put("java/lang/Math.acos(D)D", (UnDoubleStaticFunc) Math::acos);
        STATIC_FUNCS.put("java/lang/Math.asin(D)D", (UnDoubleStaticFunc) Math::asin);
        STATIC_FUNCS.put("java/lang/Math.atan(D)D", (UnDoubleStaticFunc) Math::atan);
        STATIC_FUNCS.put("java/lang/Math.tan(D)D", (UnDoubleStaticFunc) Math::tan);
        STATIC_FUNCS.put("java/lang/Math.tanh(D)D", (UnDoubleStaticFunc) Math::tanh);
        STATIC_FUNCS.put("java/lang/Math.sin(D)D", (UnDoubleStaticFunc) Math::sin);
        STATIC_FUNCS.put("java/lang/Math.sinh(D)D", (UnDoubleStaticFunc) Math::sinh);
        STATIC_FUNCS.put("java/lang/Math.cos(D)D", (UnDoubleStaticFunc) Math::cos);
        STATIC_FUNCS.put("java/lang/Math.cosh(D)D", (UnDoubleStaticFunc) Math::cosh);
        STATIC_FUNCS.put("java/lang/Math.cbrt(D)D", (UnDoubleStaticFunc) Math::cbrt);
        STATIC_FUNCS.put("java/lang/Math.sqrt(D)D", (UnDoubleStaticFunc) Math::sqrt);
        STATIC_FUNCS.put("java/lang/Math.exp(D)D", (UnDoubleStaticFunc) Math::exp);
        STATIC_FUNCS.put("java/lang/Math.expm1(D)D", (UnDoubleStaticFunc) Math::expm1);
        STATIC_FUNCS.put("java/lang/Math.getExponent(D)D", (UnDoubleStaticFunc) Math::getExponent);
        STATIC_FUNCS.put("java/lang/Math.getExponent(F)F", (UnFloatStaticFunc) Math::getExponent);
        STATIC_FUNCS.put("java/lang/Math.signum(D)D", (UnDoubleStaticFunc) Math::signum);
        STATIC_FUNCS.put("java/lang/Math.signum(F)F", (UnFloatStaticFunc) Math::signum);
        STATIC_FUNCS.put("java/lang/Math.round(F)F", (UnFloatStaticFunc) Math::round);
        STATIC_FUNCS.put("java/lang/Math.round(D)D", (UnDoubleStaticFunc) Math::round);
        STATIC_FUNCS.put("java/lang/Math.log(D)D", (UnDoubleStaticFunc) Math::log);
        STATIC_FUNCS.put("java/lang/Math.log10(D)D", (UnDoubleStaticFunc) Math::log10);
        STATIC_FUNCS.put("java/lang/Math.log1p(D)D", (UnDoubleStaticFunc) Math::log1p);
        STATIC_FUNCS.put("java/lang/Math.ulp(D)D", (UnDoubleStaticFunc) Math::ulp);
        STATIC_FUNCS.put("java/lang/Math.ulp(F)F", (UnFloatStaticFunc) Math::ulp);
        STATIC_FUNCS.put("java/lang/Math.toDegrees(D)D", (UnDoubleStaticFunc) Math::toDegrees);
        STATIC_FUNCS.put("java/lang/Math.toRadians(D)D", (UnDoubleStaticFunc) Math::toRadians);
        STATIC_FUNCS.put("java/lang/Math.IEEEremainder(DD)D", (BiDoubleStaticFunc) Math::IEEEremainder);
        STATIC_FUNCS.put("java/lang/Math.hypot(DD)D", (BiDoubleStaticFunc) Math::hypot);
        STATIC_FUNCS.put("java/lang/Math.scalb(FI)F", params -> {
            if (params.size() != 2)
                return Values.FLOAT_VALUE;
            if (params.get(0)instanceof Value.KnownFloatValue a && params.get(1)instanceof Value.KnownIntValue b) {
                return Values.valueOf(Math.scalb(a.value(), b.value()));
            }
            return Values.FLOAT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.scalb(DI)D", params -> {
            if (params.size() != 2)
                return Values.DOUBLE_VALUE;
            if (params.get(0)instanceof Value.KnownDoubleValue a && params.get(1)instanceof Value.KnownIntValue b) {
                return Values.valueOf(Math.scalb(a.value(), b.value()));
            }
            return Values.DOUBLE_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.fma(FFF)F", params -> {
            if (params.size() != 3)
                return Values.FLOAT_VALUE;
            if (
                params.get(0)instanceof Value.KnownFloatValue a && params.get(1)instanceof Value.KnownFloatValue b
                        && params.get(2)instanceof Value.KnownFloatValue c
            ) {
                return Values.valueOf(Math.fma(a.value(), b.value(), c.value()));

            }
            return Values.FLOAT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.fma(DDD)D", params -> {
            if (params.size() != 3)
                return Values.DOUBLE_VALUE;
            if (
                params.get(0)instanceof Value.KnownDoubleValue a && params.get(1)instanceof Value.KnownIntValue b
                        && params.get(2)instanceof Value.KnownIntValue c
            ) {
                return Values.valueOf(Math.fma(a.value(), b.value(), c.value()));

            }
            return Values.DOUBLE_VALUE;
        });

        // Duplicate for StrictMath
        for (var entry : new ArrayList<>(STATIC_FUNCS.entrySet())) {
            String key = entry.getKey();
            if (key.startsWith("java/lang/Math.")) {
                STATIC_FUNCS.put(key.replace("/Math.", "/StrictMath."), entry.getValue());
            }
        }
    }

    /**
     * Add {@link System} methods to {@link #STATIC_FUNCS}.
     */
    private static void initSystemStaticFuncs() {
        // STATIC_FUNCS.put("java/lang/System.currentTimeMillis()J", params -> Values.valueOf(System.currentTimeMillis()));
        // STATIC_FUNCS.put("java/lang/System.nanoTime()J", params -> Values.valueOf(System.nanoTime()));
        STATIC_FUNCS.put(
                "java/lang/System.lineSeparator()Ljava/lang/String;",
                params -> Values.valueOfString(System.lineSeparator())
        );
        STATIC_FUNCS.put("java/lang/System.getProperty(Ljava/lang/String;)Ljava/lang/String;", params -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue key) {
                try {
                    String property = System.getProperty(key.value());
                    if (property != null)
                        return Values.valueOfString(property);
                } catch (Throwable ignored) {
                }
            }
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS
                .put("java/lang/System.getProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", params -> {
                    if (params.isEmpty())
                        return Values.STRING_VALUE;
                    if (
                        params.get(0)instanceof Value.KnownStringValue key
                                && params.get(1)instanceof Value.KnownStringValue fallback
                    ) {
                        try {
                            String property = System.getProperty(key.value(), fallback.value());
                            if (property != null)
                                return Values.valueOfString(property);
                        } catch (Throwable ignored) {
                        }
                    }
                    return Values.STRING_VALUE;
                });
    }

    /**
     * Add {@link String} methods to {@link #INSTANCE_STRING_FUNCS} and
     * {@link #STATIC_FUNCS}.
     */
    private static void initStringFuncs() {
        INSTANCE_STRING_FUNCS.put("length()I", (value, params) -> Values.valueOf(value.length()));
        INSTANCE_STRING_FUNCS.put("hashCode()I", (value, params) -> Values.valueOf(value.hashCode()));
        INSTANCE_STRING_FUNCS
                .put("toLowerCase()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toLowerCase()));
        INSTANCE_STRING_FUNCS.put(
                "toLowerCase(Ljava/util/Locale;)Ljava/lang/String;",
                (value, params) -> Values.valueOfString(value.toLowerCase())
        );
        INSTANCE_STRING_FUNCS
                .put("toUpperCase()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toUpperCase()));
        INSTANCE_STRING_FUNCS.put(
                "toUpperCase(Ljava/util/Locale;)Ljava/lang/String;",
                (value, params) -> Values.valueOfString(value.toUpperCase())
        );
        INSTANCE_STRING_FUNCS.put("trim()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.trim()));
        INSTANCE_STRING_FUNCS.put("strip()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.strip()));
        INSTANCE_STRING_FUNCS
                .put("stripIndent()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.stripIndent()));
        INSTANCE_STRING_FUNCS
                .put("stripLeading()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.stripLeading()));
        INSTANCE_STRING_FUNCS.put(
                "stripTrailing()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.stripTrailing())
        );
        INSTANCE_STRING_FUNCS.put("intern()Ljava/lang/String;", (value, params) -> Values.valueOfString(value));
        INSTANCE_STRING_FUNCS.put("toString()Ljava/lang/String;", (value, params) -> Values.valueOfString(value));
        INSTANCE_STRING_FUNCS.put("isBlank()Z", (value, params) -> Values.valueOf(value.isBlank()));
        INSTANCE_STRING_FUNCS.put("isEmpty()Z", (value, params) -> Values.valueOf(value.isEmpty()));
        INSTANCE_STRING_FUNCS.put("repeat(I)Ljava/lang/String;", (value, params) -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue count && count.value() > 0)
                return Values.valueOfString(value.repeat(count.value()));
            return Values.STRING_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("equals(Ljava/lang/Object;)Z", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.equals(strParam.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("equalsIgnoreCase(Ljava/lang/String;)Z", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.equalsIgnoreCase(strParam.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("contentEquals(Ljava/lang/CharSequence;)Z", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.contentEquals(strParam.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("contains(Ljava/lang/Object;)Z", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.contains(strParam.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("charAt(I)C", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (
                params.get(0)instanceof Value.KnownIntValue index && index.value() >= 0
                        && index.value() < value.length()
            )
                return Values.valueOf(value.charAt(index.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("indexOf(I)I", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue ch)
                return Values.valueOf(value.indexOf(ch.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("indexOf(II)I", (value, params) -> {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue ch && params.get(1)instanceof Value.KnownIntValue from)
                return Values.valueOf(value.indexOf(ch.value(), from.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("indexOf(Ljava/lang/String;)I", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue ch)
                return Values.valueOf(value.indexOf(ch.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("indexOf(Ljava/lang/String;I)I", (value, params) -> {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue ch && params.get(1)instanceof Value.KnownIntValue from)
                return Values.valueOf(value.indexOf(ch.value(), from.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("startsWith(Ljava/lang/String;I)Z", (value, params) -> {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (
                params.get(0)instanceof Value.KnownStringValue prefix
                        && params.get(1)instanceof Value.KnownIntValue toOffset
            )
                return Values.valueOf(value.startsWith(prefix.value(), toOffset.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("startsWith(Ljava/lang/String;)Z", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue prefix)
                return Values.valueOf(value.startsWith(prefix.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("endsWith(Ljava/lang/String;)Z", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue suffix)
                return Values.valueOf(value.endsWith(suffix.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("compareTo(Ljava/lang/String;)I", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue other)
                return Values.valueOf(value.compareTo(other.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("compareToIgnoreCase(Ljava/lang/String;)I", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue other)
                return Values.valueOf(value.compareToIgnoreCase(other.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("substring(I)Ljava/lang/String;", (value, params) -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (
                params.get(0)instanceof Value.KnownIntValue begin && begin.value() >= 0
                        && begin.value() < value.length()
            )
                return Values.valueOfString(value.substring(begin.value()));
            return Values.STRING_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("substring(II)Ljava/lang/String;", (value, params) -> {
            if (params.size() != 2)
                return Values.STRING_VALUE;
            if (
                params.get(0)instanceof Value.KnownIntValue begin && begin.value() >= 0
                        && begin.value() < value.length() && params.get(1)instanceof Value.KnownIntValue end
                        && end.value() >= begin.value() && end.value() < value.length()
            )
                return Values.valueOfString(value.substring(begin.value(), end.value()));
            return Values.STRING_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("codePointAt(I)I", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (
                params.get(0)instanceof Value.KnownIntValue index && index.value() >= 0
                        && index.value() < value.length()
            )
                return Values.valueOf(value.codePointAt(index.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("codePointBefore(I)I", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (
                params.get(0)instanceof Value.KnownIntValue index && index.value() >= 0
                        && index.value() < value.length()
            )
                return Values.valueOf(value.codePointBefore(index.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("codePointCount(II)I", (value, params) -> {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (
                params.get(0)instanceof Value.KnownIntValue begin && begin.value() >= 0
                        && begin.value() < value.length() && params.get(1)instanceof Value.KnownIntValue end
                        && end.value() >= begin.value() && end.value() < value.length()
            )
                return Values.valueOf(value.codePointCount(begin.value(), end.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("offsetByCodePoints(II)I", (value, params) -> {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (
                params.get(0)instanceof Value.KnownIntValue index && index.value() >= 0
                        && index.value() < value.length() && params.get(1)instanceof Value.KnownIntValue offset
            )
                return Values.valueOf(value.offsetByCodePoints(index.value(), offset.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("regex(Ljava/lang/String;)Z", (value, params) -> {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownStringValue regex)
                return Values.valueOf(value.matches(regex.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(I)Ljava/lang/String;", params -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(F)Ljava/lang/String;", params -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownFloatValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(D)Ljava/lang/String;", params -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownDoubleValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(J)Ljava/lang/String;", params -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownLongValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(Z)Ljava/lang/String;", params -> {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue param)
                return Values.valueOfString(String.valueOf(param.value() != 0));
            return Values.STRING_VALUE;
        });
    }

    /**
     * Instance method executor on {@link String}.
     */
    protected interface StringFunc {
        @Nullable
        Value apply(@NotNull String value, @NotNull List<Value> params);
    }

    /**
     * Static method executor.
     */
    protected interface StaticFunc {
        @Nullable
        Value apply(@NotNull List<Value> params);
    }

    /**
     * Static {@code int method(int)} executor.
     */
    protected interface UnIntStaticFunc extends StaticFunc {
        int apply(int d);

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue a) {
                return Values.valueOf(apply(a.value()));
            }
            return Values.INT_VALUE;
        }
    }

    /**
     * Static {@code float method(float)} executor.
     */
    protected interface UnFloatStaticFunc extends StaticFunc {
        float apply(float d);

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.isEmpty())
                return Values.FLOAT_VALUE;
            if (params.get(0)instanceof Value.KnownFloatValue a) {
                return Values.valueOf(apply(a.value()));
            }
            return Values.FLOAT_VALUE;
        }
    }

    /**
     * Static {@code long method(long)} executor.
     */
    protected interface UnLongStaticFunc extends StaticFunc {
        long apply(long d);

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.isEmpty())
                return Values.LONG_VALUE;
            if (params.get(0)instanceof Value.KnownLongValue a) {
                return Values.valueOf(apply(a.value()));
            }
            return Values.LONG_VALUE;
        }
    }

    /**
     * Static {@code double method(double)} executor.
     */
    protected interface UnDoubleStaticFunc extends StaticFunc {
        double apply(double d);

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.isEmpty())
                return Values.DOUBLE_VALUE;
            if (params.get(0)instanceof Value.KnownDoubleValue a) {
                return Values.valueOf(apply(a.value()));
            }
            return Values.DOUBLE_VALUE;
        }
    }

    /**
     * Static {@code String method(int)} executor.
     */
    protected interface IntToStringStaticFunc extends StaticFunc {
        String apply(int d);

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.isEmpty())
                return Values.STRING_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue a) {
                String value = apply(a.value());
                if (value != null)
                    return Values.valueOfString(value);
            }
            return Values.STRING_VALUE;
        }
    }

    /**
     * Static {@code int method(int, int)} executor.
     */
    protected interface BiIntStaticFunc extends StaticFunc {
        int apply(int a, int b);

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue a && params.get(1)instanceof Value.KnownIntValue b) {
                return Values.valueOf(apply(a.value(), b.value()));
            }
            return Values.INT_VALUE;
        }
    }

    /**
     * Static {@code float method(float, float)} executor.
     */
    protected interface BiFloatStaticFunc extends StaticFunc {
        float apply(float a, float b);

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.size() != 2)
                return Values.FLOAT_VALUE;
            if (params.get(0)instanceof Value.KnownFloatValue a && params.get(1)instanceof Value.KnownFloatValue b) {
                return Values.valueOf(apply(a.value(), b.value()));
            }
            return Values.FLOAT_VALUE;
        }
    }

    /**
     * Static {@code long method(long, long)} executor.
     */
    protected interface BiLongStaticFunc extends StaticFunc {
        long apply(long a, long b);

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.size() != 2)
                return Values.LONG_VALUE;
            if (params.get(0)instanceof Value.KnownLongValue a && params.get(1)instanceof Value.KnownLongValue b) {
                return Values.valueOf(apply(a.value(), b.value()));
            }
            return Values.LONG_VALUE;
        }
    }

    /**
     * Static {@code double method(double, double)} executor.
     */
    protected interface BiDoubleStaticFunc extends StaticFunc {
        double apply(double a, double b);

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.size() != 2)
                return Values.DOUBLE_VALUE;
            if (params.get(0)instanceof Value.KnownDoubleValue a && params.get(1)instanceof Value.KnownDoubleValue b) {
                return Values.valueOf(apply(a.value(), b.value()));
            }
            return Values.DOUBLE_VALUE;
        }
    }

    /**
     * Static {@code int method(int, int)} executor.
     */
    protected interface BiIntThrowingStaticFunc extends StaticFunc {
        int apply(int a, int b) throws ArithmeticException;

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.size() != 2)
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownIntValue a && params.get(1)instanceof Value.KnownIntValue b) {
                try {
                    return Values.valueOf(apply(a.value(), b.value()));
                } catch (ArithmeticException ignored) {
                }
            }
            return Values.INT_VALUE;
        }
    }

    /**
     * Static {@code long method(long, long) throws ArithmeticException} executor.
     */
    protected interface BiLongThrowingStaticFunc extends StaticFunc {
        long apply(long a, long b) throws ArithmeticException;

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.size() != 2)
                return Values.LONG_VALUE;
            if (params.get(0)instanceof Value.KnownLongValue a && params.get(1)instanceof Value.KnownLongValue b) {
                try {
                    return Values.valueOf(apply(a.value(), b.value()));
                } catch (ArithmeticException ignored) {
                }
            }
            return Values.LONG_VALUE;
        }
    }

    /**
     * Static {@code int method(long) throws ArithmeticException} executor.
     */
    protected interface LongToIntThrowingStaticFunc extends StaticFunc {
        int apply(long d) throws ArithmeticException;

        @Override
        @Nullable
        default Value apply(@NotNull List<Value> params) {
            if (params.isEmpty())
                return Values.INT_VALUE;
            if (params.get(0)instanceof Value.KnownLongValue a) {
                try {
                    return Values.valueOf(apply(a.value()));
                } catch (ArithmeticException ignored) {
                }
            }
            return Values.INT_VALUE;
        }
    }
}

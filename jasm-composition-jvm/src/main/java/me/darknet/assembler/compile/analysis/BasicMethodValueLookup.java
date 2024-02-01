package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.instruction.MethodInstruction;
import me.darknet.assembler.compile.analysis.func.*;
import me.darknet.assembler.compile.analysis.jvm.MethodValueLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic implementation of {@link MethodValueLookup} with some common methods implemented.
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
            String method = instruction.owner().internalName() + "." + instruction.name() + instruction.type().descriptor();
            StaticFunc func = STATIC_FUNCS.get(method);
            if (func != null)
                return func.apply(parameters);
        }
        return null;
    }

    static {
        initMathStaticFuncs();
        initSystemStaticFuncs();
        initStringFuncs();
        initLongStaticFuncs();
        initDoubleStaticFuncs();
        initFloatStaticFuncs();
        initIntegerStaticFuncs();
        initCharacterStaticFuncs();
        initShortStaticFuncs();
        initByteStaticFuncs();
        initBooleanStaticFuncs();
    }

    /**
     * Add {@link Character} methods to {@link #STATIC_FUNCS}.
     */
    private static void initCharacterStaticFuncs() {
        // Since we don't track array contents, a number of methods interacting with those are not here.
        STATIC_FUNCS.put("java/lang/Character.codePointOf(Ljava/lang/CharSequence;I)I", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                return Values.valueOf(Character.codePointOf(text.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Character.codePointAt(Ljava/lang/CharSequence;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue index)
                return Values.valueOf(Character.codePointAt(text.value(), index.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Character.codePointBefore(Ljava/lang/CharSequence;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue index)
                return Values.valueOf(Character.codePointBefore(text.value(), index.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Character.codePointCount(Ljava/lang/CharSequence;II)I", params -> {
            if (params.size() == 3 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue begin &&
                    params.get(2) instanceof Value.KnownIntValue end)
                return Values.valueOf(Character.codePointCount(text.value(), begin.value(), end.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Character.offsetByCodePoints(Ljava/lang/CharSequence;II)I", params -> {
            if (params.size() == 3 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue index &&
                    params.get(2) instanceof Value.KnownIntValue off)
                return Values.valueOf(Character.offsetByCodePoints(text.value(), index.value(), off.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Character.digit(CI)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue ch &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                return Values.valueOf(Character.digit((char) ch.value(), radix.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Character.charCount(I)I", (I2IStaticFunc) Character::charCount);
        STATIC_FUNCS.put("java/lang/Character.digit(II)I", (II2IStaticFunc) Character::digit);
        STATIC_FUNCS.put("java/lang/Character.compare(CC)I", (CC2IStaticFunc) Character::compare);
        STATIC_FUNCS.put("java/lang/Character.toCodePoint(CC)I", (CC2IStaticFunc) Character::toCodePoint);
        STATIC_FUNCS.put("java/lang/Character.getType(C)I", (C2IStaticFunc) Character::getType);
        STATIC_FUNCS.put("java/lang/Character.getNumericValue(I)I", (I2IStaticFunc) Character::getNumericValue);
        STATIC_FUNCS.put("java/lang/Character.getNumericValue(C)I", (C2IStaticFunc) Character::getNumericValue);
        STATIC_FUNCS.put("java/lang/Character.lowSurrogate(I)C", (I2CStaticFunc) Character::lowSurrogate);
        STATIC_FUNCS.put("java/lang/Character.highSurrogate(I)C", (I2CStaticFunc) Character::highSurrogate);
        STATIC_FUNCS.put("java/lang/Character.toLowerCase(C)C", (C2CStaticFunc) Character::toLowerCase);
        STATIC_FUNCS.put("java/lang/Character.toLowerCase(I)I", (I2IStaticFunc) Character::toLowerCase);
        STATIC_FUNCS.put("java/lang/Character.toUpperCase(C)C", (C2CStaticFunc) Character::toUpperCase);
        STATIC_FUNCS.put("java/lang/Character.toUpperCase(I)I", (I2IStaticFunc) Character::toUpperCase);
        STATIC_FUNCS.put("java/lang/Character.toTitleCase(C)C", (C2CStaticFunc) Character::toTitleCase);
        STATIC_FUNCS.put("java/lang/Character.toTitleCase(I)I", (I2IStaticFunc) Character::toTitleCase);
        STATIC_FUNCS.put("java/lang/Character.isValidCodePoint(I)Z", (I2ZStaticFunc) Character::isValidCodePoint);
        STATIC_FUNCS.put("java/lang/Character.isBmpCodePoint(I)Z", (I2ZStaticFunc) Character::isBmpCodePoint);
        STATIC_FUNCS.put("java/lang/Character.isSupplementaryCodePoint(I)Z", (I2ZStaticFunc) Character::isSupplementaryCodePoint);
        STATIC_FUNCS.put("java/lang/Character.isHighSurrogate(C)Z", (C2ZStaticFunc) Character::isHighSurrogate);
        STATIC_FUNCS.put("java/lang/Character.isLowSurrogate(C)Z", (C2ZStaticFunc) Character::isLowSurrogate);
        STATIC_FUNCS.put("java/lang/Character.isSurrogatePair(CC)Z", (CC2ZStaticFunc) Character::isSurrogatePair);
        STATIC_FUNCS.put("java/lang/Character.isLowerCase(C)Z", (C2ZStaticFunc) Character::isLowerCase);
        STATIC_FUNCS.put("java/lang/Character.isLowerCase(I)Z", (I2ZStaticFunc) Character::isLowerCase);
        STATIC_FUNCS.put("java/lang/Character.isUpperCase(C)Z", (C2ZStaticFunc) Character::isUpperCase);
        STATIC_FUNCS.put("java/lang/Character.isUpperCase(I)Z", (I2ZStaticFunc) Character::isUpperCase);
        STATIC_FUNCS.put("java/lang/Character.isTitleCase(C)Z", (C2ZStaticFunc) Character::isTitleCase);
        STATIC_FUNCS.put("java/lang/Character.isTitleCase(I)Z", (I2ZStaticFunc) Character::isTitleCase);
        STATIC_FUNCS.put("java/lang/Character.isDefined(C)Z", (C2ZStaticFunc) Character::isDefined);
        STATIC_FUNCS.put("java/lang/Character.isDefined(I)Z", (I2ZStaticFunc) Character::isDefined);
        STATIC_FUNCS.put("java/lang/Character.isSpaceChar(C)Z", (C2ZStaticFunc) Character::isSpaceChar);
        STATIC_FUNCS.put("java/lang/Character.isSpaceChar(I)Z", (I2ZStaticFunc) Character::isSpaceChar);
        STATIC_FUNCS.put("java/lang/Character.isSpace(C)Z", (C2ZStaticFunc) Character::isSpace);
        STATIC_FUNCS.put("java/lang/Character.isWhitespace(C)Z", (C2ZStaticFunc) Character::isWhitespace);
        STATIC_FUNCS.put("java/lang/Character.isWhitespace(I)Z", (I2ZStaticFunc) Character::isWhitespace);
        STATIC_FUNCS.put("java/lang/Character.isISOControl(C)Z", (C2ZStaticFunc) Character::isISOControl);
        STATIC_FUNCS.put("java/lang/Character.isISOControl(I)Z", (I2ZStaticFunc) Character::isISOControl);
        STATIC_FUNCS.put("java/lang/Character.isLetter(C)Z", (C2ZStaticFunc) Character::isLetter);
        STATIC_FUNCS.put("java/lang/Character.isLetter(I)Z", (I2ZStaticFunc) Character::isLetter);
        STATIC_FUNCS.put("java/lang/Character.isLetterOrDigit(C)Z", (C2ZStaticFunc) Character::isLetterOrDigit);
        STATIC_FUNCS.put("java/lang/Character.isLetterOrDigit(I)Z", (I2ZStaticFunc) Character::isLetterOrDigit);
        STATIC_FUNCS.put("java/lang/Character.isJavaLetter(C)Z", (C2ZStaticFunc) Character::isJavaLetter);
        STATIC_FUNCS.put("java/lang/Character.isJavaLetterOrDigit(C)Z", (C2ZStaticFunc) Character::isJavaLetterOrDigit);
        STATIC_FUNCS.put("java/lang/Character.isJavaIdentifierStart(C)Z", (C2ZStaticFunc) Character::isJavaIdentifierStart);
        STATIC_FUNCS.put("java/lang/Character.isJavaIdentifierStart(I)Z", (I2ZStaticFunc) Character::isJavaIdentifierStart);
        STATIC_FUNCS.put("java/lang/Character.isJavaIdentifierPart(C)Z", (C2ZStaticFunc) Character::isJavaIdentifierPart);
        STATIC_FUNCS.put("java/lang/Character.isJavaIdentifierPart(I)Z", (I2ZStaticFunc) Character::isJavaIdentifierPart);
        STATIC_FUNCS.put("java/lang/Character.isUnicodeIdentifierStart(C)Z", (C2ZStaticFunc) Character::isUnicodeIdentifierStart);
        STATIC_FUNCS.put("java/lang/Character.isUnicodeIdentifierStart(I)Z", (I2ZStaticFunc) Character::isUnicodeIdentifierStart);
        STATIC_FUNCS.put("java/lang/Character.isUnicodeIdentifierPart(C)Z", (C2ZStaticFunc) Character::isUnicodeIdentifierPart);
        STATIC_FUNCS.put("java/lang/Character.isUnicodeIdentifierPart(I)Z", (I2ZStaticFunc) Character::isUnicodeIdentifierPart);
        STATIC_FUNCS.put("java/lang/Character.isIdentifierIgnorable(C)Z", (C2ZStaticFunc) Character::isIdentifierIgnorable);
        STATIC_FUNCS.put("java/lang/Character.isIdentifierIgnorable(I)Z", (I2ZStaticFunc) Character::isIdentifierIgnorable);
        STATIC_FUNCS.put("java/lang/Character.isAlphabetic(I)Z", (I2ZStaticFunc) Character::isAlphabetic);
        STATIC_FUNCS.put("java/lang/Character.isIdeographic(I)Z", (I2ZStaticFunc) Character::isIdeographic);
    }

    /**
     * Add {@link Double} methods to {@link #STATIC_FUNCS}.
     */
    private static void initDoubleStaticFuncs() {
        STATIC_FUNCS.put("java/lang/Double.parseDouble(Ljava/lang/String;)D", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Double.parseDouble(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.DOUBLE_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Double.toString(D)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownDoubleValue value)
                return Values.valueOfString(Double.toString(value.value()));
            return Values.DOUBLE_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Double.toHexString(D)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownDoubleValue value)
                return Values.valueOfString(Double.toHexString(value.value()));
            return Values.DOUBLE_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Double.isNaN(D)Z", (D2ZStaticFunc) Double::isNaN);
        STATIC_FUNCS.put("java/lang/Double.isFinite(D)Z", (D2ZStaticFunc) Double::isFinite);
        STATIC_FUNCS.put("java/lang/Double.isInfinite(D)Z", (D2ZStaticFunc) Double::isInfinite);
        STATIC_FUNCS.put("java/lang/Double.hashCode(D)I", (D2IStaticFunc) Double::hashCode);
        STATIC_FUNCS.put("java/lang/Double.doubleToLongBits(D)J", (D2JStaticFunc) Double::doubleToLongBits);
        STATIC_FUNCS.put("java/lang/Double.doubleToRawLongBits(D)J", (D2JStaticFunc) Double::doubleToRawLongBits);
        STATIC_FUNCS.put("java/lang/Double.longBitsToDouble(J)D", (J2DStaticFunc) Double::longBitsToDouble);
        STATIC_FUNCS.put("java/lang/Double.min(DD)D", (DD2DStaticFunc) Double::min);
        STATIC_FUNCS.put("java/lang/Double.max(DD)D", (DD2DStaticFunc) Double::max);
        STATIC_FUNCS.put("java/lang/Double.sum(DD)D", (DD2DStaticFunc) Double::sum);
        STATIC_FUNCS.put("java/lang/Double.compare(DD)I", (DD2IStaticFunc) Double::compare);
    }

    /**
     * Add {@link Float} methods to {@link #STATIC_FUNCS}.
     */
    private static void initFloatStaticFuncs() {
        STATIC_FUNCS.put("java/lang/Float.parseFloat(Ljava/lang/String;)F", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Float.parseFloat(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.FLOAT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Float.toHexString(F)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownFloatValue value)
                return Values.valueOfString(Float.toHexString(value.value()));
            return Values.FLOAT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Float.toString(F)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownFloatValue value)
                return Values.valueOfString(Float.toString(value.value()));
            return Values.FLOAT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Float.isNaN(F)Z", (F2ZStaticFunc) Float::isNaN);
        STATIC_FUNCS.put("java/lang/Float.isFinite(F)Z", (F2ZStaticFunc) Float::isFinite);
        STATIC_FUNCS.put("java/lang/Float.isInfinite(F)Z", (F2ZStaticFunc) Float::isInfinite);
        STATIC_FUNCS.put("java/lang/Float.floatToRawIntBits(F)I", (F2IStaticFunc) Float::floatToRawIntBits);
        STATIC_FUNCS.put("java/lang/Float.floatToIntBits(F)I", (F2IStaticFunc) Float::floatToIntBits);
        STATIC_FUNCS.put("java/lang/Float.intBitsToFloat(I)F", (I2FStaticFunc) Float::intBitsToFloat);
        STATIC_FUNCS.put("java/lang/Float.hashCode(F)I", (F2IStaticFunc) Float::hashCode);
        STATIC_FUNCS.put("java/lang/Float.compare(FF)I", (FF2IStaticFunc) Float::compare);
        STATIC_FUNCS.put("java/lang/Float.min(FF)F", (FF2FStaticFunc) Float::min);
        STATIC_FUNCS.put("java/lang/Float.max(FF)F", (FF2FStaticFunc) Float::max);
        STATIC_FUNCS.put("java/lang/Float.sum(FF)F", (FF2FStaticFunc) Float::sum);
    }

    /**
     * Add {@link Short} methods to {@link #STATIC_FUNCS}.
     */
    private static void initShortStaticFuncs() {
        STATIC_FUNCS.put("java/lang/Short.parseShort(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Short.parseShort(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Short.parseShort(Ljava/lang/String;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Short.parseShort(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Short.toString(S)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue value)
                return Values.valueOfString(Short.toString((short) value.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Short.toUnsignedInt(S)I", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue value)
                return Values.valueOf(Short.toUnsignedInt((short) value.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Short.toUnsignedLong(S)J", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue value)
                return Values.valueOf(Short.toUnsignedLong((short) value.value()));
            return Values.LONG_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Short.reverseBytes(S)S", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue value)
                return Values.valueOf(Short.reverseBytes((short) value.value()));
            return Values.INT_VALUE;
        });

        STATIC_FUNCS.put("java/lang/Short.hashCode(S)I", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue value)
                return Values.valueOf(Short.hashCode((short) value.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Short.compare(SS)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Short.compare((short) a.value(), (short) b.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Short.compareUnsigned(SS)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Short.compareUnsigned((short) a.value(), (short) b.value()));
            return Values.INT_VALUE;
        });
    }

    /**
     * Add {@link Byte} methods to {@link #STATIC_FUNCS}.
     */
    private static void initByteStaticFuncs() {
        STATIC_FUNCS.put("java/lang/Byte.parseByte(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Byte.parseByte(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Byte.parseByte(Ljava/lang/String;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Byte.parseByte(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Byte.decode(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Byte.decode(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Byte.compare(BB)I", (BB2IStaticFunc) Byte::compare);
        STATIC_FUNCS.put("java/lang/Byte.compareUnsigned(BB)I", (BB2IStaticFunc) Byte::compareUnsigned);
        STATIC_FUNCS.put("java/lang/Byte.toString(B)Ljava/lang/String;", (B2StringStaticFunc) Byte::toString);
        STATIC_FUNCS.put("java/lang/Byte.hashCode(B)I", (B2IStaticFunc) Byte::hashCode);
        STATIC_FUNCS.put("java/lang/Byte.toUnsignedInt(B)I", (B2IStaticFunc) Byte::toUnsignedInt);
        STATIC_FUNCS.put("java/lang/Byte.toUnsignedLong(B)I", (B2JStaticFunc) Byte::toUnsignedLong);
    }

    /**
     * Add {@link Long} methods to {@link #STATIC_FUNCS}.
     */
    private static void initLongStaticFuncs() {
        /* Boxed type
        STATIC_FUNCS.put("java/lang/Long.decode(Ljava/lang/String;)Ljava/lang/Long;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Long.decode(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.LONG_BOXED_VALUE;
        });
         */
        STATIC_FUNCS.put("java/lang/Long.parseLong(Ljava/lang/String;)J", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Long.parseLong(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.LONG_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Long.parseLong(Ljava/lang/String;I)J", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Long.parseLong(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.LONG_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Long.parseLong(Ljava/lang/CharSequence;III)J", params -> {
            if (params.size() == 4
                    && params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue begin &&
                    params.get(1) instanceof Value.KnownIntValue end &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Long.parseLong(text.value(), begin.value(), end.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.LONG_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Long.toString(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownLongValue value)
                return Values.valueOfString(Long.toString(value.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Long.toString(JI)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownLongValue value &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                return Values.valueOfString(Long.toString(value.value(), radix.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Long.toUnsignedString(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownLongValue value)
                return Values.valueOfString(Long.toUnsignedString(value.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Long.toUnsignedString(JI)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownLongValue value &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                return Values.valueOfString(Long.toUnsignedString(value.value(), radix.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Long.toHexString(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownLongValue value)
                return Values.valueOfString(Long.toHexString(value.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Long.toOctalString(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownLongValue value)
                return Values.valueOfString(Long.toOctalString(value.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Long.hashCode(J)I", (J2IStaticFunc) Long::hashCode);
        STATIC_FUNCS.put("java/lang/Long.signum(J)I", (J2IStaticFunc) Long::signum);
        STATIC_FUNCS.put("java/lang/Long.reverse(J)J", (J2JStaticFunc) Long::reverse);
        STATIC_FUNCS.put("java/lang/Long.compare(JJ)I", (JJ2IThrowingStaticFunc) Long::compare);
        STATIC_FUNCS.put("java/lang/Long.compareUnsigned(JJ)I", (JJ2IThrowingStaticFunc) Long::compareUnsigned);
        STATIC_FUNCS.put("java/lang/Long.divideUnsigned(JJ)J", (JJ2JThrowingStaticFunc) Long::divideUnsigned);
        STATIC_FUNCS.put("java/lang/Long.remainderUnsigned(JJ)J", (JJ2JThrowingStaticFunc) Long::remainderUnsigned);
        STATIC_FUNCS.put("java/lang/Long.highestOneBit(J)J", (J2JStaticFunc) Long::highestOneBit);
        STATIC_FUNCS.put("java/lang/Long.lowestOneBit(J)J", (J2JStaticFunc) Long::lowestOneBit);
        STATIC_FUNCS.put("java/lang/Long.remainderUnsigned(J)I", (J2IStaticFunc) Long::numberOfLeadingZeros);
        STATIC_FUNCS.put("java/lang/Long.numberOfTrailingZeros(J)I", (J2IStaticFunc) Long::numberOfTrailingZeros);
        STATIC_FUNCS.put("java/lang/Long.bitCount(J)I", (J2IStaticFunc) Long::bitCount);
        STATIC_FUNCS.put("java/lang/Long.rotateLeft(JI)J", (JI2JThrowingStaticFunc) Long::rotateLeft);
        STATIC_FUNCS.put("java/lang/Long.rotateRight(JI)J", (JI2JThrowingStaticFunc) Long::rotateRight);
    }

    /**
     * Add {@link Integer} methods to {@link #STATIC_FUNCS}.
     */
    private static void initIntegerStaticFuncs() {
        STATIC_FUNCS.put("java/lang/Integer.parseInt(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Integer.parseInt(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.parseInt(Ljava/lang/String;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Integer.parseInt(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.parseUnsignedInt(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Integer.parseUnsignedInt(text.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.parseUnsignedInt(Ljava/lang/String;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                try {
                    return Values.valueOf(Integer.parseUnsignedInt(text.value(), radix.value()));
                } catch (NumberFormatException ignored) {
                }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.signum(I)I", (I2IStaticFunc) Integer::signum);
        STATIC_FUNCS.put("java/lang/Integer.hashCode(I)I", (I2IStaticFunc) Integer::hashCode);
        STATIC_FUNCS.put("java/lang/Integer.reverse(I)I", (I2IStaticFunc) Integer::reverse);
        STATIC_FUNCS.put("java/lang/Integer.reverseBytes(I)I", (I2IStaticFunc) Integer::reverseBytes);
        STATIC_FUNCS.put("java/lang/Integer.lowestOneBit(I)I", (I2IStaticFunc) Integer::lowestOneBit);
        STATIC_FUNCS.put("java/lang/Integer.highestOneBit(I)I", (I2IStaticFunc) Integer::highestOneBit);
        STATIC_FUNCS.put("java/lang/Integer.numberOfLeadingZeros(I)I", (I2IStaticFunc) Integer::numberOfLeadingZeros);
        STATIC_FUNCS.put("java/lang/Integer.numberOfTrailingZeros(I)I", (I2IStaticFunc) Integer::numberOfTrailingZeros);
        STATIC_FUNCS.put("java/lang/Integer.bitCount(I)I", (I2IStaticFunc) Integer::bitCount);
        STATIC_FUNCS.put("java/lang/Integer.compare(II)I", (II2IStaticFunc) Integer::compare);
        STATIC_FUNCS.put("java/lang/Integer.compareUnsigned(II)I", (II2IStaticFunc) Integer::compareUnsigned);
        STATIC_FUNCS.put("java/lang/Integer.divideUnsigned(II)I", (II2IStaticFunc) Integer::divideUnsigned);
        STATIC_FUNCS.put("java/lang/Integer.remainderUnsigned(II)I", (II2IStaticFunc) Integer::remainderUnsigned);
        STATIC_FUNCS.put("java/lang/Integer.min(II)I", (II2IStaticFunc) Integer::min);
        STATIC_FUNCS.put("java/lang/Integer.max(II)I", (II2IStaticFunc) Integer::max);
        STATIC_FUNCS.put("java/lang/Integer.rotateLeft(II)I", (II2IStaticFunc) Integer::rotateLeft);
        STATIC_FUNCS.put("java/lang/Integer.rotateRight(II)I", (II2IStaticFunc) Integer::rotateRight);
        STATIC_FUNCS.put("java/lang/Integer.toUnsignedLong(I)J", (I2JStaticFunc) Integer::toUnsignedLong);
        STATIC_FUNCS.put("java/lang/Integer.toBinaryString(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toBinaryString);
        STATIC_FUNCS.put("java/lang/Integer.toHexString(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toHexString);
        STATIC_FUNCS.put("java/lang/Integer.toOctalString(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toOctalString);
        STATIC_FUNCS.put("java/lang/Integer.toUnsignedString(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toUnsignedString);
        STATIC_FUNCS.put("java/lang/Integer.toUnsignedString(II)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b) {
                return Values.valueOfString(Integer.toUnsignedString(a.value(), b.value()));
            }
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Integer.toString(I)Ljava/lang/String;", (I2StringStaticFunc) Integer::toString);
        STATIC_FUNCS.put("java/lang/Integer.toString(II)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b) {
                return Values.valueOfString(Integer.toString(a.value(), b.value()));
            }
            return Values.STRING_VALUE;
        });
    }

    /**
     * Add {@link Boolean} methods to {@link #STATIC_FUNCS}.
     */
    private static void initBooleanStaticFuncs() {
        STATIC_FUNCS.put("java/lang/Boolean.parseBoolean(Ljava/lang/String;)Z", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue value)
                return Values.valueOf(Boolean.parseBoolean(value.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Boolean.getBoolean(Ljava/lang/String;)Z", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue value)
                return Values.valueOf(Boolean.getBoolean(value.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Boolean.logicalAnd(ZZ)Z", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a  &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Boolean.logicalAnd(a.value() != 0, b.value() != 0));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Boolean.logicalOr(ZZ)Z", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a  &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Boolean.logicalOr(a.value() != 0, b.value() != 0));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Boolean.logicalXor(ZZ)Z", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a  &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Boolean.logicalXor(a.value() != 0, b.value() != 0));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Boolean.compare(ZZ)Z", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a  &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Boolean.compare(a.value() != 0, b.value() != 0));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Boolean.toString(Z)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue value)
                return Values.valueOfString(Boolean.toString(value.value() != 0));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Boolean.hashCode(Z)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue value)
                return Values.valueOf(Boolean.hashCode(value.value() != 0));
            return Values.INT_VALUE;
        });
    }

    /**
     * Add {@link Math} methods to {@link #STATIC_FUNCS}.
     */
    private static void initMathStaticFuncs() {
        STATIC_FUNCS.put("java/lang/Math.max(II)I", (II2IStaticFunc) Math::max);
        STATIC_FUNCS.put("java/lang/Math.max(JJ)J", (JJ2JStaticFunc) Math::max);
        STATIC_FUNCS.put("java/lang/Math.max(FF)F", (FF2FStaticFunc) Math::max);
        STATIC_FUNCS.put("java/lang/Math.max(DD)D", (DD2DStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.min(II)I", (II2IStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.min(JJ)J", (JJ2JStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.min(FF)F", (FF2FStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.min(DD)D", (DD2DStaticFunc) Math::min);
        STATIC_FUNCS.put("java/lang/Math.floorDiv(II)I", (II2IThrowingStaticFunc) Math::floorDiv);
        STATIC_FUNCS.put("java/lang/Math.floorDiv(JI)J", (JI2JThrowingStaticFunc) Math::floorDiv);
        STATIC_FUNCS.put("java/lang/Math.floorDiv(JJ)J", (JJ2JThrowingStaticFunc) Math::floorDiv);
        STATIC_FUNCS.put("java/lang/Math.floorMod(II)I", (II2IThrowingStaticFunc) Math::floorMod);
        STATIC_FUNCS.put("java/lang/Math.floorMod(JI)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownLongValue a &&
                    params.get(1) instanceof Value.KnownIntValue b) {
                try {
                    return Values.valueOf(Math.floorMod(a.value(), b.value()));
                } catch (Throwable ignored) {
                }
            }
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.floorMod(JJ)J", (JJ2JThrowingStaticFunc) Math::floorMod);
        STATIC_FUNCS.put("java/lang/Math.addExact(II)I", (II2IThrowingStaticFunc) Math::addExact);
        STATIC_FUNCS.put("java/lang/Math.addExact(JJ)J", (JJ2JThrowingStaticFunc) Math::addExact);
        STATIC_FUNCS.put("java/lang/Math.multiplyHigh(JJ)J", (JJ2JThrowingStaticFunc) Math::multiplyHigh);
        STATIC_FUNCS.put("java/lang/Math.multiplyFull(II)J", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b) {
                try {
                    return Values.valueOf(Math.multiplyFull(a.value(), b.value()));
                } catch (Throwable ignored) {
                }
            }
            return Values.LONG_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.multiplyExact(II)I", (II2IThrowingStaticFunc) Math::multiplyExact);
        STATIC_FUNCS.put("java/lang/Math.multiplyExact(JI)J", (JI2JThrowingStaticFunc) Math::multiplyExact);
        STATIC_FUNCS.put("java/lang/Math.multiplyExact(JJ)J", (JJ2JThrowingStaticFunc) Math::multiplyExact);
        STATIC_FUNCS.put("java/lang/Math.pow(DD)D", (DD2DStaticFunc) Math::pow);
        STATIC_FUNCS.put("java/lang/Math.nextAfter(DD)D", (DD2DStaticFunc) Math::nextAfter);
        STATIC_FUNCS.put("java/lang/Math.nextAfter(FD)F", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownFloatValue a &&
                    params.get(1) instanceof Value.KnownDoubleValue b) {
                return Values.valueOf(Math.nextAfter(a.value(), b.value()));
            }
            return Values.FLOAT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.floor(D)D", (D2DStaticFunc) Math::floor);
        STATIC_FUNCS.put("java/lang/Math.nextUp(F)F", (F2FStaticFunc) Math::nextUp);
        STATIC_FUNCS.put("java/lang/Math.nextUp(D)D", (D2DStaticFunc) Math::nextUp);
        STATIC_FUNCS.put("java/lang/Math.nextDown(F)F", (F2FStaticFunc) Math::nextDown);
        STATIC_FUNCS.put("java/lang/Math.nextDown(D)D", (D2DStaticFunc) Math::nextDown);
        STATIC_FUNCS.put("java/lang/Math.absExact(I)I", (I2IStaticFunc) Math::absExact);
        STATIC_FUNCS.put("java/lang/Math.absExact(J)J", (J2JStaticFunc) Math::absExact);
        STATIC_FUNCS.put("java/lang/Math.toIntExact(J)I", (J2IThrowingStaticFunc) Math::toIntExact);
        STATIC_FUNCS.put("java/lang/Math.incrementExact(I)I", (I2IStaticFunc) Math::incrementExact);
        STATIC_FUNCS.put("java/lang/Math.incrementExact(J)J", (J2JStaticFunc) Math::incrementExact);
        STATIC_FUNCS.put("java/lang/Math.decrementExact(I)I", (I2IStaticFunc) Math::decrementExact);
        STATIC_FUNCS.put("java/lang/Math.decrementExact(J)J", (J2JStaticFunc) Math::decrementExact);
        STATIC_FUNCS.put("java/lang/Math.subtractExact(II)I", (II2IThrowingStaticFunc) Math::subtractExact);
        STATIC_FUNCS.put("java/lang/Math.subtractExact(JJ)J", (JJ2JThrowingStaticFunc) Math::subtractExact);
        STATIC_FUNCS.put("java/lang/Math.abs(I)I", (I2IStaticFunc) Math::abs);
        STATIC_FUNCS.put("java/lang/Math.abs(J)J", (J2JStaticFunc) Math::abs);
        STATIC_FUNCS.put("java/lang/Math.abs(F)F", (F2FStaticFunc) Math::abs);
        STATIC_FUNCS.put("java/lang/Math.abs(D)D", (D2DStaticFunc) Math::abs);
        STATIC_FUNCS.put("java/lang/Math.atan2(DD)D", (DD2DStaticFunc) Math::atan2);
        STATIC_FUNCS.put("java/lang/Math.copySign(DD)D", (DD2DStaticFunc) Math::copySign);
        STATIC_FUNCS.put("java/lang/Math.copySign(FF)F", (FF2FStaticFunc) Math::copySign);
        STATIC_FUNCS.put("java/lang/Math.acos(D)D", (D2DStaticFunc) Math::acos);
        STATIC_FUNCS.put("java/lang/Math.asin(D)D", (D2DStaticFunc) Math::asin);
        STATIC_FUNCS.put("java/lang/Math.atan(D)D", (D2DStaticFunc) Math::atan);
        STATIC_FUNCS.put("java/lang/Math.tan(D)D", (D2DStaticFunc) Math::tan);
        STATIC_FUNCS.put("java/lang/Math.tanh(D)D", (D2DStaticFunc) Math::tanh);
        STATIC_FUNCS.put("java/lang/Math.sin(D)D", (D2DStaticFunc) Math::sin);
        STATIC_FUNCS.put("java/lang/Math.sinh(D)D", (D2DStaticFunc) Math::sinh);
        STATIC_FUNCS.put("java/lang/Math.cos(D)D", (D2DStaticFunc) Math::cos);
        STATIC_FUNCS.put("java/lang/Math.cosh(D)D", (D2DStaticFunc) Math::cosh);
        STATIC_FUNCS.put("java/lang/Math.cbrt(D)D", (D2DStaticFunc) Math::cbrt);
        STATIC_FUNCS.put("java/lang/Math.sqrt(D)D", (D2DStaticFunc) Math::sqrt);
        STATIC_FUNCS.put("java/lang/Math.exp(D)D", (D2DStaticFunc) Math::exp);
        STATIC_FUNCS.put("java/lang/Math.expm1(D)D", (D2DStaticFunc) Math::expm1);
        STATIC_FUNCS.put("java/lang/Math.getExponent(D)D", (D2DStaticFunc) Math::getExponent);
        STATIC_FUNCS.put("java/lang/Math.getExponent(F)F", (F2FStaticFunc) Math::getExponent);
        STATIC_FUNCS.put("java/lang/Math.signum(D)D", (D2DStaticFunc) Math::signum);
        STATIC_FUNCS.put("java/lang/Math.signum(F)F", (F2FStaticFunc) Math::signum);
        STATIC_FUNCS.put("java/lang/Math.round(F)F", (F2FStaticFunc) Math::round);
        STATIC_FUNCS.put("java/lang/Math.round(D)D", (D2DStaticFunc) Math::round);
        STATIC_FUNCS.put("java/lang/Math.log(D)D", (D2DStaticFunc) Math::log);
        STATIC_FUNCS.put("java/lang/Math.log10(D)D", (D2DStaticFunc) Math::log10);
        STATIC_FUNCS.put("java/lang/Math.log1p(D)D", (D2DStaticFunc) Math::log1p);
        STATIC_FUNCS.put("java/lang/Math.ulp(D)D", (D2DStaticFunc) Math::ulp);
        STATIC_FUNCS.put("java/lang/Math.ulp(F)F", (F2FStaticFunc) Math::ulp);
        STATIC_FUNCS.put("java/lang/Math.toDegrees(D)D", (D2DStaticFunc) Math::toDegrees);
        STATIC_FUNCS.put("java/lang/Math.toRadians(D)D", (D2DStaticFunc) Math::toRadians);
        STATIC_FUNCS.put("java/lang/Math.IEEEremainder(DD)D", (DD2DStaticFunc) Math::IEEEremainder);
        STATIC_FUNCS.put("java/lang/Math.hypot(DD)D", (DD2DStaticFunc) Math::hypot);
        STATIC_FUNCS.put("java/lang/Math.scalb(FI)F", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownFloatValue a &&
                    params.get(1) instanceof Value.KnownIntValue b) {
                return Values.valueOf(Math.scalb(a.value(), b.value()));
            }
            return Values.FLOAT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.scalb(DI)D", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownDoubleValue a &&
                    params.get(1) instanceof Value.KnownIntValue b) {
                return Values.valueOf(Math.scalb(a.value(), b.value()));
            }
            return Values.DOUBLE_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.fma(FFF)F", params -> {
            if (params.size() == 3 &&
                    params.get(0) instanceof Value.KnownFloatValue a &&
                    params.get(1) instanceof Value.KnownFloatValue b &&
                    params.get(2) instanceof Value.KnownFloatValue c) {
                return Values.valueOf(Math.fma(a.value(), b.value(), c.value()));

            }
            return Values.FLOAT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/Math.fma(DDD)D", params -> {
            if (params.size() == 3 &&
                    params.get(0) instanceof Value.KnownDoubleValue a &&
                    params.get(1) instanceof Value.KnownIntValue b &&
                    params.get(2) instanceof Value.KnownIntValue c) {
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
        STATIC_FUNCS.put("java/lang/System.lineSeparator()Ljava/lang/String;", params -> Values.valueOfString(System.lineSeparator()));
        STATIC_FUNCS.put("java/lang/System.getProperty(Ljava/lang/String;)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue key) {
                try {
                    String property = System.getProperty(key.value());
                    if (property != null)
                        return Values.valueOfString(property);
                } catch (Throwable ignored) {
                }
            }
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/System.getProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue key &&
                    params.get(1) instanceof Value.KnownStringValue fallback) {
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
        INSTANCE_STRING_FUNCS.put("toLowerCase()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toLowerCase()));
        INSTANCE_STRING_FUNCS.put("toLowerCase(Ljava/util/Locale;)Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toLowerCase()));
        INSTANCE_STRING_FUNCS.put("toUpperCase()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toUpperCase()));
        INSTANCE_STRING_FUNCS.put("toUpperCase(Ljava/util/Locale;)Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toUpperCase()));
        INSTANCE_STRING_FUNCS.put("trim()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.trim()));
        INSTANCE_STRING_FUNCS.put("strip()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.strip()));
        INSTANCE_STRING_FUNCS.put("stripIndent()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.stripIndent()));
        INSTANCE_STRING_FUNCS.put("stripLeading()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.stripLeading()));
        INSTANCE_STRING_FUNCS.put("stripTrailing()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.stripTrailing()));
        INSTANCE_STRING_FUNCS.put("intern()Ljava/lang/String;", (value, params) -> Values.valueOfString(value));
        INSTANCE_STRING_FUNCS.put("toString()Ljava/lang/String;", (value, params) -> Values.valueOfString(value));
        INSTANCE_STRING_FUNCS.put("isBlank()Z", (value, params) -> Values.valueOf(value.isBlank()));
        INSTANCE_STRING_FUNCS.put("isEmpty()Z", (value, params) -> Values.valueOf(value.isEmpty()));
        INSTANCE_STRING_FUNCS.put("repeat(I)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue count && count.value() > 0)
                return Values.valueOfString(value.repeat(count.value()));
            return Values.STRING_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("equals(Ljava/lang/Object;)Z", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.equals(strParam.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("equalsIgnoreCase(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.equalsIgnoreCase(strParam.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("contentEquals(Ljava/lang/CharSequence;)Z", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.contentEquals(strParam.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("contains(Ljava/lang/Object;)Z", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.contains(strParam.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("charAt(I)C", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue index &&
                    index.value() >= 0 &&
                    index.value() < value.length())
                return Values.valueOf(value.charAt(index.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("indexOf(I)I", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue ch)
                return Values.valueOf(value.indexOf(ch.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("indexOf(II)I", (value, params) -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue ch &&
                    params.get(1) instanceof Value.KnownIntValue from)
                return Values.valueOf(value.indexOf(ch.value(), from.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("indexOf(Ljava/lang/String;)I", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue ch)
                return Values.valueOf(value.indexOf(ch.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("indexOf(Ljava/lang/String;I)I", (value, params) -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue ch &&
                    params.get(1) instanceof Value.KnownIntValue from)
                return Values.valueOf(value.indexOf(ch.value(), from.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("startsWith(Ljava/lang/String;I)Z", (value, params) -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue prefix &&
                    params.get(1) instanceof Value.KnownIntValue toOffset)
                return Values.valueOf(value.startsWith(prefix.value(), toOffset.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("startsWith(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue prefix)
                return Values.valueOf(value.startsWith(prefix.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("endsWith(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue suffix)
                return Values.valueOf(value.endsWith(suffix.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("compareTo(Ljava/lang/String;)I", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue other)
                return Values.valueOf(value.compareTo(other.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("compareToIgnoreCase(Ljava/lang/String;)I", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue other)
                return Values.valueOf(value.compareToIgnoreCase(other.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("substring(I)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue begin &&
                    begin.value() >= 0 &&
                    begin.value() < value.length())
                return Values.valueOfString(value.substring(begin.value()));
            return Values.STRING_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("substring(II)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 2 && params.get(0) instanceof Value.KnownIntValue begin &&
                    begin.value() >= 0 &&
                    begin.value() < value.length() &&
                    params.get(1) instanceof Value.KnownIntValue end &&
                    end.value() >= begin.value() &&
                    end.value() < value.length())
                return Values.valueOfString(value.substring(begin.value(), end.value()));
            return Values.STRING_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("codePointAt(I)I", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue index &&
                    index.value() >= 0 && index.value() < value.length())
                return Values.valueOf(value.codePointAt(index.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("codePointBefore(I)I", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue index &&
                    index.value() >= 0 && index.value() < value.length())
                return Values.valueOf(value.codePointBefore(index.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("codePointCount(II)I", (value, params) -> {
            if (params.size() == 2 && params.get(0) instanceof Value.KnownIntValue begin &&
                    begin.value() >= 0 &&
                    begin.value() < value.length() &&
                    params.get(1) instanceof Value.KnownIntValue end &&
                    end.value() >= begin.value() &&
                    end.value() < value.length())
                return Values.valueOf(value.codePointCount(begin.value(), end.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("offsetByCodePoints(II)I", (value, params) -> {
            if (params.size() == 2 && params.get(0) instanceof Value.KnownIntValue index &&
                    index.value() >= 0 &&
                    index.value() < value.length() &&
                    params.get(1) instanceof Value.KnownIntValue offset)
                return Values.valueOf(value.offsetByCodePoints(index.value(), offset.value()));
            return Values.INT_VALUE;
        });
        INSTANCE_STRING_FUNCS.put("regex(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownStringValue regex)
                return Values.valueOf(value.matches(regex.value()));
            return Values.INT_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(I)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(F)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownFloatValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(D)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownDoubleValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownLongValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        STATIC_FUNCS.put("java/lang/String.valueOf(Z)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.get(0) instanceof Value.KnownIntValue param)
                return Values.valueOfString(String.valueOf(param.value() != 0));
            return Values.STRING_VALUE;
        });
    }
}

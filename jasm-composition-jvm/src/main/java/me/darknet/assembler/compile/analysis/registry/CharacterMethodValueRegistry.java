package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.func.C2CStaticFunc;
import me.darknet.assembler.compile.analysis.func.C2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.C2ZStaticFunc;
import me.darknet.assembler.compile.analysis.func.CC2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.CC2ZStaticFunc;
import me.darknet.assembler.compile.analysis.func.I2CStaticFunc;
import me.darknet.assembler.compile.analysis.func.I2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.I2ZStaticFunc;
import me.darknet.assembler.compile.analysis.func.II2IStaticFunc;

/**
 * Registry for {@link Character} static methods.
 */
@SuppressWarnings("deprecation")
public final class CharacterMethodValueRegistry {
    private CharacterMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        // Since we don't track array contents, a number of methods interacting with those are not here.
        builder.registerStatic("java/lang/Character", "codePointOf", "(Ljava/lang/String;)I", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue text)
                try {
                    return Values.valueOf(Character.codePointOf(text.value()));
                } catch (IllegalArgumentException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Character", "codePointAt", "(Ljava/lang/CharSequence;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue index)
                try {
                    return Values.valueOf(Character.codePointAt(text.value(), index.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Character", "codePointBefore", "(Ljava/lang/CharSequence;I)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue index)
                try {
                    return Values.valueOf(Character.codePointBefore(text.value(), index.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Character", "codePointCount", "(Ljava/lang/CharSequence;II)I", params -> {
            if (params.size() == 3 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue begin &&
                    params.get(2) instanceof Value.KnownIntValue end)
                try {
                    return Values.valueOf(Character.codePointCount(text.value(), begin.value(), end.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Character", "offsetByCodePoints", "(Ljava/lang/CharSequence;II)I", params -> {
            if (params.size() == 3 &&
                    params.get(0) instanceof Value.KnownStringValue text &&
                    params.get(1) instanceof Value.KnownIntValue index &&
                    params.get(2) instanceof Value.KnownIntValue off)
                try {
                    return Values.valueOf(Character.offsetByCodePoints(text.value(), index.value(), off.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Character", "digit", "(CI)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue ch &&
                    params.get(1) instanceof Value.KnownIntValue radix)
                return Values.valueOf(Character.digit((char) ch.value(), radix.value()));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/Character", "charCount", "(I)I", (I2IStaticFunc) Character::charCount);
        builder.registerStatic("java/lang/Character", "digit", "(II)I", (II2IStaticFunc) Character::digit);
        builder.registerStatic("java/lang/Character", "compare", "(CC)I", (CC2IStaticFunc) Character::compare);
        builder.registerStatic("java/lang/Character", "toCodePoint", "(CC)I", (CC2IStaticFunc) Character::toCodePoint);
        builder.registerStatic("java/lang/Character", "getType", "(C)I", (C2IStaticFunc) Character::getType);
        builder.registerStatic("java/lang/Character", "getNumericValue", "(I)I", (I2IStaticFunc) Character::getNumericValue);
        builder.registerStatic("java/lang/Character", "getNumericValue", "(C)I", (C2IStaticFunc) Character::getNumericValue);
        builder.registerStatic("java/lang/Character", "lowSurrogate", "(I)C", (I2CStaticFunc) Character::lowSurrogate);
        builder.registerStatic("java/lang/Character", "highSurrogate", "(I)C", (I2CStaticFunc) Character::highSurrogate);
        builder.registerStatic("java/lang/Character", "toLowerCase", "(C)C", (C2CStaticFunc) Character::toLowerCase);
        builder.registerStatic("java/lang/Character", "toLowerCase", "(I)I", (I2IStaticFunc) Character::toLowerCase);
        builder.registerStatic("java/lang/Character", "toUpperCase", "(C)C", (C2CStaticFunc) Character::toUpperCase);
        builder.registerStatic("java/lang/Character", "toUpperCase", "(I)I", (I2IStaticFunc) Character::toUpperCase);
        builder.registerStatic("java/lang/Character", "toTitleCase", "(C)C", (C2CStaticFunc) Character::toTitleCase);
        builder.registerStatic("java/lang/Character", "toTitleCase", "(I)I", (I2IStaticFunc) Character::toTitleCase);
        builder.registerStatic("java/lang/Character", "isValidCodePoint", "(I)Z", (I2ZStaticFunc) Character::isValidCodePoint);
        builder.registerStatic("java/lang/Character", "isBmpCodePoint", "(I)Z", (I2ZStaticFunc) Character::isBmpCodePoint);
        builder.registerStatic("java/lang/Character", "isSupplementaryCodePoint", "(I)Z", (I2ZStaticFunc) Character::isSupplementaryCodePoint);
        builder.registerStatic("java/lang/Character", "isHighSurrogate", "(C)Z", (C2ZStaticFunc) Character::isHighSurrogate);
        builder.registerStatic("java/lang/Character", "isLowSurrogate", "(C)Z", (C2ZStaticFunc) Character::isLowSurrogate);
        builder.registerStatic("java/lang/Character", "isSurrogatePair", "(CC)Z", (CC2ZStaticFunc) Character::isSurrogatePair);
        builder.registerStatic("java/lang/Character", "isLowerCase", "(C)Z", (C2ZStaticFunc) Character::isLowerCase);
        builder.registerStatic("java/lang/Character", "isLowerCase", "(I)Z", (I2ZStaticFunc) Character::isLowerCase);
        builder.registerStatic("java/lang/Character", "isUpperCase", "(C)Z", (C2ZStaticFunc) Character::isUpperCase);
        builder.registerStatic("java/lang/Character", "isUpperCase", "(I)Z", (I2ZStaticFunc) Character::isUpperCase);
        builder.registerStatic("java/lang/Character", "isTitleCase", "(C)Z", (C2ZStaticFunc) Character::isTitleCase);
        builder.registerStatic("java/lang/Character", "isTitleCase", "(I)Z", (I2ZStaticFunc) Character::isTitleCase);
        builder.registerStatic("java/lang/Character", "isDefined", "(C)Z", (C2ZStaticFunc) Character::isDefined);
        builder.registerStatic("java/lang/Character", "isDefined", "(I)Z", (I2ZStaticFunc) Character::isDefined);
        builder.registerStatic("java/lang/Character", "isSpaceChar", "(C)Z", (C2ZStaticFunc) Character::isSpaceChar);
        builder.registerStatic("java/lang/Character", "isSpaceChar", "(I)Z", (I2ZStaticFunc) Character::isSpaceChar);
        builder.registerStatic("java/lang/Character", "isSpace", "(C)Z", (C2ZStaticFunc) Character::isSpace);
        builder.registerStatic("java/lang/Character", "isWhitespace", "(C)Z", (C2ZStaticFunc) Character::isWhitespace);
        builder.registerStatic("java/lang/Character", "isWhitespace", "(I)Z", (I2ZStaticFunc) Character::isWhitespace);
        builder.registerStatic("java/lang/Character", "isISOControl", "(C)Z", (C2ZStaticFunc) Character::isISOControl);
        builder.registerStatic("java/lang/Character", "isISOControl", "(I)Z", (I2ZStaticFunc) Character::isISOControl);
        builder.registerStatic("java/lang/Character", "isLetter", "(C)Z", (C2ZStaticFunc) Character::isLetter);
        builder.registerStatic("java/lang/Character", "isLetter", "(I)Z", (I2ZStaticFunc) Character::isLetter);
        builder.registerStatic("java/lang/Character", "isLetterOrDigit", "(C)Z", (C2ZStaticFunc) Character::isLetterOrDigit);
        builder.registerStatic("java/lang/Character", "isLetterOrDigit", "(I)Z", (I2ZStaticFunc) Character::isLetterOrDigit);
        builder.registerStatic("java/lang/Character", "isJavaLetter", "(C)Z", (C2ZStaticFunc) Character::isJavaLetter);
        builder.registerStatic("java/lang/Character", "isJavaLetterOrDigit", "(C)Z", (C2ZStaticFunc) Character::isJavaLetterOrDigit);
        builder.registerStatic("java/lang/Character", "isJavaIdentifierStart", "(C)Z", (C2ZStaticFunc) Character::isJavaIdentifierStart);
        builder.registerStatic("java/lang/Character", "isJavaIdentifierStart", "(I)Z", (I2ZStaticFunc) Character::isJavaIdentifierStart);
        builder.registerStatic("java/lang/Character", "isJavaIdentifierPart", "(C)Z", (C2ZStaticFunc) Character::isJavaIdentifierPart);
        builder.registerStatic("java/lang/Character", "isJavaIdentifierPart", "(I)Z", (I2ZStaticFunc) Character::isJavaIdentifierPart);
        builder.registerStatic("java/lang/Character", "isUnicodeIdentifierStart", "(C)Z", (C2ZStaticFunc) Character::isUnicodeIdentifierStart);
        builder.registerStatic("java/lang/Character", "isUnicodeIdentifierStart", "(I)Z", (I2ZStaticFunc) Character::isUnicodeIdentifierStart);
        builder.registerStatic("java/lang/Character", "isUnicodeIdentifierPart", "(C)Z", (C2ZStaticFunc) Character::isUnicodeIdentifierPart);
        builder.registerStatic("java/lang/Character", "isUnicodeIdentifierPart", "(I)Z", (I2ZStaticFunc) Character::isUnicodeIdentifierPart);
        builder.registerStatic("java/lang/Character", "isIdentifierIgnorable", "(C)Z", (C2ZStaticFunc) Character::isIdentifierIgnorable);
        builder.registerStatic("java/lang/Character", "isIdentifierIgnorable", "(I)Z", (I2ZStaticFunc) Character::isIdentifierIgnorable);
        builder.registerStatic("java/lang/Character", "isAlphabetic", "(I)Z", (I2ZStaticFunc) Character::isAlphabetic);
        builder.registerStatic("java/lang/Character", "isIdeographic", "(I)Z", (I2ZStaticFunc) Character::isIdeographic);
        return builder.build();
    }
}

package me.darknet.assembler.analysis.registry;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.Values;

/**
 * Registry for {@link String} instance and static methods.
 */
public final class StringMethodValueRegistry {
    private StringMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        builder.registerStringInstance("length", "()I", (value, params) -> Values.valueOf(value.length()));
        builder.registerStringInstance("hashCode", "()I", (value, params) -> Values.valueOf(value.hashCode()));
        builder.registerStringInstance("toLowerCase", "()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toLowerCase()));
        // Approximate: ignores the explicit locale argument and uses the host default locale.
        builder.registerStringInstance("toLowerCase", "(Ljava/util/Locale;)Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toLowerCase()));
        builder.registerStringInstance("toUpperCase", "()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toUpperCase()));
        // Approximate: ignores the explicit locale argument and uses the host default locale.
        builder.registerStringInstance("toUpperCase", "(Ljava/util/Locale;)Ljava/lang/String;", (value, params) -> Values.valueOfString(value.toUpperCase()));
        builder.registerStringInstance("trim", "()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.trim()));
        builder.registerStringInstance("strip", "()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.strip()));
        builder.registerStringInstance("stripIndent", "()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.stripIndent()));
        builder.registerStringInstance("stripLeading", "()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.stripLeading()));
        builder.registerStringInstance("stripTrailing", "()Ljava/lang/String;", (value, params) -> Values.valueOfString(value.stripTrailing()));
        builder.registerStringInstance("intern", "()Ljava/lang/String;", (value, params) -> Values.valueOfString(value));
        builder.registerStringInstance("toString", "()Ljava/lang/String;", (value, params) -> Values.valueOfString(value));
        builder.registerStringInstance("isBlank", "()Z", (value, params) -> Values.valueOf(value.isBlank()));
        builder.registerStringInstance("isEmpty", "()Z", (value, params) -> Values.valueOf(value.isEmpty()));
        builder.registerStringInstance("repeat", "(I)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int count))
                try {
                    return Values.valueOfString(value.repeat(count));
                } catch (IllegalArgumentException ignored) {
                }
            return Values.STRING_VALUE;
        });
        builder.registerStringInstance("concat", "(Ljava/lang/String;)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String str))
                return Values.valueOfString(value.concat(str));
            return Values.STRING_VALUE;
        });
        builder.registerStringInstance("equals", "(Ljava/lang/Object;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String str))
                return Values.valueOf(value.equals(str));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("equalsIgnoreCase", "(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String str))
                return Values.valueOf(value.equalsIgnoreCase(str));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("contentEquals", "(Ljava/lang/CharSequence;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String str))
                return Values.valueOf(value.contentEquals(str));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("contains", "(Ljava/lang/CharSequence;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String str))
                return Values.valueOf(value.contains(str));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("charAt", "(I)C", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int index))
                try {
                    return Values.valueOf(value.charAt(index));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("indexOf", "(I)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int ch))
                return Values.valueOf(value.indexOf(ch));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("indexOf", "(II)I", (value, params) -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownIntValue(int ch) &&
		            params.get(1) instanceof Value.KnownIntValue(int from))
                return Values.valueOf(value.indexOf(ch, from));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("indexOf", "(Ljava/lang/String;)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String ch))
                return Values.valueOf(value.indexOf(ch));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("indexOf", "(Ljava/lang/String;I)I", (value, params) -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownStringValue(String ch) &&
		            params.get(1) instanceof Value.KnownIntValue(int from))
                return Values.valueOf(value.indexOf(ch, from));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("startsWith", "(Ljava/lang/String;I)Z", (value, params) -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownStringValue(String prefix) &&
		            params.get(1) instanceof Value.KnownIntValue(int toOffset))
                return Values.valueOf(value.startsWith(prefix, toOffset));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("startsWith", "(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String prefix))
                return Values.valueOf(value.startsWith(prefix));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("endsWith", "(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String suffix))
                return Values.valueOf(value.endsWith(suffix));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("compareTo", "(Ljava/lang/String;)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String other))
                return Values.valueOf(value.compareTo(other));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("compareToIgnoreCase", "(Ljava/lang/String;)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String other))
                return Values.valueOf(value.compareToIgnoreCase(other));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("substring", "(I)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int begin))
                try {
                    return Values.valueOfString(value.substring(begin));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.STRING_VALUE;
        });
        builder.registerStringInstance("substring", "(II)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownIntValue(int begin) &&
		            params.get(1) instanceof Value.KnownIntValue(int end))
                try {
                    return Values.valueOfString(value.substring(begin, end));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.STRING_VALUE;
        });
        builder.registerStringInstance("codePointAt", "(I)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int index))
                try {
                    return Values.valueOf(value.codePointAt(index));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("codePointBefore", "(I)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int index))
                try {
                    return Values.valueOf(value.codePointBefore(index));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("codePointCount", "(II)I", (value, params) -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownIntValue(int begin) &&
		            params.get(1) instanceof Value.KnownIntValue(int end))
                try {
                    return Values.valueOf(value.codePointCount(begin, end));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("offsetByCodePoints", "(II)I", (value, params) -> {
            if (params.size() == 2 &&
		            params.get(0) instanceof Value.KnownIntValue(int index) &&
		            params.get(1) instanceof Value.KnownIntValue(int offset))
                try {
                    return Values.valueOf(value.offsetByCodePoints(index, offset));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("matches", "(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue(String regex))
                return Values.valueOf(value.matches(regex));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(I)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int value))
                return Values.valueOfString(String.valueOf(value));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(F)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownFloatValue(float value))
                return Values.valueOfString(String.valueOf(value));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(D)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownDoubleValue(double value))
                return Values.valueOfString(String.valueOf(value));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownLongValue(long value))
                return Values.valueOfString(String.valueOf(value));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(Z)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue(int value))
                return Values.valueOfString(String.valueOf(value != 0));
            return Values.STRING_VALUE;
        });
        return builder.build();
    }
}

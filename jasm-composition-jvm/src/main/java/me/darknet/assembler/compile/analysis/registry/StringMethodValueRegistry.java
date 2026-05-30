package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;

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
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue count)
                try {
                    return Values.valueOfString(value.repeat(count.value()));
                } catch (IllegalArgumentException ignored) {
                }
            return Values.STRING_VALUE;
        });
        builder.registerStringInstance("concat", "(Ljava/lang/String;)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue strParam)
                return Values.valueOfString(value.concat(strParam.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStringInstance("equals", "(Ljava/lang/Object;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.equals(strParam.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("equalsIgnoreCase", "(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.equalsIgnoreCase(strParam.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("contentEquals", "(Ljava/lang/CharSequence;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.contentEquals(strParam.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("contains", "(Ljava/lang/CharSequence;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue strParam)
                return Values.valueOf(value.contains(strParam.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("charAt", "(I)C", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue index)
                try {
                    return Values.valueOf(value.charAt(index.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("indexOf", "(I)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue ch)
                return Values.valueOf(value.indexOf(ch.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("indexOf", "(II)I", (value, params) -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue ch &&
                    params.get(1) instanceof Value.KnownIntValue from)
                return Values.valueOf(value.indexOf(ch.value(), from.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("indexOf", "(Ljava/lang/String;)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue ch)
                return Values.valueOf(value.indexOf(ch.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("indexOf", "(Ljava/lang/String;I)I", (value, params) -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue ch &&
                    params.get(1) instanceof Value.KnownIntValue from)
                return Values.valueOf(value.indexOf(ch.value(), from.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("startsWith", "(Ljava/lang/String;I)Z", (value, params) -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownStringValue prefix &&
                    params.get(1) instanceof Value.KnownIntValue toOffset)
                return Values.valueOf(value.startsWith(prefix.value(), toOffset.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("startsWith", "(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue prefix)
                return Values.valueOf(value.startsWith(prefix.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("endsWith", "(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue suffix)
                return Values.valueOf(value.endsWith(suffix.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("compareTo", "(Ljava/lang/String;)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue other)
                return Values.valueOf(value.compareTo(other.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("compareToIgnoreCase", "(Ljava/lang/String;)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue other)
                return Values.valueOf(value.compareToIgnoreCase(other.value()));
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("substring", "(I)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue begin)
                try {
                    return Values.valueOfString(value.substring(begin.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.STRING_VALUE;
        });
        builder.registerStringInstance("substring", "(II)Ljava/lang/String;", (value, params) -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue begin &&
                    params.get(1) instanceof Value.KnownIntValue end)
                try {
                    return Values.valueOfString(value.substring(begin.value(), end.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.STRING_VALUE;
        });
        builder.registerStringInstance("codePointAt", "(I)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue index)
                try {
                    return Values.valueOf(value.codePointAt(index.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("codePointBefore", "(I)I", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue index)
                try {
                    return Values.valueOf(value.codePointBefore(index.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("codePointCount", "(II)I", (value, params) -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue begin &&
                    params.get(1) instanceof Value.KnownIntValue end)
                try {
                    return Values.valueOf(value.codePointCount(begin.value(), end.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("offsetByCodePoints", "(II)I", (value, params) -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue index &&
                    params.get(1) instanceof Value.KnownIntValue offset)
                try {
                    return Values.valueOf(value.offsetByCodePoints(index.value(), offset.value()));
                } catch (IndexOutOfBoundsException ignored) {
                }
            return Values.INT_VALUE;
        });
        builder.registerStringInstance("matches", "(Ljava/lang/String;)Z", (value, params) -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownStringValue regex)
                return Values.valueOf(value.matches(regex.value()));
            return Values.INT_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(I)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(F)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownFloatValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(D)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownDoubleValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(J)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownLongValue param)
                return Values.valueOfString(String.valueOf(param.value()));
            return Values.STRING_VALUE;
        });
        builder.registerStatic("java/lang/String", "valueOf", "(Z)Ljava/lang/String;", params -> {
            if (params.size() == 1 && params.getFirst() instanceof Value.KnownIntValue param)
                return Values.valueOfString(String.valueOf(param.value() != 0));
            return Values.STRING_VALUE;
        });
        return builder.build();
    }
}

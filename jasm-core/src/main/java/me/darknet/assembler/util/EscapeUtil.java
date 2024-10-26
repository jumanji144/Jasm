package me.darknet.assembler.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EscapeUtil {

    private static final Map<Character, String> BASE_ESCAPE_MAP = new HashMap<>(
            Map.ofEntries(
                    Map.entry('\b', "\\b"), Map.entry('\t', "\\t"), Map.entry('\n', "\\n"), Map.entry('\f', "\\f"),
                    Map.entry('\r', "\\r"), Map.entry('\"', "\\\""), Map.entry('\\', "\\\\"), Map.entry('\'', "\\'")
            )
    );
    private static final Map<Character, String> LITERAL_ESCAPE_MAP = new HashMap<>();
    private static final List<Range> LITERAL_UNICODE_ESCAPE_RANGES = List.of(
            new Range(0x0000, 0x001F), new Range(0x007F, 0x009F), new Range(0x06E5, 0x06E5), new Range(0x17B4, 0x17B4),
            new Range(0x180B, 0x180E), new Range(0x2000, 0x200E), new Range(0x2028, 0x202E), new Range(0x205F, 0x206E),
            new Range(0x2400, 0x243E), new Range(0xE000, 0xF8FF), new Range(0xFE00, 0xFE0F), new Range(0xFE1A, 0xFE20),
            new Range(0xFFF0, 0xFFFF)
    );

    static {
        for (Range range : LITERAL_UNICODE_ESCAPE_RANGES) {
            for (int i = range.start(); i <= range.end(); i++) {
                BASE_ESCAPE_MAP.put((char) i, "\\u" + String.format("%04X", i));
            }
        }
        LITERAL_ESCAPE_MAP.putAll(BASE_ESCAPE_MAP);
        LITERAL_ESCAPE_MAP.put(' ', "\\u0020");
        LITERAL_ESCAPE_MAP.put(',', "\\u002C");
        LITERAL_ESCAPE_MAP.put(':', "\\u003A");
        LITERAL_ESCAPE_MAP.put('{', "\\u007B");
        LITERAL_ESCAPE_MAP.put('}', "\\u007D");
        LITERAL_ESCAPE_MAP.put('\"', "\\u0022");
        LITERAL_ESCAPE_MAP.put('\'', "\\u0027");
        // build unicode escape map
    }

    public static String escape(String string, Map<Character, String> escapeMap) {
        StringBuilder sb = new StringBuilder();
        for (char c : string.toCharArray()) {
            String escaped = escapeMap.get(c);
            if (escaped != null)
                sb.append(escaped);
            else
                sb.append(c);
        }
        return sb.toString();
    }

    public static String escapeString(String string) {
        return escape(string, BASE_ESCAPE_MAP);
    }

    public static String escapeLiteral(String string) {
        return escape(string, LITERAL_ESCAPE_MAP);
    }

    public static String unescape(String escaped) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < escaped.toCharArray().length; i++) {
            char c = escaped.charAt(i);
            if (c == '\\') {
                char next = escaped.charAt(++i);
                switch (next) {
                    case 'n' -> buffer.append('\n');
                    case 'r' -> buffer.append('\r');
                    case 't' -> buffer.append('\t');
                    case 'b' -> buffer.append('\b');
                    case 'f' -> buffer.append('\f');
                    case '"' -> buffer.append('"');
                    case '\'' -> buffer.append('\'');
                    case '\\' -> buffer.append('\\');
                    case 'u' -> {
                        buffer.append((char) Integer.parseInt(escaped.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                    default -> {
                        buffer.append('\\');
                        buffer.append(next);
                    }
                }

            } else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

}

package me.darknet.assembler.util;

public class DescriptorUtil {

    public static boolean isValidMethodDescriptor(CharSequence csq) {
        return isValidMethodType(csq, 0);
    }

    public static boolean isValidMethodType(CharSequence csq, int offset) {
        int start = offset;
        if ('(' != csq.charAt(offset++)) return false;
        while (true) {
            if (offset >= csq.length()) return false;
            char ch = csq.charAt(offset);
            if (ch == ')') {
                return skipClassType(csq, offset + 1, false) == csq.length() - start;
            }
            if ((offset = skipClassType(csq, offset, true)) == -1) return false;
        }
    }

    public static int skipClassType(CharSequence csq, int offset, boolean noVoid) {
        char ch;
        int len = csq.length();
        do {
            if (offset == len) return -1;
            if ((ch = csq.charAt(offset)) != '[') break;
            noVoid = true;
            offset++;
        } while (true);
        if (ch == 'L') {
            while ((ch = csq.charAt(offset)) != ';') {
                if (++offset == len || ch == '[') return -1;
            }
        } else {
            switch (ch) {
                case 'J':
                case 'D':
                case 'I':
                case 'F':
                case 'C':
                case 'S':
                case 'B':
                case 'Z':
                    break;
                case 'V':
                    if (noVoid) return -1;
                    break;
                default:
                    return -1;
            }
        }
        return offset + 1;
    }

    public static boolean isValidFieldDescriptor(String fieldDescriptor) {
        if (fieldDescriptor == null || fieldDescriptor.isEmpty())
            return false;

        return skipClassType(fieldDescriptor, 0, false) == fieldDescriptor.length();
    }

}

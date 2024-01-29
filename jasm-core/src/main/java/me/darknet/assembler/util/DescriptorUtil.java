package me.darknet.assembler.util;

public class DescriptorUtil {

    public static boolean isValidMethodDescriptor(String methodDescriptor) {
        if(methodDescriptor == null || methodDescriptor.length() < 3)
            return false;

        int parenthesesCount = 0;

        char[] chars = methodDescriptor.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
                case '(' -> parenthesesCount++;
                case ')' -> {
                    parenthesesCount--;
                    if (parenthesesCount < 0)
                        return false;
                }
                case 'V', 'Z', 'B', 'C', 'S', 'I', 'F', 'J', 'D' -> {
                }
                case 'L' -> {
                    int end = methodDescriptor.indexOf(';', i);
                    if (end == -1)
                        return false;
                    i = end;
                }
                case '[' -> {
                    if (i + 1 >= chars.length)
                        return false;
                }
                default -> {
                    return false;
                }
            }
        }

        return parenthesesCount == 0;
    }

    public static boolean isValidFieldDescriptor(String fieldDescriptor) {
        if (fieldDescriptor == null || fieldDescriptor.isEmpty())
            return false;

        char[] chars = fieldDescriptor.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
                case 'V', 'Z', 'B', 'C', 'S', 'I', 'F', 'J', 'D' -> {
                }
                case 'L' -> {
                    int end = fieldDescriptor.indexOf(';', i);
                    if (end == -1)
                        return false;
                    i = end;
                }
                case '[' -> {
                    if (i + 1 >= chars.length)
                        return false;
                }
                default -> {
                    return false;
                }
            }
        }

        return true;
    }

}

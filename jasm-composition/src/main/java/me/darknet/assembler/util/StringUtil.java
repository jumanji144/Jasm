package me.darknet.assembler.util;

public class StringUtil {

	public static boolean removeLast(StringBuilder sb, String toRemove, int searchLength) {
		int length = sb.length();
		if (length < searchLength) {
			return false;
		}
		int index = sb.lastIndexOf(toRemove);
		if (index == -1 || index < length - searchLength) {
			return false;
		}
		sb.delete(index, index + toRemove.length());
		return true;
	}
}

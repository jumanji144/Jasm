package me.darknet.assembler.printer.jvm.util;

public class LabelUtil {

	public static String getLabelName(int index) {
		// 0-25: A-Z, 26-51: AA-ZZ, 52-77: AAA-ZZZ, etc.
		int base = 26;
		int offset = 65;
		int count = 0;
		int i = index;
		while (i >= count) {
			i -= count;
			count = count * base + base;
		}
		StringBuilder builder = new StringBuilder();
		while (i >= 0) {
			builder.append((char) (i % base + offset));
			i /= base;
			i--;
		}
		return builder.reverse().toString();
	}

}

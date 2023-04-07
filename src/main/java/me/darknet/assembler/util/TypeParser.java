package me.darknet.assembler.util;

import me.darknet.assembler.info.FieldInfo;
import me.darknet.assembler.info.MemberInfo;
import me.darknet.assembler.info.MethodInfo;
import org.objectweb.asm.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class TypeParser {

	public static boolean isValidDescriptorChar(char a) {
		switch (a) {
			case 'B':
			case 'C':
			case 'D':
			case 'F':
			case 'I':
			case 'J':
			case 'S':
			case 'Z':
			case 'V':
			case 'L':
			case '[':
				return true;
			default:
				return false;
		}
	}

	public static boolean isPrimitiveDescriptorChar(char a) {
		switch (a) {
			case 'B':
			case 'C':
			case 'D':
			case 'F':
			case 'I':
			case 'J':
			case 'S':
			case 'Z':
			case 'V':
				return true;
			default:
				return false;
		}
	}

	/**
	 * Parses descriptors in format {@code [owner?].name (params)ret/fieldType } into a MemberInfo object.
	 * @param name The name of the method.
	 * @param descriptor The descriptor to parse.
	 * @return The MethodInfo object.
	 */
	public static MemberInfo parseMemberInfo(String name, String descriptor) {
		String owner = null;
		if(name.contains(".")) {
			String[] split = name.split("\\.");
			owner = split[0];
			name = split[1];
		}
		if(descriptor.startsWith("(")) {
			return new MethodInfo(owner, name, descriptor);
		} else {
			return new FieldInfo(owner, name, descriptor);
		}
	}

}

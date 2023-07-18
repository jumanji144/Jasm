package me.darknet.assembler.printer.util;

import java.util.Map;

import static dev.xdark.blw.classfile.AccessFlag.*;

public class Modifiers {

	public static final int CLASS = 1;
	public static final int METHOD = 2;
	public static final int FIELD = 3;

	private static final Map<Integer, String> UNIQUE_MODIFIER_NAMES = Map.ofEntries(
			Map.entry(ACC_PUBLIC, "public"),
			Map.entry(ACC_PRIVATE, "private"),
			Map.entry(ACC_PROTECTED, "protected"),
			Map.entry(ACC_STATIC, "static"),
			Map.entry(ACC_FINAL, "final"),
			Map.entry(ACC_NATIVE, "native"),
			Map.entry(ACC_ABSTRACT, "abstract"),
			Map.entry(ACC_INTERFACE, "interface"),
			Map.entry(ACC_SYNTHETIC, "synthetic"),
			Map.entry(ACC_STRICT, "strict"),
			Map.entry(ACC_ANNOTATION, "annotation"),
			Map.entry(ACC_ENUM, "enum")
	);

	private static final Map<Integer, String> CLASS_MODIFIER_NAMES = Map.ofEntries(
			Map.entry(ACC_SUPER, "super"),
			Map.entry(ACC_MODULE, "module")
	);

	private static final Map<Integer, String> METHOD_MODIFIER_NAMES = Map.ofEntries(
			Map.entry(ACC_SYNCHRONIZED, "synchronized"),
			Map.entry(ACC_BRIDGE, "bridge"),
			Map.entry(ACC_VARARGS, "varargs")
	);

	private static final Map<Integer, String> FIELD_MODIFIER_NAMES = Map.ofEntries(
			Map.entry(ACC_VOLATILE, "volatile"),
			Map.entry(ACC_TRANSIENT, "transient")
	);

	public static String getModifierName(int modifier) {
		return UNIQUE_MODIFIER_NAMES.get(modifier);
	}

	public static String getClassModifierName(int modifier) {
		if (UNIQUE_MODIFIER_NAMES.containsKey(modifier)) {
			return UNIQUE_MODIFIER_NAMES.get(modifier);
		}
		return CLASS_MODIFIER_NAMES.get(modifier);
	}

	public static String getMethodModifierName(int modifier) {
		if (UNIQUE_MODIFIER_NAMES.containsKey(modifier)) {
			return UNIQUE_MODIFIER_NAMES.get(modifier);
		}
		return METHOD_MODIFIER_NAMES.get(modifier);
	}

	public static String getFieldModifierName(int modifier) {
		if (UNIQUE_MODIFIER_NAMES.containsKey(modifier)) {
			return UNIQUE_MODIFIER_NAMES.get(modifier);
		}
		return FIELD_MODIFIER_NAMES.get(modifier);
	}

	public static String modifiers(int modifiers, int type) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 32; i++) {
			int flag = 1 << i;
			if ((modifiers & flag) != 0) {
				String modifierName = switch (type) {
					case 0 -> getModifierName(flag);
					case 1 -> getClassModifierName(flag);
					case 2 -> getMethodModifierName(flag);
					case 3 -> getFieldModifierName(flag);
					default -> null;
				};
				if (modifierName != null) {
					builder.append(modifierName).append(" ");
				}
			}
		}
		return builder.toString();
	}


}

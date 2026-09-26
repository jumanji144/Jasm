package me.darknet.assembler.backend.jvm.util;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * JVM mnemonic-to-ASM opcode lookup tables.
 */
public class JvmOpcodes {
	private static final Map<String, Integer> opcodes = new HashMap<>();
	private static final Map<String, Integer> filteredOpcodes = new HashMap<>();

	/**
	 * Resolves a JASM or ASM mnemonic to its opcode value.
	 * <p>
	 * Interface invocation aliases are mapped to the corresponding base opcode
	 * when ASM does not expose a distinct constant for the alias.
	 *
	 * @param name
	 * 		Mnemonic to resolve.
	 *
	 * @return ASM opcode value for {@code name}.
	 */
	public static int opcode(String name) {
		if (name.endsWith("interface")) {
			String prefix = name.substring(0, name.length() - 9);
			if (prefix.length() == 6)
				return Opcodes.INVOKEINTERFACE;
			else
				return opcodes.get(prefix);
		}
		return opcodes.get(name);
	}

	/**
	 * @return Map of every ASM opcode mnemonic and the synthetic JASM {@code line} entry.
	 */
	public static @NotNull Map<String, Integer> getOpcodes() {
		return opcodes;
	}

	/**
	 * Filtered map of ASM opcode mnemonics, where encoding-specific names are removed
	 * and interface invocation aliases are mapped to the corresponding base opcode.
	 *
	 * @return Map of filtered ASM opcode mnemonics.
	 */
	public static @NotNull Map<String, Integer> getFilteredOpcodes() {
		return filteredOpcodes;
	}

	static {
		Field[] fields = Opcodes.class.getFields();
		for (Field field : fields) {
			try {
				if (field.getType() == int.class) {
					opcodes.put(field.getName().toLowerCase(), field.getInt(null));
				}
			} catch (IllegalAccessException e) {
				throw new ExceptionInInitializerError(e);
			}
		}
		opcodes.put("line", -1);

		// Remove encoding-specific names from the source-level completion table.
		filteredOpcodes.putAll(opcodes);
		filteredOpcodes.keySet().removeIf(name -> name.endsWith("_w"));
		filteredOpcodes.remove("jsr");
		filteredOpcodes.remove("ret");
		filteredOpcodes.put("invokestaticinterface", Opcodes.INVOKESTATIC);
		filteredOpcodes.put("invokevirtualinterface", Opcodes.INVOKEINTERFACE);
		filteredOpcodes.put("invokespecialinterface", Opcodes.INVOKESPECIAL);
	}
}


package me.darknet.assembler.backend.jvm.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
	 * @param name
	 * 		Mnemonic to resolve.
	 *
	 * @return ASM opcode value, or {@code null} when the mnemonic has no ASM opcode.
	 */
	public static @Nullable Integer opcodeOrNull(String name) {
		if (name.endsWith("interface")) {
			String prefix = name.substring(0, name.length() - 9);
			return prefix.length() == 6 ? Opcodes.INVOKEINTERFACE : opcodes.get(prefix);
		}
		return opcodes.get(name);
	}

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
	 *
	 * @throws IllegalStateException
	 * 		If {@code name} has no ASM opcode, so an unknown mnemonic fails diagnosably instead of
	 * 		unboxing {@code null}.
	 */
	public static int opcode(String name) {
		Integer opcode = opcodeOrNull(name);
		if (opcode == null)
			throw new IllegalStateException("No ASM opcode for mnemonic: " + name);
		return opcode;
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


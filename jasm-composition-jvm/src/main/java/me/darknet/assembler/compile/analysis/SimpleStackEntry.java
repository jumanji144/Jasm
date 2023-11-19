package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.PrimitiveKind;
import dev.xdark.blw.type.PrimitiveType;
import dev.xdark.blw.type.Types;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record SimpleStackEntry(@NotNull ClassType type) implements StackEntry {
	private static final Map<String, SimpleStackEntry> ENTRY_MAP = new HashMap<>();
	public static SimpleStackEntry VOID = new SimpleStackEntry(Types.VOID);
	public static SimpleStackEntry LONG = new SimpleStackEntry(Types.LONG);
	public static SimpleStackEntry DOUBLE = new SimpleStackEntry(Types.DOUBLE);
	public static SimpleStackEntry INT = new SimpleStackEntry(Types.INT);
	public static SimpleStackEntry FLOAT = new SimpleStackEntry(Types.FLOAT);
	public static SimpleStackEntry CHAR = new SimpleStackEntry(Types.CHAR);
	public static SimpleStackEntry SHORT = new SimpleStackEntry(Types.SHORT);
	public static SimpleStackEntry BYTE = new SimpleStackEntry(Types.BYTE);
	public static SimpleStackEntry BOOLEAN = new SimpleStackEntry(Types.BOOLEAN);

	@NotNull
	public static SimpleStackEntry get(@NotNull ClassType type) {
		if (type instanceof PrimitiveType primitiveType) {
			return switch (primitiveType.kind()) {
				case PrimitiveKind.T_VOID -> VOID;
				case PrimitiveKind.T_LONG -> LONG;
				case PrimitiveKind.T_DOUBLE -> DOUBLE;
				case PrimitiveKind.T_INT -> INT;
				case PrimitiveKind.T_FLOAT -> FLOAT;
				case PrimitiveKind.T_CHAR -> CHAR;
				case PrimitiveKind.T_SHORT -> SHORT;
				case PrimitiveKind.T_BYTE -> BYTE;
				case PrimitiveKind.T_BOOLEAN -> BOOLEAN;
				default -> throw new IllegalStateException("Unexpected value: " + primitiveType.descriptor());
			};
		}
		return ENTRY_MAP.computeIfAbsent(type.descriptor(), d -> new SimpleStackEntry(type));
	}
}

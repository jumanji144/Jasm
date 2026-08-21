package me.darknet.assembler.query;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * General AST utilities.
 */
public final class AssemblyUtils {
	private static final Set<String> JVM_FLOW_CONTROL_INSNS = Set.of(
			"goto", "goto_w", "jsr", "jsr_w",
			"ifnull", "ifnonnull", "ifeq", "ifne", "ifle", "ifge", "iflt", "ifgt",
			"if_acmpeq", "if_acmpne", "if_icmpeq", "if_icmpge", "if_icmpgt", "if_icmple", "if_icmplt", "if_icmpne"
	);
	private static final Set<String> DALVIK_FLOW_CONTROL_INSNS = Set.of(
			"goto", "if-eq", "if-ne", "if-lt", "if-ge", "if-gt", "if-le", "if-eqz", "if-nez"
	);
	private static final Set<String> JVM_SWITCH_INSNS = Set.of("tableswitch", "lookupswitch");
	private static final Set<String> DALVIK_SWITCH_INSNS = Set.of("packed-switch", "sparse-switch");
	private static final Set<String> JVM_TYPE_REFERENCE_INSNS = Set.of(
			"new", "anewarray", "checkcast", "instanceof", "multianewarray"
	);
	private static final Set<String> DALVIK_TYPE_REFERENCE_INSNS = Set.of(
			"const-class", "check-cast", "instance-of", "new-instance", "new-array", "filled-new-array"
	);

	private AssemblyUtils() {}

	/**
	 * @param method
	 * 		Method to search through.
	 * @param target
	 * 		Label name to find.
	 *
	 * @return Label declaration with the given name, or {@code null} if not found.
	 */
	@Nullable
	public static ASTLabel getLabelDeclaration(@NotNull ASTMethod method, @NotNull String target) {
		for (ASTInstruction instruction : method.code().instructions())
			if (instruction instanceof ASTLabel label && Objects.equals(label.identifier().content(), target))
				return label;
		return null;
	}

	/**
	 * @param format
	 * 		Bytecode format to check against.
	 * @param name
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a flow control instruction.
	 */
	public static boolean isFlowControlInstruction(@NotNull BytecodeFormat format, @Nullable String name) {
		return name != null && switch (format) {
			case JVM -> JVM_FLOW_CONTROL_INSNS.contains(name);
			case DALVIK -> DALVIK_FLOW_CONTROL_INSNS.contains(name);
		};
	}

	/**
	 * @param format
	 * 		Bytecode format to check against.
	 * @param name
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a switch instruction.
	 */
	public static boolean isSwitchInstruction(@NotNull BytecodeFormat format, @Nullable String name) {
		return name != null && switch (format) {
			case JVM -> JVM_SWITCH_INSNS.contains(name);
			case DALVIK -> DALVIK_SWITCH_INSNS.contains(name);
		};
	}

	/**
	 * @param format
	 * 		Bytecode format to check against.
	 * @param name
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a type reference.
	 */
	public static boolean isTypeReferenceInstruction(@NotNull BytecodeFormat format, @Nullable String name) {
		return name != null && switch (format) {
			case JVM -> JVM_TYPE_REFERENCE_INSNS.contains(name);
			case DALVIK -> DALVIK_TYPE_REFERENCE_INSNS.contains(name);
		};
	}

	/**
	 * @param name
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a variable reference (load, store, increment, or return).
	 */
	public static boolean isVariableReferenceInstruction(@Nullable String name) {
		if (name == null)
			return false;
		return "ret".equals(name) || "iinc".equals(name) || name.endsWith("load") || name.endsWith("store");
	}

	/**
	 * @param astElements
	 * 		AST elements to search through.
	 * @param position
	 * 		Position to check for.
	 * @param line
	 * 		Line to check for.
	 *
	 * @return Instruction at the given position and line, or {@code null} if not found.
	 */
	@Nullable
	public static ASTInstruction findInstruction(@Nullable List<ASTElement> astElements, int position, int line) {
		if (astElements == null)
			return null;

		ASTInstruction[] selected = new ASTInstruction[1];
		for (ASTElement element : astElements) {
			// Some weird edge cases where JASM can have null entries, so sanity check,
			if (element == null)
				continue;

			// Skip elements not within the given range.
			if (!element.range().within(position))
				continue;

			// Walk down the tree to find a deeper match.
			element.walk(ast -> {
				if (ast instanceof ASTInstruction instruction) {
					Location location = ast.location();
					if (location != null && location.line() == line) {
						selected[0] = instruction;
					} else {
						String identifier = instruction.identifier().content();
						if (isSwitchInstruction(BytecodeFormat.JVM, identifier) && ast.range().within(position))
							selected[0] = instruction;
						else if (isSwitchInstruction(BytecodeFormat.DALVIK, identifier) && ast.range().within(position))
							selected[0] = instruction;

					}
				}
				return selected[0] == null;
			});

			if (selected[0] != null)
				break;
		}

		return selected[0];
	}

	/**
	 * Pick the deepest AST element at the given offset within the provided list of AST elements.
	 *
	 * @param ast
	 * 		List of AST elements to search through.
	 * @param offset
	 * 		Absolute offset to pick an element at.
	 *
	 * @return Deepest AST element at the given offset, or {@code null} if no element matches.
	 */
	public static @Nullable ASTElement pickElementAt(@Nullable List<? extends ASTElement> ast, int offset) {
		if (ast == null || offset < 0)
			return null;

		for (ASTElement element : ast)
			if (contains(element.range(), offset))
				return element.pick(offset);

		return null;
	}

	/**
	 * Pick the deepest AST element at the given position within the provided list of AST elements.
	 *
	 * @param ast
	 * 		List of AST elements to search through.
	 * @param line
	 * 		Line number to pick an element at (1-based).
	 * @param column
	 * 		Column number to pick an element at (1-based).
	 *
	 * @return Deepest AST element at the given position, or {@code null} if no element matches.
	 */
	public static @Nullable ASTElement pickElementAt(@Nullable List<? extends ASTElement> ast, int line, int column) {
		if (ast == null || line < 1 || column < 1)
			return null;

		for (ASTElement element : ast) {
			ASTElement matched = pickByLineColumn(element, line, column);
			if (matched != null)
				return matched;
		}
		return null;
	}

	/**
	 * Find the instruction at the given offset within the provided list of AST elements.
	 *
	 * @param ast
	 * 		List of AST elements to search through.
	 * @param offset
	 * 		Absolute offset to find an instruction at.
	 *
	 * @return Instruction at the given offset, or {@code null} if no instruction matches.
	 */
	public static @Nullable ASTInstruction findInstructionAt(@Nullable List<? extends ASTElement> ast, int offset) {
		return findEnclosingInstruction(pickElementAt(ast, offset));
	}

	/**
	 * Find the instruction at the given position within the provided list of AST elements.
	 *
	 * @param ast
	 * 		List of AST elements to search through.
	 * @param line
	 * 		Line number to pick an instruction at (1-based).
	 * @param column
	 * 		Column number to pick an instruction at (1-based).
	 *
	 * @return Instruction at the given position, or {@code null} if no instruction matches.
	 */
	public static @Nullable ASTInstruction findInstructionAt(@Nullable List<? extends ASTElement> ast, int line, int column) {
		return findEnclosingInstruction(pickElementAt(ast, line, column));
	}

	/**
	 * Find the label declaration with the given name within the provided method.
	 *
	 * @param method
	 * 		Method to search through.
	 * @param name
	 * 		Label name to find.
	 *
	 * @return Label declaration with the given name, or {@code null} if not found.
	 */
	public static @Nullable ASTLabel findLabelDeclaration(@NotNull ASTMethod method, @NotNull String name) {
		if (method.code() == null)
			return null;

		for (ASTInstruction instruction : method.code().instructions())
			if (instruction instanceof ASTLabel label && name.equals(label.identifier().literal()))
				return label;
		return null;
	}

	/**
	 * @param instruction
	 * 		Instruction to check.
	 *
	 * @return Variable access kind if the instruction is a variable reference, or {@code null} if not.
	 */
	public static @Nullable VariableAccessKind variableAccessKind(@NotNull ASTInstruction instruction) {
		String name = instruction.identifier().content();
		if (!isVariableReferenceInstruction(name))
			return null;

		if ("ret".equals(name))
			return VariableAccessKind.READ;
		if ("iinc".equals(name))
			return VariableAccessKind.INCREMENT;
		return name.endsWith("load") ? VariableAccessKind.READ : VariableAccessKind.WRITE;
	}

	/**
	 * @param format
	 * 		Bytecode format to check against.
	 * @param offset
	 * 		Absolute offset to check for.
	 * @param instruction
	 * 		Instruction to check.
	 *
	 * @return Type reference identifier if the instruction is a type reference
	 * and the offset is within the type reference argument, or {@code null} otherwise.
	 */
	public static @Nullable ASTIdentifier resolveInstructionTypeReference(@NotNull BytecodeFormat format, int offset,
	                                                                      @NotNull ASTInstruction instruction) {
		String name = instruction.identifier().content();
		if (name == null || !isTypeReferenceInstruction(format, name))
			return null;

		int typeArgumentIndex = typeReferenceArgumentIndex(format, name);
		if (typeArgumentIndex < 0 || typeArgumentIndex >= instruction.arguments().size())
			return null;

		ASTElement argument = instruction.arguments().get(typeArgumentIndex);
		return argument instanceof ASTIdentifier identifier && contains(identifier.range(), offset) ? identifier : null;
	}

	/**
	 * @param element
	 * 		AST element to parse an integer from.
	 * @param fallback
	 * 		Fallback value to return if the element is {@code null} or does not contain a valid integer.
	 *
	 * @return Parsed integer from the element, or the fallback value if parsing fails.
	 */
	public static int parseInt(@Nullable ASTElement element, int fallback) {
		if (element == null || element.content() == null)
			return fallback;

		try {
			if (element instanceof ASTNumber number)
				return number.asInt();
			return Integer.parseInt(element.content());
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}

	/**
	 * Get the index of the type reference argument for the given instruction name and bytecode format.
	 *
	 * @param format
	 * 		Bytecode format to check against.
	 * @param name
	 * 		Instruction name to check.
	 *
	 * @return Index of the type reference argument for the given instruction name and bytecode format,
	 * or -1 if the instruction is not a type reference or does not have a fixed type reference argument index.
	 */
	public static int typeReferenceArgumentIndex(@NotNull BytecodeFormat format, @NotNull String name) {
		return switch (format) {
			// JVM type-referencing instructions all have the type reference as their first argument.
			case JVM -> 0;

			// Dalvik type-referencing instructions have varying type reference argument indices, so check each instruction name separately.
			case DALVIK -> switch (name) {
				case "const-class", "check-cast", "new-instance", "filled-new-array", "filled-new-array/range" -> 1;
				case "instance-of", "new-array" -> 2;
				default -> -1;
			};
		};
	}

	/**
	 * @param element
	 * 		AST element to find an enclosing instruction for.
	 *
	 * @return Enclosing instruction for the given AST element, or {@code null} if no enclosing instruction exists.
	 */
	public static @Nullable ASTInstruction findEnclosingInstruction(@Nullable ASTElement element) {
		while (element != null && !(element instanceof ASTInstruction))
			element = element.parent();
		return (ASTInstruction) element;
	}

	/**
	 * @param element
	 * 		AST element to pick from.
	 * @param line
	 * 		Line number to pick an element at (1-based).
	 * @param column
	 * 		Column number to pick an element at (1-based).
	 *
	 * @return Deepest AST element at the given position within the provided element, or {@code null} if no element matches.
	 */
	public static @Nullable ASTElement pickByLineColumn(@NotNull ASTElement element, int line, int column) {
		if (!contains(element, line, column))
			return null;

		for (ASTElement child : element.children()) {
			ASTElement matched = pickByLineColumn(child, line, column);
			if (matched != null)
				return matched;
		}
		return element;
	}

	/**
	 * Check if the given AST element contains the given line and column position.
	 *
	 * @param element
	 * 		AST element to check.
	 * @param line
	 * 		Line number to check for (1-based).
	 * @param column
	 * 		Column number to check for (1-based).
	 *
	 * @return {@code true} if the given AST element contains the given line and column position, {@code false} otherwise.
	 */
	public static boolean contains(@NotNull ASTElement element, int line, int column) {
		Location location = element.location();
		if (location != null && location.line() == line) {
			int start = location.column();
			int end = Math.max(start, start + Math.max(location.length(), 1) - 1);
			if (column >= start && column <= end)
				return true;
		}

		for (ASTElement child : element.children())
			if (contains(child, line, column))
				return true;

		return false;
	}

	/**
	 * @param range
	 * 		Range to check.
	 * @param offset
	 * 		Absolute offset to check for.
	 *
	 * @return {@code true} if the given range contains the given offset, {@code false} otherwise.
	 */
	public static boolean contains(@NotNull Range range, int offset) {
		return range != Range.EMPTY && range.within(offset);
	}
}

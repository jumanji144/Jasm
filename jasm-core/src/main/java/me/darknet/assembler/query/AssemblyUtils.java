package me.darknet.assembler.query;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * General AST utilities.
 */
public final class AssemblyUtils {
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
		for (ASTInstruction instruction : method.getCode().getInstructions())
			if (instruction instanceof ASTLabel label && Objects.equals(label.identifier().content(), target))
				return label;
		return null;
	}

	/**
	 * @param name
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a flow control instruction.
	 */
	public static boolean isFlowControlInstruction(@NotNull TargetContext context, @Nullable String name) {
		return isBranchInstruction(context, name) || isSwitchInstruction(context, name);
	}

	/**
	 * @param target
	 * 		Target whose instruction metadata should be queried.
	 * @param name
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a conditional or unconditional branch.
	 */
	public static boolean isBranchInstruction(@NotNull TargetContext target, @Nullable String name) {
		return hasTrait(target, name, InstructionTrait.CONDITIONAL_BRANCH)
				|| hasTrait(target, name, InstructionTrait.UNCONDITIONAL_BRANCH);
	}

	/**
	 * @param target
	 * 		Target whose instruction metadata should be queried.
	 * @param name
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a switch instruction.
	 */
	public static boolean isSwitchInstruction(@NotNull TargetContext target, @Nullable String name) {
		return hasTrait(target, name, InstructionTrait.SWITCH);
	}

	/**
	 * @param target
	 * 		Target whose instruction metadata should be queried.
	 * @param name
	 * 		Instruction name to check.
	 *
	 * @return {@code true} if the instruction is a type reference.
	 */
	public static boolean isTypeReferenceInstruction(@NotNull TargetContext target, @Nullable String name) {
		return hasTrait(target, name, InstructionTrait.TYPE_REFERENCE);
	}

	/**
	 * @param target
	 * 		Target whose instruction metadata should be queried.
	 * @param name
	 * 		Instruction name to check.
	 * @param trait
	 * 		Trait to check for.
	 *
	 * @return {@code true} if the instruction has the given trait, {@code false} otherwise.
	 */
	public static boolean hasTrait(@NotNull TargetContext target, @Nullable String name, @NotNull InstructionTrait trait) {
		if (name == null)
			return false;
		Instruction<?> definition = target.instructions().get(name);
		return definition != null && definition.hasTrait(trait);
	}

	/**
	 * @param astElements
	 * 		AST elements to search through.
	 * @param position
	 * 		Position to check for.
	 * @param line
	 * 		Line to check for.
	 * @param target
	 * 		Target whose instruction metadata should be queried.
	 *
	 * @return Instruction at the given position and line, or {@code null} if not found.
	 */
	@Nullable
	public static ASTInstruction findInstruction(@Nullable List<ASTElement> astElements, int position, int line,
	                                             @NotNull TargetContext target) {
		if (astElements == null)
			return null;

		ASTInstruction[] selected = new ASTInstruction[1];
		for (ASTElement element : astElements) {
			// AST collections may contain null entries after recovery, so skip them safely.
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
						if (isSwitchInstruction(target, identifier) && ast.range().within(position))
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
		if (method.getCode() == null)
			return null;

		for (ASTInstruction instruction : method.getCode().getInstructions())
			if (instruction instanceof ASTLabel label && name.equals(label.identifier().literal()))
				return label;
		return null;
	}

	/**
	 * @param target
	 * 		Target whose instruction metadata should be queried.
	 * @param instruction
	 * 		Instruction to check.
	 *
	 * @return Variable access kind if the instruction carries a variable trait, or {@code null} if not.
	 */
	public static @Nullable VariableAccessKind variableAccessKind(@NotNull TargetContext target,
	                                                              @NotNull ASTInstruction instruction) {
		Instruction<?> definition = target.instructions().get(instruction.identifier().content());
		if (definition == null || definition.variableOperandIndex() < 0)
			return null;
		if (definition.hasTrait(InstructionTrait.VARIABLE_INCREMENT))
			return VariableAccessKind.INCREMENT;
		if (definition.hasTrait(InstructionTrait.VARIABLE_READ))
			return VariableAccessKind.READ;
		if (definition.hasTrait(InstructionTrait.VARIABLE_WRITE))
			return VariableAccessKind.WRITE;
		return null;
	}

	/**
	 * @param target
	 * 		Target whose instruction metadata should be queried.
	 * @param offset
	 * 		Absolute offset to check for.
	 * @param instruction
	 * 		Instruction to check.
	 *
	 * @return Type reference identifier if the instruction is a type reference
	 * and the offset is within the type reference argument, or {@code null} otherwise.
	 */
	public static @Nullable ASTIdentifier resolveInstructionTypeReference(@NotNull TargetContext target, int offset,
	                                                                      @NotNull ASTInstruction instruction) {
		Instruction<?> definition = target.instructions().get(instruction.identifier().content());
		if (definition == null || !definition.hasTrait(InstructionTrait.TYPE_REFERENCE))
			return null;

		int typeArgumentIndex = definition.typeReferenceOperandIndex();
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

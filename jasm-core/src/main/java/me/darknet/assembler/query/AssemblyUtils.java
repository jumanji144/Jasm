package me.darknet.assembler.query;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.instructions.SwitchShape;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * General AST utilities.
 */
@SuppressWarnings("unused")
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
	 * @param contexts
	 * 		Targets whose instruction metadata should be queried.
	 *
	 * @return {@code true} if the instruction is a flow control instruction.
	 */
	public static boolean isFlowControlInstruction(@Nullable String name, @NotNull TargetContext... contexts) {
		for (TargetContext context : contexts) {
			if (isFlowControlInstruction(context, name))
				return true;
		}
		return false;
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
	 * @param name
	 * 		Instruction name to check.
	 * @param targets
	 * 		Targets whose instruction metadata should be queried.
	 *
	 * @return {@code true} if the instruction is a conditional or unconditional branch.
	 */
	public static boolean isBranchInstruction(@Nullable String name, @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			if (isBranchInstruction(target, name))
				return true;
		}
		return false;
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
	 * @param name
	 * 		Instruction name to check.
	 * @param targets
	 * 		Targets whose instruction metadata should be queried.
	 *
	 * @return {@code true} if the instruction is a conditional or unconditional branch.
	 */
	public static boolean isSwitchInstruction(@Nullable String name, @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			if (isSwitchInstruction(target, name))
				return true;
		}
		return false;
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
	 * @param name
	 * 		Instruction name to check.
	 * @param targets
	 * 		Targets whose instruction metadata should be queried.
	 *
	 * @return {@code true} if the instruction is a type reference.
	 */
	public static boolean isTypeReferenceInstruction(@Nullable String name, @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			if (isTypeReferenceInstruction(target, name))
				return true;
		}
		return false;
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
	 * @param name
	 * 		Instruction name to check.
	 * @param trait
	 * 		Trait to check for.
	 * @param targets
	 * 		Targets whose instruction metadata should be queried.
	 *
	 * @return {@code true} if the instruction has the given trait, {@code false} otherwise.
	 */
	public static boolean hasTrait(@Nullable String name, @NotNull InstructionTrait trait, @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			if (hasTrait(target, name, trait))
				return true;
		}
		return false;
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
	 * @param contexts
	 * 		Targets whose instruction metadata should be queried.
	 *
	 * @return Instruction at the given position and line, or {@code null} if not found.
	 */
	public static @Nullable ASTInstruction findInstruction(@Nullable List<ASTElement> astElements, int position, int line, @NotNull TargetContext... contexts) {
		for (TargetContext context : contexts) {
			ASTInstruction instruction = findInstruction(astElements, position, line, context);
			if (instruction != null)
				return instruction;
		}
		return null;
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
	public static @Nullable ASTInstruction findInstruction(@Nullable List<ASTElement> astElements, int position, int line,
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
	 * @param instruction
	 * 		Instruction to check.
	 * @param targets
	 * 		Targets whose instruction metadata should be queried.
	 *
	 * @return Variable access kind if the instruction carries a variable trait, or {@code null} if not.
	 */
	public static @Nullable VariableAccessKind variableAccessKind(@NotNull ASTInstruction instruction, @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			VariableAccessKind kind = variableAccessKind(target, instruction);
			if (kind != null)
				return kind;
		}
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
	 * @param offset
	 * 		Absolute offset to check for.
	 * @param instruction
	 * 		Instruction to check.
	 * @param targets
	 * 		Targets whose instruction metadata should be queried.
	 *
	 * @return Type reference identifier if the instruction is a type reference
	 * and the offset is within the type reference argument, or {@code null} otherwise.
	 */
	public static @Nullable ASTIdentifier resolveInstructionTypeReference(int offset, @NotNull ASTInstruction instruction,
	                                                                      @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			ASTIdentifier identifier = resolveInstructionTypeReference(target, offset, instruction);
			if (identifier != null)
				return identifier;
		}
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
	 * Resolves the member referenced by an instruction, such as the owner, name, and descriptor of a field or method access.
	 *
	 * @param target
	 * 		Target whose instruction metadata should be queried.
	 * @param instruction
	 * 		Instruction to resolve the member of.
	 *
	 * @return Member reference, or {@code null} if the instruction does not reference a field or method, or its
	 * member path or descriptor is missing or malformed.
	 */
	public static @Nullable MemberReference resolveMemberReference(@NotNull TargetContext target,
	                                                               @NotNull ASTInstruction instruction) {
		ASTIdentifier identifier = instruction.identifier();
		if (identifier == null)
			return null;

		Instruction<?> definition = target.instructions().get(identifier.content());
		if (definition == null)
			return null;

		boolean isMethod = definition.hasTrait(InstructionTrait.METHOD_REFERENCE);
		if (!isMethod && !definition.hasTrait(InstructionTrait.FIELD_REFERENCE))
			return null;

		int memberIndex = definition.memberReferenceOperandIndex();
		List<? extends ASTElement> arguments = instruction.arguments();
		int descriptorIndex = memberIndex + 1;
		if (memberIndex < 0 || descriptorIndex >= arguments.size())
			return null;

		ASTElement memberElement = arguments.get(memberIndex);
		ASTElement descriptorElement = arguments.get(descriptorIndex);
		if (memberElement == null || descriptorElement == null)
			return null;

		String memberPath = memberElement.content();
		String descriptor = descriptorElement.content();
		if (memberPath == null || descriptor == null)
			return null;

		int split = memberPath.lastIndexOf('.');
		if (split <= 0 || split >= memberPath.length() - 1)
			return null;

		return new MemberReference(memberPath.substring(0, split), memberPath.substring(split + 1), descriptor, isMethod);
	}

	/**
	 * Resolves the labels a switch instruction can branch to, one entry per case and one for the default when the shape has one.
	 * <ul>
	 *     <li>{@link SwitchShape#TABLE}: A {@code min} base, a {@code cases} label list, and a {@code default} label.</li>
	 *     <li>{@link SwitchShape#LOOKUP}: Explicit integer keys mapped to labels, including {@code default}.</li>
	 *     <li>{@link SwitchShape#PACKED}: A {@code first} base and a {@code targets} label list, with no default.</li>
	 *     <li>{@link SwitchShape#SPARSE}: Explicit integer keys mapped to labels, with no default.</li>
	 * </ul>
	 * Instructions without a switch payload produce an empty list.
	 *
	 * @param target
	 * 		Target whose instruction metadata should be queried.
	 * @param instruction
	 * 		Instruction to resolve the switch targets of.
	 *
	 * @return Switch targets in payload order, with the default first for table switches.
	 */
	public static @NotNull List<SwitchTarget> resolveSwitchTargets(@NotNull TargetContext target,
	                                                               @NotNull ASTInstruction instruction) {
		ASTIdentifier identifier = instruction.identifier();
		if (identifier == null || instruction.arguments().isEmpty())
			return List.of();

		Instruction<?> definition = target.instructions().get(identifier.content());
		if (definition == null || definition.switchShape() == null)
			return List.of();

		ASTObject payload = instruction.argumentObject(instruction.arguments().size() - 1);
		if (payload == null)
			return List.of();

		return switch (definition.switchShape()) {
			case LOOKUP, SPARSE -> resolveLookupSwitchTargets(payload);
			case TABLE -> resolveTableSwitchTargets(payload, "min", "cases", "default");
			case PACKED -> resolveTableSwitchTargets(payload, "first", "targets", null);
			case null -> Collections.emptyList();
		};
	}

	/**
	 * Resolves the targets of a lookup switch instruction, which are explicit integer keys mapped to labels.
	 *
	 * @param payload
	 * 		Payload object of the lookup switch instruction.
	 *
	 * @return Switch targets in payload order, with the default first if present.
	 */
	private static @NotNull List<SwitchTarget> resolveLookupSwitchTargets(@NotNull ASTObject payload) {
		List<SwitchTarget> targets = new ArrayList<>();
		for (int i = 0; i < payload.values().size(); i++) {
			ASTIdentifier key = payload.values().key(i);
			ASTElement value = payload.values().get(i);

			// Skip malformed entries that are missing a key or value.
			String context = key == null ? null : key.literal();
			String labelName = value == null ? null : value.content();
			if (context == null || labelName == null)
				continue;

			targets.add(new SwitchTarget(context, labelName, key, value));
		}
		return targets;
	}

	/**
	 * Resolves the targets of a table or packed switch instruction, which are a base value and a list of labels.
	 *
	 * @param payload
	 * 		Payload object of the table or packed switch instruction.
	 * @param baseKey
	 * 		Key for the base value in the payload.
	 * @param casesKey
	 * 		Key for the label list in the payload.
	 * @param defaultKey
	 * 		Key for the default label in the payload, or {@code null} if there is no default.
	 *
	 * @return Switch targets in payload order, with the default first if present.
	 */
	private static @NotNull List<SwitchTarget> resolveTableSwitchTargets(@NotNull ASTObject payload, @NotNull String baseKey,
	                                                                     @NotNull String casesKey, @Nullable String defaultKey) {
		List<SwitchTarget> targets = new ArrayList<>();
		if (defaultKey != null) {
			ASTElement defaultValue = payload.value(defaultKey);
			String defaultLabel = defaultValue == null ? null : defaultValue.content();
			if (defaultLabel != null)
				targets.add(new SwitchTarget("default", defaultLabel, payload.values().key(defaultKey), defaultValue));
		}

		// Get lower bound for table switch, or first key for packed switch.
		// If the base is missing or malformed, default to 0.
		long base = 0;
		if (payload.value(baseKey) instanceof ASTNumber number)
			base = number.asLong();

		// If there are no cases then only the default target (if we found one) is returned.
		ASTArray cases = payload.value(casesKey);
		if (cases == null)
			return targets;

		// Collect case targets in order.
		long currentValue = base;
		for (ASTElement caseTarget : cases.values()) {
			String labelName = caseTarget.content();
			if (labelName == null || labelName.isBlank())
				continue;
			String caseContext = Long.toString(currentValue++);
			targets.add(new SwitchTarget(caseContext, labelName, caseTarget, caseTarget));
		}
		return targets;
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

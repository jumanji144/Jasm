package me.darknet.assembler.query;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTException;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.query.resolution.ClassResolution;
import me.darknet.assembler.query.resolution.EmptyResolution;
import me.darknet.assembler.query.resolution.FieldResolution;
import me.darknet.assembler.query.resolution.InstructionResolution;
import me.darknet.assembler.query.resolution.LabelDeclarationResolution;
import me.darknet.assembler.query.resolution.LabelReferenceResolution;
import me.darknet.assembler.query.resolution.MethodResolution;
import me.darknet.assembler.query.resolution.Resolution;
import me.darknet.assembler.query.resolution.TypeReferenceResolution;
import me.darknet.assembler.query.resolution.VariableDeclarationResolution;
import me.darknet.assembler.query.resolution.VariableReferenceResolution;
import me.darknet.assembler.util.ElementMapView;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Pair;
import me.darknet.assembler.util.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Query API for semantic AST lookups and usage collection.
 */
public final class AssemblyQueries {
	private static final List<String> FLOW_CONTROL_INSNS = List.of(
			"goto", "goto_w", "jsr", "jsr_w",
			"ifnull", "ifnonnull", "ifeq", "ifne", "ifle", "ifge", "iflt", "ifgt",
			"if_acmpeq", "if_acmpne", "if_icmpeq", "if_icmpge", "if_icmpgt", "if_icmple", "if_icmplt", "if_icmpne"
	);
	private static final List<String> TYPE_REFERENCE_INSNS = List.of("new", "anewarray", "checkcast", "instanceof", "multianewarray");

	private AssemblyQueries() {}

	/**
	 * Resolve the content at a given offset within the provided AST.
	 *
	 * @param ast
	 * 		AST to query.
	 * @param offset
	 * 		Offset to query at.
	 *
	 * @return Resolution of the content at the given offset, or an empty resolution if no content was found or the offset was invalid.
	 *
	 * @see #resolveAt(List, int, int) For line/column based resolution
	 */
	public static @NotNull Resolution resolveAt(@Nullable List<? extends ASTElement> ast, int offset) {
		if (ast == null || offset < 0)
			return EmptyResolution.INSTANCE;

		Resolution resolution = resolveOffset(offset, null, ast);
		return resolution == null ? EmptyResolution.INSTANCE : resolution;
	}

	/**
	 * Resolve the content at a given line/column within the provided AST.
	 *
	 * @param ast
	 * 		AST to query.
	 * @param line
	 * 		Line number to query at (1-based).
	 * @param column
	 * 		Column number to query at (1-based).
	 *
	 * @return Resolution of the content at the given line/column, or an empty resolution if no content was found or the line/column was invalid.
	 *
	 * @see #resolveAt(List, int) For offset based resolution
	 */
	public static @NotNull Resolution resolveAt(@Nullable List<? extends ASTElement> ast, int line, int column) {
		if (ast == null || line < 1 || column < 1)
			return EmptyResolution.INSTANCE;

		ASTElement matched = null;
		for (ASTElement element : ast) {
			ASTElement candidate = pickByLineColumn(element, line, column);
			if (candidate != null) {
				matched = candidate;
				break;
			}
		}
		if (matched == null || matched.range() == Range.EMPTY)
			return EmptyResolution.INSTANCE;

		return resolveAt(ast, matched.range().start());
	}

	/**
	 * Collect variable declarations and usages within the given method.
	 *
	 * @param method
	 * 		Method to query.
	 *
	 * @return Declarations and usages of variables within the given method.
	 */
	public static @NotNull VariableQueryResult variables(@NotNull ASTMethod method) {
		Map<String, List<VariableInfo>> variablesByName = new LinkedHashMap<>();
		List<VariableInfo> declarations = new ArrayList<>();
		List<VariableUsage> usages = new ArrayList<>();
		Map<String, Integer> ordinals = new LinkedHashMap<>();

		for (ASTIdentifier parameter : method.parameters()) {
			VariableInfo info = new VariableInfo(
					new VariableIdentity(parameter.literal(), nextOrdinal(ordinals, parameter.literal()), VariableDeclarationKind.PARAMETER),
					parameter,
					true
			);
			declarations.add(info);
			variablesByName.computeIfAbsent(info.identity().name(), __ -> new ArrayList<>()).add(info);
		}

		ASTCode code = method.code();
		if (code == null) {
			return new VariableQueryResult(List.copyOf(declarations), List.copyOf(usages));
		}

		for (ASTInstruction instruction : code.instructions()) {
			VariableAccessKind kind = variableKind(instruction);
			if (kind == null || instruction.arguments().isEmpty())
				continue;

			ASTElement argument = instruction.arguments().getFirst();
			if (!(argument instanceof ASTIdentifier identifier))
				continue;

			String name = identifier.literal();
			List<VariableInfo> matches = variablesByName.get(name);
			VariableIdentity resolved = null;
			boolean ambiguous = false;
			if (matches == null || matches.isEmpty()) {
				VariableInfo info = new VariableInfo(
						new VariableIdentity(name, nextOrdinal(ordinals, name), VariableDeclarationKind.INFERRED_LOCAL),
						identifier,
						false
				);
				declarations.add(info);
				variablesByName.computeIfAbsent(name, __ -> new ArrayList<>()).add(info);
				resolved = info.identity();
			} else if (matches.size() == 1) {
				resolved = matches.getFirst().identity();
			} else {
				ambiguous = true;
			}
			usages.add(new VariableUsage(resolved, name, identifier, instruction, kind, ambiguous));
		}

		return new VariableQueryResult(List.copyOf(declarations), List.copyOf(usages));
	}

	/**
	 * Collect label declarations and usages within the given method.
	 *
	 * @param method
	 * 		Method to query.
	 *
	 * @return Declarations and usages of labels within the given method.
	 */
	public static @NotNull LabelQueryResult labels(@NotNull ASTMethod method) {
		Map<String, List<ASTLabel>> declarationsByName = new LinkedHashMap<>();
		List<LabelInfo> declarations = new ArrayList<>();
		List<LabelUsage> usages = new ArrayList<>();
		ASTCode code = method.code();
		if (code != null) for (ASTInstruction instruction : code.instructions())
			if (instruction instanceof ASTLabel label)
				declarationsByName.computeIfAbsent(label.identifier().literal(), __ -> new ArrayList<>()).add(label);

		for (Map.Entry<String, List<ASTLabel>> entry : declarationsByName.entrySet()) {
			boolean duplicate = entry.getValue().size() > 1;
			for (ASTLabel label : entry.getValue())
				declarations.add(new LabelInfo(entry.getKey(), label, duplicate));
		}

		for (ASTException exception : method.exceptions()) {
			usages.add(resolveLabelUsage(declarationsByName, exception.start(), LabelReferenceKind.TRY_START, null));
			usages.add(resolveLabelUsage(declarationsByName, exception.end(), LabelReferenceKind.TRY_END, null));
			usages.add(resolveLabelUsage(declarationsByName, exception.handler(), LabelReferenceKind.HANDLER, null));
		}

		if (code != null) {
			for (ASTInstruction instruction : code.instructions()) {
				if (instruction instanceof ASTLabel) {
					continue;
				}
				String name = instruction.identifier().content();
				if (name == null || instruction.arguments().isEmpty()) {
					continue;
				}
				if (FLOW_CONTROL_INSNS.contains(name)) {
					ASTElement target = instruction.arguments().getFirst();
					if (target instanceof ASTIdentifier identifier) {
						usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.FLOW, name));
					}
				} else if ("tableswitch".equals(name)) {
					collectTableSwitchLabels(declarationsByName, instruction, usages);
				} else if ("lookupswitch".equals(name)) {
					collectLookupSwitchLabels(declarationsByName, instruction, usages);
				}
			}
		}

		return new LabelQueryResult(List.copyOf(declarations), List.copyOf(usages));
	}

	/**
	 * Recursively resolve the content at the given offset within the provided list of AST elements,
	 * starting from the top-level and drilling down into nested classes and methods as needed.
	 *
	 * @param offset
	 * 		Offset to resolve at.
	 * @param parentClass
	 * 		Parent class of the current elements being resolved, or {@code null} if resolving at the top-level.
	 * @param elements
	 * 		List of AST elements to resolve within.
	 *
	 * @return Resolution of the content at the given offset,
	 * or {@code null} if no content was found at that offset within the provided elements.
	 */
	private static @Nullable Resolution resolveOffset(int offset, @Nullable ASTClass parentClass,
	                                                  @NotNull List<? extends ASTElement> elements) {
		for (ASTElement element : elements) {
			if (!contains(element.range(), offset))
				continue;
			switch (element) {
				case ASTClass klass -> {
					TypeReferenceResolution typeResolution = resolveClassTypeReference(offset, klass);
					if (typeResolution != null)
						return typeResolution;

					Resolution nested = resolveOffset(offset, klass, klass.contents());
					return nested != null ? nested : new ClassResolution(klass);
				}
				case ASTMethod method -> {
					Resolution resolved = resolveMethod(offset, parentClass, method);
					return resolved != null ? resolved : new MethodResolution(parentClass, method);
				}
				case ASTField field -> {
					return new FieldResolution(parentClass, field);
				}
				default -> {
					// We could walk the children here, but if it is not a class/method/field then
					// it won't have any nested declarations or references, so we can just return it directly.
				}
			}
		}
		return null;
	}

	/**
	 * Resolve the content at the given offset within the provided method,
	 * including variable declarations/usages,label declarations/usages, and instruction selections.
	 *
	 * @param offset
	 * 		Offset to resolve at.
	 * @param parentClass
	 * 		Parent class of the method being resolved, or {@code null} if resolving within a method that is not nested within a class.
	 * @param method
	 * 		Method to resolve within.
	 *
	 * @return Resolution of the content at the given offset within the provided method,
	 * or {@code null} if no content was found at that offset within the method.
	 */
	private static @Nullable Resolution resolveMethod(int offset, @Nullable ASTClass parentClass, @NotNull ASTMethod method) {
		VariableQueryResult variables = variables(method);
		for (VariableInfo declaration : variables.declarations())
			if (contains(declaration.declaration().range(), offset))
				return new VariableDeclarationResolution(parentClass, method, declaration.declaration(), declaration);

		LabelQueryResult labels = labels(method);
		for (LabelInfo declaration : labels.declarations())
			if (contains(declaration.declaration().range(), offset))
				return new LabelDeclarationResolution(parentClass, method, declaration.declaration(), declaration);

		for (ASTException exception : method.exceptions()) {
			if (contains(exception.start().range(), offset))
				return new LabelReferenceResolution(parentClass, method, exception.start(),
						resolveLabelUsage(labels, exception.start(), LabelReferenceKind.TRY_START, null));

			if (contains(exception.end().range(), offset))
				return new LabelReferenceResolution(parentClass, method, exception.end(),
						resolveLabelUsage(labels, exception.end(), LabelReferenceKind.TRY_END, null));

			if (contains(exception.handler().range(), offset))
				return new LabelReferenceResolution(parentClass, method, exception.handler(),
						resolveLabelUsage(labels, exception.handler(), LabelReferenceKind.HANDLER, null));

			if (contains(exception.exceptionType().range(), offset))
				return new TypeReferenceResolution(parentClass, method, exception.exceptionType());
		}

		ASTCode code = method.code();
		if (code == null)
			return null;

		for (ASTInstruction instruction : code.instructions()) {
			if (!contains(instruction.range(), offset))
				continue;

			ASTElement selected = instruction.pick(offset);
			for (VariableUsage usage : variables.usages())
				if (usage.reference() == selected)
					return new VariableReferenceResolution(parentClass, method, usage.reference(), usage);

			for (LabelUsage usage : labels.usages())
				if (usage.reference() == selected)
					return new LabelReferenceResolution(parentClass, method, usage.reference(), usage);

			ASTIdentifier typeReference = resolveInstructionTypeReference(offset, instruction);
			if (typeReference != null)
				return new TypeReferenceResolution(parentClass, method, typeReference);

			return instruction instanceof ASTLabel label
					? new LabelDeclarationResolution(parentClass, method, label, labelInfoOf(labels, label))
					: new InstructionResolution(parentClass, method, instruction);
		}
		return null;
	}

	/**
	 * Resolve a type reference at the given offset within the provided class,
	 * including superclass, interfaces, and permitted subclasses.
	 *
	 * @param offset
	 * 		Offset to resolve at.
	 * @param klass
	 * 		Class to resolve within.
	 *
	 * @return Resolution of the type reference at the given offset within the provided class,
	 * or {@code null} if no type reference was found at that offset within the class
	 */
	private static @Nullable TypeReferenceResolution resolveClassTypeReference(int offset, @NotNull ASTClass klass) {
		ASTIdentifier superName = klass.getSuperName();
		if (superName != null && contains(superName.range(), offset))
			return new TypeReferenceResolution(klass, null, superName);

		for (ASTIdentifier identifier : klass.getInterfaces())
			if (contains(identifier.range(), offset))
				return new TypeReferenceResolution(klass, null, identifier);

		for (ASTIdentifier identifier : klass.getPermittedSubclasses())
			if (contains(identifier.range(), offset))
				return new TypeReferenceResolution(klass, null, identifier);

		return null;
	}

	/**
	 * Resolve a type reference at the given offset within the provided instruction,
	 * including instructions that reference types such as "new", "checkcast", etc.
	 *
	 * @param offset
	 * 		Offset to resolve at.
	 * @param instruction
	 * 		Instruction to resolve within.
	 *
	 * @return Resolution of the type reference at the given offset within the provided instruction,
	 * or {@code null} if no type reference was found at that offset within the instruction
	 */
	private static @Nullable ASTIdentifier resolveInstructionTypeReference(int offset, @NotNull ASTInstruction instruction) {
		String name = instruction.identifier().content();
		if (name == null || !TYPE_REFERENCE_INSNS.contains(name) || instruction.arguments().isEmpty())
			return null;

		ASTElement argument = instruction.arguments().getFirst();
		return argument instanceof ASTIdentifier identifier && contains(identifier.range(), offset) ? identifier : null;
	}

	/**
	 * Find the label info for the given label declaration, or create a new one if it is not declared within the method.
	 *
	 * @param labels
	 * 		Label query result containing the label declarations to search through.
	 * @param label
	 * 		Label declaration to find the info for.
	 *
	 * @return Label info for the given label declaration, or a new one if it is not declared within the method.
	 */
	private static @NotNull LabelInfo labelInfoOf(@NotNull LabelQueryResult labels, @NotNull ASTLabel label) {
		for (LabelInfo info : labels.declarations())
			if (info.declaration() == label)
				return info;
		return new LabelInfo(label.identifier().literal(), label, false);
	}

	/**
	 * Find the label usage for the given label reference, or create a new one if it is not declared within the method.
	 *
	 * @param labels
	 * 		Label query result containing the label declarations and usages to search through.
	 * @param reference
	 * 		Label reference to find the usage for.
	 * @param kind
	 * 		Kind of label reference to find the usage for.
	 * @param context
	 * 		Optional context for the label reference, such as the instruction name for flow control instructions
	 * 		or the case value for switch instructions. May be {@code null} if not applicable.
	 *
	 * @return Label usage for the given label reference, or a new one if it is not declared within the method.
	 */
	private static @NotNull LabelUsage resolveLabelUsage(@NotNull LabelQueryResult labels, @NotNull ASTIdentifier reference,
	                                                     @NotNull LabelReferenceKind kind, @Nullable String context) {
		for (LabelUsage usage : labels.usages())
			if (usage.reference() == reference && usage.kind() == kind && Objects.equals(usage.context(), context))
				return usage;

		return resolveLabelUsage(Collections.emptyMap(), reference, kind, context);
	}

	/**
	 * Find the label usage for the given label reference, or create a new one if it is not declared within the method.
	 *
	 * @param declarationsByName
	 * 		Map of label declarations by name to search through for matching declarations.
	 * @param reference
	 * 		Label reference to find the usage for.
	 * @param kind
	 * 		Kind of label reference to find the usage for.
	 * @param context
	 * 		Optional context for the label reference, such as the instruction name for flow control instructions
	 * 		or the case value for switch instructions. May be {@code null} if not applicable.
	 *
	 * @return Label usage for the given label reference, or a new one if it is not declared within the method.
	 */
	private static @NotNull LabelUsage resolveLabelUsage(@NotNull Map<String, List<ASTLabel>> declarationsByName,
	                                                     @NotNull ASTIdentifier reference,
	                                                     @NotNull LabelReferenceKind kind,
	                                                     @Nullable String context) {
		String name = reference.literal();
		List<ASTLabel> matches = declarationsByName.get(name);
		if (matches == null || matches.isEmpty())
			return new LabelUsage(null, name, reference, kind, context, false);

		if (matches.size() > 1)
			return new LabelUsage(null, name, reference, kind, context, true);

		return new LabelUsage(new LabelInfo(name, matches.getFirst(), false), name, reference, kind, context, false);
	}

	/**
	 * Collect label usages from a tableswitch instruction, including the default case and all switch cases.
	 *
	 * @param declarationsByName
	 * 		Map of label declarations by name to search through for matching declarations.
	 * @param instruction
	 * 		Instruction to collect label usages from.
	 * @param usages
	 * 		List to add the collected label usages to.
	 */
	private static void collectTableSwitchLabels(@NotNull Map<String, List<ASTLabel>> declarationsByName,
	                                             @NotNull ASTInstruction instruction,
	                                             @NotNull List<LabelUsage> usages) {
		ASTObject object = instruction.argumentObject(0);
		if (object == null)
			return;

		ASTElement defaultCase = object.value("default");
		if (defaultCase instanceof ASTIdentifier identifier)
			usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.SWITCH_DEFAULT, null));

		ASTArray cases = object.value("cases");
		int min = parseInt(object.value("min"), 0);
		if (cases == null)
			return;

		for (int i = 0; i < cases.values().size(); i++) {
			ASTElement value = cases.values().get(i);
			if (value instanceof ASTIdentifier identifier)
				usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.SWITCH_CASE, String.valueOf(min + i)));
		}
	}

	/**
	 * Collect label usages from a lookupswitch instruction, including the default case and all switch cases.
	 *
	 * @param declarationsByName
	 * 		Map of label declarations by name to search through for matching declarations.
	 * @param instruction
	 * 		Instruction to collect label usages from.
	 * @param usages
	 * 		List to add the collected label usages to.
	 */
	private static void collectLookupSwitchLabels(@NotNull Map<String, List<ASTLabel>> declarationsByName,
	                                              @NotNull ASTInstruction instruction,
	                                              @NotNull List<LabelUsage> usages) {
		ASTObject object = instruction.argumentObject(0);
		if (object == null)
			return;

		ASTElement defaultCase = object.value("default");
		if (defaultCase instanceof ASTIdentifier identifier)
			usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.SWITCH_DEFAULT, null));

		ElementMapView<ASTIdentifier, ASTElement> values = object.values();
		for (Pair<ASTIdentifier, ASTElement> pair : values.pairs()) {
			if (Objects.equals("default", pair.first().content()))
				continue;

			if (pair.second() instanceof ASTIdentifier identifier)
				usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.SWITCH_CASE, pair.first().content()));
		}
	}

	/**
	 * Recursively pick the most specific AST element that contains the given line and column, starting from the provided element.
	 *
	 * @param element
	 * 		Element to start picking from.
	 * @param line
	 * 		Line number to pick at (1-based).
	 * @param column
	 * 		Column number to pick at (1-based).
	 *
	 * @return The most specific AST element that contains the given line and column,
	 * or {@code null} if no such element exists within the provided element.
	 */
	private static @Nullable ASTElement pickByLineColumn(@NotNull ASTElement element, int line, int column) {
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
	 * Check if the given AST element or any of its children contain the given line and column.
	 *
	 * @param element
	 * 		Element to check.
	 * @param line
	 * 		Line number to check (1-based).
	 * @param column
	 * 		Column number to check (1-based).
	 *
	 * @return {@code true} if the given AST element or any of its children contain the given line and column, {@code false} otherwise.
	 */
	private static boolean contains(@NotNull ASTElement element, int line, int column) {
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
	 * 		Offset to check.
	 *
	 * @return {@code true} if the given range contains the given offset, {@code false} otherwise.
	 */
	private static boolean contains(@NotNull Range range, int offset) {
		return range != Range.EMPTY && range.within(offset);
	}

	/**
	 * @param ordinals
	 * 		Map of ordinals by name to update and retrieve from.
	 * @param name
	 * 		Name to get the next ordinal for.
	 *
	 * @return Next ordinal for the given name, which is the current ordinal for that name in the map,
	 * or 0 if the name is not yet in the map. The ordinal for that name in the map is then incremented by 1.
	 */
	private static int nextOrdinal(@NotNull Map<String, Integer> ordinals, @NotNull String name) {
		int ordinal = ordinals.getOrDefault(name, 0);
		ordinals.put(name, ordinal + 1);
		return ordinal;
	}

	/**
	 * @param element
	 * 		Element to parse the integer from.
	 * @param fallback
	 * 		Value to return if the element is {@code null}, has no content, or if the content cannot be parsed as an integer.
	 *
	 * @return Integer parsed from the content of the given element, or the fallback value if the element is {@code null},
	 * has no content, or if the content cannot be parsed as an integer.
	 */
	private static int parseInt(@Nullable ASTElement element, int fallback) {
		if (element == null || element.content() == null)
			return fallback;

		try {
			return Integer.parseInt(element.content());
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}

	/**
	 * Determine the kind of variable access represented by the given instruction, if any.
	 *
	 * @param instruction
	 * 		Instruction to check.
	 *
	 * @return Kind of variable access represented by the given instruction,
	 * or {@code null} if the instruction does not represent a variable access.
	 */
	private static @Nullable VariableAccessKind variableKind(@NotNull ASTInstruction instruction) {
		String name = instruction.identifier().content();
		if (name == null)
			return null;

		if ("ret".equals(name))
			return VariableAccessKind.READ;

		if ("iinc".equals(name))
			return VariableAccessKind.INCREMENT;

		if (name.endsWith("load"))
			return VariableAccessKind.READ;

		if (name.endsWith("store"))
			return VariableAccessKind.WRITE;

		return null;
	}
}

package me.darknet.assembler.query;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTAnnotated;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTException;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTInner;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.SwitchShape;
import me.darknet.assembler.query.resolution.ClassAnnotationResolution;
import me.darknet.assembler.query.resolution.ClassExtends;
import me.darknet.assembler.query.resolution.ClassImplements;
import me.darknet.assembler.query.resolution.ClassResolution;
import me.darknet.assembler.query.resolution.EmptyResolution;
import me.darknet.assembler.query.resolution.FieldAnnotationResolution;
import me.darknet.assembler.query.resolution.FieldResolution;
import me.darknet.assembler.query.resolution.IndependentAnnotationResolution;
import me.darknet.assembler.query.resolution.InnerClassResolution;
import me.darknet.assembler.query.resolution.InstructionResolution;
import me.darknet.assembler.query.resolution.LabelDeclarationResolution;
import me.darknet.assembler.query.resolution.LabelReferenceResolution;
import me.darknet.assembler.query.resolution.MethodAnnotationResolution;
import me.darknet.assembler.query.resolution.MethodResolution;
import me.darknet.assembler.query.resolution.Resolution;
import me.darknet.assembler.query.resolution.TypeReferenceResolution;
import me.darknet.assembler.query.resolution.VariableDeclarationResolution;
import me.darknet.assembler.query.resolution.VariableReferenceResolution;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.util.ElementMapView;
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

import static me.darknet.assembler.query.AssemblyUtils.*;

/**
 * Query API for semantic AST lookups and usage collection.
 */
public final class AssemblyQueries {
	private AssemblyQueries() {}

	/**
	 * Resolve the content at a given offset within the provided AST.
	 *
	 * @param ast
	 * 		AST to query.
	 * @param offset
	 * 		Offset to query at.
	 * @param targets
	 * 		Targets whose instruction semantics should be used.
	 *
	 * @return Resolution of the content at the given offset, or an empty resolution if no content was found or the offset was invalid.
	 *
	 * @see #resolveAt(List, int, int, TargetContext...) For line/column based resolution.
	 */
	public static @NotNull Resolution resolveAt(@Nullable List<? extends ASTElement> ast, int offset, @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			Resolution resolution = resolveAt(ast, offset, target);
			if (resolution != EmptyResolution.INSTANCE)
				return resolution;
		}
		return EmptyResolution.INSTANCE;
	}

	/**
	 * Resolve the content at a given offset within the provided AST.
	 *
	 * @param ast
	 * 		AST to query.
	 * @param offset
	 * 		Offset to query at.
	 * @param target
	 * 		Target whose instruction semantics should be used.
	 *
	 * @return Resolution of the content at the given offset, or an empty resolution if no content was found or the offset was invalid.
	 *
	 * @see #resolveAt(List, int, int, TargetContext) For line/column based resolution.
	 */
	public static @NotNull Resolution resolveAt(@Nullable List<? extends ASTElement> ast, int offset, @NotNull TargetContext target) {
		if (ast == null || offset < 0)
			return EmptyResolution.INSTANCE;

		Resolution resolution = resolveOffset(offset, null, ast, target);
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
	 * @param targets
	 * 		Targets whose instruction semantics should be used.
	 *
	 * @return Resolution of the content at the given line/column, or an empty resolution if no content was found or the line/column was invalid.
	 */
	public static @NotNull Resolution resolveAt(@Nullable List<? extends ASTElement> ast, int line, int column, @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			Resolution resolution = resolveAt(ast, line, column, target);
			if (resolution != EmptyResolution.INSTANCE)
				return resolution;
		}
		return EmptyResolution.INSTANCE;
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
	 * @param target
	 * 		Target whose instruction semantics should be used.
	 *
	 * @return Resolution of the content at the given line/column, or an empty resolution if no content was found or the line/column was invalid.
	 *
	 * @see #resolveAt(List, int, TargetContext) For offset based resolution
	 */
	public static @NotNull Resolution resolveAt(@Nullable List<? extends ASTElement> ast, int line, int column, @NotNull TargetContext target) {
		if (ast == null || line < 1 || column < 1)
			return EmptyResolution.INSTANCE;

		ASTElement matched = pickElementAt(ast, line, column);
		if (matched == null || matched.range() == Range.EMPTY)
			return EmptyResolution.INSTANCE;

		return resolveAt(ast, matched.range().start(), target);
	}

	/**
	 * Collect variable declarations and usages within the given method.
	 *
	 * @param method
	 * 		Method to query.
	 * @param targets
	 * 		Targets whose variable metadata should be used.
	 *
	 * @return Declarations and usages of variables within the given method.
	 */
	public static @NotNull VariableQueryResult variables(@NotNull ASTMethod method, @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			VariableQueryResult result = variables(method, target);
			if (!result.declarations().isEmpty() || !result.usages().isEmpty())
				return result;
		}
		return new VariableQueryResult(Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Collect variable declarations and usages within the given method.
	 *
	 * @param method
	 * 		Method to query.
	 * @param target
	 * 		Target whose variable metadata should be used.
	 *
	 * @return Declarations and usages of variables within the given method.
	 */
	public static @NotNull VariableQueryResult variables(@NotNull ASTMethod method, @NotNull TargetContext target) {
		Map<String, List<VariableInfo>> variablesByName = new LinkedHashMap<>();
		List<VariableInfo> declarations = new ArrayList<>();
		List<VariableUsage> usages = new ArrayList<>();
		Map<String, Integer> ordinals = new LinkedHashMap<>();

		for (ASTIdentifier parameter : method.getParameters()) {
			VariableInfo info = new VariableInfo(
					new VariableIdentity(parameter.literal(), nextOrdinal(ordinals, parameter.literal()), VariableDeclarationKind.PARAMETER),
					parameter,
					true
			);
			declarations.add(info);
			variablesByName.computeIfAbsent(info.identity().name(), __ -> new ArrayList<>()).add(info);
		}

		ASTCode code = method.getCode();
		if (code == null)
			return new VariableQueryResult(List.copyOf(declarations), List.copyOf(usages));

		for (ASTInstruction instruction : code.getInstructions()) {
			VariableAccessKind kind = variableAccessKind(target, instruction);
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
	 * @param targets
	 * 		Targets whose label metadata should be used.
	 *
	 * @return Declarations and usages of labels within the given method.
	 */
	public static @NotNull LabelQueryResult labels(@NotNull ASTMethod method, @NotNull TargetContext... targets) {
		for (TargetContext target : targets) {
			LabelQueryResult result = labels(method, target);
			if (!result.declarations().isEmpty() || !result.usages().isEmpty())
				return result;
		}
		return new LabelQueryResult(Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Collect label declarations and usages within the given method.
	 *
	 * @param method
	 * 		Method to query.
	 * @param target
	 * 		Target whose label metadata should be used.
	 *
	 * @return Declarations and usages of labels within the given method.
	 */
	public static @NotNull LabelQueryResult labels(@NotNull ASTMethod method, @NotNull TargetContext target) {
		Map<String, List<ASTLabel>> declarationsByName = new LinkedHashMap<>();
		List<LabelInfo> declarations = new ArrayList<>();
		List<LabelUsage> usages = new ArrayList<>();
		ASTCode code = method.getCode();
		if (code != null) {
			for (ASTInstruction instruction : code.getInstructions()) {
				if (instruction instanceof ASTLabel label)
					declarationsByName.computeIfAbsent(label.identifier().literal(), __ -> new ArrayList<>()).add(label);
			}
		}

		for (Map.Entry<String, List<ASTLabel>> entry : declarationsByName.entrySet()) {
			boolean duplicate = entry.getValue().size() > 1;
			for (ASTLabel label : entry.getValue())
				declarations.add(new LabelInfo(entry.getKey(), label, duplicate));
		}

		for (ASTException exception : method.getExceptionHandlers()) {
			usages.add(resolveLabelUsage(declarationsByName, exception.start(), LabelReferenceKind.TRY_START, null));
			usages.add(resolveLabelUsage(declarationsByName, exception.end(), LabelReferenceKind.TRY_END, null));
			usages.add(resolveLabelUsage(declarationsByName, exception.handler(), LabelReferenceKind.HANDLER, null));
		}

		if (code != null) {
			for (ASTInstruction instruction : code.getInstructions()) {
				if (instruction instanceof ASTLabel)
					continue;

				String name = instruction.identifier().content();
				if (name == null || instruction.arguments().isEmpty())
					continue;

				if (isBranchInstruction(target, name)) {
					ASTIdentifier identifier = flowControlTarget(instruction, target);
					if (identifier != null)
						usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.FLOW, name));
				} else if (isSwitchInstruction(target, name)) {
					collectSwitchLabels(declarationsByName, instruction, target, usages);
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
	                                                  @NotNull List<? extends ASTElement> elements,
	                                                  @NotNull TargetContext target) {
		for (ASTElement element : elements) {
			if (!contains(element.range(), offset))
				continue;

			switch (element) {
				case ASTClass klass -> {
					ASTIdentifier implemented = resolveImplementedInterface(offset, klass);
					if (implemented != null)
						return new ClassImplements(klass, implemented);

					ASTInner inner = resolveInnerClass(offset, klass);
					if (inner != null)
						return new InnerClassResolution(klass, inner);

					ASTAnnotation annotation = resolveAnnotation(offset, klass);
					if (annotation != null)
						return new ClassAnnotationResolution(klass, annotation);

					ASTIdentifier superName = klass.getSuperName();
					if (superName != null && contains(superName.range(), offset))
						return new ClassExtends(klass, superName);

					TypeReferenceResolution typeResolution = resolveAdditionalClassTypeReference(offset, klass);
					if (typeResolution != null)
						return typeResolution;

					Resolution nested = resolveOffset(offset, klass, klass.contents(), target);
					return nested != null ? nested : new ClassResolution(klass);
				}
				case ASTMethod method -> {
					Resolution resolved = resolveMethod(offset, parentClass, method, target);
					return resolved != null ? resolved : new MethodResolution(parentClass, method);
				}
				case ASTField field -> {
					ASTAnnotation annotation = resolveAnnotation(offset, field);
					if (annotation != null)
						return new FieldAnnotationResolution(parentClass, field, annotation);

					return new FieldResolution(parentClass, field);
				}
				case ASTAnnotation annotation -> {
					return new IndependentAnnotationResolution(annotation);
				}
				default -> {
					// Only classes/methods/fields have semantic nested declarations to resolve.
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
	private static @Nullable Resolution resolveMethod(int offset, @Nullable ASTClass parentClass, @NotNull ASTMethod method, @NotNull TargetContext target) {
		ASTAnnotation annotation = resolveAnnotation(offset, method);
		if (annotation != null)
			return new MethodAnnotationResolution(parentClass, method, annotation);

		VariableQueryResult variables = variables(method, target);
		for (VariableInfo declaration : variables.declarations())
			if (contains(declaration.declaration().range(), offset))
				return new VariableDeclarationResolution(parentClass, method, declaration.declaration(), declaration);

		LabelQueryResult labels = labels(method, target);
		for (LabelInfo declaration : labels.declarations())
			if (contains(declaration.declaration().range(), offset))
				return new LabelDeclarationResolution(parentClass, method, declaration.declaration(), declaration);

		for (ASTException exception : method.getExceptionHandlers()) {
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

		ASTCode code = method.getCode();
		if (code == null)
			return null;

		for (ASTInstruction instruction : code.getInstructions()) {
			if (!contains(instruction.range(), offset))
				continue;

			ASTElement selected = instruction.pick(offset);
			for (VariableUsage usage : variables.usages()) {
				if (usage.reference() == selected)
					return new VariableReferenceResolution(parentClass, method, usage.reference(), usage);
			}

			for (LabelUsage usage : labels.usages()) {
				if (usage.reference() == selected)
					return new LabelReferenceResolution(parentClass, method, usage.reference(), usage);
			}

			ASTIdentifier typeReference = resolveInstructionTypeReference(target, offset, instruction);
			if (typeReference != null)
				return new TypeReferenceResolution(parentClass, method, typeReference);

			return instruction instanceof ASTLabel label
					? new LabelDeclarationResolution(parentClass, method, label, labelInfoOf(labels, label))
					: new InstructionResolution(parentClass, method, instruction);
		}
		return null;
	}

	/**
	 * Resolve a type reference at the given offset within the provided class, including superclass, interfaces, and permitted subclasses.
	 *
	 * @param offset
	 * 		Offset to resolve at.
	 * @param klass
	 * 		Class to resolve within.
	 *
	 * @return Resolution of the type reference at the given offset within the provided class,
	 * or {@code null} if no type reference was found at that offset within the class
	 */
	private static @Nullable TypeReferenceResolution resolveAdditionalClassTypeReference(int offset, @NotNull ASTClass klass) {
		for (ASTIdentifier identifier : klass.getPermittedSubclasses()) {
			if (contains(identifier.range(), offset))
				return new TypeReferenceResolution(klass, null, identifier);
		}

		return null;
	}

	/**
	 * Resolve an implemented interface at the given offset within the provided class.
	 *
	 * @param offset
	 * 		Offset to resolve at.
	 * @param klass
	 * 		Class to resolve within.
	 *
	 * @return Implemented interface at the given offset within the provided class,
	 * or {@code null} if no implemented interface was found at that offset within the class
	 */
	private static @Nullable ASTIdentifier resolveImplementedInterface(int offset, @NotNull ASTClass klass) {
		for (ASTIdentifier identifier : klass.getInterfaces()) {
			if (contains(identifier.range(), offset))
				return identifier;
		}
		return null;
	}

	/**
	 * Resolve an inner class at the given offset within the provided class.
	 *
	 * @param offset
	 * 		Offset to resolve at.
	 * @param klass
	 * 		Class to resolve within.
	 *
	 * @return Inner class at the given offset within the provided class,
	 * or {@code null} if no inner class was found at that offset within the class
	 */
	private static @Nullable ASTInner resolveInnerClass(int offset, @NotNull ASTClass klass) {
		for (ASTInner inner : klass.getInners()) {
			if (contains(inner.range(), offset))
				return inner;
		}
		return null;
	}

	/**
	 * Resolve an annotation at the given offset within the provided annotated element.
	 *
	 * @param offset
	 * 		Offset to resolve at.
	 * @param annotated
	 * 		Annotated element to resolve within.
	 *
	 * @return Annotation at the given offset within the provided annotated element,
	 * or {@code null} if no annotation was found at that offset within the element
	 */
	private static @Nullable ASTAnnotation resolveAnnotation(int offset, @NotNull ASTAnnotated annotated) {
		ASTAnnotation annotation = resolveAnnotation(offset, annotated.getVisibleAnnotations());
		if (annotation != null)
			return annotation;

		annotation = resolveAnnotation(offset, annotated.getInvisibleAnnotations());
		if (annotation != null)
			return annotation;

		annotation = resolveAnnotation(offset, annotated.getVisibleTypeAnnotations());
		if (annotation != null)
			return annotation;

		return resolveAnnotation(offset, annotated.getInvisibleTypeAnnotations());
	}

	/**
	 * Resolve an annotation at the given offset within the provided list of annotations.
	 *
	 * @param offset
	 * 		Offset to resolve at.
	 * @param annotations
	 * 		List of annotations to resolve within.
	 *
	 * @return Annotation at the given offset within the provided list of annotations,
	 * or {@code null} if no annotation was found at that offset within the list
	 */
	private static @Nullable ASTAnnotation resolveAnnotation(int offset, @NotNull List<ASTAnnotation> annotations) {
		for (ASTAnnotation annotation : annotations) {
			if (contains(annotation.range(), offset))
				return annotation;
		}
		return null;
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
		for (LabelInfo info : labels.declarations()) {
			if (info.declaration() == label)
				return info;
		}
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
		for (LabelUsage usage : labels.usages()) {
			if (usage.reference() == reference && usage.kind() == kind && Objects.equals(usage.context(), context))
				return usage;
		}

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
	 * Find the target label for a flow control instruction, if it has one.
	 *
	 * @param instruction
	 * 		Flow control instruction to find the target for.
	 * @param target
	 * 		Target whose instruction semantics should be used.
	 *
	 * @return Target label for the given flow control instruction, or {@code null} if it does not have a target.
	 */
	private static @Nullable ASTIdentifier flowControlTarget(@NotNull ASTInstruction instruction,
	                                                         @NotNull TargetContext target) {
		Instruction<?> definition = target.instructions().get(instruction.identifier().content());
		if (definition == null)
			return null;

		int labelIndex = definition.operandIndex(OperandRole.RoleKind.LABEL);
		if (labelIndex < 0 || labelIndex >= instruction.arguments().size())
			return null;

		ASTElement targetElement = instruction.arguments().get(labelIndex);
		return targetElement instanceof ASTIdentifier identifier ? identifier : null;
	}

	/**
	 * Collect label usages for a switch instruction, if it has any.
	 *
	 * @param declarationsByName
	 * 		Map of label declarations by name to search through for matching declarations.
	 * @param instruction
	 * 		Switch instruction to collect label usages for.
	 * @param target
	 * 		Target whose instruction semantics should be used.
	 * @param usages
	 * 		List to add the collected label usages to.
	 */
	private static void collectSwitchLabels(@NotNull Map<String, List<ASTLabel>> declarationsByName,
	                                        @NotNull ASTInstruction instruction,
	                                        @NotNull TargetContext target,
	                                        @NotNull List<LabelUsage> usages) {
		Instruction<?> definition = target.instructions().get(instruction.identifier().content());
		if (definition == null)
			return;
		SwitchShape shape = definition.switchShape();
		if (shape == null)
			return;

		int payloadIndex = definition.operandIndex(OperandRole.RoleKind.SWITCH_PAYLOAD);
		if (payloadIndex < 0 || payloadIndex >= instruction.arguments().size())
			return;
		ASTElement payload = instruction.arguments().get(payloadIndex);
		if (!(payload instanceof ASTObject object))
			return;

		// The shape owns the payload vocabulary, so core never spells a target's keys itself.
		switch (shape) {
			case TABLE, PACKED ->
					collectPositionalSwitchLabels(declarationsByName, object, shape.baseKey(), shape.casesKey(), shape.defaultKey(), usages);
			case LOOKUP -> collectKeyedSwitchLabels(declarationsByName, object, shape.defaultKey(), usages);
			case SPARSE -> collectKeyedSwitchLabels(declarationsByName, object, null, usages);
		}
	}

	/**
	 * Collect label usages for a positional switch instruction, if it has any.
	 *
	 * @param declarationsByName
	 * 		Map of label declarations by name to search through for matching declarations.
	 * @param object
	 * 		Switch payload object to collect label usages from.
	 * @param baseKey
	 * 		Key for the base value of the switch cases.
	 * @param casesKey
	 * 		Key for the array of switch case labels.
	 * @param defaultKey
	 * 		Optional key for the default case label, or {@code null} if there is no default case.
	 * @param usages
	 * 		List to add the collected label usages to.
	 */
	private static void collectPositionalSwitchLabels(@NotNull Map<String, List<ASTLabel>> declarationsByName,
	                                                  @NotNull ASTObject object,
	                                                  @NotNull String baseKey,
	                                                  @NotNull String casesKey,
	                                                  @Nullable String defaultKey,
	                                                  @NotNull List<LabelUsage> usages) {
		if (defaultKey != null) {
			ASTElement defaultCase = object.value(defaultKey);
			if (defaultCase instanceof ASTIdentifier identifier)
				usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.SWITCH_DEFAULT, null));
		}

		ASTArray cases = object.value(casesKey);
		if (cases == null)
			return;

		int base = parseInt(object.value(baseKey), 0);
		for (int i = 0; i < cases.values().size(); i++) {
			ASTElement value = cases.values().get(i);
			if (value instanceof ASTIdentifier identifier)
				usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.SWITCH_CASE, String.valueOf(base + i)));
		}
	}

	/**
	 * Collect label usages for a keyed switch instruction, if it has any.
	 *
	 * @param declarationsByName
	 * 		Map of label declarations by name to search through for matching declarations.
	 * @param object
	 * 		Switch payload object to collect label usages from.
	 * @param defaultKey
	 * 		Optional key for the default case label, or {@code null} if there is no default case.
	 * @param usages
	 * 		List to add the collected label usages to.
	 */
	private static void collectKeyedSwitchLabels(@NotNull Map<String, List<ASTLabel>> declarationsByName,
	                                             @NotNull ASTObject object,
	                                             @Nullable String defaultKey,
	                                             @NotNull List<LabelUsage> usages) {
		if (defaultKey != null) {
			ASTElement defaultCase = object.value(defaultKey);
			if (defaultCase instanceof ASTIdentifier identifier)
				usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.SWITCH_DEFAULT, null));
		}

		ElementMapView<ASTIdentifier, ASTElement> values = object.values();
		for (Pair<ASTIdentifier, ASTElement> pair : values.pairs()) {
			if (defaultKey != null && Objects.equals(defaultKey, pair.first().content()))
				continue;

			if (pair.second() instanceof ASTIdentifier identifier)
				usages.add(resolveLabelUsage(declarationsByName, identifier, LabelReferenceKind.SWITCH_CASE, pair.first().content()));
		}
	}

	private static boolean contains(@NotNull Range range, int offset) {
		return range != Range.EMPTY && range.within(offset);
	}

	private static int nextOrdinal(@NotNull Map<String, Integer> ordinals, @NotNull String name) {
		int ordinal = ordinals.getOrDefault(name, 0);
		ordinals.put(name, ordinal + 1);
		return ordinal;
	}
}

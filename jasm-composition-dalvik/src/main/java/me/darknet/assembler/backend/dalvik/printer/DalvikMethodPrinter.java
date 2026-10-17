package me.darknet.assembler.backend.dalvik.printer;

import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikConstantTypeResolver;
import me.darknet.assembler.printer.AnnotationPrinter;
import me.darknet.assembler.printer.MethodPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.simulation.StraightForwardSimulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DalvikMethodPrinter implements MethodPrinter {
	private final MethodMember definition;
	private final DalvikMemberPrinter memberPrinter;
	private final boolean numericRegisters;

	/**
	 * @param definition
	 * 		The method definition to print.
	 * @param numericRegisters
	 * 		{@code true} to print every slot as {@code vN} and to omit the source names of parameters and locals.
	 */
	public DalvikMethodPrinter(MethodMember definition, boolean numericRegisters) {
		this.definition = definition;
		this.numericRegisters = numericRegisters;
		this.memberPrinter = new DalvikMemberPrinter(definition, definition, DalvikMemberPrinter.Type.METHOD);
	}

	@Override
	public @Nullable AnnotationPrinter annotation(int index) {
		return memberPrinter.printAnnotation(index);
	}

	@Override
	public @Nullable AnnotationPrinter visibleAnnotation(int index) {
		return memberPrinter.printAnnotation(index);
	}

	@Override
	public @Nullable AnnotationPrinter invisibleAnnotation(int index) {
		return memberPrinter.printAnnotation(index);
	}

	private static String getLabelName(int index) {
		StringBuilder label = new StringBuilder();

		while (index >= 0) {
			label.insert(0, (char) ('A' + index % 26));
			index = (index / 26) - 1;
		}

		return label.toString();
	}

	@Override
	public void print(PrintContext<?> ctx) {
		memberPrinter.printAttributes(ctx);
		if (definition.getSignature() != null) {
			ctx.begin().element(".signature").string(definition.getSignature()).next();
		}
		var obj = memberPrinter.printDeclaration(ctx).literal(definition.getName()).print(" ")
				.literal(definition.getType().descriptor()).print(" ").object();

		var code = definition.getCode();
		Map<Label, String> labelNames = code == null ? Map.of() : getLabelNames(code);

		boolean hasPrior = false;
		int parameterCount = definition.getType().parameterTypes().size();
		// Numeric output names no parameter, so its signature and body carry no source names.
		List<String> parameterNames = numericRegisters ? unnamed(parameterCount) : parameterNamesForPrint(code);
		boolean allParametersNamed = parameterCount > 0 && allNamed(parameterNames);
		// The body names the receiver and the parameters only when every parameter has a name. Without parameters
		// that holds trivially, so an instance method still prints its receiver as this.
		boolean bodyParametersNamed = !numericRegisters && allNamed(parameterNames);
		if (code != null) {
			obj.value("registers").print(String.valueOf(code.getRegisters()));
			hasPrior = true;
		}
		if (allParametersNamed) {
			if (hasPrior) {
				obj.next();
			}
			PrintContext.ArrayPrint parameters = obj.value("parameters").array();
			for (int i = 0; i < parameterCount; i++) {
				if (i > 0) {
					parameters.arg();
				}
				parameters.print(parameterNames.get(i));
			}
			parameters.end();
			hasPrior = true;

			List<List<Annotation>> parameterAnnotations = definition.getParameterAnnotations();
			if (parameterAnnotations.stream().anyMatch(annotations -> !annotations.isEmpty())) {
				obj.next();
				PrintContext.ObjectPrint parameterAnnotationObject = obj.value("parameter-annotations").object();
				int limit = Math.min(parameterAnnotations.size(), parameterCount);
				boolean firstEntry = true;
				for (int i = 0; i < limit; i++) {
					List<Annotation> annotations = parameterAnnotations.get(i);
					if (annotations.isEmpty()) {
						continue;
					}
					if (!firstEntry) {
						parameterAnnotationObject.next();
					}
					firstEntry = false;
					parameterAnnotationObject.literalValue(parameterNames.get(i));
					PrintContext.ArrayPrint annotationArray = parameterAnnotationObject.array();
					for (Annotation annotation : annotations) {
						new DalvikAnnotationPrinter(annotation).print(annotationArray);
					}
					annotationArray.end();
				}
				parameterAnnotationObject.end();
			}
		}

		if (code != null && !definition.getThrownTypes().isEmpty()) {
			obj.next();
			PrintContext.ArrayPrint thrownTypes = obj.value("throws").array();
			for (int i = 0; i < definition.getThrownTypes().size(); i++) {
				thrownTypes.literal(definition.getThrownTypes().get(i));
				if (i < definition.getThrownTypes().size() - 1) {
					thrownTypes.arg();
				}
			}
			thrownTypes.end();
		}

		if (definition.getDefaultValue() != null) {
			if (hasPrior) {
				obj.next();
			}
			DalvikConstantPrinter.printConstant(obj.value("default-value"), definition.getDefaultValue());
			hasPrior = true;
		}

		if (code != null && !code.tryCatch().isEmpty()) {
			if (hasPrior) obj.next();

			PrintContext.ArrayPrint exceptions = obj.value("exceptions").array();
			boolean firstHandler = true;
			for (var tryCatch : code.tryCatch()) {
				if (tryCatch.handlers().isEmpty()) {
					throw new IllegalStateException("Cannot print a try-catch entry without handlers");
				}
				for (var handler : tryCatch.handlers()) {
					if (!firstHandler) {
						exceptions.arg();
					}
					exceptions.array()
							.print(labelNames.get(tryCatch.begin())).arg()
							.print(labelNames.get(tryCatch.end())).arg()
							.print(labelNames.get(handler.handler())).arg()
							.literal(handler.exceptionType() == null ? "*" : handler.exceptionType().internalName())
							.end();
					firstHandler = false;
				}
			}
			exceptions.end();
			hasPrior = true;
		}

		if (code != null) {
			if (hasPrior) obj.next();
			var codeObj = obj.value("code").code();
			Map<Integer, String> registers = numericRegisters
					? DalvikRegisterNames.numeric(code)
					: DalvikRegisterNames.resolve(code, isInstance(), definition.getType().parameterTypes(),
					bodyParametersNamed ? parameterNames : null);
			DalvikCodePrinter printer = new DalvikCodePrinter(codeObj, registers, labelNames,
					DalvikConstantTypeResolver.resolve(definition, code));
			StraightForwardSimulation simulation = new StraightForwardSimulation();
			simulation.execute(printer, code);
			codeObj.end();
		}

		obj.end();
	}

	/**
	 * @return {@code true} when the method has a receiver, which occupies the first incoming register.
	 */
	private boolean isInstance() {
		return (definition.getAccess() & DalvikModifiers.ACC_STATIC) == 0;
	}

	/**
	 * @param code
	 * 		The code to extract label names from.
	 *
	 * @return A map of labels to their corresponding names.
	 */
	private static @NotNull Map<Label, String> getLabelNames(@NotNull Code code) {
		Map<Label, String> labelNames = new IdentityHashMap<>();
		int labelIndex = 0;
		for (var instruction : code.getInstructions()) {
			if (instruction instanceof Label label) {
				labelNames.put(label, getLabelName(labelIndex++));
			}
		}
		return labelNames;
	}

	/**
	 * @param code
	 * 		The code to extract parameter names from.
	 *
	 * @return A list of parameter names, in order, with null or empty strings for unnamed parameters.
	 */
	private @NotNull List<String> parameterNamesForPrint(@Nullable Code code) {
		int parameterCount = definition.getType().parameterTypes().size();
		if (parameterCount == 0)
			return List.of();

		List<String> memberNames = definition.getParameterNames() == null
				? List.of()
				: definition.getParameterNames();

		DebugInformation debug = code == null ? null : code.getDebugInfo();
		List<String> debugNames = debug == null || debug.parameterNames() == null
				? List.of()
				: debug.parameterNames();

		// Use member names if available, otherwise use debug names, and fallback to "pN" for unnamed parameters.
		List<String> names = new ArrayList<>(parameterCount);
		Set<Integer> fallbackAliases = new HashSet<>();
		for (int i = 0; i < parameterCount; i++) {
			String name = i < memberNames.size() ? memberNames.get(i) : null;
			if ((name == null || name.isEmpty()) && i < debugNames.size())
				name = debugNames.get(i);
			if (name == null || name.isEmpty()) {
				names.add(null);
			} else if (DalvikRegisterNames.isPrintableIdentifier(name)) {
				names.add(name);
			} else {
				names.add("p" + i);
				fallbackAliases.add(i);
			}
		}

		// Remove fallback names that conflict with other parameter names.
		for (int index : fallbackAliases) {
			String alias = names.get(index);
			for (int other = 0; other < parameterCount; other++) {
				if (other != index && alias.equals(names.get(other))) {
					names.set(index, null);
					break;
				}
			}
		}
		return names;
	}

	/**
	 * @param count
	 * 		Number of parameters.
	 *
	 * @return A list of {@code count} unnamed parameters.
	 */
	private static @NotNull List<String> unnamed(int count) {
		List<String> names = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
			names.add(null);
		return names;
	}

	/**
	 * @param names
	 * 		Parameter names, with null or empty strings for unnamed parameters.
	 *
	 * @return {@code true} when every parameter has a name.
	 */
	private static boolean allNamed(@NotNull List<String> names) {
		return names.stream().allMatch(name -> name != null && !name.isEmpty());
	}
}

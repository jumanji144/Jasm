package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.Code;
import dev.xdark.blw.code.CodeBuilder;
import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.attribute.generic.GenericLocal;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.ErrorCollector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Populates local variable table entries from analysis frames.
 */
public final class AnalysisLocalTableSynthesizer {
	private AnalysisLocalTableSynthesizer() {}

	/**
	 * Synthesizes local variable table entries from analysis frames, merging type information where possible.
	 *
	 * @param builder
	 * 		Builder to emit local variable entries to.
	 * @param results
	 * 		Analysis results containing frames to synthesize from.
	 * @param errorCollector
	 * 		Error collector to report errors to.
	 * @param checker
	 * 		Inheritance checker to use for type merging.
	 * @param code
	 * 		Containing code the local variables are defined within.
	 * @param parameters
	 * 		List of method parameters, which will be excluded from synthesis.
	 * @param begin
	 * 		Label marking the beginning of the method's code.
	 * @param end
	 * 		Label marking the end of the method's code.
	 */
	public static void synthesize(@NotNull CodeBuilder<?> builder, @NotNull AnalysisResults results,
	                              @NotNull ErrorCollector errorCollector, @NotNull InheritanceChecker checker,
	                              @NotNull Code code, @NotNull List<Local> parameters, @NotNull Label begin,
	                              @NotNull Label end) {
		int paramOffset = parameters.size();

		// Merge all local variable information from all frames, preferring the most specific type information available.
		Map<String, Local> localsMap = new HashMap<>();
		results.frames().forEach((index, frame) -> {
			for (Local local : frame.locals().toList()) {
				if (local.index() < paramOffset)
					continue;

				localsMap.merge(local.name(), local, (a, b) -> mergeLocals(results, errorCollector, checker, code, index, a, b));
			}
		});

		// Emit to builder.
		localsMap.forEach((name, local) -> builder.localVariable(new GenericLocal(
				begin,
				end,
				local.index(),
				name,
				local.safeType(),
				null
		)));
	}

	/**
	 * Merges two local variable entries, preferring the most specific type information available.
	 * If the types are incompatible, will degrade to {@link Types#OBJECT}.
	 *
	 * @param results
	 * 		Results to use for error reporting.
	 * @param errorCollector
	 * 		Error collector to report errors to.
	 * @param checker
	 * 		Inheritance checker to use for type merging.
	 * @param code
	 * 		Containing code the local variable is defined within.
	 * @param index
	 * 		Instruction index the local variable is defined at.
	 * @param a
	 * 		First local variable entry.
	 * @param b
	 * 		Second local variable entry.
	 *
	 * @return Merged local variable entry.
	 */
	private static @NotNull Local mergeLocals(@NotNull AnalysisResults results, @NotNull ErrorCollector errorCollector,
	                                          @NotNull InheritanceChecker checker, @NotNull Code code, int index,
	                                          @NotNull Local a, @NotNull Local b) {
		ClassType at = a.type();
		ClassType bt = b.type();

		if (at == null)
			return b;
		if (bt == null)
			return a;
		if (at.getClass() != bt.getClass())
			return a.adaptType(Types.OBJECT);

		try {
			return a.adaptType(Objects.requireNonNullElse(AnalysisUtils.commonType(checker, at, bt), Types.OBJECT));
		} catch (RuntimeException ex) {
			CodeElement element = code.elements().get(index);
			ASTInstruction ast = results.getCodeToAstMap().get(element);
			if (ast != null) {
				errorCollector.addError(ex.getMessage(), ast.location());
				return a.adaptType(Types.OBJECT);
			}
			throw new IllegalStateException("Failed merging synthesized locals without AST mapping", ex);
		}
	}
}

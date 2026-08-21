package me.darknet.assembler.compile.analysis.jvm;

import me.darknet.assembler.compile.analysis.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

/**
 * Used to plug in return values into the analysis engine.
 *
 * @see ValuedJvmAnalysisEngine
 */
public interface MethodValueLookup {
	/**
	 * @param instruction
	 * 		Instruction with method declaration.
	 * @param context
	 * 		Method context, for non-static invokes.
	 * @param parameters
	 * 		Parameter values for method arguments.
	 *
	 * @return Value of method return, or {@code null} if unknown.
	 */
	@Nullable
	Value accept(@NotNull MethodInsnNode instruction, @Nullable Value.ObjectValue context, @NotNull List<Value> parameters);
}

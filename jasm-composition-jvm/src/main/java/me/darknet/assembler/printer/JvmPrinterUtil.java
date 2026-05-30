package me.darknet.assembler.printer;

import me.darknet.assembler.helper.Variables;
import me.darknet.assembler.util.LabelUtil;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for printing JVM instructions in the JASM format.
 * <p>
 * Largely intended for testing and debugging purposes.
 * You can even use this to make custom IntelliJ type renderers for ASM instructions.
 */
public class JvmPrinterUtil {
	private static final Variables NO_VARIABLES = new Variables(Collections.emptyNavigableMap(), Collections.emptyList());

	/**
	 * Print a sequence of instructions to the JASM format.
	 *
	 * @param instructions
	 * 		Instructions to print.
	 *
	 * @return The printed instructions as a string.
	 */
	public static @NotNull String toString(@NotNull Iterable<@NotNull AbstractInsnNode> instructions) {
		List<AbstractInsnNode> collected = new ArrayList<>();
		for (AbstractInsnNode instruction : instructions)
			collected.add(instruction);

		Map<LabelNode, String> labelNames = collectLabelNames(collected);
		StringBuilder sb = new StringBuilder();
		for (AbstractInsnNode insn : collected) {
			String rendered = toString(insn, labelNames);
			if (!rendered.isEmpty())
				sb.append(rendered).append("\n");
		}

		return sb.toString().trim();
	}

	/**
	 * Print a single instruction to the JASM format.
	 *
	 * @param insn
	 * 		Instruction to print.
	 *
	 * @return The printed instruction as a string.
	 */
	public static @NotNull String toString(@NotNull AbstractInsnNode insn) {
		return toString(insn, false);
	}

	/**
	 * Print a single instruction to the JASM format, optionally scanning adjacent instructions for label names.
	 *
	 * @param insn
	 * 		Instruction to print.
	 * @param scanAdjacent
	 *        {@code true} to scan adjacent instructions for label names, {@code false} to only consider the given instruction.
	 *
	 * @return The printed instruction as a string.
	 */
	public static @NotNull String toString(@NotNull AbstractInsnNode insn, boolean scanAdjacent) {
		List<AbstractInsnNode> instructions;
		if (scanAdjacent) {
			instructions = new ArrayList<>();
			AbstractInsnNode current = insn;
			while (current != null) {
				instructions.add(current);
				current = current.getPrevious();
			}
			Collections.reverse(instructions);
			current = insn.getNext();
			while (current != null) {
				instructions.add(current);
				current = current.getNext();
			}
		} else {
			instructions = List.of(insn);
		}

		return toString(insn, collectLabelNames(instructions));
	}

	/**
	 * Print a single instruction to the JASM format, using the provided label names.
	 *
	 * @param insn
	 * 		Instruction to print.
	 * @param labelNames
	 * 		Mapping of label nodes to their printed names.
	 *
	 * @return The printed instruction as a string.
	 */
	public static @NotNull String toString(@NotNull AbstractInsnNode insn,
	                                       @NotNull Map<LabelNode, String> labelNames) {
		PrintContext<?> ctx = new PrintContext<>(PrintContext.NO_INDENT);

		JvmInstructionPrinter printer = new JvmInstructionPrinter(ctx.code(), List.of(), JvmPrinterUtil.NO_VARIABLES, labelNames);
		printer.execute(insn);

		return ctx.toString().substring(2).trim();
	}

	private static @NotNull Map<LabelNode, String> collectLabelNames(@NotNull Iterable<? extends AbstractInsnNode> instructions) {
		int labelIndex = 0;
		Map<LabelNode, String> labelNames = new IdentityHashMap<>();
		for (AbstractInsnNode instruction : instructions)
			for (LabelNode label : referencedLabels(instruction))
				if (!labelNames.containsKey(label))
					labelNames.put(label, LabelUtil.getLabelName(labelIndex++));
		return labelNames;
	}

	private static @NotNull List<LabelNode> referencedLabels(@NotNull AbstractInsnNode instruction) {
		return switch (instruction) {
			case LabelNode label -> List.of(label);
			case JumpInsnNode jumpInsn -> List.of(jumpInsn.label);
			case LineNumberNode lineNumber -> List.of(lineNumber.start);
			case LookupSwitchInsnNode lookupSwitchInsn -> {
				List<LabelNode> labels = new ArrayList<>(lookupSwitchInsn.labels.size() + 1);
				labels.addAll(lookupSwitchInsn.labels);
				labels.add(lookupSwitchInsn.dflt);
				yield labels;
			}
			case TableSwitchInsnNode tableSwitchInsn -> {
				List<LabelNode> labels = new ArrayList<>(tableSwitchInsn.labels.size() + 1);
				labels.addAll(tableSwitchInsn.labels);
				labels.add(tableSwitchInsn.dflt);
				yield labels;
			}
			default -> Collections.emptyList();
		};
	}
}

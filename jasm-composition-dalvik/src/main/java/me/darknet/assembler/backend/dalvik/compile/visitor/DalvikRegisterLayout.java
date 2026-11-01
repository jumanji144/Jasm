package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.backend.dalvik.instructions.ArithmeticLowering;
import me.darknet.assembler.backend.dalvik.instructions.ArrayData;
import me.darknet.assembler.backend.dalvik.instructions.DalvikLowering;
import me.darknet.assembler.backend.dalvik.instructions.IntegerLiteral;
import me.darknet.assembler.backend.dalvik.instructions.PackedSwitchPayload;
import me.darknet.assembler.backend.dalvik.instructions.RegisterList;
import me.darknet.assembler.backend.dalvik.instructions.RegisterOperands;
import me.darknet.assembler.backend.dalvik.instructions.RegisterRef;
import me.darknet.assembler.backend.dalvik.instructions.SparseSwitchPayload;
import me.darknet.assembler.backend.dalvik.visitor.ASTDalvikInstructionVisitor;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.error.Severity;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.SemanticInstruction;
import me.darknet.assembler.processing.ProcessedCodeEntry;
import me.darknet.assembler.processing.ProcessedInstruction;
import me.darknet.assembler.processing.ProcessedMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates the fixed local area followed by a method's incoming Dalvik parameter words.
 */
final class DalvikRegisterLayout {
	private static final int MAX_REGISTER_COUNT = 65535;

	private DalvikRegisterLayout() {}

	/**
	 * Calculates a method's register layout from its explicit slots, parameter aliases, and named local uses.
	 *
	 * @param method
	 * 		Processed method whose instructions determine named local allocation order.
	 * @param incomingRegisterWords
	 * 		Incoming parameter words, including the instance receiver and both words of wide parameters.
	 * @param explicitRegisters
	 * 		Numeric slots reserved by explicit {@code vN} references.
	 * @param parameterAliases
	 * 		Names and {@code pN} aliases already bound to incoming parameter slots.
	 * @param sink
	 * 		Sink receiving a diagnostic if the method exceeds the DEX register limit.
	 *
	 * @return The calculated layout, or {@code null} after reporting a register limit error.
	 */
	static @Nullable Layout calculate(@NotNull ProcessedMethod method, int incomingRegisterWords,
	                                  @NotNull Set<Integer> explicitRegisters,
	                                  @NotNull Set<String> parameterAliases,
	                                  @NotNull DiagnosticSink sink) {
		// Determine the minimum base for the incoming parameter words, which is one more than the maximum explicit register.
		int maximumExplicitRegister = -1;
		for (int register : explicitRegisters)
			if (register >= 0)
				maximumExplicitRegister = Math.max(maximumExplicitRegister, register);
		long minimumBase = (long) maximumExplicitRegister + 1;
		long maximumBase = (long) MAX_REGISTER_COUNT - incomingRegisterWords;
		if (minimumBase > maximumBase) {
			reportRegisterLimit(method, sink);
			return null;
		}

		// If the minimum base fits, then we can use it.
		int initialBase = (int) minimumBase;
		if (fits(method, initialBase, incomingRegisterWords, explicitRegisters, parameterAliases))
			return new Layout(initialBase, initialBase + incomingRegisterWords);

		// Otherwise, we need to search for a base that fits.
		long lastFailure = initialBase;
		long step = 1;
		long upper = -1;
		while (lastFailure < maximumBase) {
			int candidateBase = (int) Math.min(maximumBase, lastFailure + step);
			if (fits(method, candidateBase, incomingRegisterWords, explicitRegisters, parameterAliases)) {
				upper = candidateBase;
				break;
			}
			lastFailure = candidateBase;
			step *= 2;
		}
		if (upper < 0) {
			// Never found a base that fits, so the method exceeds the register limit.
			reportRegisterLimit(method, sink);
			return null;
		}
		long lower = lastFailure + 1;
		while (lower < upper) {
			int candidateBase = (int) (lower + (upper - lower) / 2);
			if (fits(method, candidateBase, incomingRegisterWords, explicitRegisters, parameterAliases))
				upper = candidateBase;
			else
				lower = candidateBase + 1;
		}
		return new Layout((int) lower, (int) (lower + incomingRegisterWords));
	}

	/**
	 * @param method
	 * 		Method to test.
	 * @param parameterBase
	 * 		Base index of the incoming parameter words.
	 * @param incomingRegisterWords
	 * 		Total number of incoming parameter words, including the instance receiver and both words of wide parameters.
	 * @param explicitRegisters
	 * 		Numeric slots reserved by explicit {@code vN} references.
	 * @param parameterAliases
	 * 		Names and {@code pN} aliases already bound to incoming parameter slots.
	 *
	 * @return {@code true} when the method fits within the DEX register limit with the given base for its incoming parameter words.
	 */
	private static boolean fits(@NotNull ProcessedMethod method, int parameterBase, int incomingRegisterWords,
	                            @NotNull Set<Integer> explicitRegisters,
	                            @NotNull Set<String> parameterAliases) {
		Candidate candidate = new Candidate(parameterBase, incomingRegisterWords, explicitRegisters, parameterAliases);
		for (ProcessedCodeEntry entry : method.code()) {
			if (entry instanceof ProcessedInstruction instruction)
				instruction.definition().transform(instruction, candidate);
		}
		return candidate.fits();
	}

	/**
	 * Reports a register limit error for the given method.
	 *
	 * @param method
	 * 		Method that exceeds the DEX register limit.
	 * @param sink
	 * 		Sink to receive the diagnostic.
	 */
	private static void reportRegisterLimit(@NotNull ProcessedMethod method, @NotNull DiagnosticSink sink) {
		sink.add(new Diagnostic(
				Severity.ERROR,
				DiagnosticPhase.BACKEND_EMISSION,
				DiagnosticCode.TARGET_LIMIT,
				"Method requires more than 65535 Dalvik registers",
				method.source().location(),
				List.of()
		));
	}

	/**
	 * Layout of a method's Dalvik registers.
	 *
	 * @param parameterBase
	 * 		Base index of the incoming parameter words.
	 * @param registerCount
	 * 		Total register count, including the local area and incoming parameter words.
	 */
	protected record Layout(int parameterBase, int registerCount) {}

	/**
	 * Tracks the local area and incoming parameter words of a method's Dalvik register layout.
	 */
	private static final class Candidate implements ASTDalvikInstructionVisitor {
		private final int parameterBase;
		private final int incomingRegisterWords;
		private final Set<String> parameterAliases;
		private final Set<Integer> usedRegisters;
		private final Map<String, Integer> localRegisters = new HashMap<>();
		private final Set<Integer> wideBases = new HashSet<>();
		private int nextLocalRegister;
		private boolean fits = true;

		private Candidate(int parameterBase, int incomingRegisterWords,
		                  Set<Integer> explicitRegisters, Set<String> parameterAliases) {
			this.parameterBase = parameterBase;
			this.incomingRegisterWords = incomingRegisterWords;
			this.parameterAliases = parameterAliases;
			this.usedRegisters = new HashSet<>(explicitRegisters);
		}

		private boolean fits() {
			return fits;
		}

		private void register(RegisterRef register) {
			if (!fits || register.index() != null || parameterAliases.contains(register.name()))
				return;

			Integer existing = localRegisters.get(register.name());
			if (existing != null) {
				if (register.width() == OperandRole.WidthPolicy.WIDE)
					claimWideHighWord(existing);
				return;
			}

			boolean wide = register.width() == OperandRole.WidthPolicy.WIDE;
			if (wide) {
				int index = DalvikRegisterAllocation.firstFreeWide(usedRegisters, nextLocalRegister, parameterBase);
				if ((long) index + 1 >= parameterBase) {
					fits = false;
					return;
				}
				localRegisters.put(register.name(), index);
				usedRegisters.add(index);
				usedRegisters.add(index + 1);
				wideBases.add(index);
				nextLocalRegister = index + 1;
			} else {
				int index = DalvikRegisterAllocation.firstFree(usedRegisters, nextLocalRegister, parameterBase);
				if (index >= parameterBase) {
					fits = false;
					return;
				}
				localRegisters.put(register.name(), index);
				usedRegisters.add(index);
				nextLocalRegister = index + 1;
			}
		}

		private void claimWideHighWord(int index) {
			if (wideBases.contains(index))
				return;

			int high = index + 1;
			if (high >= parameterBase) {
				// With incoming words, the first slot at the base is occupied by a parameter. Preserve the
				// existing visitor's REGISTER_LIMIT diagnostic instead of growing the method to hide the conflict.
				if (incomingRegisterWords == 0)
					fits = false;
				return;
			}
			if (usedRegisters.contains(high))
				return;

			wideBases.add(index);
			usedRegisters.add(high);
		}

		private void registers(RegisterOperands registers) {
			for (RegisterRef register : registers.registers())
				register(register);
		}

		@Override
		public void visitInstruction(@NotNull SemanticInstruction instruction) {}

		@Override
		public void visitLabel(@NotNull ASTIdentifier label) {}

		@Override
		public void visitLineNumber(@NotNull ASTNumber line) {}

		@Override
		public void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end,
		                           @NotNull ASTIdentifier handler, @NotNull ASTIdentifier type) {}

		@Override
		public void visitEnd() {}

		@Override
		public void visitNop() {}

		@Override
		public void visitMove(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from) {
			register(to);
			register(from);
		}

		@Override
		public void visitMoveResult(@NotNull DalvikLowering lowering, RegisterRef to) {
			register(to);
		}

		@Override
		public void visitMoveException(RegisterRef to) {
			register(to);
		}

		@Override
		public void visitReturn(@NotNull DalvikLowering lowering, RegisterRef returnValue) {
			register(returnValue);
		}

		@Override
		public void visitReturnVoid() {}

		@Override
		public void visitConst(@NotNull DalvikLowering lowering, RegisterRef to, ASTElement value) {
			register(to);
		}

		@Override
		public void visitMonitorEnter(RegisterRef register) {
			register(register);
		}

		@Override
		public void visitMonitorExit(RegisterRef register) {
			register(register);
		}

		@Override
		public void visitCheckCast(RegisterRef register, @NotNull ASTIdentifier type) {
			register(register);
		}

		@Override
		public void visitInstanceOf(RegisterRef result, RegisterRef check, @NotNull ASTIdentifier type) {
			register(result);
			register(check);
		}

		@Override
		public void visitArrayLength(RegisterRef result, RegisterRef array) {
			register(result);
			register(array);
		}

		@Override
		public void visitNewInstance(RegisterRef result, @NotNull ASTIdentifier type) {
			register(result);
		}

		@Override
		public void visitNewArray(RegisterRef result, RegisterRef size, @NotNull ASTIdentifier type) {
			register(result);
			register(size);
		}

		@Override
		public void visitFilledNewArray(@NotNull RegisterList args, @NotNull ASTIdentifier type) {
			registers(args);
		}

		@Override
		public void visitFillArrayData(RegisterRef to, @NotNull ArrayData data) {
			register(to);
		}

		@Override
		public void visitThrow(RegisterRef exception) {
			register(exception);
		}

		@Override
		public void visitGoto(@NotNull ASTIdentifier label) {}

		@Override
		public void visitPackedSwitch(RegisterRef register, @NotNull PackedSwitchPayload payload) {
			register(register);
		}

		@Override
		public void visitSparseSwitch(RegisterRef register, @NotNull SparseSwitchPayload payload) {
			register(register);
		}

		@Override
		public void visitCmp(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from1, RegisterRef from2) {
			register(to);
			register(from1);
			register(from2);
		}

		@Override
		public void visitIf(@NotNull DalvikLowering lowering, RegisterRef a, RegisterRef b,
		                    @NotNull ASTIdentifier label) {
			register(a);
			register(b);
		}

		@Override
		public void visitIfZero(@NotNull DalvikLowering lowering, RegisterRef a, @NotNull ASTIdentifier label) {
			register(a);
		}

		@Override
		public void visitBinaryOperation(@NotNull ArithmeticLowering lowering, RegisterRef to,
		                                 RegisterRef from1, RegisterRef from2) {
			register(to);
			register(from1);
			register(from2);
		}

		@Override
		public void visitBinaryLiteralOperation(@NotNull ArithmeticLowering lowering, RegisterRef to,
		                                        RegisterRef from, @NotNull IntegerLiteral literal) {
			register(to);
			register(from);
		}

		@Override
		public void visitArrayOperation(@NotNull DalvikLowering lowering, RegisterRef array,
		                                RegisterRef index, RegisterRef value) {
			register(array);
			register(index);
			register(value);
		}

		@Override
		public void visitVirtualFieldOperation(@NotNull DalvikLowering lowering, RegisterRef value,
		                                       RegisterRef instance, @NotNull MemberPath path,
		                                       @NotNull ASTIdentifier descriptor) {
			register(value);
			register(instance);
		}

		@Override
		public void visitStaticFieldOperation(@NotNull DalvikLowering lowering, RegisterRef value,
		                                      @NotNull MemberPath path, @NotNull ASTIdentifier descriptor) {
			register(value);
		}

		@Override
		public void visitInvoke(@NotNull DalvikLowering lowering, @NotNull RegisterOperands registers,
		                        @NotNull MemberPath method, @NotNull ASTIdentifier descriptor) {
			registers(registers);
		}

		@Override
		public void visitInvokePolymorphic(@NotNull RegisterOperands registers, @NotNull MemberPath method,
		                                   @NotNull ASTIdentifier descriptor, @NotNull ASTIdentifier proto) {
			registers(registers);
		}

		@Override
		public void visitInvokeCustom(@NotNull RegisterOperands registers, @NotNull ASTIdentifier name,
		                              @NotNull ASTIdentifier type, @NotNull ASTElement handle,
		                              @NotNull ASTArray arguments) {
			registers(registers);
		}

		@Override
		public void visitUnaryOperation(@NotNull DalvikLowering lowering, RegisterRef to, RegisterRef from) {
			register(to);
			register(from);
		}
	}
}
